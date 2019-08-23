(ns piaojuocr.config
  (:require [cprop.core :refer [load-config]]
            [piaojuocr.util :as util]
            [cprop.source :as source]))


(util/extract-resource! "config.edn")

(def env (load-config
          :file "config.edn"
          :merge
          [(source/from-system-props)
           (source/from-env)]))

(def config (atom {}))

(defn get-config
  "从全局配置和环境中 获取配置项k的值"
  ([k] (get-config k nil))
  ([k default] (or
                (get @config k)
                (get env k)
                default)))

(defn get-in-config
  "从全局配置和环境中 获取配置path的值"
  [path]
  (or
   (get-in @config path)
   (get-in env path)))

(defn add-config!
  "添加配置项到全局配置"
  [k v]
  (swap! config assoc k v))

(defn add-in-config!
  "添加配置项到全局配置"
  [path v]
  (swap! config assoc-in path v))

(defn save-config!
  ([] (save-config! (or (:conf env) "config.edn")))
  ([file-name]
   (spit file-name (-> (merge (load-config :file file-name)
                              @config)
                       pr-str))))
