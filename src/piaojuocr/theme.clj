(ns piaojuocr.theme
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

(defmacro wrap-theme [& body]
  `(do
     (gui/native!)
     (JFrame/setDefaultLookAndFeelDecorated true)
     ~@body
     (-> (config/get-config :theme)
         all-themes
         set-laf)))

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

