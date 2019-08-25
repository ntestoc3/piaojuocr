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
            [piaojuocr.mapviewer :as mapviewer]
            [piaojuocr.ocr-api :as ocr-api]
            [taoensso.timbre :as log]
            [piaojuocr.img :as img])
  (:use com.rpl.specter))

(defn hide! [root id]
  (-> (gui/select root [(util/->select-id id)])
      gui/hide!))

(defn show! [root id]
  (-> (gui/select root [(util/->select-id id)])
      gui/show!))

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

(defn switch-card!
  "显示table结果页面"
  [root card-id]
  (let [panel (gui/select root [:#switcher])]
    (gui/show-card! panel card-id)))

(def ocr-img (atom nil))
(defn a-ocr-req [ocr-fn result-type e]
  (try (let [root (gui/to-root e)
             table-menu-item (gui/select root [(util/->select-id :menu-show-table)])
             json-menu-item (gui/select root [(util/->select-id :menu-show-json)])
             ]
         (if-let [img (iviewer/get-image-bytes root :main-image "jpg")]
           (let [result (ocr-fn img ocr-api/options)
                 bimg (iviewer/get-image root :main-image)]
             (log/trace (str ocr-fn) "result:" result)
             (case result-type
               :table
               (do
                 (log/info (str ocr-fn) "table result words num:" (:words-result-num result))
                 (gui/selection! table-menu-item true)
                 (reset! ocr-img (img/deep-copy bimg))
                 (ocr/set-model! root :main-ocr result))
               :json
               (do
                 (log/info (str ocr-fn) "json result return.")
                 (gui/selection! json-menu-item true)
                 (mapviewer/set-model! root :main-json result))))
           (gui/alert "还没有打开图片")))
       (catch Exception e
         (log/error :a-ocr-req e))))

(def a-ocr-general (partial a-ocr-req ocr-api/general :table))
(def a-ocr-accurate-general (partial a-ocr-req ocr-api/accurate-general :table))
(def a-ocr-receipt (partial a-ocr-req ocr-api/receipt :table))

(def a-ocr-vat-invoice (partial a-ocr-req ocr-api/vat-invoice :json))

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
        a-ocr-accurate-general (gui/action :handler a-ocr-accurate-general :name "通用高精度(含位置)" :tip "识别图片中的文字-高精度版本，耗时更长(包含位置信息)")
        a-ocr-receipt (gui/action :handler a-ocr-receipt :name "通用票据识别")
        a-ocr-vat-invoice (gui/action :handler a-ocr-vat-invoice :name "增值税发票识别")
        panel-group (gui/button-group)
        a-show-table-result (gui/radio-menu-item :group panel-group
                                                 :id :menu-show-table
                                                 :selected? true
                                                 :text "table结果页")
        a-show-json-result (gui/radio-menu-item :group panel-group
                                                :id :menu-show-json
                                                :text "json结果页")
        a-ocr-restore (gui/action :handler a-ocr-restore :name "还原文字标记图片" :tip "还原画了方框的图片")
        ]
    (bind/bind
     (bind/property a-pic-auto-update :selected?)
     auto-update)
    (gui/menubar
     :items [(gui/menu :text "文件" :items [a-open a-save a-exit])
             (gui/menu :text "图片" :items [a-pic-edit a-pic-update a-pic-auto-update])
             (gui/menu :text "OCR" :items [a-ocr-general
                                           a-ocr-accurate-general
                                           a-ocr-receipt
                                           :separator
                                           a-ocr-vat-invoice
                                           :separator
                                           a-show-table-result
                                           a-show-json-result
                                           :separator
                                           a-ocr-restore])])))

(defn make-pic-ocr-view [frame]
  (let [img-panel (iviewer/make-pic-viewer :main-image)
        ocr-panel (ocr/make-view [] :main-ocr)
        json-panel (mapviewer/make-view nil :main-json)]
    (gui/hide! json-panel)
    (gui/left-right-split img-panel
                          (gui/horizontal-panel :items [(gui/card-panel
                                                         :id :switcher
                                                         :items [[ocr-panel :table]
                                                                 [json-panel :json]])])
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
  (let [ocr-tbl (gui/select root [(util/->select-id :main-ocr)])
        table-menu-item (gui/select root [(util/->select-id :menu-show-table)])
        json-menu-item (gui/select root [(util/->select-id :menu-show-json)])
        ]
    (bind/bind
     (bind/selection ocr-tbl)
     (bind/transform
      #(tbl-sel-draw! root ocr-tbl %1)))
    (bind/bind
     (bind/selection table-menu-item)
     (bind/transform (fn [_] (switch-card! root :table))))
    (bind/bind
     (bind/selection json-menu-item)
     (bind/transform (fn [_] (switch-card! root :json)))
     )))

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
            :menubar (make-menus))
         cb (img/image-callback {:fn-updated (make-imagej-updated-cb f)})]
     (gui/listen f :window-closing
                 (fn [e]
                   (log/info "close frame window.")
                   (if (img/close-all!)
                     (do
                       (gui/config! f :on-close :exit)
                       ;;(gui/config! f :on-close :dispose)
                       (img/remove-image-callback cb)
                       (config/save-config!)
                       (log/info "exit over."))
                     (do
                       (gui/config! f :on-close :nothing)
                       (log/info "exit canceled.")))))
     (img/add-image-callback cb)
     (gui/config! f :content (make-main-view f))
     (gui/invoke-later
      (add-behaviors f))
     f)))

(defn show-frame [f]
  (-> f gui/pack! gui/show!))

(comment
  (def f1 (-> (make-frame)
              show-frame))

  )
