(ns piaojuocr.dft
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

(defn image-size-optimize!
  [img]
  (let [rows (.rows img)
        cols (.cols img)
        new-rows (cv/get-optimal-dft-size rows)
        new-cols (cv/get-optimal-dft-size cols)]
    (cv/copy-make-border! img
                          0 (- new-rows rows)
                          0 (- new-cols cols)
                          cv/BORDER_CONSTANT)))

(def lemu (cv/imread "resources/lemu.jpg"))
(-> lemu
    cv/clone
    image-size-optimize!
    (show-pic!))

(defn add-image-text
  [img text]
  )
