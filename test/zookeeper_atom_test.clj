(ns zookeeper-atom-test
  (require [midje.sweet :refer :all]
           [zookeeper-atom :as zk]
           [zookeeper :as zoo]))

(facts "about race conditions"
  (fact "(heuristic) has correct value after 100 swaps"
    (let [cl (zk/connect "127.0.0.1")
          a (zk/atom cl "/atom")
          b (zk/atom cl "/atom")
          c (zk/atom cl "/atom")
          inc (fnil inc 0)]
      (zk/reset a {})
      (Thread/sleep 100)
      (future (doseq [i (range 100)] (Thread/sleep 9) (zk/swap a update-in [:a] inc)))
      (future (doseq [i (range 100)] (Thread/sleep 8) (zk/swap b update-in [:b] inc)))
      (future (doseq [i (range 100)] (Thread/sleep 7) (zk/swap c update-in [:c] inc)))
      (Thread/sleep 2500)
      @a => {:a 100, :b 100, :c 100}
      @b => {:a 100, :b 100, :c 100}
      @c => {:a 100, :b 100, :c 100})))
