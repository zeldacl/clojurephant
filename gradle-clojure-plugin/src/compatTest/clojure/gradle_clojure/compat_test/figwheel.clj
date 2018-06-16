(ns gradle-clojure.compat-test.figwheel
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [gradle-clojure.compat-test.test-kit :as gradle]
            [ike.cljj.file :as file]
            [nrepl.core :as repl]
            [etaoin.api :as etaoin])
  (:import [org.gradle.testkit.runner TaskOutcome]
           [gradle_clojure.compat_test LineProcessingWriter]
           [java.time LocalDate]))

(defn parse-port [port]
  (fn [line]
    (if-let [match (re-find #"nREPL server started on port\s+(\d+)" line)]
      (deliver port (Integer/parseInt (nth match 1))))))

(defn start-repl [port project & args]
  (gradle/with-project project
    (let [runner (gradle/runner (concat ["clojureRepl"] args))
          writer (LineProcessingWriter. *out* (parse-port port))]
      (.forwardStdOutput runner writer)
      (.forwardStdError runner writer)
      (try
        (.build runner)
        (finally
          (deliver port :build-failed))))))

(defn send-repl [client msg]
  (let [response (first (repl/message client msg))]
    (or (:value response) response)))

(defn eval-repl [client form]
  (send-repl client {:op "eval" :code (pr-str form)}))

(defmacro with-client [[client project & args] & body]
  `(let [port-promise# (promise)
         build-thread# (Thread. #(start-repl port-promise# ~project ~@args))]
     (.start build-thread#)
     (let [port# (deref port-promise# 30000 :timeout)]
       (if (int? port#)
         (with-open [conn# (repl/connect :port port#)]
           (let [~client (repl/client conn# 1000)]
             (try
               ~@body
               (finally
                 (repl/message ~client {:op "interrupt"})
                 (repl/message ~client {:op "eval" :code (pr-str :cljs/quit)})
                 (repl/message ~client {:op "eval" :code (pr-str '(do (require 'gradle-clojure.tools.clojure-nrepl)  (gradle-clojure.tools.clojure-nrepl/stop!)))})))))
         (throw (ex-info "Could not determine port REPL started on." {}))))))

(def ^:dynamic *driver*)

(defn driver-fixture [f]
  (etaoin/with-chrome {} driver
    (binding [*driver* driver]
      (f))))

(use-fixtures :each driver-fixture)

(deftest figwheel-starts
  (testing "Figwheel can start and communicate with the browser"
    (with-client [client "BasicClojureScriptProjectTest"]
      (is (nil? (eval-repl client '(do (require '[gradle-clojure.tools.figwheel :as fw]) (fw/start "dev")))))
      (etaoin/wait 10)
      (etaoin/go *driver* "http://localhost:9500")
      (etaoin/wait 3)
      (eval-repl client '(js/alert "Hello from driver!"))
      (etaoin/wait-has-alert *driver*)
      (is (= "Hello from driver!" (etaoin/get-alert-text *driver*)))
      (etaoin/dismiss-alert *driver*))))
