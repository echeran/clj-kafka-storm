(ns twitter-kafka-producer.core
  (:import [twitter4j FilterQuery StatusListener TwitterStreamFactory]
           [twitter4j.conf ConfigurationBuilder])
  (:use [clj-kafka producer])
  (:gen-class))

;; test kafka v 0.7 instance configuration:
;; - zookeeper
;;  - client port = 2181
;;  - running zookeeper command = bin/zookeeper-server-start.sh
;; config/zookeeper.properties
;; - kafka
;;  - local mode servers
;;   - default: brokerid=0, port=9092, log.dir=/tmp/kfk0.7-logs-0
;;   - brokerid=1 , port=9093, log.dir=/tmp/kfk0.7-logs-1
;;   - brokerid=2 , port=9094 , log.dir=/tmp/kfk0.7-logs-2
;;  - note: local mode server (aka broker) .properties config file must specific zookeeper host as IP address, so localhost = 127.0.0.1
;;  - running the servers
;;   - JMX_PORT=2002 bin/kafka-server-start.sh config/server.properties
;;   - JMX_PORT=2003 bin/kafka-server-start.sh config/server-1.properties
;;   - JMX_PORT=2004 bin/kafka-server-start.sh config/server-2.properties
;;  - topic = twitter-to-storm
;;   - topics get created automatically when referenced for first time


;; (def zk-host "localhost")
(def zk-host "127.0.0.1")
(def zk-port 2181) ;; default port for Kafka's built-in zookeeper


(def DEFAULT-ZK-LEAD-BROKER-PORT 9092)
(def DEFAULT-ZK-TOPIC "twitter-to-storm")


(defn -main
  ([]
     (-main DEFAULT-ZK-LEAD-BROKER-PORT DEFAULT-ZK-TOPIC))
  ([zk-broker-port zk-topic]
     (let [
           ;; instantiate Kafka producer
           p (producer {"metadata.broker.list" (str zk-host ":" zk-broker-port)
                        "zk.connect" (str zk-host ":" zk-port)
                        "serializer.class" "kafka.serializer.DefaultEncoder"
                        "partitioner.class" "kafka.producer.DefaultPartitioner"})
           ;; configure (but don't yet run) object representing Twitter stream
           track-terms (into-array ["AAPL" "Mac" "iPhone" "iStore" "Apple"])
           filter (doto (FilterQuery.)
                    (.count 0)
                    (.track track-terms))
           listener (proxy [StatusListener]
                        []
                      (onStatus [status]
                        (let [tweet-text (.getText status)]
                          (println "TWEET received: text = " tweet-text)
                          (send-messages p zk-topic (message (.getBytes tweet-text)))))
                      (onStallWarning [arg0]
                        (println "Stall warning received!"))
                      (onException [e]
                        (.println *err* "Exception in Twitter4J Tweet Stream at:")
                        (throw e)))
           config (-> (doto (ConfigurationBuilder.)
                        ;; NOTE: set your Twitter Dev OAuth settings here:
                        (.setOAuthConsumerKey "")
                        (.setOAuthConsumerSecret "")
                        (.setOAuthAccessToken "")
                        (.setOAuthAccessTokenSecret "")
                        (.setJSONStoreEnabled true) ;; required for JSON
                        ;; serialization
                        )
                      .build)]

       ;; instantiate the Twitter tweet stream (and thereby start the
       ;; flow of tweets)
       (doto (.getInstance (TwitterStreamFactory. config))
         (.addListener listener)
         (.filter filter)))))
