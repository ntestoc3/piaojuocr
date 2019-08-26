(ns piaojuocr.gui
  (:require [seesaw.core :as gui]
            [seesaw.icon :as icon]
            [seesaw.bind :as bind]
            [seesaw.table :as table]
            [seesaw.mig :refer [mig-panel]]
            [piaojuocr.viewer :as iviewer]
            [piaojuocr.logging :as logging]
            [piaojuocr.setting :as setting]
            [piaojuocr.iocr :as iocr]
            [piaojuocr.config :as config]
            [piaojuocr.util :as util]
            [piaojuocr.theme :as theme]
            [piaojuocr.ocr :as ocr]
            [piaojuocr.ocr-table :as ocr-table]
            [piaojuocr.mapviewer :as mapviewer]
            [piaojuocr.ocr-api :as ocr-api]
            [taoensso.timbre :as log]
            [piaojuocr.img :as img]
            [piaojuocr.ocr-api :as api]
            [clojure.java.io :as io])
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

(defn switch-card!
  "显示table结果页面"
  [root card-id]
  (let [panel (gui/select root [:#switcher])]
    (gui/show-card! panel card-id)))

(def ocr-img (atom nil))
(defn a-ocr-req
  "请求ocr函数，并对结果进行处理"
  [ocr-fn args result-type e]
  (try (let [root (gui/to-root e)
             location-menu-item (gui/select root [(util/->select-id :menu-show-location)])
             json-menu-item (gui/select root [(util/->select-id :menu-show-json)])
             table-menu-item (gui/select root [(util/->select-id :menu-show-table)])
             iocr-menu-item (gui/select root [(util/->select-id :menu-show-iocr)])
             ]
         (if-let [img (iviewer/get-image-bytes root :main-image "jpg")]
           (let [result (apply ocr-fn img args)
                 bimg (iviewer/get-image root :main-image)]
             (log/trace (str ocr-fn) "result:" result)
             (case result-type
               :location
               (do
                 (log/info (str ocr-fn) "words location result count:" (:words-result-num result))
                 (gui/selection! location-menu-item true)
                 (reset! ocr-img (img/deep-copy bimg))
                 (ocr/set-model! root :main-ocr (ocr/make-ocr-model result)))
               :json
               (do
                 (log/info (str ocr-fn) "json return.")
                 (gui/selection! json-menu-item true)
                 (mapviewer/set-model! root :main-json result))
               :table
               (do
                 (log/info (str ocr-fn) "table result form-num:" (:form_num result))
                 (gui/selection! table-menu-item true)
                 (reset! ocr-img (img/deep-copy bimg))
                 (ocr-table/set-model! root :main-ocr-table result))
               :iocr
               (do
                 (log/info (str ocr-fn) "iocr result count:" (count result))
                 (gui/selection! iocr-menu-item true)
                 (reset! ocr-img (img/deep-copy bimg))
                 (ocr/set-model! root :main-iocr (ocr/make-iocr-model result)))
               (log/error :a-ocr-req "unknown result type" result-type)))
           (gui/alert "还没有打开图片")))
       (catch Exception e
         (log/error :a-ocr-req e))))

(defn ocr-action
  "定义ocr识别动作,如果不指定item-name,api-fn必须使用#'fn-name 的方式传递
  使用doc作为item-name"
  ([api-fn result-type]
   (ocr-action api-fn (-> api-fn meta :doc) result-type))

  ([api-fn item-name result-type]
   (ocr-action api-fn [api/options] item-name result-type))

  ([api-fn args item-name result-type]
   (let [my-fn #(a-ocr-req api-fn args result-type %1)]
     (gui/action :handler my-fn :name item-name))))



(defn a-ocr-restore [e]
  (when-let [img @ocr-img]
    (log/trace :a-ocr-restore)
    (iviewer/set-image! (gui/to-root e) :main-image img)))

(defn a-iocr-action [e]
  (let [options (iocr/make-template-dlg :iocr-dlg)]
    (when options
      (a-ocr-req api/custom [options] :iocr e))))

(defn make-menus []
  (let [a-open (gui/action :handler a-open :name "打开" :tip "打开图片文件" :key "menu O")
        a-save (gui/action :handler a-save :name "保存" :tip "保存当前图片" :key "menu S")
        a-exit (gui/action :handler a-exit :name"退出" :tip "退出程序" :key "menu X")
        a-pic-edit (gui/action :handler a-pic-edit :name "编辑图片" :tip "使用ImageJ编辑图片" :key "menu E")
        a-pic-update (gui/action :handler a-pic-update :name "更新图片" :tip "更新显示编辑后的图片")
        a-pic-auto-update (gui/checkbox-menu-item :text "自动更新图片"
                                                  :selected? @auto-update)
        panel-group (gui/button-group)
        a-show-location-result (gui/radio-menu-item :group panel-group
                                                    :id :menu-show-location
                                                    :selected? true
                                                    :text "带位置单词结果页")
        a-show-json-result (gui/radio-menu-item :group panel-group
                                                :id :menu-show-json
                                                :text "json结果页")
        a-show-table-result (gui/radio-menu-item :group panel-group
                                                 :id :menu-show-table
                                                 :text "表格结果页")
        a-show-iocr-result (gui/radio-menu-item :group panel-group
                                                :id :menu-show-iocr
                                                :text "模板识别结果页")
        a-ocr-restore (gui/action :handler a-ocr-restore :name "还原文字标记图片" :tip "还原画了方框的图片")
        ]
    (bind/bind
     (bind/property a-pic-auto-update :selected?)
     auto-update)
    (gui/menubar
     :items [(gui/menu :text "文件" :items [a-open a-save a-exit])
             (gui/menu :text "图片" :items [a-pic-edit a-pic-update a-pic-auto-update])
             (gui/menu :text "OCR" :items [
                                           (ocr-action #'ocr-api/general :location)
                                           (ocr-action #'ocr-api/accurate-general :location)
                                           (ocr-action #'ocr-api/receipt  :location)
                                           :separator
                                           (ocr-action #'ocr-api/table-recognize-to-json nil "表格识别" :table)
                                           (ocr-action #'ocr-api/vat-invoice :json)
                                           :separator
                                           (gui/action :handler a-iocr-action :name "iocr模板识别")
                                           :separator
                                           (ocr-action #'ocr-api/passport :json)
                                           (ocr-action #'ocr-api/taxi-receipt :json)
                                           (ocr-action #'ocr-api/train-ticket :json)
                                           (ocr-action #'ocr-api/business-license :json)
                                           (ocr-action #'ocr-api/plate-license :json)
                                           (ocr-action #'ocr-api/driving-license :json)
                                           (ocr-action #'ocr-api/bankcard :json)
                                           :separator
                                           a-show-location-result
                                           a-show-json-result
                                           a-show-table-result
                                           a-show-iocr-result
                                           :separator
                                           a-ocr-restore])])))

(defn make-pic-ocr-view [frame]
  "创建识别页面视图"
  (let [img-panel (iviewer/make-pic-viewer :main-image)
        ocr-panel (ocr/make-view (ocr/make-ocr-model []) :main-ocr)
        json-panel (mapviewer/make-view nil :main-json)
        table-panel (ocr-table/make-view nil :main-ocr-table)
        iocr-panel (ocr/make-view (ocr/make-iocr-model []) :main-iocr)
        ]
    (gui/left-right-split img-panel
                          (gui/card-panel
                           :id :switcher
                           :items [[ocr-panel :location]
                                   [json-panel :json]
                                   [table-panel :table]
                                   [iocr-panel :iocr]])
                          :divider-location 0.5)))


(defn draw-image-rects! [root rects]
  (let [new-img (img/deep-copy @ocr-img)]
    (log/info :draw-image-rects (count rects))
    (iviewer/set-image! root :main-image new-img)
    (->> rects
         vec
         (transform [ALL] (fn [loc]
                            [(:left loc)
                             (:top loc)
                             (:width loc)
                             (:height loc)]))
         (iviewer/draw-rects! root :main-image))))

(defn tbl-sel-draw! [root ocr-tbl e]
  (some->> (gui/selection ocr-tbl {:multi? true})
           (map #(table/value-at ocr-tbl %1))
           (draw-image-rects! root)))

(defn ocr-table-sel-draw! [root tbl e]
  "表格结果图片框选绘制"
  (when-let [sels (seq (ocr-table/get-selected-rects tbl))]
    (log/info :ocr-table-sel-draw! sels)
    (draw-image-rects! root sels)))

(defn add-behaviors
  [root]
  (let [ocr-tbl (gui/select root [(util/->select-id :main-ocr)])
        ocr-table-result (gui/select root [(util/->select-id :main-ocr-table)])
        iocr-table (gui/select root [(util/->select-id :main-iocr)])
        location-menu-item (gui/select root [(util/->select-id :menu-show-location)])
        json-menu-item (gui/select root [(util/->select-id :menu-show-json)])
        table-menu-item (gui/select root [(util/->select-id :menu-show-table)])
        iocr-menu-item (gui/select root [(util/->select-id :menu-show-iocr)])
        ]
    ;; 绘制框选事件
    (bind/bind
     (bind/selection ocr-tbl)
     (bind/transform
      #(tbl-sel-draw! root ocr-tbl %1)))
    (bind/bind
     (bind/selection ocr-table-result)
     (bind/transform
      #(ocr-table-sel-draw! root ocr-table-result %1)))
    (bind/bind
     (bind/selection iocr-table)
     (bind/transform
      #(tbl-sel-draw! root iocr-table %1)))

    (bind/bind
     (bind/selection location-menu-item)
     (bind/transform (fn [_] (switch-card! root :location))))
    (bind/bind
     (bind/selection json-menu-item)
     (bind/transform (fn [_] (switch-card! root :json))))
    (bind/bind
     (bind/selection table-menu-item)
     (bind/transform (fn [_] (switch-card! root :table))))
    (bind/bind
     (bind/selection iocr-menu-item)
     (bind/transform (fn [_] (switch-card! root :iocr))))
    ))

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
            :icon (io/resource "ocr.png")
            :menubar (make-menus))
         cb (img/image-callback {:fn-updated (make-imagej-updated-cb f)})]
     (gui/listen f :window-closing
                 (fn [e]
                   (log/info "close frame window.")
                   (if (img/close-all!)
                     (do
                       ;; (gui/config! f :on-close :exit)
                       (gui/config! f :on-close :dispose)
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
