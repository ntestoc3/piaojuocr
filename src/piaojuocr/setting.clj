(ns piaojuocr.setting
  (:require [seesaw.core :as gui]
            [seesaw.border :as border]
            [seesaw.mig :refer [mig-panel]]
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
   :model    (keys all-themes)
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
  ([label-text id config-path] (text-config-panel label-text id config-path identity))
  ([label-text id config-path text-trans-fn]
   (gui/horizontal-panel
    :items [(gui/label label-text)
            (gui/text :id id
                      :text (str (config/get-in-config config-path))
                      :listen [:document (fn [e]
                                           (->> (gui/text e)
                                                text-trans-fn
                                                (config/add-in-config! config-path)))])])))


(defn make-view
  [f]
  (mig-panel
   :border (border/empty-border :left 10 :top 10)
   :items [
           [(gui/label "主题")
            "wrap, gaptop 20"]

           [(laf-selector)
            "wrap, grow"]

           [(gui/separator)
            "span,  grow"]

           [(gui/label "百度ocr API设置")
            "wrap, gaptop 20"]

           [(text-config-panel "app-id:" :app-id-text [:app-id])
            "wrap, grow"]

           [(text-config-panel "api key:" :api-key-text [:api-key])
            "wrap, grow"]

           [(text-config-panel "api secret key:" :api-secret-text [:api-secret-key])
            "wrap, grow"]

           [(gui/separator)
            "span,  grow"]

           [(gui/label "日志级别设置")
            "wrap, gaptop 20"]

           [(gui/combobox :id :log-level-combo
                          :model util/log-levels
                          :selected-item (config/get-config :log-level :info)
                          :listen [:selection (fn [e]
                                                (let [level (gui/selection e)]
                                                  (util/log-config! level)
                                                  (config/add-config! :log-level level)))])
            "wrap,  grow"]
           ]))
