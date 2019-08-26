(ns piaojuocr.ocr-table
  (:require [seesaw.core :as gui]
            [seesaw.border :as border]
            [seesaw.swingx :as guix]
            [seesaw.mig :refer [mig-panel]]
            [piaojuocr.util :as util :refer [show-ui]]
            [piaojuocr.config :as config]
            [piaojuocr.ocr-api :as api]
            [taoensso.timbre :as log]
            [seesaw.table :as table])
  (:use com.rpl.specter)
  (:import javax.swing.table.DefaultTableCellRenderer)
  )

(defn cell-range
  ([one]
   [one])
  ([start end]
   (range start (inc end))))

(defn format-cell [cell]
  (for [;;row (apply cell-range (:row cell))
        ;;col (apply cell-range (:column cell))
        row (:row cell)
        col (:column cell)
        ]
    (merge cell
           {:column col
            :row row})))

(defn format-row [row]
  (let [cols (map (fn [col] {(keyword (str "column-" (:column col))) (:word col)
                             (keyword (str "rect-" (:column col))) (:rect col)})
                  row)]
    (apply merge {:column-count (count cols)} cols)))

(defn format-rows [data]
  (some->> (mapcat format-cell data)
           (group-by :row)
           (map (comp format-row second))))

(defn get-max-col-keys [rows]
  (some->> rows
           (apply max-key :column-count)
           :column-count
           inc
           (range 1)
           (map #(keyword (str "column-" %1)))))

(defn fix-top-left [rows]
  (transform [ALL :rect] (fn [rect]
                           (assoc rect
                                  :top (:left rect)
                                  :left (:top rect)))
             rows))

(defn format-form [form]
  ;;; 返回结果中header和footer的top和left是反的,需要纠正。
  (let [footer (fix-top-left (:footer form))
        header (fix-top-left (:header form))
        body (:body form)]
    (mapcat format-rows [header body footer])))

(defn make-model [data]
  (let [rows (some->> (:forms data)
                      (mapcat format-form))
        cols (get-max-col-keys rows)]
    [:columns cols
     :rows rows]))

(defn get-selected-cells [tbl]
  "获取选中的单元格,返回[[row col]...]"
  (for [row (.getSelectedRows tbl)
        col (.getSelectedColumns tbl)]
    [row col]))

(defn get-row-value [tbl row]
  (->> (.convertRowIndexToModel tbl row)
       (table/value-at tbl)))

(defn get-cell-value
  "获取table的单元格值"
  [tbl row col]
  (let [row (get-row-value tbl row)
        col (.convertColumnIndexToModel tbl col)
        rect-key (keyword (str "rect-" (inc col)))
        word-key (keyword (str "column-" (inc col)))]
    {:rect (get row rect-key)
     :word (get row word-key)}))

(defn get-cell-rect
  "获取一个单元格的区域"
  [tbl row col]
  (some-> (get-cell-value tbl row col)
          :rect))

(defn get-selected-rects [tbl]
  (some->> (get-selected-cells tbl)
           (map #(apply get-cell-rect tbl %1))))

(defn cell-render [tbl]
  (proxy [DefaultTableCellRenderer] []
    (getTableCellRendererComponent
      [tbl
       ^java.lang.Object value
       ^java.lang.Boolean selected
       ^java.lang.Boolean has-focus
       ^java.lang.Integer row
       ^java.lang.Integer column]
      (some->> (get-cell-value tbl row column)
               (str "value:" )
               (.setToolTipText this))
      (proxy-super getTableCellRendererComponent tbl value selected has-focus row column))))

(defn make-view [data id]
  (let [tbl (guix/table-x :id id
                          :selection-mode :multi-interval
                          :model (make-model data))]
    (doto tbl
      (.setDefaultRenderer java.lang.Object (cell-render tbl))
      (.setCellSelectionEnabled true))
    (gui/scrollable tbl)))

(defn set-model! [root id data]
  (let [tbl (gui/select root [(util/->select-id id)])]
    (->> (make-model data)
         (gui/config! tbl :model))))

(comment

  (def r1 (nth (:body (first (:forms api/res9))) 10))

  (format-cell r1)


  (def r2 (:body (first (:forms api/res9))))

  (def r3 (format-rows r2))

  (doseq [r r3]
    (let [cr (count (second r))]
      (println "row:" (first r) "count:" cr)
      ;; (assert (= 7 cr) (str "error:" r "count:" cr))
    ))

  (def m1 (make-model api/res9))

  (util/show-ui (make-view api/res9 :test))

  (def t1 (gui/select *1 [:#test]))

  (util/show-ui (make-view [] :test))


  (fix-top-left (:footer (first (:forms api/res9))))

  (fix-top-left (:header (first (:forms api/res9))))

  )
