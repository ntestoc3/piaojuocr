(ns piaojuocr.img
  (:import (ij ImageJ IJ ImagePlus)
           [java.awt BasicStroke Color]
           java.awt.image.BufferedImage)
  )

(defn draw-rect!
  "画矩形"
  ([^BufferedImage img x y width height] (draw-rect img x y width height 2))
  ([^BufferedImage img x y width height thickness]
   (let [g2d (.createGraphics img)]
     (doto g2d
       (.setColor Color/RED)
       (.setStroke (BasicStroke. thickness))
       (.drawRect x y width height)))))

(defonce ij-main (atom nil))

(defn main-window []
  (let [main @ij-main]
    (if (or (nil? main)
            (.quitting main))
      (reset! ij-main (ImageJ.))
      main)))

;; (def img (IJ/openImage "code.jpg"))

;; (.show img)
;; (.setColor img Color/RED)
;; (.setRoi img 42 45 30 50)
;; (.draw img 42 45 30 50)

;; (IJ/save img "a2aa.jpg")
