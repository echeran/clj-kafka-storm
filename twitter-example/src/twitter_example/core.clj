(ns twitter-example.core
  (:import [backtype.storm StormSubmitter LocalCluster]
           java.util.concurrent.LinkedBlockingQueue
           [storm.kafka KafkaConfig KafkaConfig$ZkHosts KafkaSpout SpoutConfig StringScheme])
  (:use [backtype.storm clojure config]
        [clojure.tools.logging]) 
  (:require [clojure.string :as string]
            [clojure.data.priority-map :as pri-map])
  (:gen-class))

;;
;; constants
;;


(def ^{:private true
       :doc "How many of the most frequently occurring words we emit at the end of this Storm topology"}
  TOP-K-NUM 5)

(def ^{:private true
       :doc "Number of new lines before and after each top-k word tuple emission / printing in the console"}
  SPACER-NLS 3)

;;
;; configs for Kafka and Storm's KafkaSpout
;; 

(def zk-host "localhost")
(def zk-port 2181) ;; default port for Kafka's built-in zookeeper
;; note: using port 2000 as default port for Storm's built-in zookeeper


(def ^{:private true
       :doc "the host string for Zookeeper"}
  zk-hosts ;; "localhost:2181"
  (str zk-host ":" zk-port))

(def ^{:private true
       :doc "Zookeeper's broker path"}
  zk-broker-path "/brokers")

;; need to use ZkHosts instead of StaticHosts to configure hosts --
;; see https://groups.google.com/forum/#!topic/storm-user/RniihgIQxCI
;; (def kafka-hosts (KafkaConfig$StaticHosts/fromHostString host 1))
(def kafka-zk-hosts (new KafkaConfig$ZkHosts zk-hosts zk-broker-path))

(def ^{:private true
       :doc "Topic Name"}
  topic "twitter-to-storm")
 
(def ^{:private true
       :doc "root path of Zookeeper for the spout to store the consumer offsets"}
  zkRoot "/kafkastorm"
  )
 
(def ^{:private true
       :doc "id for this consumer for storing the consumer offsets in zookeeper"}
  id "discovery")
 
(def ^{:private true
       :doc "kafka spout config definition"}
  spout-config (let [cfg (SpoutConfig. kafka-zk-hosts topic zkRoot id)]
                 (set! (. cfg scheme) (StringScheme.))
                 ;; to test whether spout
                 ;; works. -1 = start with latest offset, -2 = start
                 ;; with earliest offset, according to
                 ;; http://stackoverflow.com/questions/17807292/kafkaspout-is-not-receiving-anything-from-kafka
                 (.forceStartOffsetTime cfg -2) 
                 cfg))

(def kafka-spout (KafkaSpout. spout-config))

;;
;; Storm - bolts and topology
;;


(defbolt split-sentence ["word" "count"] [tuple collector]
  (let [words (.split (.getString tuple 0) " ")
        normalize-field (fn [s]
                                (str
                                 (when s
                                   (if (re-find #"^http" s)
                                     "http"
                                     s))))]
    (doseq [w words]
      (emit-bolt! collector [(normalize-field w) 1] :anchor tuple))
    (ack! collector tuple)))
 

(defbolt simple-chunk-agg-word-counts ["counts-map"] {:prepare true}
  [conf context collector]
  (let [chunk-counter (ref 0)
        chunk-counts-map (ref {})
        COUNTER-RESET-VAL 100]
    (bolt
     (execute
      [tuple]
      (let [word (.getStringByField tuple "word")
            word-count (.getValueByField tuple "count")]
        (dosync
         (alter chunk-counter inc)
         (alter chunk-counts-map update-in [word] (fnil (partial + word-count) 0)))
        (when (= COUNTER-RESET-VAL @chunk-counter)
          (emit-bolt! collector [@chunk-counts-map]) 
          (println "reached the reset val of" COUNTER-RESET-VAL "tweets. now resetting...")
          (dosync
           (ref-set chunk-counter 0)
           (ref-set chunk-counts-map {}))))))))

(defbolt top-five-counts-bolt ["first" "second" "third" "fourth" "fifth"] {:prepare true}
  [conf context collector]
  (let [p (atom (pri-map/priority-map))
        normalize-field (fn [s]
                          (str
                           (when s
                             (if (re-find #"^http" s)
                               "http"
                               s))))
        merge-into-pri-map (fn [pm x] (merge-with + pm {x 1}))]
    (bolt
     (execute
      [tuple]
      (let [in-field-names ["word"] 
            str-from-field #(.getStringByField tuple %) 
            in-field-vals (map str-from-field in-field-names)
            in-fields (map normalize-field in-field-vals)
            ]
        (swap! p #(reduce merge-into-pri-map % in-fields))
        (let [pri-map @p
              pri-map-get-str #(str (first %) ": " (second %))
              filler-vals (repeat TOP-K-NUM ["" 0])
              buffered-top-five-kvs (concat (->> pri-map
                                                 rseq
                                                 (take TOP-K-NUM))
                                            filler-vals)
              ret-seq (->> buffered-top-five-kvs
                           (map pri-map-get-str)
                           (take TOP-K-NUM)
                           (into []))]
          (emit-bolt! collector ret-seq :anchor tuple)))))))

(defbolt simple-top-five-counts-bolt ["first" "second" "third" "fourth" "fifth"] {:prepare true}
  [conf context collector]
  (let [p (atom (pri-map/priority-map))]
    (bolt
     (execute
      [tuple]
      (let [counts-map (.getValueByField tuple "counts-map")]
        (swap! p into counts-map) ;; priority-map does not support merge
        (let [pri-map @p
              pri-map-get-str #(str (first %) ": " (second %))
              filler-vals (repeat TOP-K-NUM ["" 0])
              buffered-top-five-kvs (concat (->> pri-map
                                                 rseq
                                                 (take TOP-K-NUM))
                                            filler-vals)
              ret-seq (->> buffered-top-five-kvs
                           (map pri-map-get-str)
                           (take TOP-K-NUM)
                           (into []))]
          (dotimes [i SPACER-NLS]
            (println ""))
          (emit-bolt! collector ret-seq :anchor tuple)
          (dotimes [i SPACER-NLS]
            (println ""))))))))

(defn mk-topology []  
  (topology
   {"twitter-spout" (spout-spec kafka-spout :p 1)}
   {"split-sentence" (bolt-spec {"twitter-spout" :shuffle} split-sentence :p 10)
    "simple-chunk-agg-word-counts" (bolt-spec {"split-sentence" ["word"]} simple-chunk-agg-word-counts :p 10)
    "simple-top-five-counts-bolt" (bolt-spec {"simple-chunk-agg-word-counts" :global} simple-top-five-counts-bolt :p 1)})) 

;; copied from storm-starter clj example

(defn run-local! []
  (let [cluster (LocalCluster.)]
    (.submitTopology
     cluster
     "twitter-stream-topology"
     {TOPOLOGY-DEBUG true} 
     (mk-topology))
     (try (while true (do))
         (catch InterruptedException e
           (.shutdown cluster)))))

(defn submit-topology! [name]
  (StormSubmitter/submitTopology
   name 
   {TOPOLOGY-DEBUG true
    TOPOLOGY-WORKERS 20}
   (mk-topology)))

;;
;; main
;;

(defn -main
  ([]
   (run-local!))
  ([name]
   (submit-topology! name)))
