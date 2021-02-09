(ns piaojuocr.core
  (:require [piaojuocr.util :as util]
            [piaojuocr.gui :refer [make-frame show-frame]]
            [taoensso.timbre :as log]
            [piaojuocr.config :as config])
  (:gen-class))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (log/set-level! (config/get-config :log-level :info))
  (util/log-to-file!)
  (-> (make-frame)
      show-frame))
