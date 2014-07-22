(defproject zookeeper-atom "1.0.0"
  :description "Provides a reference type that looks and behaves like a Clojure atom but uses Zookeeper to store and synchronize distributed access to it's value."
  :url "https://github.com/torsten/zookeeper-atom"
  :min-lein-version "2.3.0"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [zookeeper-clj "0.9.1"]
                 [org.clojure/tools.logging "0.2.6"]]
  :main zookeeper-atom.core
  :aliases {"test" ["midje"]
            "all" ["with-profile" "dev,1.6:dev,1.5"]}
  :profiles {:1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :dev {:dependencies [[midje "1.6.3"]
                                  [clj-logging-config "1.9.10"]]
                   :plugins [[lein-midje "3.0.0"]]}})
