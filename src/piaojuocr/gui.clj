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
            [piaojuocr.logging :as logging]
            [piaojuocr.setting :as setting]
            [piaojuocr.config :as config]
            [piaojuocr.theme :as theme]
            [taoensso.timbre :as log]
            [opencv4.colors.rgb :as color])
  )


(def current-img-path "当前图片路径" (atom nil))
(def current-mat "当前显示的矩阵" (atom nil))

(defn set-current-img-path [path]
  (reset! current-img-path path))

(defn set-current-mat [mat]
  (reset! current-mat mat))

(defn a-open [e]
  (when-let [f (iviewer/choose-pic)]
    (set-current-img-path f)))

(defn a-save [e]
  (when-let [f (iviewer/choose-pic :save)]
    (-> @current-mat
        (cv/imwrite f))))

(defn a-exit  [e] (gui/dispose! e))

(defn make-menus []
  (let [a-open (gui/action :handler a-open :name "打开" :tip "打开图片文件" :key "menu O")
        a-save (gui/action :handler a-save :name "保存" :tip "保存当前图片" :key "menu S")
        a-exit (gui/action :handler a-exit :name"退出" :tip "退出程序" :key "menu X")]
    (gui/menubar
     :items [(gui/menu :text "文件" :items [a-open a-save a-exit])])))

(defn make-pic-ocr-view [frame]
  (let [img-panel (iviewer/make-pic-viewer :main-image)]
    (gui/border-panel
     :center (gui/scrollable img-panel))))

(defn add-behaviors
  [root]
  (bind/bind current-img-path
             (bind/transform cv/imread)
             (bind/transform (fn [mat]
                               (iviewer/set-image! root :main-image mat)))))

(defn make-main-view [frame]
  (gui/tabbed-panel :placement :top :overflow :scroll
                    :tabs [{:title "图片处理"
                            :tip "图片处理与ocr功能"
                            :content (make-pic-ocr-view frame)}
                           {:title "设置"
                            :tip "系统设置"
                            :content (setting/make-view frame)}
                           {:title "日志"
                            :tip "日志面板"
                            :content (logging/make-view frame)}]))

(defn make-frame []
  (theme/wrap-theme
   (let [f (gui/frame
            :title "文字识别测试"
            :menubar (make-menus)
            :listen [:window-closing (fn [e]
                                       (log/info "close frame window.")
                                       (config/save-config!)
                                       (log/info "exit over."))])]
     (add-behaviors f)
     (gui/config! f :content (make-main-view f)))))

(defn show-frame [f]
  (-> f gui/pack! gui/show!))

(comment
  (-> (make-frame)
      show-frame)

  )
