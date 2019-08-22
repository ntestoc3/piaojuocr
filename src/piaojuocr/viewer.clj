(ns piaojuocr.viewer
  (:require [opencv4.core :as cv]
            [opencv4.utils :as u]
            [opencv4.colors.rgb :as rgb]
            [opencv4.colors.html :as html]
            [seesaw.core :as gui]
            [seesaw.icon :as icon]
            [seesaw.bind :as bind]
            [seesaw.mig :refer [mig-panel]]
            [piaojuocr.config :as config]
            [taoensso.timbre :as log])
  (:import java.awt.Color
           [javax.imageio ImageIO])
  (:use seesaw.chooser
        piaojuocr.util)
  )

(defn rgb->hsv
  "rgb色彩空间转换到hsv色彩空间"
  [r g b]
  (let [mat (cv/new-mat 1 1 cv/CV_8UC3 (cv/new-scalar b g r))]
    (cv/cvt-color! mat cv/COLOR_BGR2HSV)
    (map int (.get mat 0 0))))

(defn make-pic-viewer [id]
  (let [rgb-txt (gui/label :id (replace-keyword #(str %1 "-txt") id)
                           :text "X: Y: R: G: B: H: S: V:") ;; 必须是rgb图片，值才准确
        img (atom nil)
        pic (gui/label :text ""
                       :id id
                       :halign :left
                       :valign :top
                       :background :white
                       :user-data img
                       :listen [:mouse-motion
                                (fn [e]
                                  (let [x (.getX e)
                                        y (.getY e)]
                                    (when-let [img @img]
                                      (when (and (< x (.getWidth img))
                                                 (< y (.getHeight img)))
                                        (let [color (-> (.getRGB img x y)
                                                        (Color.  true))
                                              r (.getRed color)
                                              g (.getGreen color)
                                              b (.getBlue color)
                                              [h s v] (rgb->hsv r g b)]
                                          (gui/text! rgb-txt (format "X: %d Y: %d, R: %d G: %d B: %d, H: %d S: %d V: %d"
                                                                     x y r g b h s v)))))))])]
    (bind/bind img
               (bind/transform gui/icon)
               (bind/property pic :icon))
    (gui/vertical-panel
     :items [rgb-txt
             (gui/scrollable pic)])))

(defn set-image!
  ([root id mat] (set-image! root id mat false))
  ([root id mat gray]
   (when-let [img! (some-> (gui/select root [(->select-id id)])
                           gui/user-data)]
     (let [mat2 (if gray
                  (-> mat
                      cv/clone
                      (cv/cvt-color! cv/COLOR_GRAY2BGR))
                  mat)]
       (reset! img! (u/mat-to-buffered-image mat2))))))

(defn get-image
  [root id]
  (some-> (gui/select root [(->select-id id)])
          gui/user-data
          deref))

(defn img->bytes [img]
  (let [baos (java.io.ByteArrayOutputStream.)]
    (ImageIO/write img "jpg" baos)
    (.toByteArray baos)))

(defn get-image-bytes
  "获得当前显示图片的字节数组"
  [root id]
  (some-> (get-image root id)
          img->bytes))

(defn show-pic!
  "显示图片，gray表示是否为灰度图"
  ([mat] (show-pic! mat false))
  ([mat gray]
   (let [f (gui/frame :title "pic view")
         image (make-pic-viewer :test)]
     (gui/config! f :content image)
     (set-image! f :test mat gray)
     (-> f gui/pack! gui/show!)
     f)))

(defn choose-pic
  "选择图片文件,返回文件路径"
  ([] (choose-pic :open))
  ([type]
   (choose-file :filters [["图片"  ["png" "jpeg" "jpg"]]]
                :all-files? true
                :dir (config/get-config :last-choose-dir "./")
                :type type
                :success-fn (fn [_ x] (let [p  (.getAbsolutePath x)]
                                        (->> (.getParent x)
                                             (config/add-config! :last-choose-dir))
                                        (if (re-find #"\.(jpg|png|jpeg|bmp)$" p)
                                          p
                                          (do (gui/alert (str p "不是一个图片文件!"))
                                              (choose-pic type))))))))

(defn open-img
  ([]
   (when-let [path (choose-pic)]
     (cv/imread path)))
  ([type]
   (when-let [path (choose-pic)]
     (cv/imread path type))))

(defn save-img
  "保存图片"
  ([mat]
   (when-let [path (choose-pic)]
     (cv/imwrite mat path)))
  ([mat path]
   (cv/imwrite mat path)))

(comment

  (def f1 (some-> (open-img)
                  show-pic!))

  (set-image! f1 :test (open-img))

  (choose-pic)

  (choose-pic :save)

  )
