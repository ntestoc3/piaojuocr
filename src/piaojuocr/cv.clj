(ns piaojuocr.cv
  (:require [opencv4.core :as cv]
            [opencv4.utils :as u]
            [opencv4.colors.rgb :as rgb]
            [opencv4.colors.html :as html]
            [seesaw.core :as gui]
            [seesaw.icon :as icon]
            [piaojuocr.ocr :as ocr]
            [taoensso.timbre :as log]
            [opencv4.colors.rgb :as color])
  (:import java.awt.Color))

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


(import 'org.opencv.objdetect.QRCodeDetector)
(defn resize-square!
  "拉伸img为正方形"
  [img]
  (let [edge-len (max (.cols img)
                      (.rows img))
        new-size (cv/new-size edge-len edge-len)]
    (cv/resize! img new-size)))

(defn auto-decode
  [img]
  (let [dector (QRCodeDetector.)
        points (cv/new-mat)
        result (.detectAndDecode dector img points)]
    (if (empty? result)
      (let [new-img (cv/submat img (cv/bounding-rect points))]
        (resize-square! new-img)
        (cv/imwrite new-img "clean_code.jpg")
        [(.detectAndDecode dector new-img) points])
      [result points])))

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


(def code (cv/imread "code.jpg"))
(let [[r points] (auto-decode code)
      rect (cv/bounding-rect points)]
  (println "decode str:" r)
  (draw-rect! code points)
  (show-pic! code))


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

; resize按比例放大
(-> (u/matrix-to-mat [[0 0 170]
                      [0 0 170]
                      [100 100 0]])
    (cv/cvt-color! cv/COLOR_GRAY2BGR)
    (cv/threshold! 150 255 cv/THRESH_BINARY) ;150以下的过滤掉
    (cv/resize! (cv/new-size 50 50) 1 1 cv/INTER_AREA) ; 默认resize会进行blur,使用INTER_AREA取消
    (show-pic!))

(def orig (cv/imread "resources/rose.jpg" cv/IMREAD_REDUCED_COLOR_4))
(-> orig
    cv/clone
    (cv/threshold! 100 255 cv/THRESH_BINARY)
    show-pic!)

; threshold也是过滤
(-> "resources/cat.jpg"
    (cv/imread cv/IMREAD_REDUCED_COLOR_4)
    show-pic!)

(-> "resources/cat.jpg"
    (cv/imread cv/IMREAD_REDUCED_GRAYSCALE_4)
    (cv/threshold! 150 255 cv/THRESH_BINARY)
    show-pic!)

(defn low-high!
  "分离出阈值t1, 背景色为color2, 前景色为color1"
  [image t1 color1 color2]
  (let [copy (-> image cv/clone (cv/cvt-color! cv/COLOR_BGR2HSV))
        work (cv/clone image)
        thresh-1 (cv/new-mat)]
    (cv/threshold copy thresh-1 t1 255 cv/THRESH_BINARY)
    (cv/cvt-color! thresh-1 cv/COLOR_BGR2GRAY)
    (cv/set-to work color1 thresh-1)
    (->> (cv/bitwise-not! thresh-1)
         (cv/set-to work color2))
    work))

(-> (cv/imread "resources/rose.jpg" cv/IMREAD_REDUCED_COLOR_2)
    (low-high! 150 rgb/white-smoke- rgb/lightblue-1)
    show-pic!)

;; channel操作
(defn update-channel!
  [mat fn-c n-chan]
  (let [channels (cv/new-arraylist)]
    (cv/split mat channels)
    (let [old-ch (nth channels n-chan)
          new-ch (u/bytes-to-mat!
                  (cv/new-mat (.height mat) (.width mat) (.type old-ch))
                  (byte-array (map fn-c (u/mat-to-bytes old-ch))))]
      (.set channels n-chan new-ch)
      (cv/merge channels mat)
      mat)))

(def my-cat (cv/imread "resources/girls.jpg" cv/IMREAD_REDUCED_COLOR_4))

; 更新某个channel
(-> my-cat
    cv/clone
    (update-channel! (fn [_] 255) 1)
    (update-channel! (fn [_] 255) 0)
    show-pic!)

;; 使用HSV色彩空间，过滤蓝色
(-> my-cat
    cv/clone
    (cv/cvt-color! cv/COLOR_RGB2HSV)
    (update-channel! (fn [x] 10) 0)
    (cv/cvt-color! cv/COLOR_HSV2RGB)
    show-pic!)

;; YUV 调整亮度
(-> my-cat
    cv/clone
    (cv/cvt-color! cv/COLOR_BGR2YUV)
    (update-channel! (fn [_] 255) 0)
    (cv/cvt-color! cv/COLOR_YUV2BGR)
    show-pic!)

;;;;; 变换 Transform
(def s-mat (cv/new-mat 3 3 cv/CV_8UC1))
(.put s-mat 0 0 (byte-array [100 255 200
                             100 255 200
                             100 255 200]))
(cv/dump s-mat)
(-> s-mat
    cv/clone
    (cv/resize! (cv/new-size 30 30) 1 1 cv/INTER_AREA)
    show-pic!)

(def t-mat (cv/new-mat 1 1 cv/CV_32F (cv/new-scalar 0.7)))
(-> s-mat
    (cv/transform! t-mat)
    cv/dump)

;; 灰度转彩色
(def s-mat (cv/new-mat 3 3 cv/CV_8UC1))
(.put s-mat 0 0 (byte-array [100 255 200
                             100 255 200
                             100 255 200]))
(cv/cvt-color! s-mat cv/COLOR_GRAY2BGR)
;; 3个chanel需要3*3的变换矩阵
(def t-mat (cv/new-mat 3 3 cv/CV_32F))
(.put t-mat 0 0 (float-array [2 0 0
                              0 1 0
                              0 0 1]))
(-> s-mat
    (cv/transform! t-mat)
    (cv/resize! (cv/new-size 50 50) 1 1 cv/INTER_AREA)
    show-pic!)

;; 增加亮度
(-> my-cat
    cv/clone
    (cv/cvt-color! cv/COLOR_BGR2HSV)
    (cv/transform! (u/matrix-to-mat [[5 0 0] [0 1 0] [0 0 1]]))
    (cv/cvt-color! cv/COLOR_HSV2BGR)
    show-pic!)

(def
  usui-cat
  (-> my-cat
      cv/clone
      (cv/cvt-color! cv/COLOR_BGR2YUV)
      (cv/transform! (u/matrix-to-mat [[20 0 0]
                                       [0 1 0]
                                       [0 0 1]]))
      (cv/cvt-color! cv/COLOR_YUV2BGR)
      (cv/transform! (u/matrix-to-mat [[3 0 0]
                                       [0 1 0]
                                       [0 0 2]]))
      (cv/cvt-color! cv/COLOR_BGR2HSV)
      (cv/transform! (u/matrix-to-mat [[1 0 0]
                                       [0 3 0]
                                       [0 0 1]]))
      (cv/cvt-color! cv/COLOR_HSV2BGR)))
(show-pic! usui-cat)

(def line-cat
  (-> my-cat
      cv/clone
      (cv/cvt-color! cv/COLOR_BGR2GRAY)
      (cv/canny! 100. 150. 3 true)
      (cv/cvt-color! cv/COLOR_GRAY2BGR)
      (cv/bitwise-not!)))
(show-pic! line-cat)

(def target (cv/new-mat))
(cv/bitwise-and usui-cat line-cat target)
(show-pic! target)

;;;;;;;;;;;;;;;; cartoons

;; median-blur或者gaussian-blur移除多余的线条

(defn art2
  [img thresh1 thresh2 sigma1 sigma2 color]
  (let [ c (-> img
               cv/clone
               (cv/cvt-color! cv/COLOR_BGR2GRAY)
               (cv/canny! thresh1 thresh2 3 false)
               (cv/bilateral-filter! 10 sigma1 sigma2))
        ;; 前景
        colored (u/mat-from img)
        _ (cv/set-to colored color)

        ;; 背景
        target (u/mat-from img)
        _ (cv/set-to target rgb/white)]
    (cv/copy-to colored target c)
    (show-pic! target)))

(def girl (cv/imread "resources/girl2.jpg" cv/IMREAD_REDUCED_COLOR_4))
(art2 girl 40. 140. 90 20 color/red-2)

(def girl (cv/imread "resources/girl.jpg" cv/IMREAD_REDUCED_COLOR_4))
(art2 girl 80. 160. 90 20 color/red-2)

(defn art3!
  [buffer sigma1 sigma2]
  (-> buffer
      (cv/cvt-color! cv/COLOR_RGB2GRAY)
      (cv/bilateral-filter! 10 sigma1 sigma2)
      (cv/median-blur! 7)
      (cv/adaptive-threshold! 255 cv/ADAPTIVE_THRESH_MEAN_C cv/THRESH_BINARY 9 3)
      (cv/cvt-color! cv/COLOR_GRAY2BGR)))

(-> (cv/imread "resources/girl3.jpg" cv/IMREAD_REDUCED_COLOR_4)
    (art3! 250 30)
    show-pic!)

(-> (cv/imread "resources/girl2.jpg" cv/IMREAD_REDUCED_COLOR_4)
    (art3! 9 7)
    show-pic!)

;; 3阶灰度
(-> (cv/imread "resources/girl.jpg" cv/IMREAD_REDUCED_COLOR_4)
    (cv/median-blur! 1)
    (cv/cvt-color! cv/COLOR_BGR2GRAY)
    (update-channel! (fn [x] (cond
                               (< x 70) 0
                               (< x 180) 100
                               :else 255))
                     0)
    (cv/bitwise-not!)
    (show-pic! true))

;;;;;;; 创建素描效果
;; 使用bitwise-and 组合结果

(def img (cv/imread "resources/building.jpg" cv/IMREAD_REDUCED_COLOR_2))
(def img (cv/imread "resources/girl2.jpg" cv/IMREAD_REDUCED_COLOR_4))
(show-pic! img)

(def factor 1)
(def work (cv/clone img))

(dotimes [_ factor]
  (cv/pyr-down! work))

(dotimes [_ factor]
  (cv/pyr-up!work))
(show-pic! work)

(def edge
  (-> img
      (cv/clone)
      (cv/resize! (cv/new-size (.cols work) (.rows work)))
      (cv/cvt-color! cv/COLOR_RGB2GRAY)
      (cv/median-blur! 7)
      (cv/adaptive-threshold! 255 cv/ADAPTIVE_THRESH_MEAN_C cv/THRESH_BINARY 3 3)
      (cv/cvt-color! cv/COLOR_GRAY2RGB)
      ))

(show-pic! edge)

(let [result (cv/new-mat)]
  (cv/bitwise-and work edge result)
  (show-pic! result))

;;; 使用sketch!函数
(defn smoothing!
  [img factor filter-size filter-value]
  (let [work (cv/clone img)
        output (cv/new-mat)]
    (dotimes [_ factor]
      (cv/pyr-down! work))
    (cv/bilateral-filter work output filter-size filter-size filter-value)
    (dotimes [_ factor]
      (cv/pyr-up! output))
    (cv/resize! output (cv/new-size (.cols img) (.rows img)))))

(defn edges!
  [img e1 e2 e3]
  (-> img
      cv/clone
      (cv/cvt-color! cv/COLOR_RGB2GRAY)
      (cv/median-blur! e1)
      (cv/adaptive-threshold! 255 cv/ADAPTIVE_THRESH_MEAN_C cv/THRESH_BINARY e2 e3)
      (cv/cvt-color! cv/COLOR_GRAY2RGB)))

(defn sketch!
  [img s1 s2 s3 e1 e2 e3]
  (let [output (smoothing! img s1 s2 s3)
        edge (edges! img e1 e2 e3)]
    (cv/bitwise-and output edge output)
    output))

(-> (cv/imread "resources/cat.jpg" cv/IMREAD_REDUCED_COLOR_4)
    (sketch! 6 9 7 7 9 11)
    show-pic!)

;;;;;;;

(defn get-channel
  [img idx]
  (let [channels (cv/new-arraylist)]
    (cv/split img channels)
    (nth channels idx)))

;;;;;;;; 直线检测和圆检测

(def zuqiu (-> "resources/zuqiu.jpg"
               cv/imread))

(def can (-> zuqiu
             cv/clone
             (cv/cvt-color! cv/COLOR_BGR2GRAY)
             (cv/median-blur! 5)
             (cv/canny! 50. 180. 3 false)))

(show-pic! can)

(def lines (cv/new-mat))
(cv/hough-lines can lines 1 (/ Math/PI 180) 100)
;; 返回的结果是rho 和theta
(cv/dump lines)

;; 绘制结果
(def result (cv/clone zuqiu))
(dotimes [ i (.rows lines)]
  (let [ val_ (.get lines i 0)
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
             (Math/round (- y0 (* 1000 a))))
        ]
    (cv/line result pt1 pt2 color/black 1)))

(show-pic! result)
;; hough-lines-p
(def gray (-> zuqiu
              cv/clone
              (cv/cvt-color! cv/COLOR_BGR2GRAY)
              (cv/gaussian-blur! (cv/new-size 5 5) 0)))
(show-pic! gray)
(def edges (-> gray
               cv/clone
               (cv/canny! 100 200)))
(show-pic! edges)

(def rho 1)
(def theta (/ Math/PI 180))
(def min-intersections 30)
(def min-line-length 10)
(def max-line-gap 50)

(def lines (cv/new-mat))
(cv/hough-lines-p
 edges
 lines
 rho
 theta
 min-intersections
 min-line-length
 max-line-gap)

(def result (cv/clone zuqiu))
(dotimes [i (.rows lines)]
  (let [val (.get lines i 0)]
    (cv/line result
             (cv/new-point (nth val 0) (nth val 1))
             (cv/new-point (nth val 2) (nth val 3))
             color/red-2 1)))
(show-pic! result)

;; 圆检测
(def pool (-> "resources/taiqiu.jpg"
              (cv/imread)))

(defn find-circles
  ([img min-radius max-radius color] (find-circles img
                                             (-> img cv/clone (cv/cvt-color! cv/COLOR_BGR2GRAY))
                                             min-radius
                                             max-radius
                                             color))
  ([img gray min-radius max-radius color]
   (let [circles (cv/new-mat)
         _ (cv/hough-circles gray circles cv/CV_HOUGH_GRADIENT 1 min-radius 120 10 min-radius max-radius)
         output (cv/clone img)]
     (dotimes [i (.cols circles)]
       (let [[x y r] (.get circles 0 i)
             p (cv/new-point x y)]
         (cv/circle output p (int r) color 3)))
     output)))

(-> (find-circles pool 15 20 color/red-2)
    show-pic!)


;;; 识别颜色不同的圆
(def bgr-image (-> "resources/red.jpg"
                   cv/imread))

(defn in-range!
  [mat scalar1 scalar2]
  (cv/in-range mat scalar1 scalar2 mat)
  mat)

(show-pic! bgr-image)

;;过滤出红色
(def ogr-image
  (-> bgr-image
      (cv/clone)
      (cv/median-blur! 5)
      (cv/cvt-color! cv/COLOR_BGR2HSV)
      (in-range! (cv/new-scalar 0 200 200) (cv/new-scalar 5 255 255))
      (cv/gaussian-blur! (cv/new-size 9 9) 2 2)
      ))

(show-pic! ogr-image)

(-> (find-circles bgr-image ogr-image 50 100 color/green)
    show-pic!)

;;; 划线
(def zuqiuchang (-> "resources/zuqiu.jpg"
                    (cv/imread cv/IMREAD_GRAYSCALE)
                    (cv/gaussian-blur! (cv/new-size 5 5) 3)
                    (cv/canny! 50. 250.)))


(def det (cv/create-line-segment-detector))
(def lines (cv/new-mat))
(def result (cv/clone zuqiuchang))
(.detect det zuqiuchang lines)
(.drawSegments det result lines)
(show-pic! result)

;; 查找轮廓
(def head-phone (-> "resources/headphone.png"
                    (cv/imread)))
(show-pic! head-phone)

(def mask (-> head-phone
              (cv/cvt-color! cv/COLOR_BGR2GRAY)
              (cv/clone)
              (cv/threshold! 250 255 cv/THRESH_BINARY)
              (cv/median-blur! 7)))
(show-pic! mask)

(def masked-input (cv/clone head-phone))

(cv/set-to masked-input (cv/new-scalar 0 0 0) mask)
(cv/set-to masked-input (cv/new-scalar 255 255 255) (cv/bitwise-not! mask))
(show-pic! masked-input)

;; 查找轮廓
(def contours (cv/new-arraylist))
(cv/find-contours
 masked-input
 contours
 (cv/new-mat)
 cv/RETR_TREE
 cv/CHAIN_APPROX_NONE)


(def exe-1 (-> (cv/clone head-phone)
               (cv/cvt-color! cv/COLOR_GRAY2BGR)))
(doseq [c contours]
  (let [rect (cv/bounding-rect c)]
    (cv/rectangle
     exe-1
     (cv/new-point (.x rect) (.y rect))
     (cv/new-point (+ (.x rect) (.width rect))
                   (+ (.y rect) (.height rect)))
     color/green-2
     2)))

(show-pic! exe-1)

;; 过滤轮廓
(def interesting-contours
  (filter
   #(and
     (> (cv/contour-area %) 10000)
     (< (.height (cv/bounding-rect %)) (- (.height head-phone) 10)))
   contours))

(def exe-1 (-> (cv/clone head-phone)
               (cv/cvt-color! cv/COLOR_GRAY2BGR)))
(doseq [c interesting-contours]
  (let [rect (cv/bounding-rect c)]
    (cv/rectangle
     exe-1
     (cv/new-point (.x rect) (.y rect))
     (cv/new-point (+ (.x rect) (.width rect))
                   (+ (.y rect) (.height rect)))
     color/green-2
     2)))

(show-pic! exe-1)

;; 画圆
(def exe-2 (-> (cv/clone head-phone)
               (cv/cvt-color! cv/COLOR_GRAY2BGR)))
(doseq [c interesting-contours]
  (let [rect (cv/bounding-rect c)
        center (u/center-of-rect rect)]
    (cv/circle exe-2
               center
               (u/distance-of-two-points center (.tl rect))
               color/green-2
               2)))
(show-pic! exe-2)

;; 直接画轮廓
(def exe-3 (-> (cv/clone head-phone)
               (cv/cvt-color! cv/COLOR_GRAY2BGR)))
(dotimes [ci (.size interesting-contours)]
  (cv/draw-contours
   exe-3
   interesting-contours
   ci
   color/green-2
   3))
(show-pic! exe-3)

;; 轮廓示例2
(def kikyu (cv/imread "resources/sky.jpg"))
(def wrong-mask
  (-> kikyu
      cv/clone
      (cv/cvt-color! cv/COLOR_BGR2GRAY)
      (cv/threshold! 250 255 cv/THRESH_BINARY)
      (cv/median-blur! 7)))
;; 没有显示任何东西
(show-pic! wrong-mask)

(show-pic! kikyu)
;; 过滤掉蓝色
(def mask
  (-> kikyu
      (cv/clone)
      (cv/cvt-color! cv/COLOR_RGB2HSV)
      (in-range! (cv/new-scalar 10 30 30) (cv/new-scalar 30 250 250))
      (cv/median-blur! 7)
      ))
(show-pic! mask)

(def work (-> mask cv/bitwise-not!))
(show-pic! work)

(def contours (cv/new-arraylist))
(cv/find-contours work contours (cv/new-mat) cv/RETR_LIST cv/CHAIN_APPROX_SIMPLE
                  (cv/new-point 0 0))

(def output (cv/clone kikyu))
(doseq [c contours]
  (if (> (cv/contour-area c) 50)
    (let [rect (cv/bounding-rect c)]
      (if (and (> (.height rect) 40)
               (> (.width rect) 60))
        (cv/circle
         output
         (cv/new-point
          (+ (/ (.width rect) 2) (.x rect))
          (+ (.y rect) (/ (.height rect) 2)))
         (+ 5 (/ (.width rect) 2))
         rgb/tan
         5)))))

(show-pic! output)

;; 显示矩形
(def my-contours
  (filter
   #(and (> (cv/contour-area %) 50)
         (> (.height (cv/bounding-rect %)) 40)
         (> (.width (cv/bounding-rect %)) 60))
   contours))

(def output (cv/clone kikyu))
(doseq [c my-contours]
  (let [rect (cv/bounding-rect c)]
    (cv/rectangle
     output
     (cv/new-point (.x rect) (.y rect))
     (cv/new-point (+ (.width rect) (.x rect))
                   (+ (.y rect) (.height rect)))
     rgb/tan
     5)))
(show-pic! output)

;;;; 更多轮廓操作，不同的轮廓显示不同的颜色, 计算多边形
(def shapes (-> "resources/poly.jpg"
                (cv/imread)))

(def thresh (-> shapes
                cv/clone
                (cv/cvt-color! cv/COLOR_BGR2GRAY)
                (cv/threshold! 210 240 1)))
(show-pic! thresh)

(def contours (cv/new-arraylist))
(cv/find-contours thresh contours (cv/new-mat) cv/RETR_LIST cv/CHAIN_APPROX_SIMPLE)
(defn draw-contours!
  [img contours]
  (dotimes [i (.size contours)]
    (let [c (.get contours i)]
      (cv/draw-contours img contours i rgb/magenta-2 3)))
  img)

(-> shapes
    (draw-contours! contours)
    show-pic!)


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
    (case side
     1 rgb/pink
     2 rgb/magenta-
     3 rgb/green
     4 rgb/blue
     5 rgb/yellow-1-
     6 rgb/cyan-2
     rgb/orange)))

(defn draw-contours!
  [img contours]
  (doall (map-indexed (fn [idx c]
                        (cv/draw-contours img contours idx (which-color c) 3))
                      (seq contours)))
  img)

(-> shapes
    (draw-contours! contours)
    show-pic!)

;;;; 去除背景噪声
(def kernel (cv/new-mat 3 3 cv/CV_8UC1))
(-> "resources/test.jpg"
    (cv/imread)
    (get-channel 2)
    (cv/gaussian-blur! (cv/new-size 5 5) 0)
    ;(cv/morphology-ex! cv/MORPH_CLOSE kernel)
    (cv/median-blur! 5)
    (cv/threshold! 160 250 cv/THRESH_BINARY)
    (cv/canny! 50 150)
    show-pic!)

;;;;; 解决迷宫

(def maze (cv/imread "resources/maze2.png"))

(def maze (cv/imread "resources/maze3.jpg"))
(def maze-bin (-> maze
                  cv/clone
                  (cv/cvt-color! cv/COLOR_BGR2GRAY)
                  (cv/threshold! 200 255 cv/THRESH_BINARY_INV)
                  ))
(show-pic! maze-bin)
(def contours (cv/new-arraylist))
(cv/find-contours maze-bin contours (cv/new-mat) cv/RETR_EXTERNAL cv/CHAIN_APPROX_NONE)
(def res (u/mat-from maze))
(cv/set-to res rgb/green)
(draw-contours! res contours)
(show-pic! res)

(def path (u/mat-from maze-bin))
(cv/set-to path rgb/black)
(cv/draw-contours path contours 0 color/white cv/FILLED)
(cv/threshold! path 240 255 cv/THRESH_BINARY)
(show-pic! path)

(def kernel (cv/new-mat 19 19 cv/CV_8UC1))
(cv/dilate! path kernel)
(def path-erode (cv/new-mat))
(cv/erode path path-erode kernel)
(cv/absdiff path path-erode path)
(cv/threshold! path 200 250 cv/THRESH_BINARY)
(show-pic! path)

(def path-c (u/mat-from maze))
(cv/set-to path-c rgb/red-2)
;; bitwise-and!实现有问题,不符合api标准
;; (cv/bitwise-and path-c (cv/cvt-color! path cv/COLOR_GRAY2BGR) path-c)
;; (show-pic! path-c)

(let [res (cv/clone maze)]
  (cv/copy-to path-c res path)
  (show-pic! res))

