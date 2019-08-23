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

(defn render-ocr [renderer info]
  (let [v (:value info)]
    (gui/config! renderer :text (str (:words v)
                                     "  ---  "
                                     (get-in v [:probability :average])))))

(defn make-view [data]
  (gui/scrollable (gui/listbox :model data
                               :renderer render-ocr)))


(defn show-ui
  ([widget]
   (let [f (gui/frame :title "test ui"
                      :content widget)]
     (-> f gui/pack! gui/show!)
     f)))

(comment (show-ui (make-view (:words-result api/res4))))
