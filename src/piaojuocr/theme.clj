(ns piaojuocr.theme
  (:require [seesaw.core :as gui]
            [seesaw.border :as border]
            [seesaw.mig :refer [mig-panel]]
            [seesaw.font :refer [font default-font font-families]]
            [piaojuocr.util :as util]
            [piaojuocr.config :as config]
            [taoensso.timbre :as log])
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

(defn reset-theme!
  "重置主题为系统默认,这样就可以直接在非swing线程创建swing组件"
  []
  (gui/native!))

(defn update-theme! []
  (-> (config/get-config :theme)
      all-themes
      set-laf))

(defn set-fonts
  [font]
  (doseq [[k v] (UIManager/getDefaults)]
    (when (instance? javax.swing.plaf.FontUIResource v)
      (UIManager/put k font))))

(def all-fonts (font-families))

(defn update-font! []
  (set-fonts (font :name (config/get-config :font (default-font "Label.font"))
                   :size (config/get-config :font-size 12)))
  (update-theme!))

(defmacro wrap-theme [& body]
  `(do
     (reset-theme!)
     (JFrame/setDefaultLookAndFeelDecorated true)
     (let [r# (do ~@body)]
       (update-theme!)
       (update-font!)
       r#)))

(defn laf-selector []
  (gui/combobox
   :model    (-> (keys all-themes)
                 sort)
   :selected-item (config/get-config :theme "Moderate")
   :listen   [:selection (fn [e]
                           (let [theme (gui/selection e)]
                             (config/add-config! :theme theme)
                             (log/info "change theme to:" theme)
                             (update-theme!)
                             ))]))

(defn font-selector []
  (gui/combobox
   :model all-fonts
   :selected-item (config/get-config :font (default-font "Label.font"))
   :listen [:selection (fn [e]
                         (let [new-font (gui/selection e)]
                           (config/add-config! :font new-font)
                           (log/info "change font to:" new-font)
                           (update-font!)))]))


(defn font-size-selector []
  (gui/spinner
   :model (gui/spinner-model (config/get-config :font-size 12)
                             :from 10
                             :to 50
                             :by 1)
   :listen [:selection (fn [e]
                         (let [new-size (gui/selection e)]
                           (config/add-config! :font-size new-size)
                           (log/info "change font size to:" new-size)
                           (update-font!)))]))
