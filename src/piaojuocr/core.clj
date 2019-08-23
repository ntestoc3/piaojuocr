(ns piaojuocr.core
  (:require [piaojuocr.util :as util]
            [piaojuocr.gui :refer [make-frame show-frame]])
  (:gen-class))






(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (-> (make-frame)
      show-frame))
