(ns piaojuocr.viewer
  (:require [seesaw.core :as gui]
            [seesaw.bind :as bind]
            [seesaw.mig :refer [mig-panel]]
            [seesaw.behave :as behave]
            [piaojuocr.config :as config]
            [piaojuocr.img :as img]
            [taoensso.timbre :as log]
            [me.raynes.fs :as fs])
  (:import java.awt.Color
           [javax.imageio ImageIO])
  (:use seesaw.chooser
        piaojuocr.util)
  )

(defn get-icon-image
  [id]
  (some-> (gui/config id :icon)
          .getImage))

(defn pan [view-to-pan dx dy]
  (let [^javax.swing.JViewport  viewport (.. view-to-pan getParent)
        rect      (.getViewRect viewport)
        full-size (.getViewSize viewport)
        [x y w h] [(.x rect) (.y rect) (.width rect) (.height rect)]
        new-x (Math/min (Math/max 0 (+ x (int dx))) (- (.width full-size) w))
        new-y (Math/min (Math/max 0 (+ y (int dy))) (- (.height full-size) h))]
    (.setViewPosition viewport (java.awt.Point. new-x new-y))))

(defn- calculate-scales [panner view-to-pan]
  [(/ (.getWidth view-to-pan) (.getWidth panner))
   (/ (.getHeight view-to-pan) (.getHeight panner))])

(defn pan-on-drag
  "speed 正值按照滚动条方向，负值按鼠标移动方向"
  ([view-to-pan & {:keys [panner speed] :or {panner view-to-pan speed 1.0}}]
   (behave/when-mouse-dragged panner
                              :drag (fn [e [dx dy]]
                                      (let [[sx sy] (calculate-scales panner view-to-pan)]
                                        (pan view-to-pan (* dx sx speed) (* dy sy speed)))))))

(defn make-pic-viewer [id]
  (let [rgb-txt (gui/label :id (replace-keyword #(str %1 "-txt") id)
                           :text "X: Y: R: G: B:") ;; 必须是rgb图片，值才准确
        pic (gui/label :text ""
                       :id id
                       :halign :left
                       :valign :top
                       :background :white
                       :listen [:mouse-motion
                                (fn [e]
                                  (let [x (.getX e)
                                        y (.getY e)]
                                    (when-let [img (get-icon-image e)]
                                      (when (and (< x (.getWidth img))
                                                 (< y (.getHeight img)))
                                        (let [color (-> (.getRGB img x y)
                                                        (Color.  true))
                                              r (.getRed color)
                                              g (.getGreen color)
                                              b (.getBlue color)]
                                          (gui/text! rgb-txt (format "X: %d Y: %d, R: %d G: %d B: %d"
                                                                     x y r g b)))))))])]
    (pan-on-drag pic :speed -0.8)
    (gui/vertical-panel
     :items [rgb-txt
             (gui/scrollable pic)])))

(defn set-image!
  "设置当前图片"
  ([root id image]
   (let [ico (gui/icon image)]
     (some-> (gui/select root [(->select-id id)])
             (gui/config! :icon ico)))))

(defn draw-rect!
  [root id x y width height]
  (let [lbl (gui/select root [(->select-id id)])
        img (get-icon-image lbl)]
    (img/draw-rect! img x y width height)
    (gui/repaint! lbl)))

(defn draw-rects!
  "rects格式为[[x y width height]...]"
  [root id rects]
  (let [lbl (gui/select root [(->select-id id)])
        img (get-icon-image lbl)]
    (doseq [[x y width height] rects]
      (img/draw-rect! img x y width height))
    (gui/repaint! lbl)))

(defn get-image
  "获取当前图片"
  [root id]
  (some-> (gui/select root [(->select-id id)])
          get-icon-image))

(defn- img->bytes [img format]
  (let [baos (java.io.ByteArrayOutputStream.)]
    (ImageIO/write img format baos)
    (.toByteArray baos)))

(defn get-image-bytes
  "获得当前显示图片的字节数组"
  [root id format]
  (some-> (get-image root id)
          (img->bytes format)))

(defn- file-format
  "返回文件格式"
  [file-path]
  (-> (fs/extension file-path)
      (subs 1)))

(defn save-image
  "保存图片"
  [root id file-path]
  (let [ext (file-format file-path)]
    (some-> (get-image root id)
            (ImageIO/write ext (fs/file file-path)))))

(defn show-pic
  "显示图片"
  [path]
  (let [f (gui/frame :title "pic viewer")
        image (make-pic-viewer :test)]
    (gui/config! f :content image)
    (set-image! f :test path)
    (-> f gui/pack! gui/show!)
    f))

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

(defn read-image
  "从文件读取图片"
  [file]
  (-> (fs/file file)
      ImageIO/read))

(comment

  (gui/native!)

  (def f1 (some-> (choose-pic)
                  read-image
                  show-pic))

  (type (get-image f1 :test))


  (choose-pic)

  (choose-pic :save)

  )
