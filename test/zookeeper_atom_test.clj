(ns zookeeper-atom-test
  (require [midje.sweet :refer :all]
           [zookeeper-atom :as zk]
           [zookeeper :as zoo]))

; (use 'clj-logging-config.log4j)
; (set-loggers! :root {:level :warn} "zookeeper-atom" {:level :debug})

(defn- rand-str
  [len]
  (let [letters (range (int \A) (inc (int \Z)))]
    (apply str (repeatedly len #(-> letters rand-nth char)))))

(facts "about race conditions"
  (fact "(heuristic) has correct value after 100 swaps"
    (let [client (zk/connect "127.0.0.1")
          path (str "/" *ns* "/swap-100")
          a (zk/atom client path)
          b (zk/atom client path)
          c (zk/atom client path)
          inc (fnil inc 0)
          limit 100]
      (zk/reset a {})
      (Thread/sleep 500)
      (future (doseq [_ (range limit)] (Thread/sleep 9) (zk/swap a update-in [:a] inc)))
      (future (doseq [_ (range limit)] (Thread/sleep 8) (zk/swap b update-in [:b] inc)))
      (future (doseq [_ (range limit)] (Thread/sleep 7) (zk/swap c update-in [:c] inc)))
      (Thread/sleep 4000)
      @a => {:a limit, :b limit, :c limit}
      @b => {:a limit, :b limit, :c limit}
      @c => {:a limit, :b limit, :c limit})))

(facts "about atom function"
  (fact "sets an initial value"
    (let [client (zk/connect "127.0.0.1")
          path (str "/" *ns* "/" (rand-str 20))
          a (zk/atom client path "initial")]
      (Thread/sleep 50)
      @a => "initial"))
  (fact "initial value does not overwrite an existing value"
    (let [client (zk/connect "127.0.0.1")
          path (str "/" *ns* "/" (rand-str 20))
          first (zk/atom client path "init")]
      (fact "'init' value is readable"
        (Thread/sleep 200)
        @first => "init")
      (let [second (zk/atom client path "second init")]
        (fact "second atom does not overwrite 'init' value"
          (Thread/sleep 200)
          @second => "init"
          @first => "init")
        (fact "reset updates both atoms' values"
          (zk/reset second "reset")
          (Thread/sleep 50)
          @first => "reset"
          @second => "reset")))))
