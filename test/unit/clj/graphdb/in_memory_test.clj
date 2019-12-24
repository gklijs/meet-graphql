(ns graphdb.in-memory-test
  (:require [clojure.test :refer :all]
            [graphdb.in-memory]
            [graphdb.test-api :refer [initial-state run-api-tests]]
            [com.stuartsierra.component :as component])
  (:import graphdb.in_memory.InMemoryGraphDB))

(run-api-tests (component/start (InMemoryGraphDB. initial-state)))