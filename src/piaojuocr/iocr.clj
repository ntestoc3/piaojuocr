(ns piaojuocr.iocr
  (:require [seesaw.core :as gui]
            [seesaw.border :as border]
            [seesaw.icon :as icon]
            [seesaw.swingx :as guix]
            [seesaw.mig :refer [mig-panel]]
            [seesaw.font :refer [font default-font]]
            [piaojuocr.util :as util :refer [show-ui]]
            [piaojuocr.setting :refer [text-config-panel]]
            [piaojuocr.config :as config]
            [piaojuocr.ocr-api :as api]
            [taoensso.timbre :as log]
            [seesaw.table :as table]
            [clojure.java.io :as io]))


(defn make-tpl-info-model [data]
  [:columns [{:key :name :text "名称"}
             {:key :id :text "Id"}]
   :rows data])

(defn update-template! [root update-fn]
  (let [tmpl-list (gui/select root [:#iocr-templates-list])
        templates (config/get-config :iocr-templates #{})
        new-tmpl (update-fn templates)]
    (->> (make-tpl-info-model new-tmpl)
         (gui/config! tmpl-list :model))
    (config/add-config! :iocr-templates new-tmpl)))

(def add-template! #(update-template! %1 (fn [tmpls] (conj tmpls %2))))
(def remove-template! #(update-template! %1 (fn [tmpls] (disj tmpls %2))))

(defn template-info-dlg [parent]
  (-> (gui/dialog
       :modal? true
       :parent parent
       :option-type :ok-cancel
       :type :question
       :content (mig-panel
                 :id :template-info-input-form
                 :constraints ["fill"]
                 :items [["名称:"
                          "right"
                          ]
                         [(gui/text :id :tpl-name)
                          "wrap, grow, wmin 250,"]

                         ["模板id:"
                          "right"]
                         [(gui/text :id :tpl-id)
                          "grow"]])
       :success-fn (fn [p]
                     (let [root (gui/to-root p)
                           name (-> (gui/select root [:#tpl-name])
                                     gui/text)
                           id (-> (gui/select root [:#tpl-id])
                                  gui/text)]
                       (if (or (empty? name)
                               (empty? id))
                         (gui/alert "模板名称和id不能为空")
                         {:name name
                          :id id})))
       :cancel-fn (fn [p] nil))
      gui/pack!
      gui/show!))

(defn make-template-form []
  (let [use-classifier (config/get-config :use-classifier false)]
    (mig-panel
    :id :template-form
    :border (border/empty-border :left 10 :top 10)
    :constraints ["fill"]
    :items [
            ["选择模板id:"
             "right"]

            [(gui/scrollable
              (gui/table
               :id :iocr-templates-list
               :model (make-tpl-info-model
                       (config/get-config :iocr-templates []))))
             "grow, wmin 250, hmin 250"]

            [(mig-panel
              :border (border/empty-border :left 10 :top 10)
              :items [
                      [(gui/button :text "添加"
                                   :icon (io/resource "add.png")
                                   :listen [:action
                                            (fn [e]
                                              (let [root (gui/to-root e)
                                                    new-val (template-info-dlg root)]
                                                (when new-val
                                                  (add-template! root new-val))))])
                       "wrap"]

                      [(gui/button :text "删除"
                                   :icon (io/resource "del.png")
                                   :listen [:action
                                            (fn [e]
                                              (let [root (gui/to-root e)
                                                    tbl (gui/select root [:#iocr-templates-list])]
                                                (some->> (gui/selection tbl)
                                                         (table/value-at tbl)
                                                         (remove-template! root))))])
                       "wrap"]
                      ])
             "wrap"]

            [(gui/checkbox :text "使用分类器:"
                           :id :use-classifier-check
                           :selected? use-classifier
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

            [(text-config-panel :classifier-text [:classifier-id] [:enabled? use-classifier])
             "wrap, wmin 250, growx"]

            [(gui/separator)
             "span, grow, gaptop 10"]
            ])))

(defn make-template-dlg [id]
  (-> (gui/dialog :id id
                  :modal? true
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
