(ns piaojuocr.img

  (:import (ij ImageJ IJ ImagePlus)
           java.awt.Color)
  )

(defonce ij-main (atom nil))

(defn main-window []
  (let [main @ij-main]
    (if (or (nil? main)
            (.quitting main))
      (reset! ij-main (ImageJ.))
      main)))

(def img (IJ/openImage "code.jpg"))

(.show img)
(.setColor img Color/RED)
(.setRoi img 42 45 30 50)
(.draw img 42 45 30 50)

(IJ/save img "a2aa.jpg")
