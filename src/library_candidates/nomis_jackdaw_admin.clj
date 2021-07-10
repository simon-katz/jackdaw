(ns library-candidates.nomis-jackdaw-admin
  (:require
   [clojure.stacktrace :as stacktrace]
   [jackdaw.admin :as ja]
   [library-candidates.nomis-jackdaw-utils :as nju]
   [safely.core :as safely])
  (:import
   org.apache.kafka.common.errors.TopicExistsException))

;;;; ___________________________________________________________________________
;;;; ---- ->AdminClient ----

(defn ->AdminClient [kafka-config]
  ;; Avoid bug in `ja/->AdminClient`, which has a precondition for a key of
  ;; "bootstrap.servers", so doesn't allow a keyword.
  (ja/->AdminClient (nju/->props kafka-config)))

;;;; ___________________________________________________________________________
;;;; ---- ensure-topics-exist ----

(def ensure-topics-exist-max-retries 5)

(defn ensure-topics-exist
  "Create any of the topics that don't already exist, and wait for them
  to be created."
  [kafka-config topic-metadata-s]
  (with-open [client (->AdminClient kafka-config)]
    (let [create-topic
          (fn [topic-metadata]
            (try (ja/create-topics! client [topic-metadata])
                 (catch Exception e
                   (when (not= TopicExistsException
                               (type (stacktrace/root-cause e)))
                     (throw e)))))
          ;;
          wait-for-topic-to-exist
          (fn [topic-metadata]
            (safely/safely
                (ja/topic-exists? client topic-metadata)
              :on-error
              :failed? #(not %)
              :max-retries ensure-topics-exist-max-retries))]
      (run! create-topic topic-metadata-s)
      (run! wait-for-topic-to-exist topic-metadata-s))))
