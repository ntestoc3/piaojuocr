(ns piaojuocr.setting
  (:require [seesaw.core :as gui]
            [seesaw.border :as border]
            [seesaw.mig :refer [mig-panel]]
            [piaojuocr.util :as util]
            [piaojuocr.config :as config])
  (:import org.pushingpixels.substance.api.SubstanceLookAndFeel
           org.pushingpixels.substance.api.skin.SubstanceGraphiteLookAndFeel
           com.bulenkov.darcula.DarculaLaf
           [javax.swing JFrame UIManager]
           ))

(defn get-installed-laf []
  (->> (UIManager/getInstalledLookAndFeels)
       (map (fn [laf] [(.getName laf)
                       {:type :system
                        :class (.getClassName laf)}]))
       (into {})))

(defn get-substance-laf []
  (->> (SubstanceLookAndFeel/getAllSkins)
       (map (fn [[k v]] [k
                         {:type :substance
                          :class (.getClassName v)}]))
       (into {})))

(defn get-darcula-laf []
  {"Darcula" {:type :system
              :class (DarculaLaf.)}})

(def all-themes (merge (get-installed-laf)
                       (get-substance-laf)
                       (get-darcula-laf)))

(defn set-laf [laf-info]
  (case (:type laf-info)
    :system (UIManager/setLookAndFeel (:class laf-info))
    :substance (SubstanceLookAndFeel/setSkin (:class laf-info))))

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
                           ;; Invoke later because CB doens't like changing L&F
                           ;; while it's doing stuff.
                           (gui/invoke-later
                            (-> e
                                gui/selection
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
