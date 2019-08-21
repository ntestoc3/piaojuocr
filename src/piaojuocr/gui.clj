(ns piaojuocr.gui
  (:require [opencv4.core :as cv]
            [opencv4.utils :as u]
            [opencv4.colors.rgb :as rgb]
            [opencv4.colors.html :as html]
            [seesaw.core :as gui]
            [seesaw.icon :as icon]
            [seesaw.bind :as bind]
            [seesaw.mig :refer [mig-panel]]
            [piaojuocr.ocr :as ocr]
            [taoensso.timbre :as log]
            [opencv4.colors.rgb :as color])
  (:import java.awt.Color)
  (:use seesaw.chooser)
  )

(gui/native!)

(defn replace-keyword [f kw]
  "修改关键字，f为 str -> str"
  (-> (name kw)
      f
      keyword))

(def ->select-id "转换为id选择器" (partial replace-keyword #(str "#" %1)))

(defn rgb->hsv
  [r g b]
  (let [mat (cv/new-mat 1 1 cv/CV_8UC3 (cv/new-scalar b g r))]
    (cv/cvt-color! mat cv/COLOR_BGR2HSV)
    (map int (.get mat 0 0))))

(defprotocol MatView
  (set-image [this mat] [this mat gray])
  (get-image [this])
  (viewer [this]))

(deftype CVImageViewer [img! control]
  MatView
  (set-image [this mat] (set-image this mat false))
  (set-image [_ mat gray]
    (let [mat2 (if gray
                 (-> mat
                     cv/clone
                     (cv/cvt-color! cv/COLOR_GRAY2BGR))
                 mat)]
      (reset! img! (u/mat-to-buffered-image mat2))))

  (get-image [_] @img!)

  (viewer [_] control))

(defn make-pic-viewer [id]
  (let [rgb-txt (gui/label :id (replace-keyword #(str %1 "-txt") id)
                           :text "X: Y: R: G: B: H: S: V:") ;; 必须是rgb图片，值才准确
        img (atom nil)
        pic (gui/label :text ""
                       :id id
                       :halign :left
                       :valign :top
                       :background :white
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
    (->CVImageViewer img (gui/vertical-panel
                          :items [rgb-txt
                                  (gui/scrollable pic)]))))


(defn show-pic!
  "显示图片，gray表示是否为灰度图"
  ([mat] (show-pic! mat false))
  ([mat gray]
   (let [f (gui/frame :title "pic view")
         image (make-pic-viewer :test)]
     (set-image image mat gray)
     (gui/config! f :content (viewer image))
     (-> f gui/pack! gui/show!))))

(defn choose-pic
  "选择图片文件,返回文件路径"
  ([] (choose-pic :open))
  ([type]
   (choose-file :filters [["图片"  ["png" "jpeg" "jpg"]]]
                :all-files? true
                :remember-directory? true
                :type type
                :success-fn (fn [_ x] (let [p  (.getAbsolutePath x)]
                                        (if (re-find #"\.(jpg|png|jpeg|bmp)$" p)
                                          p
                                          (do (gui/alert (str p "不是一个图片文件!"))
                                              (choose-pic type))))))))

(def states (atom {}))
(defn set-current-img-path [path]
  (swap! states assoc :current-img-path path))

(defn set-current-mat [mat]
  (swap! states assoc :current-mat mat))

(defn a-open [e]
  (when-let [f (choose-pic)]
    (set-current-img-path f)))

(defn a-save [e]
  (when-let [f (choose-pic :save)]
    (-> @states
        :current-mat
        (cv/imwrite f))))

(defn a-exit  [e] (gui/dispose! e))

(def menus
  (let [a-open (gui/action :handler a-open :name "打开" :tip "打开图片文件" :key "menu O")
        a-save (gui/action :handler a-save :name "保存" :tip "保存当前图片" :key "menu S")
        a-exit (gui/action :handler a-exit :anem "退出" :tip "退出程序" :key "menu X")]
    (gui/menubar
     :items [(gui/menu :text "文件" :items [a-open a-save a-exit])])))

(defn make-main-panel []
  (mig/mig-panel
   :constraints ["fill, ins 0"]
   :items [(gui/scrollable )]))

(defn show-frame []
  (let [frame (gui/frame
               :title "文字识别测试"
               :content (make-main-panel)
               :menubar menus)])
  )

(comment
  (show-pic! (cv/imread "output.jpg"))

  (choose-pic)

  (choose-pic :save)

  )
