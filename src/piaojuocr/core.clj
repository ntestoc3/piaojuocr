(ns piaojuocr.core
  (:require [piaojuocr.util :as util]
            [piaojuocr.gui :refer [make-frame show-frame]])
  (:gen-class))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (util/extract-resource! "config.edn")
  (-> (make-frame)
      show-frame))
