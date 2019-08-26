(ns piaojuocr.iocr
  (:require [seesaw.core :as gui]
            [seesaw.border :as border]
            [seesaw.swingx :as guix]
            [seesaw.mig :refer [mig-panel]]
            [seesaw.font :refer [font default-font]]
            [piaojuocr.util :as util :refer [show-ui]]
            [piaojuocr.setting :refer [text-config-panel]]
            [piaojuocr.config :as config]
            [piaojuocr.ocr-api :as api]
            [taoensso.timbre :as log]
            [seesaw.table :as table]))


(defn update-template! [root update-fn]
  (let [tmpl-list (gui/select root [:#iocr-templates-list])
        templates (config/get-config :iocr-templates #{})
        new-tmpl (update-fn templates)]
    (gui/config! tmpl-list :model new-tmpl)
    (config/add-config! :iocr-templates new-tmpl)))

(def add-template! #(update-template! %1 (fn [tmpls] (conj tmpls %2))))
(def remove-template! #(update-template! %1 (fn [tmpls] (disj tmpls %2))))

(defn make-template-form []
  (mig-panel
   :id :template-form
   :border (border/empty-border :left 10 :top 10)
   :constraints ["fill"]
   :items [
           ["选择模板id:"
            "right"]

           [(gui/listbox
             :border [5 "模板列表" 10]
             :id :iocr-templates-list
             :model (config/get-config :iocr-templates #{}))
            " grow, wmin 250, hmin 250"]

           [(mig-panel
             :border (border/empty-border :left 10 :top 10)
             :items [
                     [(gui/button :text "添加"
                                  :listen [:action
                                           (fn [e]
                                             (let [root (gui/to-root e)
                                                   new-val (gui/input "输入模板id"
                                                                      :title "添加模板"
                                                                      :type :question)]
                                               (when new-val
                                                 (add-template! root new-val))))])
                      "wrap"]

                     [(gui/button :text "删除"
                                   :listen [:action
                                            (fn [e]
                                              (let [root (gui/to-root e)]
                                                (some->> (gui/select root [:#iocr-templates-list])
                                                         gui/selection
                                                         (remove-template! root))))])
                      "wrap"]
                     ])
            "wrap"]

           [(gui/checkbox :text "使用分类器:"
                          :id :use-classifier-check
                          :selected? (config/get-config :use-classifier false)
                          :listen [:selection
                                   (fn [e]
                                     (let [root (gui/to-root e)
                                           sel (gui/selection e)]
                                       (config/add-config! :use-classifier sel)
                                       (gui/config! (-> root
                                                        (gui/select [:#classifier-text]))
                                                    :enabled?
                                                    sel)))])
            "right"]

           [(text-config-panel :classifier-text [:classifier-id])
            "wrap, wmin 250, growx"]

           [(gui/separator)
            "span, grow, gaptop 10"]
           ]))

(defn make-template-dlg [id]
  (-> (gui/dialog :id id
                  :title "选择模板或者分类器"
                  :success-fn (fn [p]
                                (let [root (gui/to-frame p)
                                      templates-lst (gui/select root [:#iocr-templates-list])
                                      classifier-txt (gui/select root [:#classifier-text])
                                      template-sign (gui/selection templates-lst)
                                      use-classifier (config/get-config :use-classifier false)
                                      classifier-id (when use-classifier
                                                      (gui/text classifier-txt))]
                                  (if-not (or template-sign
                                              use-classifier)
                                    (gui/alert "没有选择任何模板或分类器")
                                    (cond-> {:type :caml-case}
                                      template-sign (assoc :template-sign template-sign)
                                      classifier-id (assoc :classifier-id classifier-id)))))
                  :cancel-fn (fn [p] nil)
                  :minimum-size [480 :by 400]
                  :option-type :ok-cancel
                  :content (make-template-form)
                  )
      gui/pack!
      gui/show!))

(comment

   (make-template-dlg :test)

  )
