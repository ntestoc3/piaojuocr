(ns piaojuocr.cv
  (:require [opencv4.core :as cv]
            [opencv4.utils :as u]
            [opencv4.colors.rgb :as color]
            [seesaw.core :as gui]
            [seesaw.icon :as icon]
            [piaojuocr.ocr :as ocr]
            [taoensso.timbre :as log]))

(defn show-pic!
  [mat]
  (let [f (gui/frame :title "pic view")
        pic (gui/label "")]
    (.setIcon pic (gui/icon (u/mat-to-buffered-image mat)))
    (gui/config! f :content (gui/scrollable pic))
    (-> f gui/pack! gui/show!)))

(-> 
 (cv/imread "resources/cat.jpg")
 (cv/resize! (cv/new-size 150 100))
 (cv/cvt-color! cv/COLORMAP_JET)
 (cv/imwrite "output.jpg"))

(->
 (u/mat-from-url "http://eskipaper.com/images/jump-cat-1.jpg")
 (u/resize-by 0.3)
 (cv/imwrite "output.jpg"))

;;; 矩阵操作
; 数据随机
(def mat (cv/new-mat 30 30 cv/CV_8UC1))
(show-pic! mat)

(def mat (cv/new-mat 30 30 cv/CV_8UC1 (cv/new-scalar 105.)))
(show-pic! mat)

(->> 
 (cv/new-scalar 128.)
 (cv/new-mat 3 3 cv/CV_8UC1)
 (cv/dump))

(def out "output.jpg")
;; 颜色矩阵
(def red-mat (cv/new-mat 30 30 cv/CV_8UC3 (cv/new-scalar 0 0 255)))
(cv/imwrite red-mat out)

(def green-mat (cv/new-mat 30 30 cv/CV_8UC3 (cv/new-scalar 0 255 0)))
(cv/imwrite green-mat out)

(def blue-mat (cv/new-mat 30 30 cv/CV_8UC3 (cv/new-scalar 255 0 0)))
(cv/imwrite blue-mat out)

;; 子矩阵
(def mat (cv/new-mat 30 30 cv/CV_8UC3 (cv/new-scalar 255 255 0)))
(def sub (cv/submat mat (cv/new-rect 10 10 10 10)))
(cv/set-to! sub (cv/new-scalar 0 255 255))
(cv/imwrite mat out)
(-> (repeat 10 mat)
    cv/hconcat!
    (cv/imwrite out))

;; 设置一个像素颜色
(def yellow (byte-array [0 238 238]))
(def a (cv/new-mat 3 3 cv/CV_8UC3))
(.put a 0 0 yellow)
(.dump a)
(.cols a)
(.rows a)

(def height 100)
(def width 100)

(def a (cv/new-mat height width cv/CV_8UC3))
(doseq [x (range width)
        y (range height)]
  (.put a x y (byte-array [0 0 (rand 255)])))
(cv/imwrite a out)

(->> (range 25)
     (map #(cv/new-mat 30 30 cv/CV_8UC1 (cv/new-scalar (double (* % 10)))))
     (cv/hconcat!)
     (show-pic!))

;; 平滑红色梯度图
(->> (range 255)
     (map #(cv/new-mat 20 2 cv/CV_8UC3 (cv/new-scalar 0 0 (double %))))
     (cv/hconcat!)
     (show-pic!))

;;; 读取图片矩阵 mat
;; 缩小1倍
(-> "resources/text.png"
    (cv/imread cv/IMREAD_REDUCED_COLOR_2)
    (u/imshow))

