(ns piaojuocr.img
  (:import (ij ImageJ IJ ImagePlus ImageListener)
           [java.awt BasicStroke Color]
           java.awt.image.BufferedImage))

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

(defn- open-window-show-img [img]
  (main-window)
  (.show img)
  img)

(defn image-callback
  [{:keys [fn-closed fn-opened fn-updated]}]
  (reify ImageListener
    (imageClosed [_ imp]
      (when fn-closed
        (fn-closed imp))
      nil)
    (imageOpened [_ imp]
      (when fn-opened
        (fn-opened imp))
      nil)
    (imageUpdated [_ imp]
      (when fn-updated
        (fn-updated imp))
      nil)))

(defn add-image-callback
  "添加ImageJ全局事件"
  [callback]
  (ImagePlus/addImageListener callback))

(defn remove-image-callback
  "移除ImageJ全局事件"
  [callback]
  (ImagePlus/removeImageListener callback))

(defn show-image
  "使用ImageJ显示图片,返回打开的图片"
  ([file-path]
   (-> (ImagePlus. file-path)
       open-window-show-img))
  ([title image]
   (-> (ImagePlus. title image)
       open-window-show-img)))

(defn set-image
  [imp img]
  (.setImage imp img))

(defn get-image
  "获取awt image"
  [imp]
  (.getImage imp)
  ;;(.getBufferedImage imp)
  )

(comment
  (def pic1 (show-img "code.jpg"))

  (def cb (image-callback {:fn-opened (fn [i]
                                        (println "opened call back")
                                        )
                           :fn-updated (fn [i]
                                         (println "update call back"))}))

  (add-image-callback cb)

  (remove-image-callback cb)

  )
