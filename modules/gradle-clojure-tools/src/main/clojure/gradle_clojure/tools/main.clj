(ns gradle-clojure.tools.main
  (:import [java.nio.channel Channels SocketChannel]
           [java.io ObjectOutputStream]))

(defn send-throwable [port t]
  (try
    (with-open [channel (SocketChannel/open)
                stream (ObjectOutputStream. (Channels/newOutputStream channel))]
      (.connect channel (InetSocketAddress. "localhost" port))
      (.writeObject stream t))
    (catch Throwable t2
      (.addSuppressed t t2)
      (throw t)))

(defn -main [& args]
  (let [[port namespace function args] (edn/read)
        ns-sym (symbol namespace)
        fn-sym (symbol namespace function)]
    (try
      (require ns-sym)
      (apply (find-var fn-sym) args)
      (catch Throwable t
        (send-throwable port t)))))
