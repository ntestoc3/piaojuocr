(ns piaojuocr.gui
  (:require [opencv4.core :as cv]
            [opencv4.utils :as u]
            [opencv4.colors.rgb :as rgb]
            [opencv4.colors.html :as html]
            [seesaw.core :as gui]
            [seesaw.icon :as icon]
            [seesaw.bind :as bind]
            [seesaw.mig :refer [mig-panel]]
            [piaojuocr.viewer :as iviewer]
            [piaojuocr.setting :as setting]
            [taoensso.timbre :as log]
            [opencv4.colors.rgb :as color])
  )


(def states "保存gui状态信息" (atom {}))

(defn set-current-img-path [path]
  (swap! states assoc :current-img-path path))

(defn set-current-mat [mat]
  (swap! states assoc :current-mat mat))

(defn a-open [e]
  (when-let [f (iviewer/choose-pic)]
    (set-current-img-path f)))

(defn a-save [e]
  (when-let [f (iviewer/choose-pic :save)]
    (-> @states
        :current-mat
        (cv/imwrite f))))

(defn a-exit  [e] (gui/dispose! e))

(def menus
  (let [a-open (gui/action :handler a-open :name "打开" :tip "打开图片文件" :key "menu O")
        a-save (gui/action :handler a-save :name "保存" :tip "保存当前图片" :key "menu S")
        a-exit (gui/action :handler a-exit :name"退出" :tip "退出程序" :key "menu X")]
    (gui/menubar
     :items [(gui/menu :text "文件" :items [a-open a-save a-exit])])))

(defn make-main-panel []
  (let [img-panel (iviewer/make-pic-viewer :main-image)]
    (mig-panel
    :constraints ["fill, ins 0"]
    :items [(gui/scrollable img-panel)])))

(defn add-behaviors
  [root]
  )

(defn show-frame []
  (let [frame (gui/frame
               :title "文字识别测试"
               :menubar menus)]
    (gui/config! frame :content (setting/make-view frame))  ; (make-main-panel)
    (-> frame gui/pack! gui/show!)))



(comment

  (show-frame)

  )
