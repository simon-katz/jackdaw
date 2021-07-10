(ns library-candidates.nomis-jackdaw-utils
  (:require
   [clojurewerkz.propertied.properties :as p])
  (:import
   java.util.Properties))

;;;; ___________________________________________________________________________
;;;; ---- ->props ----

(defn ->props [x]
  (if (instance? Properties x)
    x
    (p/map->properties x)))
