(ns piaojuocr.cv
  (:require [opencv4.core :as cv]
            [opencv4.utils :as u]
            [opencv4.colors.rgb :as rgb]
            [opencv4.colors.html :as html]
            [seesaw.core :as gui]
            [seesaw.icon :as icon]
            [piaojuocr.ocr :as ocr]
            [taoensso.timbre :as log])
  (:import java.awt.Color))

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
                                                       (Color.  true))]
                                         (gui/text! rgb-txt (format "X: %d Y: %d R: %d G: %d B: %d"
                                                                    x y
                                                                    (.getRed color)
                                                                    (.getGreen color)
                                                                    (.getBlue color)))))))])
         content (gui/vertical-panel
                  :items [rgb-txt
                          (gui/scrollable pic)])]
     (.setIcon pic (gui/icon img))
     (gui/config! f :content content)
     (-> f gui/pack! gui/show!))))

(require '[seesaw.dev :as dev])
(dev/show-options (gui/label))

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
(show-pic! mat true)

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

;;; colors colorMaps colorspaces
(-> (cv/new-mat 20 20 cv/CV_8UC3 rgb/red-2)
    (u/show))

(->> (html/->scalar "#66cc77")
     (cv/new-mat 20 20 cv/CV_8UC3)
     (u/show))

(-> "resources/cat.jpg"
    (cv/imread cv/IMREAD_REDUCED_COLOR_2)
    (cv/apply-color-map! cv/COLORMAP_AUTUMN)
    (u/imshow))

(def custom
  (u/matrix-to-mat [[0 0 0]
                    [0 0.5 0]
                    [0 1 0.5]]))

(-> (cv/new-mat 3 3 cv/CV_8UC3 rgb/yellow-2)
    (cv/transform! custom)
    (cv/imwrite out)
    (cv/dump))

;;; 颜色空间 RGB  HSV
(-> (cv/new-mat 1 1 cv/CV_8UC3 rgb/red-2)
    (cv/cvt-color! cv/COLOR_RGB2HSV)
    (cv/imwrite out)
    (.dump))

(defn hasv-mat
  [h]
  (let [m (cv/new-mat 20 3 cv/CV_8UC3)]
    (doto m
      (cv/cvt-color! cv/COLOR_BGR2HSV)
      (cv/set-to (cv/new-scalar h 255 255))
      (cv/cvt-color! cv/COLOR_HSV2BGR))))

(->> (range 180)
     (map hasv-mat)
     (cv/hconcat!)
     (u/imshow))

;;; 矩阵旋转与变换
;; flip
(def neko (cv/imread "resources/cat.jpg" cv/IMREAD_REDUCED_COLOR_4))
(u/imshow neko)
(show-pic! neko)

(-> neko
    (cv/clone)
    (cv/flip! 0)
    (u/imshow))

(->> [1 -1 0]
     (map #(-> neko (cv/clone) (cv/flip! %)))
     (cv/hconcat!)
     (u/imshow))

;; rotate
(-> neko
    (cv/clone)
    (cv/rotate! cv/ROTATE_90_CLOCKWISE)
    (u/imshow))

;; warp-affine 仿射变换
; 旋转45°
(def img (cv/clone neko))

(def rotation-angle 45)
(def zoom 1)
(def matrix
  (cv/get-rotation-matrix-2-d
   (cv/new-point (/ (.width img) 2) (/ (.height img) 2))
   rotation-angle
   zoom))
(.dump matrix)
(cv/warp-affine! img matrix (.size img))
(u/imshow img)

(defn rotate-by!
  ([img angle] (rotate-by! img angle 1))
  ([img angle zoom]
   (let [m2 (cv/get-rotation-matrix-2-d
             (cv/new-point (/ (.width img) 2)
                           (/ (.height img) 2))
             angle
             zoom)]
     (cv/warp-affine! img m2 (.size img)))))

(->> (range 0 360 40)
     (map #(-> neko
               cv/clone
               (rotate-by! %)))
     (cv/hconcat!)
     (show-pic!))

(->> (range 7)
     (map (fn [_] (-> neko cv/clone (rotate-by! 0 (rand 5)))))
     (cv/hconcat!)
     (show-pic!))

;; 其它变换
(def src
  (u/matrix-to-matofpoint2f [[0 0]
                             [5 5]
                             [4 6]]))
(def dst
  (u/matrix-to-matofpoint2f [[2 0]
                             [5 5]
                             [5 6]]))
(def transform-mat (cv/get-affine-transform src dst))
(cv/dump transform-mat)

(-> neko
    cv/clone
    (cv/warp-affine! transform-mat (.size neko))
    (show-pic!))

;;;;; 过滤mat  滤波
;; 手动过滤
(defn filter-buffer!
  "过滤某个channel"
  [image _mod]
  (let [total (* (.channels image)
                 (.total image))
        bytes (byte-array total)]
    (.get image 0 0 bytes)
    (doseq [^int i (range 0 total)]
      (if (not (= 0 (mod (+ i _mod) 3)))
        (aset-byte bytes i 0)))
    (.put image 0 0 bytes)
    image))

(->
 "resources/cat.jpg"
 (cv/imread cv/IMREAD_REDUCED_COLOR_8)
 (filter-buffer! 0)
 (show-pic!))

(def source (cv/imread "resources/cat.jpg" cv/IMREAD_REDUCED_COLOR_8))
(->> (range 0 3)
     (map #(filter-buffer! (cv/clone source) %))
     (cv/hconcat!)
     (show-pic!))

;; 使用multiply函数
(->
 "resources/cat.jpg"
 (cv/imread cv/IMREAD_REDUCED_COLOR_8)
 ; BGR: B*1， G*0.5， R*0
 (cv/multiply! (u/matrix-to-mat-of-double [[1.0 0.5 0.0]]))
 (show-pic!))

;修改亮度, 转换颜色空间
(->
 "resources/cat.jpg"
 (cv/imread cv/IMREAD_REDUCED_COLOR_8)
 (cv/cvt-color! cv/COLOR_BGR2HSV)
 (cv/multiply! (u/matrix-to-mat-of-double [[1.0 1.0 1.5]]))
 (cv/cvt-color! cv/COLOR_HSV2BGR)
 (show-pic!))

;局部高亮，使用submat
(def img (cv/imread "resources/cat.jpg" cv/IMREAD_REDUCED_COLOR_4))
(-> img
    (cv/submat (cv/new-rect 150 50 100 100))
    (cv/cvt-color! cv/COLOR_BGR2HSV)
    (cv/multiply! (u/matrix-to-mat-of-double [[1.0 1.3 1.3]]))
    (cv/cvt-color! cv/COLOR_HSV2BGR))
(show-pic! img)

; filter-2-d
; 使用中间为1的矩阵，图像不变
(->
 "resources/cat.jpg"
 (cv/imread cv/IMREAD_REDUCED_COLOR_8)
 (cv/filter-2-d! -1 (u/matrix-to-mat [[0 0 0]
                                      [0 1 0]
                                      [0 0 0]]))
 (show-pic!))

(def m (cv/new-mat 100 100 cv/CV_8UC1 (cv/new-scalar 200.)))
(show-pic! m)
(def s (cv/submat m (cv/new-rect 10 10 50 50)))
(cv/filter-2-d! s -1
                (u/matrix-to-mat [[0 0 0]
                                  [0 0.25 0]
                                  [0 0 0]]))
(show-pic! m)
(cv/dump m)

; 更多变换
(-> "resources/cat.jpg"
    (cv/imread)
    (cv/filter-2-d! -1 (u/matrix-to-mat
                        [[17.8824 -43.5161 4.11935]
                         [-3.45565 27.1554 -3.86714]
                         [0.0299566 0.184309 -1.46709]]))
    (cv/bitwise-not!)
    (show-pic!))

;; threshold 另一种过滤技术，通过阈值重置矩阵值
;通过threshold，设置低于150的值为0，其它的为250
(->
 (u/matrix-to-mat [[0 50 100]
                   [100 150 200]
                   [200 210 250]])
 (cv/threshold! 150 250 cv/THRESH_BINARY)
 (cv/dump))


; 反向操作
(->
 (u/matrix-to-mat [[0 50 100]
                   [100 150 200]
                   [200 210 250]])
 (cv/threshold! 150 250 cv/THRESH_BINARY_INV)
 (cv/dump))

(-> "resources/cat.jpg"
    (cv/imread cv/IMREAD_REDUCED_COLOR_4)
    (cv/cvt-color! cv/COLOR_BGR2GRAY)
    (cv/threshold! 150 250 cv/THRESH_BINARY_INV)
    (show-pic!))

; adaptive-threshold 通过周围的像素值来计算目标值
(-> "resources/cat.jpg"
    (cv/imread cv/IMREAD_REDUCED_COLOR_4)
    (cv/cvt-color! cv/COLOR_BGR2GRAY)
    (cv/adaptive-threshold! 255 cv/ADAPTIVE_THRESH_MEAN_C cv/THRESH_BINARY 55 -3.)
    (show-pic!))

;;; 掩码操作
(def rose (cv/imread "resources/rose.jpg"))
(show-pic! rose)
(def hsv (-> rose cv/clone (cv/cvt-color! cv/COLOR_RGB2HSV)))
(show-pic! hsv)

(def lower-red (cv/new-scalar 120 30 15))
(def upper-red (cv/new-scalar 130 255 255))
(def mask (cv/new-mat))
(cv/in-range hsv lower-red upper-red mask) ;; 类似于threshold
(show-pic! mask)

(def res (cv/new-mat))
(cv/bitwise-and! rose res mask)
(show-pic! res)

(def res2 (cv/new-mat))
(cv/convert-to res res2 -1 1 100)
(show-pic! res2)

(def cl (cv/clone rose))
(cv/copy-to res2 cl mask)
(show-pic! cl)
(cv/imwrite cl "bright_rose.jpg")

;;; 换头术
(def cl2 (cv/imread "resources/cat.jpg"))
(cv/resize! cl2 (cv/new-size (cv/cols mask) (cv/rows mask)))

(def cl3 (cv/clone rose))
(cv/copy-to cl2 cl3 mask)
(show-pic! cl3)

;;; blurring 模糊，虚化
(-> neko
    cv/clone
    (cv/blur! (cv/new-size 3 3))
    (show-pic!))

(->> (range 3 10 2)
     (map #(-> neko (cv/clone) (cv/blur! (cv/new-size % %))))
     (cv/hconcat!)
     (show-pic!))

(-> "resources/code.png"
    (cv/imread)
    (cv/cvt-color! cv/COLOR_BGR2GRAY)
    (cv/gaussian-blur! (cv/new-size 5 5) 0) ; sigma为0,不要太平滑
    (cv/threshold! 210 250 cv/THRESH_BINARY)
    (cv/imwrite "code.jpg")
    (show-pic! true))

(-> (cv/imread "code.jpg" cv/IMREAD_REDUCED_COLOR_8)
    cv/dump)

; 高斯模糊， 多用于去除噪声
(-> neko
    cv/clone
    (cv/gaussian-blur! (cv/new-size 5 5) 17)
    (show-pic!))

; 双边模糊，相当于背景虚化, 可以帮助提取轮廓
(-> neko
    cv/clone
    (cv/bilateral-filter! 9 9 7)
    (show-pic!))

(-> neko
    cv/clone
    (cv/cvt-color! cv/COLOR_BGR2GRAY)
    (cv/bilateral-filter! 9 9 7)
    (cv/canny! 50. 250. 3 true)
    (cv/bitwise-not!)
    (show-pic!))

; 为什么使用双边过滤的原因，对比
(-> neko
    cv/clone
    (cv/cvt-color! cv/COLOR_BGR2GRAY)
    (cv/blur! (cv/new-size 3 3))
    (cv/canny! 50. 250. 3 true)
    (cv/bitwise-not!)
    (show-pic!))

; median blur
(-> neko
    cv/clone
    (cv/median-blur! 27)
    (show-pic!))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 颜色操作，增强或降低颜色
;; threshold 限制通道值
;; set-to使用掩码

