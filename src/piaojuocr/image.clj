(ns piaojuocr.image
  (:require [opencv4.core :as cv]
            [opencv4.utils :as u]
            [opencv4.colors.rgb :as rgb]
            [opencv4.colors.html :as html]
            [seesaw.core :as gui]
            [seesaw.icon :as icon]
            [piaojuocr.ocr :as ocr]
            [taoensso.timbre :as log])
  (:import java.awt.Color)
  (:use [seesaw.chooser]))

(defn rgb->hsv
  [r g b]
  (let [mat (cv/new-mat 1 1 cv/CV_8UC3 (cv/new-scalar b g r))]
    (cv/cvt-color! mat cv/COLOR_BGR2HSV)
    (map int (.get mat 0 0))))

(defn show-pic!
  "显示图片，gray表示是否为灰度图"
  ([mat] (show-pic! mat false))
  ([mat gray]
   (let [f (gui/frame :title "pic view")
         mat2 (if gray
                (-> mat
                    cv/clone
                    (cv/cvt-color! cv/COLOR_GRAY2BGR))
                mat)
         img (u/mat-to-buffered-image mat2)
         rgb-txt (gui/label "X: Y: R: G: B:") ;; 必须是rgb图片，值才准确
         pic (gui/label :text ""
                        :halign :left
                        :valign :top
                        :background :white
                        :listen [:mouse-motion
                                 (fn [e]
                                   (let [x (.getX e)
                                         y (.getY e)]
                                     (when (and (< x (.getWidth img))
                                                (< y (.getHeight img)))
                                       (let [color (-> (.getRGB img x y)
                                                       (Color.  true))
                                             r (.getRed color)
                                             g (.getGreen color)
                                             b (.getBlue color)
                                             [h s v] (rgb->hsv r g b)]
                                         (gui/text! rgb-txt (format "X: %d Y: %d, R: %d G: %d B: %d, H: %d S: %d V: %d"
                                                                    x y r g b h s v))))))])
         content (gui/vertical-panel
                  :items [rgb-txt
                          (gui/scrollable pic)])]
     (.setIcon pic (gui/icon img))
     (gui/config! f :content content)
     (-> f gui/pack! gui/show!))))

(defn threshold!
  [source thresh]
  (-> source
      (cv/cvt-color! cv/COLOR_BGR2GRAY)
      (cv/threshold! thresh 255 cv/THRESH_BINARY)))

(def r1 (ocr/general "red_binary.jpg" {:language-type "CHN_ENG"
                                       :probability "true"}))
(def r2 (ocr/custom "red_binary.jpg" {:language-type "CHN_ENG"
                                      :type :caml-case
                                      :template-sign "eecfbc1a6645c46977ed7c5a49dc5c04"
                                      :probability "true"}))

;;;; 直线检测
;; 轮廓
(defn draw-rect!
  ([img rect-mat] (draw-rect! img rect-mat rgb/green 2))
  ([img rect-mat color width]
   (let [rect (cv/bounding-rect rect-mat)]
     (cv/rectangle
      img
      (cv/new-point (.x rect) (.y rect))
      (cv/new-point (+ (.x rect) (.width rect))
                    (+ (.y rect) (.height rect)))
      color
      width))
   img))

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
  [img rho min-intersections min-length]
  (let [edges (-> img
                  cv/clone
                  (cv/canny! 50. 200.))
        lines (cv/new-mat)
        _ (cv/hough-lines-p edges lines rho (/ Math/PI 180) min-intersections min-length 0)
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
              (cv/line result pt1 pt2 rgb/green 1)))
        thetas (map #(-> (.get lines %1 0)
                         (nth 1))
                    (range (.rows lines)))
        _ (println "thetas:" thetas)
        angel (some-> (median thetas)
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


(defn approx
  "简化多边形的边"
  [c]
  (let [m2f (cv/new-matofpoint2f (.toArray c))
        len (cv/arc-length m2f true)
        ret (cv/new-matofpoint2f)]
    (cv/approx-poly-dp m2f ret (* 0.02 len) true)
    ret))

(defn how-many-sides
  [c]
  (-> (approx c)
      .toList
      .size))

(defn which-color
  "颜色选择"
  [c]
  (let [side (how-many-sides c)]
    (println "side:" side)
    (case side
      1 rgb/pink
      2 rgb/magenta-
      3 rgb/green
      4 rgb/blue
      5 rgb/yellow-1
      6 rgb/cyan-2
      rgb/orange)))

(defn draw-contours!
  [img contours]
  (doall (map-indexed (fn [idx c]
                        (cv/draw-contours img contours idx (which-color c) 1))
                      (seq contours)))
  img)


(def img (cv/imread "./resources/text.jpg"))
(def img (cv/imread "./resources/qingdan.jpg"))
(def img (cv/imread "./resources/test.jpg" cv/IMREAD_REDUCED_COLOR_4))
(def img (cv/imread "./resources/wai4.jpg" cv/IMREAD_REDUCED_COLOR_4))

(def target (-> img
                cv/clone
                (cv/cvt-color! cv/COLOR_BGR2GRAY)))
(cv/threshold! target 120 255 cv/THRESH_BINARY_INV)
(def kernel (cv/new-mat 2 55 cv/CV_8UC1))
(cv/dilate! target kernel)
;(cv/erode! target kernel)
(cv/gaussian-blur! target (cv/new-size 5 5) 0)
(show-pic! target)

(def contours (cv/new-arraylist))
(-> target
    cv/clone
    (cv/canny! 50. 200.)
    (cv/find-contours contours (cv/new-mat) cv/RETR_CCOMP cv/CHAIN_APPROX_SIMPLE)
   ; show-pic!
    )
(show-pic! target)

(defn approx
  "简化多边形的边"
  [c]
  (let [m2f (cv/new-matofpoint2f (.toArray c))
        len (cv/arc-length m2f true)
        ret (cv/new-matofpoint2f)]
    (cv/approx-poly-dp m2f ret (* 0.02 len) true)
    ret))

(def my-contours
  (filter
   #(and
         (< 2 (.height (cv/bounding-rect %)) 50)
         (< 30 (.width (cv/bounding-rect %))))
   contours))

(def res (cv/clone img))
(def thetas (mapv (fn [c]
                    (let [rect (cv/min-area-rect (cv/new-matofpoint2f (.toArray c)))
                          pts (make-array org.opencv.core.Point 4)
                          angle (.angle rect)]
                      (.points rect pts)
                      (doseq [i (range (count pts))]
                        (cv/line res (nth pts i) (nth pts (mod (inc i) 4)) rgb/red-1))
                      (if (< (-> rect .size .width) (-> rect .size .height))
                        (- 90 angle)
                        (- angle))))
                  my-contours))

(show-pic! res)

(defn most-freq
  [cols]
  (->> (frequencies cols)
       (sort-by val)))

(def out (cv/clone img))
(def ang (- 180 (-> (most-freq thetas)
                    last
                    first)))
(rotate-by! out ang)
(show-pic! out)
(show-pic! img)

(let [[ang res] (calc-degree target 1 30 10)]
  (def ang ang)
  (def res res))

(show-pic! res)
