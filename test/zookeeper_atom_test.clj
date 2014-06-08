(ns zookeeper-atom-test
  (require [midje.sweet :refer :all]
           [zookeeper-atom :as zk]
           [zookeeper :as zoo]))

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
