(defproject twitter-kafka-producer "0.1.0-SNAPSHOT"
  :description "A Kafka producer that links the Twitter dev testing stream to Kafka"
  :url "http://github.com/echeran/clj-kafka-storm"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories [["twitter4j" "http://twitter4j.org/maven2"]]
  :dependencies [[org.clojure/clojure "1.5.1"]

                 ;; clj-kafka is versioned as:
                 ;; <clj-kafka-version>.<supported-kafka-version>
                 ;; So clj-kafka v 0.1.2-0.8 supports kafka 0.8, and v
                 ;; 0.0.7-0.7 supports kafka 0.7
                 ;; As the github project page says, api for 0.8 and
                 ;; api for 0.7 are incompatible
                 [clj-kafka "0.0.7-0.7"]

                 [org.twitter4j/twitter4j-core "3.0.6"]
                 [org.twitter4j/twitter4j-stream "3.0.6"]]
  :java-source-paths ["src/jvm"]
  :aot :all
  :main twitter-kafka-producer.core)
