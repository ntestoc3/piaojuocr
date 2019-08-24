(ns piaojuocr.theme
  (:require [seesaw.core :as gui]
            [seesaw.border :as border]
            [seesaw.mig :refer [mig-panel]]
            [seesaw.font :refer [font default-font]]
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

(defmacro wrap-theme [& body]
  `(do
     (reset-theme!)
     (JFrame/setDefaultLookAndFeelDecorated true)
     (let [r# (do ~@body)]
       (-> (config/get-config :theme)
           all-themes
           set-laf)
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
                             (-> theme
                                 all-themes
                                 set-laf)))]))

