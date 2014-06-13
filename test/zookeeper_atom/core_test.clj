(ns zookeeper-atom.core-test
  (require [midje.sweet :refer :all]
           [zookeeper-atom.core :as zk]
           [zookeeper :as zoo]))

; (use 'clj-logging-config.log4j)
; (set-loggers! :root {:level :warn} "zookeeper-atom.core" {:level :debug})

(defn- rand-str
  [len]
  (let [letters (range (int \A) (inc (int \Z)))]
    (apply str (repeatedly len #(-> letters rand-nth char)))))

(defn- rand-path
  [tag]
  (str "/" *ns* "/" tag "-" (rand-str 50)))

(facts "about race conditions"
  (fact "(heuristic) has correct value after 100 swaps"
    (let [client (zk/connect "127.0.0.1")
          path (rand-path "swap-100")
          a (zk/atom client path)
          b (zk/atom client path)
          c (zk/atom client path)
          d (zk/atom client path)
          e (zk/atom client path)
          inc (fnil inc 0)
          limit 500
          swapper #(doseq [_ (range limit)]
                    (Thread/sleep %1)
                    (zk/swap %2 update-in [%3] inc))]
      (zk/reset a {})
      (Thread/sleep 500)
      (let [futures [(future (swapper 5 a :a))
                     (future (swapper 4 b :b))
                     (future (swapper 3 c :c))
                     (future (swapper 2 c :d))
                     (future (swapper 1 c :e))]]
        (doseq [fu futures] (deref fu)))
      (Thread/sleep 500)
      @a => {:a limit, :b limit, :c limit, :d limit, :e limit}
      @b => {:a limit, :b limit, :c limit, :d limit, :e limit}
      @c => {:a limit, :b limit, :c limit, :d limit, :e limit}
      @d => {:a limit, :b limit, :c limit, :d limit, :e limit}
      @e => {:a limit, :b limit, :c limit, :d limit, :e limit}))
  (fact "(heuristic) existing value can be read immediately"
    (let [client (zk/connect "127.0.0.1")
          path (rand-path "read-immediately")
          _ (zk/atom client path "Are we there, yet?")]
      (doseq [_ (range 100)]
        @(zk/atom client path) => "Are we there, yet?"))))

(facts "about atom function"
  (fact "sets an initial value"
    (let [client (zk/connect "127.0.0.1")
          path (rand-path "initial")
          a (zk/atom client path "initial")]
      (Thread/sleep 50)
      @a => "initial"))
  (fact "initial value does not overwrite an existing value"
    (let [client (zk/connect "127.0.0.1")
          path (rand-path "overwrite")
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
