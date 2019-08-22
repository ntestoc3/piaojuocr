(ns piaojuocr.logging
  (:require [seesaw.core :as gui]
            [seesaw.border :as border]
            [seesaw.widgets.log-window :as log-window]
            [seesaw.mig :refer [mig-panel]]
            [seesaw.font :refer [font default-font]]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.core :as appenders]
            [piaojuocr.util :as util]
            [piaojuocr.config :as config]))

(defn window-appender
  "swing 窗口日志记录"
  [log-win]
  {:enabled? true
   :async? true
   :min-level nil
   :rate-limit nil
   :output-fn :inherit
   :fn (fn [data]
         (let [{:keys [output_]} data
               formatted-output-str (-> (force output_)
                                        (str "\n"))]
           (log-window/log log-win formatted-output-str)
           ))})

(defn make-view
  ([frame] (make-view frame :log-window))
  ([frame id]
   (let [win (log-window/log-window :auto-scroll? true)]
     (util/log-add-appender! {:win (window-appender win)})
     (gui/scrollable win))))
