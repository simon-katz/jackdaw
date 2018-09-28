(ns jackdaw.client-extras-test
 (:require [clojure.data.json :as json]
           [clojure.test :refer :all]
           [jackdaw.admin.topic :as topic]
           [jackdaw.admin.zk :as zk]
           [jackdaw.client :as jc]
           [jackdaw.client.extras :as jce]
           [jackdaw.serdes.avro :as avro])
 (:import java.util.UUID
          org.apache.avro.Schema$Parser
          org.apache.kafka.common.serialization.Serdes$StringSerde
          io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient))

(def zk-connect "localhost:2181")

(def producer-config {"bootstrap.servers" "localhost:9092"})

(defn arbitrary-string
  []
  (str (UUID/randomUUID)))

(defn test-topic
  [test-key topic-name]
  (let [test-schema (json/write-str
                     {:name "client_extras_test_schema"
                      :type :record
                      :fields [{:name "testkey"
                                :type :string}
                               {:name "testother"
                                :type :string}]})
        topic-config {:avro/schema test-schema
                      :schema.registry/url "http://localhost:8081"
                      :schema.registry/client (MockSchemaRegistryClient.)}
        serde-config (avro/serde-config :value
                                        topic-config)]
    {:jackdaw.topic/topic-name topic-name
     :jackdaw.topic/record-key :partition-key
     :jackdaw.serdes/key-serde (Serdes$StringSerde.)
     :jackdaw.serdes/value-serde (avro/avro-serde serde-config)
     :jackdaw.topic/partition-key test-key
     :jackdaw.topic/partitions 15}))

(defn random-message
  [topic-key]
  {topic-key (arbitrary-string)
   :testother (arbitrary-string)})

(deftest ^:integration record-map-test
  (testing "record-map predicts the correct partition"
    (let [topic-key :testkey
          topic-name  (str "jackdaw-client-extras-test-" (arbitrary-string))
          topic (test-topic topic-key topic-name)
          messages (repeatedly 10 #(random-message topic-key))
          zk (zk/zk-utils zk-connect)]
      (topic/create! zk topic-name (:jackdaw.topic/partitions topic) 1 {})
      (try
       (with-open [p (jc/producer producer-config topic)]
         (doseq [m messages
                 :let [metadata (->> (jc/producer-record topic (topic-key m) m)
                                     (jc/send! p)
                                     deref
                                     jc/record-metadata)
                       expected-envelope (jc/producer-record
                                          topic
                                          (:partition metadata)
                                          (topic-key m)
                                          m)]]
           (is (= expected-envelope (jce/record-map topic m)))))
       (finally (topic/delete! zk topic-name))))))

