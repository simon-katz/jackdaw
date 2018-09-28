(defproject fundingcircle/jackdaw-client "0.3.22-SNAPSHOT"
  :description "Clojure wrapper for Apache Kafka Producer and Consumer APIs"

  :plugins [[fundingcircle/lein-modules "[0.3.0,0.4.0)"]]

  :dependencies [[fundingcircle/jackdaw-serdes "_"]
                 [org.clojure/tools.logging "_"]
                 [org.apache.kafka/kafka-clients "_"]
                 [org.apache.kafka/kafka_2.11 "_"]
                 [org.apache.kafka/kafka-streams "_"]]
  :profiles {:kafka {:dependencies [[fundingcircle/jackdaw-admin "_"]]}
             :test [:kafka]})
