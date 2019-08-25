(ns piaojuocr.mapviewer
  (:require [seesaw.core :as gui]
            [seesaw.border :as border]
            [seesaw.swingx :as guix]
            [seesaw.tree :as tree]
            [clojure.inspector :as insp]
            [seesaw.mig :refer [mig-panel]]
            [seesaw.font :refer [font default-font]]
            [piaojuocr.util :as util :refer [show-ui]]
            [piaojuocr.config :as config]
            [piaojuocr.ocr-api :as api]
            [taoensso.timbre :as log]
            [seesaw.table :as table])
  )


(defn make-view [data id]
  (gui/scrollable (guix/tree-x :id id
                               :model (insp/tree-model data))))

(defn set-model! [root id data]
  (let [tree (gui/select root [(util/->select-id id)])]
    (->> (insp/tree-model data)
         (gui/config! tree :model))))

(comment

  (show-ui (make-view api/res8 :test))

  (show-ui (make-view nil :test))

  )
