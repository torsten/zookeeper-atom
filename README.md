

# Zookeeper Atom [![Build Status](https://travis-ci.org/torsten/zookeeper-atom.svg)](https://travis-ci.org/torsten/zookeeper-atom)

Looks and behaves like a [Clojure atom](http://clojure.org/atoms) but works distributed across many machines by storing it's data as EDN in a [Zookeeper](http://zookeeper.apache.org/) node.

## Features

 - Simple API: `atom` (to create), `deref`/`@`, `swap`, and `reset`.  Just like Clojure's API.
 - Reads don't block, they just return a cached value. All updates from Zookeeper get applied in the background.
 - Values are encoded as EDN.


## Releases and Dependency Information

zookeeper-atom is released via [Clojars](https://clojars.org/zookeeper-atom). The Latest stable release is 0.1.0

[Leiningen](https://github.com/technomancy/leiningen) dependency information:

```clojure
[zookeeper-atom "0.1.0"]
```

Maven dependency information:

```xml
<dependency>
  <groupId>zookeeper-atom</groupId>
  <artifactId>zookeeper-atom</artifactId>
  <version>0.1.0</version>
</dependency>
```


## Usage

This example requires that you have Zookeeper installed and running:

```clojure
(require '[zookeeper-atom :as zk])

(def client (zk/connect "127.0.0.1"))
(def a-atom (zk/atom client "/some/znode/path"))

@a-atom
;; => nil

(zk/swap a-atom assoc :foo "bar")
;; => {:foo "bar"}

(zk/reset a-atom {})
;; => {}
```

Alternatively to the code above, a zookeeper-atom can be initialized:

```clojure
(def b-atom (zk/atom client "/some/path" [1 2 3]))

@b-atom
;; => [1 2 3]
```

However, if there already exists a node at the given path, it's value will not be overwritten:

```clojure
(def c-atom (zk/atom client "/some/path" "WAT?!"))

@c-atom
;; => [1 2 3]
```


## Contributing

Reporting bugs and pull requests are very welcome!

Here is a little snippet to get you started in your REPL in case you want to extend zookeeper-atom:

```clojure
(use 'clj-logging-config.log4j 'midje.repl)
(set-loggers! :root {:level :warn} "zookeeper-atom" {:level :debug})
(autotest)
; hack!
```


## License

Copyright © 2014 Torsten Becker.

Distributed under the [Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html), the same as Clojure.
