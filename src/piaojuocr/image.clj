(ns piaojuocr.image
  (:require [opencv4.core :as cv]
            [opencv4.utils :as u]
            [opencv4.colors.rgb :as color]
            [seesaw.core :as gui]
            [seesaw.icon :as icon]
            [piaojuocr.ocr :as ocr]
            [taoensso.timbre :as log])
  (:use [seesaw.chooser]))

(defn threshold!
  [source thresh]
  (-> source
      (cv/cvt-color! cv/COLOR_BGR2GRAY)
      (cv/threshold! thresh 255 cv/THRESH_BINARY)))


(def file (.getPath (clojure.java.io/resource "test.jpg")))
(def img (cv/imread file))
(threshold! img 150)
(u/imshow img)

(defn get-channel
  [img idx]
  (let [channels (cv/new-arraylist)]
    (cv/split img channels)
    (nth channels idx)))

(def channels (cv/new-arraylist))
(cv/split img channels)
(count channels)
(def red (nth channels 2))
(cv/threshold! red 150 255 cv/THRESH_BINARY)

(defn show-pic!
  [mat]
  (let [f (gui/frame :title "pic view")
        pic (gui/label "")]
    (.setIcon pic (gui/icon (u/mat-to-buffered-image mat)))
    (gui/config! f :content (gui/scrollable pic))
    (-> f gui/pack! gui/show!)))

(show-pic! img)

(show-pic! red)
(u/resize-by red 0.5)
(cv/imwrite red "red_binary.jpg")

(def r1 (ocr/general "red_binary.jpg" {:language-type "CHN_ENG"
                                       :probability "true"}))
(def r2 (ocr/custom "red_binary.jpg" {:language-type "CHN_ENG"
                                      :type :caml-case
                                      :template-sign "eecfbc1a6645c46977ed7c5a49dc5c04"
                                      :probability "true"}))

;;;; 直线检测
;; 轮廓
(defn degree-trans
  [theta]
  (* 180 (/ theta Math/PI)))

(defn mean [coll]
  (let [sum (apply + coll)
        count (count coll)]
    (if (pos? count)
      (/ sum count)
      0)))

(defn median [coll]
  (let [sorted (sort coll)
        cnt (count sorted)
        halfway (quot cnt 2)]
    (if (odd? cnt)
      (nth sorted halfway) ; (1)
      (let [bottom (dec halfway)
            bottom-val (nth sorted bottom)
            top-val (nth sorted halfway)]
        (mean [bottom-val top-val]))))) ; (2)

(defn calc-degree
  "计算图片倾斜角度，返回角度和中间结果图"
  [img]
  (let [edges (-> img
                  cv/clone
                  (cv/canny! 50. 200. 3))
        lines (cv/new-mat)
        _ (cv/hough-lines edges lines 1 (/ Math/PI 180) 180 0 0)
        result (cv/clone img)
        _ (dotimes [i (.rows lines)]
            (let [val_ (.get lines i 0)
                  rho (nth val_ 0)
                  theta (nth val_ 1)
                  a (Math/cos theta)
                  b (Math/sin theta)
                  x0 (* a rho)
                  y0 (* b rho)
                  pt1 (cv/new-point
                       (Math/round (+ x0 (* 1000 (* -1 b))))
                       (Math/round (+ y0 (* 1000 a))))
                  pt2 (cv/new-point
                       (Math/round (- x0 (* 1000 (* -1 b))))
                       (Math/round (- y0 (* 1000 a))))]
              (cv/line result pt1 pt2 color/black 1)))
        thetas (map #(-> (.get lines %1 0)
                         (nth 1))
                    (range (.rows lines)))
        _ (println "thetas:" thetas)
        angel (-> (median thetas)
                  degree-trans
                  (- 90))]
    [angel result]))

(defn rotate-by!
  "旋转指定角度"
  [img angle]
  (let [M2
        (cv/get-rotation-matrix-2-d
         (cv/new-point (/ (.width img) 2) (/ (.height img) 2)) angle 1)]
    (cv/warp-affine! img M2 (.size img))))

(def img (cv/imread "./resources/text.jpg"))
(def img (cv/imread "./resources/qingdan.jpg"))
(def img (cv/imread "./resources/test2.jpg"))

(def img (get-channel img 2))
(show-pic! img)

(let [[ang res] (calc-degree img)]
     (def ang ang)
     (def res res))
(show-pic! res)
(rotate-by! img ang)
(show-pic! img)

(let [[ang res] (calc-degree horz)]
  (def ang ang)
  (def res res))
(show-pic! res)
(rotate-by! img ang)
(show-pic! img)


;;;;;;;;;;;;;;;;;;
;;;; 自动阈值二值化
(def gray (cv/new-mat))
(cv/cvt-color img gray cv/COLOR_BGR2GRAY)
(show-pic! gray)
(def gray (cv/clone img))
(def bw (cv/new-mat))
(cv/adaptive-threshold (cv/bitwise-not! gray)
                        bw
                        255
                        cv/ADAPTIVE_THRESH_MEAN_C
                        cv/THRESH_BINARY
                        15
                        -2)
(show-pic! bw)

(def horz (cv/clone bw))
(def vert (cv/clone bw))
(def horiz-size (/ (.cols horz) 20))

(def horiz_struct (cv/get-structuring-element cv/MORPH_RECT
                                              (cv/new-size horiz-size 1)))

(cv/erode! horz horiz_struct (cv/new-point -1 -1))
(cv/dilate! horz horiz_struct (cv/new-point -1 -1))
(show-pic! horz)

(def rho 1)
(def theta (/ Math/PI 90))
(def min-intersections 30)
(def min-line-length 10)
(def max-line-gap 50)
(def lines (cv/new-mat))

(cv/hough-lines-p edges lines rho theta min-intersections min-line-length max-line-gap)

(show-pic! lines)

(def result (cv/clone img))
(dotimes [i (.rows lines)]
  (let [val (.get lines i 0)]
    (cv/line result
             (cv/new-point (nth val 0) (nth val 1))
             (cv/new-point (nth val 2) (nth val 3))
             color/red-2)
    ))
(show-pic! result)

;;; 删除轮廓
(def img1 (cv/imread file))
(show-pic! img1)

(def mask
  (-> img1
      (cv/cvt-color! cv/COLOR_BGR2GRAY)
      cv/clone
      (cv/threshold! 200 255 cv/THRESH_BINARY_INV)
      (cv/median-blur! 7)))
(show-pic! mask)

(def masked-input (cv/clone mask))
(cv/set-to masked-input (cv/new-scalar 0 0 0) mask)
(cv/set-to masked-input (cv/new-scalar 255 255 255) (cv/bitwise-not! mask))
(show-pic! masked-input)

(def contours (cv/new-arraylist))
(cv/find-contours
 masked-input
 contours
 (cv/new-mat)
 cv/RETR_TREE
 cv/CHAIN_APPROX_NONE)

(def exe-1 (cv/clone img1))
(doseq [c contours]
  (let [rect (cv/bounding-rect c)]
    (cv/rectangle
     exe-1
     (cv/new-point (.x rect) (.y rect))
     (cv/new-point (+ (.width rect) (.x rect)) (+ (.y rect) (.height rect)))
     color/red-3
     2)))
(show-pic! exe-1)

;;; 过滤countours
(def interesting-contours
  (filter
   #(and
     (> (cv/contour-area %) 100)
     (< (.width (cv/bounding-rect %)) (- (.width img1) 10)))
   contours))
(def exe-1 (cv/clone img1))
(doseq [c interesting-contours]
  (let [rect (cv/bounding-rect c)]
    (cv/rectangle
     exe-1
     (cv/new-point (.x rect) (.y rect))
     (cv/new-point (+ (.width rect) (.x rect)) (+ (.y rect) (.height rect)))
     color/red-3
     2)))
(show-pic! exe-1)

