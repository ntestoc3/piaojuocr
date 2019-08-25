(ns piaojuocr.ocr
  (:require [seesaw.core :as gui]
            [seesaw.border :as border]
            [seesaw.swingx :as guix]
            [seesaw.mig :refer [mig-panel]]
            [seesaw.font :refer [font default-font]]
            [piaojuocr.util :as util :refer [show-ui]]
            [piaojuocr.config :as config]
            [piaojuocr.ocr-api :as api]
            [taoensso.timbre :as log]
            [seesaw.table :as table])
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
                :words [k v]
                nil)) wr)
       (apply merge)))

(defn make-ocr-model [ocr-result]
  [:columns [{:key :words :text "文字"}
             {:key :prob-average :text "置信度平均值"  :class java.lang.Double}
             {:key :prob-min :text "置信度最小值"  :class java.lang.Double}
             {:key :prob-variance :text "置信度方差"  :class java.lang.Double}
             {:key :left :text "X"  :class java.lang.Integer}
             {:key :top :text "Y"  :class java.lang.Integer}
             {:key :width :text "宽度"  :class java.lang.Integer}
             {:key :height :text "高度" :class java.lang.Integer}]
   :rows (transform [ALL] format-word-result (:words-result ocr-result))])

(defn make-view [data id]
  (gui/scrollable (guix/table-x :id id
                                :model (make-ocr-model data))))

(defn set-model! [root id data]
  (let [tbl (gui/select root [(util/->select-id id)])]
    (->> (make-ocr-model data)
         (gui/config! tbl :model))))



(comment


  (show-ui (make-view  api/res4 :table))


  (transform [ALL :probability ALL FIRST] (fn [k] (keyword (str "prob-" (name k)))) (vec wr))

  )
