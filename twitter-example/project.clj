(defproject twitter-example "0.1.0-SNAPSHOT"
  :description "A Storm topology that pulls from a Kafka 0.7 server using KafkaSpout"
  :url "http://github.com/echeran/clj-kafka-storm"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"] 
                 [storm "0.8.2"] 
                 [storm/storm-kafka "0.8.0-wip4"] 
                 [org.clojure/data.priority-map "0.0.4"]]
  :java-source-paths ["src/jvm"]
  :aot :all
  :main twitter-example.core)
