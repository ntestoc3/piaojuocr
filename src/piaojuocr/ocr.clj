(ns piaojuocr.ocr
  (:require [seesaw.core :as gui]
            [seesaw.border :as border]
            [seesaw.mig :refer [mig-panel]]
            [seesaw.font :refer [font default-font]]
            [piaojuocr.util :as util]
            [piaojuocr.config :as config]
            [piaojuocr.ocr-api :as api]
            [taoensso.timbre :as log])
  (:import [java.awt Dimension]
           [java.awt.image BufferedImage]
           [javax.swing ImageIcon])
  (:use com.rpl.specter))

(defn de-select [target item]
  (let [selected (gui/selection target {:multi? true})
        new-sel (select [(filterer #(not= item (:words %1)))] selected)]
    (println "new selected:" new-sel)
    (->> new-sel
         (gui/selection! target))))

(defn render-ocr [renderer info]
  (let [v (:value info)]
    (gui/config! renderer :text (:words v))))

(defn make-view [data]
  (gui/scrollable (gui/listbox :model data
                               :renderer render-ocr
                               ;; :listen [:mouse-released (fn [e]
                               ;;                            (let [this (gui/to-widget e)
                               ;;                                  index (->> (.getPoint e)
                               ;;                                             (.locationToIndex this))]
                               ;;                              (if (and (not (neg? index))
                               ;;                                       (.isSelectedIndex this index))
                               ;;                                (let [item (-> (.getModel this)
                               ;;                                               (.getElementAt index))]
                               ;;                                  (de-select this item)))))]
                               )))


(defn show-ui
  ([widget]
   (let [f (gui/frame :title "test ui"
                      :content widget)]
     (-> f gui/pack! gui/show!)
     f)))

(show-ui (make-view (take 5 (:words-result api/res4))))
