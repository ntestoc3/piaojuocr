(ns piaojuocr.ocr
  (:require [seesaw.core :as gui]
            [seesaw.border :as border]
            [seesaw.swingx :as guix]
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

(defn format-prob-key [prob]
  (->> (name prob)
       (str "prob-")
       keyword))

(defn format-word-result [wr]
  (->> (map (fn [[k v]]
              (case k
                :probability (transform [MAP-KEYS] format-prob-key v)
                :location v
                [k v])) wr)
       (apply merge)))

(defn make-ocr-model [ocr-result]
  [:columns [{:key :words :text "文字"}
             {:key :prob-average :text "置信度平均值"}
             {:key :prob-min :text "置信度最小值"}
             {:key :prob-variance :text "置信度方差"}
             {:key :left :text "X"}
             {:key :top :text "Y"}
             {:key :width :text "宽度"}
             {:key :height :text "高度"}]
   :rows (transform [ALL] format-word-result (:words-result ocr-result))])

(defn make-view [data id]
  (gui/scrollable (guix/table-x :id id
                                :model (make-ocr-model data))))

(defn set-model! [root id data]
  (let [tbl (gui/select root [(util/->select-id id)])]
    (->> (make-ocr-model data)
         (gui/config! tbl :model))))

(defn- show-ui
  ([widget]
   (let [f (gui/frame :title "test ui"
                      :content widget)]
     (-> f gui/pack! gui/show!)
     f)))

(comment

  (gui/native!)

  (show-ui (make-view  api/res4 :table))


  (transform [ALL :probability ALL FIRST] (fn [k] (keyword (str "prob-" (name k)))) (vec wr))

  )
