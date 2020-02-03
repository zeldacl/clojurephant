(ns dev.clojurephant.tools.clojure-compiler
  (:require [dev.clojurephant.tools.logger :refer [log]]
            [clojure.edn :as edn]))

(def ^:dynamic *namespaces*)

(defn -main [& args]
  (log :debug "Classpath: %s" (System/getProperty "java.class.path"))
  (let [[destination-dir namespaces options] (edn/read)]
    (try
      (binding [*namespaces* (seq namespaces)
                *compile-path* destination-dir
                *warn-on-reflection* true
                *compiler-options* options]
        (doseq [namespace namespaces]
          (log :info "Compiling %s" namespace)
          (compile (symbol namespace))))
      (catch Throwable e
        (loop [ex e]
          (if-let [msg (and ex (.getMessage ex))]
            (log :error msg)
            (recur (.getCause ex))))
        (throw e)))))
