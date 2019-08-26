(ns piaojuocr.setting
  (:require [seesaw.core :as gui]
            [seesaw.border :as border]
            [seesaw.mig :refer [mig-panel]]
            [seesaw.font :refer [font default-font]]
            [piaojuocr.util :as util]
            [piaojuocr.config :as config]
            [piaojuocr.theme :refer [laf-selector]]
            [taoensso.timbre :as log]))

;;;; 设置面板
(defn text-config-panel
  "创建文本配置项"
  ([id config-path] (text-config-panel id config-path identity nil))
  ([id config-path options] (text-config-panel id config-path identity options))
  ([id config-path text-trans-fn options]
   (apply gui/text
          :id id
          :text (str (config/get-in-config config-path))
          :listen [:document (fn [e]
                               (->> (gui/text e)
                                    text-trans-fn
                                    (config/add-in-config! config-path)))]
          options)))


(defn make-view
  [f]
  (mig-panel
   :border (border/empty-border :left 10 :top 10)
   :items [
           ["主题选择:"]

           [(laf-selector)
            "wrap, grow"]

           [(gui/separator)
            "span,  grow"]

           [(gui/label :font (font :from (default-font "Label.font") :style :bold)
                       :text "百度API设置")
            "wrap, gaptop 10"]

           ["app-id:"]

           [(text-config-panel :app-id-text [:app-id])
            "wrap, grow"]

           ["api key:"]

           [(text-config-panel :api-key-text [:api-key])
            "wrap, grow"]

           ["api secret key:"]

           [(text-config-panel :api-secret-text [:api-secret-key])
            "wrap, grow"]

           [(gui/separator)
            "span,  grow"]

           ["日志级别:"
            "gaptop 10"]

           [(gui/combobox :id :log-level-combo
                          :model util/log-levels
                          :selected-item (config/get-config :log-level :info)
                          :listen [:selection (fn [e]
                                                (let [level (gui/selection e)]
                                                  (log/info "change log level to:" level)
                                                  (log/set-level! level)
                                                  (config/add-config! :log-level level)))])
            "wrap, gaptop 10, grow"]

           [(gui/separator)
            "spanx, grow"]

           ]))
