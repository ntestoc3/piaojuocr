(ns piaojuocr.gui
  (:require [seesaw.core :as gui]
            [seesaw.icon :as icon]
            [seesaw.bind :as bind]
            [seesaw.table :as table]
            [seesaw.mig :refer [mig-panel]]
            [piaojuocr.viewer :as iviewer]
            [piaojuocr.logging :as logging]
            [piaojuocr.setting :as setting]
            [piaojuocr.config :as config]
            [piaojuocr.util :as util]
            [piaojuocr.theme :as theme]
            [piaojuocr.ocr :as ocr]
            [piaojuocr.ocr-api :as ocr-api]
            [taoensso.timbre :as log]
            [piaojuocr.img :as img])
  (:use com.rpl.specter))

(defn a-open [e]
  (when-let [f (iviewer/choose-pic)]
    (log/debug "open new image file:" f)
    (let [root (gui/to-root e)
          img (iviewer/read-image f)]
      (iviewer/set-image! root :main-image img))))

(defn a-save [e]
  (when-let [f (iviewer/choose-pic :save)]
    (log/debug "saving image file:" f)
    (iviewer/save-image (gui/to-root e)
                        :main-image
                        f)))

(defn a-exit  [e] (gui/dispose! e))

(def curr-img (atom nil))

(defn a-pic-edit [e]
  (let [root (gui/to-root e)]
    (if-let [img (iviewer/get-image root :main-image)]
      (let [img (img/show-image "编辑图片" img)]
        (log/debug "edit pic.")
        (reset! curr-img img))
      (gui/alert "还没有打开文件"))))

(def auto-update (atom (config/get-config :auto-update true)))

(defn make-imagej-updated-cb [root]
  (fn [imp]
    (when @auto-update
      (log/info "image updated.")
      (gui/invoke-later
       (->> (img/get-image imp)
            (iviewer/set-image! root :main-image))))))

(defn a-pic-update [e]
  (if-let [img @curr-img]
    (->> (img/get-image img)
         (iviewer/set-image! (gui/to-root e)
                             :main-image))
    (gui/alert "没有编辑的图片")))

(def ocr-img (atom nil))
(defn a-ocr-general [e]
  (try (let [root (gui/to-root e)]
         (if-let [img (iviewer/get-image-bytes root :main-image "jpg")]
           (let [result (ocr-api/general img ocr-api/options)
                 bimg (iviewer/get-image root :main-image)]
             (log/info "ocr general result words num" (:words-result-num result))
             (log/trace "ocr general result:" result)
             (reset! ocr-img (img/deep-copy bimg))
             (ocr/set-model! root :main-ocr result))
           (gui/alert "还没有打开图片")))
       (catch Exception e
         (log/error :a-ocr-general e))))

(defn a-ocr-restore [e]
  (when-let [img @ocr-img]
    (log/trace :a-ocr-restore)
    (iviewer/set-image! (gui/to-root e) :main-image img)))

(defn make-menus []
  (let [a-open (gui/action :handler a-open :name "打开" :tip "打开图片文件" :key "menu O")
        a-save (gui/action :handler a-save :name "保存" :tip "保存当前图片" :key "menu S")
        a-exit (gui/action :handler a-exit :name"退出" :tip "退出程序" :key "menu X")
        a-pic-edit (gui/action :handler a-pic-edit :name "编辑图片" :tip "使用ImageJ编辑图片" :key "menu E")
        a-pic-update (gui/action :handler a-pic-update :name "更新图片" :tip "更新显示编辑后的图片")
        a-pic-auto-update (gui/checkbox-menu-item :text "自动更新图片"
                                                  :selected? @auto-update)
        a-ocr-general (gui/action :handler a-ocr-general :name "通用(含位置)" :tip "识别图片中的文字(包含位置信息)")
        a-ocr-restore (gui/action :handler a-ocr-restore :name "还原文字标记图片" :tip "还原画了方框的图片")]
    (bind/bind
     (bind/property a-pic-auto-update :selected?)
     auto-update)
    (gui/menubar
     :items [(gui/menu :text "文件" :items [a-open a-save a-exit])
             (gui/menu :text "图片" :items [a-pic-edit a-pic-update a-pic-auto-update])
             (gui/menu :text "OCR" :items [a-ocr-general a-ocr-restore])])))

(defn make-pic-ocr-view [frame]
  (let [img-panel (iviewer/make-pic-viewer :main-image)
        ocr-panel (ocr/make-view [] :main-ocr)]
    (gui/left-right-split img-panel ocr-panel
                          :divider-location 0.5)))


(defn tbl-sel-draw! [root ocr-tbl e]
  (when-some [sels (gui/selection ocr-tbl {:multi? true})]
    (let [new-img (img/deep-copy @ocr-img)]
      (log/info :tbl-sel-draw! (count sels))
      (iviewer/set-image! root :main-image new-img)
      (->> (map #(table/value-at ocr-tbl %1) sels)
           vec
           (transform [ALL] (fn [loc]
                              [(:left loc)
                               (:top loc)
                               (:width loc)
                               (:height loc)]))
           (iviewer/draw-rects! root :main-image)))))

(defn add-behaviors
  [root]
  (let [ocr-tbl (gui/select root [(util/->select-id :main-ocr)])]
    (bind/bind
     (bind/selection ocr-tbl)
     (bind/transform
      #(tbl-sel-draw! root ocr-tbl %1)))))

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
            :on-close :exit
            :menubar (make-menus))
         cb (img/image-callback {:fn-updated (make-imagej-updated-cb f)})]
     (gui/listen f :window-closing
                 (fn [e]
                   (log/info "close frame window.")
                   (img/remove-image-callback cb)
                   (config/save-config!)
                   (log/info "exit over.")))
     (img/add-image-callback cb)
     (gui/config! f :content (make-main-view f))
     (add-behaviors f)
     f)))

(defn show-frame [f]
  (-> f gui/pack! gui/show!))

(comment
  (def f1 (-> (make-frame)
              show-frame))

  )
