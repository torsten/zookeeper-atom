;; Copyright (c) Torsten Becker, 2014. All rights reserved.  The use
;; and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://www.eclipse.org/legal/epl-v10.html)
;; which can be found in the file epl.html at the root of this
;; distribution.  By using this software in any fashion, you are
;; agreeing to be bound by the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns zookeeper-atom
  (:refer-clojure :rename {atom clj-atom})
  (require [zookeeper :as zoo]
           [zookeeper.data :as zoo.data]
           [clojure.tools.logging :refer [debug]])
  (import org.apache.zookeeper.KeeperException))

(defn- encode
  "Encode Clojure a data structure into bytes."
  [data]
  (when data
    (zoo.data/to-bytes (pr-str data))))

(defn- decode
  "Decode bytes back into a Clojure data structure."
  [bytes]
  (when bytes
    (read-string (zoo.data/to-string bytes))))

(defn- all-prefixes
  "Generate all prefixes for a given path, for example:
   (all-prefixes /123/asd/bbbb) => [/123, /123/asd, /123/asd/bbbb]"
  [^java.lang.String path]
  (letfn [(find-prefixes [path]
            (reduce
              (fn [[accu last-prefix] i]
                (let [prefix (str last-prefix "/" i)]
                  [(conj accu prefix) prefix]))
              [[] ""]
              path))]
    (when (.startsWith path "/")
      (-> path
        (.substring 1)
        (.split "/")
        find-prefixes
        first))))

(defrecord Atom [cache client path]
  clojure.lang.IDeref
    (deref [this] 
      (-> this :cache deref :data)))

; Somehow we have to overwrite this or Clojure fails with
;   IllegalArgumentException Multiple methods in multimethod 'print-method' ...
(defmethod print-method Atom [this ^java.io.Writer w]
  (.write w (str "#<" (-> this .getClass .getName) " path:" (:path this) ">")))

(def connect
  "Connect to zookeeper, accepts same arguments as zookeeper/connect.
   Returns a new zookeeper client."
  zoo/connect)

(defn- set-value
  "Set new data for zookeeper path by storing the encoded `value`,
   Optionally calls `retry` if the remote version does not match `version`."
  [client path value version retry]
  (let [bytes (encode value)]
    (try
      (do
        (debug path version "=>" value)
        (let [response (zoo/set-data client path bytes version)]
          (debug "ZK said" response)
          value))
      (catch KeeperException ex
        (if (= (.code ex) org.apache.zookeeper.KeeperException$Code/BADVERSION)
          (do
            (debug "caught bad-version")
            (when retry
              (retry)))
          (throw ex))))))

(defn- init
  "Only sets a value if the znode is newly created (aka version=0).
   Returns the zookeeper-atom."
  [^Atom atom value]
  (let [{:keys [client path]} atom
        version 0]
    (set-value client path value version nil))
    atom)

(defn swap
  "Swap a zookeeper-atom's value with the same semantics as Clojure's swap!.
   Returns the zookeeper-atom's value after swapping."
  [^Atom atom f & args]
  (let [{:keys [client path cache]} atom
        {:keys [data version]} @cache
        swap-args (concat [data] args)
        swapped (apply f swap-args)]
    (set-value client path swapped version #(do
      (debug "will retry")
      (apply swap (concat [atom f] args))))))

(defn reset
  "Reset a zookeeper-atom's value without regard for the value."
  [^Atom atom value]
  (swap atom (fn [_] value)))

(defn- znode-data-watcher
  "Returns a function that continuously watches a zookeeper-atom's value
   for remote changes and updates the :cache field with these changes."
  [^Atom atom]
  (fn [{:keys [event-type keeper-state path]}]
    (debug "Change" event-type keeper-state path)
    (assert (or (nil? path) (-> (:path atom) (= path))))
    (let [{:keys [client path cache]} atom
          z-data (zoo/data client path :watcher (znode-data-watcher atom))
          data (-> z-data :data decode)
          version (-> z-data :stat :version)]
      (debug "ZK watch" version "=>" data)
      (swap! cache assoc :data data :version version)
      nil)))

(defn atom
  "Create a new zookeeper-atom. Optionally sets an `initial-value`.
   The `initial-value` will only be set if the znode at `path` is
   being newly created with this call.
   Existing values will not be overwritten by `initial-value`."
  ([client ^String path]
    (doseq [node (all-prefixes path)]
      (zoo/create client node :persistent? true))
    (let [cache (clj-atom {})
          this (Atom. cache client path)]
      ((znode-data-watcher this) nil)
      this))
  ([client ^String path initial-value]
    (-> (atom client path) (init initial-value))))
