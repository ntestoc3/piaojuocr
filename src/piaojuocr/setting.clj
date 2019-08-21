(ns piaojuocr.setting
  (:require [seesaw.core :as gui]
            [seesaw.border :as border]
            [seesaw.mig :refer [mig-panel]]
            [seesaw.font :refer [font default-font]]
            [piaojuocr.util :as util]
            [piaojuocr.config :as config])
  (:import org.pushingpixels.substance.api.SubstanceCortex$GlobalScope
           [javax.swing JFrame UIManager]
           ))


(defn get-substance-laf []
  (->> (SubstanceCortex$GlobalScope/getAllSkins)
       (map (fn [[k v]] [k (.getClassName v)]))
       (into {})))

(def all-themes (get-substance-laf))

(defn set-laf [laf-info]
  (gui/invoke-later
   (SubstanceCortex$GlobalScope/setSkin laf-info)))

(defn init-ui []
  (gui/native!)
  (JFrame/setDefaultLookAndFeelDecorated true)
  (-> (config/get-config :theme)
      all-themes
      set-laf))

(defn laf-selector []
  (gui/combobox
   :model    (-> (keys all-themes)
                 sort)
   :selected-item (config/get-config :theme "Moderate")
   :listen   [:selection (fn [e]
                           (let [theme (gui/selection e)]
                             (println "selected" theme)
                             (config/add-config! :theme theme)
                             (-> theme
                                 all-themes
                                 set-laf)))]))

;;;; 设置面板
(defn text-config-panel
  "创建文本配置项"
  ([id config-path] (text-config-panel id config-path identity))
  ([id config-path text-trans-fn]
   (gui/text :id id
             :text (str (config/get-in-config config-path))
             :listen [:document (fn [e]
                                  (->> (gui/text e)
                                       text-trans-fn
                                       (config/add-in-config! config-path)))])))


(defn make-view
  [f]
  (mig-panel
   :border (border/empty-border :left 10 :top 10)
   :constraints ["fill, ins 0"]
   :items [
           ["主题选择:"]

           [(laf-selector)
            "wrap, gaptop 20, grow"]

           [(gui/separator)
            "span,  grow"]

           [(gui/label :font (font :from (default-font "Label.font") :style :bold)
                       :text "百度API设置")
            "wrap, gaptop 20"]

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

           ["日志级别:"]

           [(gui/combobox :id :log-level-combo
                          :model util/log-levels
                          :selected-item (config/get-config :log-level :info)
                          :listen [:selection (fn [e]
                                                (let [level (gui/selection e)]
                                                  (util/log-config! level)
                                                  (config/add-config! :log-level level)))])
            "wrap, gaptop 20, grow"]

           [(gui/separator)
            "spanx,  grow"]

           ]))
