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
                [k v])) wr)
       (apply merge)))

(def prob-loc-cols-info '({:key :prob-average :text "置信度平均值"  :class java.lang.Double}
                          {:key :prob-min :text "置信度最小值"  :class java.lang.Double}
                          {:key :prob-variance :text "置信度方差"  :class java.lang.Double}
                          {:key :left :text "X"  :class java.lang.Integer}
                          {:key :top :text "Y"  :class java.lang.Integer}
                          {:key :width :text "宽度"  :class java.lang.Integer}
                          {:key :height :text "高度" :class java.lang.Integer}))

(defn make-ocr-model [ocr-result]
  [:columns (conj prob-loc-cols-info {:key :words :text "文字"})
   :rows (transform [ALL] format-word-result (:words-result ocr-result))])

(defn make-iocr-model [iocr-result]
  [:columns (vec (conj prob-loc-cols-info
                       {:key :word :text "文字"}
                       {:key :word-name :text "字段名"}))
   :rows (transform [ALL] format-word-result (get-in iocr-result [:data :ret]))])

(defn make-view [model id]
  (gui/scrollable (guix/table-x :id id
                                :model model)))

(defn set-model! [root id model]
  (let [tbl (gui/select root [(util/->select-id id)])]
    (gui/config! tbl :model model)))



(comment

  (gui/native!)

  (show-ui (make-view (make-ocr-model []):table))

  (show-ui (make-view  (make-ocr-model api/res4) :table))

  (show-ui (make-view  (make-iocr-model api/res5) :table))

  (transform [ALL :probability ALL FIRST] (fn [k] (keyword (str "prob-" (name k)))) (vec wr))

  )
