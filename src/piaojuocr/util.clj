(ns piaojuocr.util
  (:require [clojure.java.io :as io]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.core :as appenders]))

(defn replace-keyword [f kw]
  "修改关键字，f为 str -> str"
  (-> (name kw)
      f
      keyword))

(def ->select-id "转换为id选择器" (partial replace-keyword #(str "#" %1)))

;;;; 日志记录相关
(def log-levels [:trace :debug :info :warn :error :fatal :report])

(defn log-time-format! []
  (log/merge-config!
   {:timestamp-opts
    {:pattern "yyyy/MM/dd HH:mm:ss"
     :locale (java.util.Locale/getDefault)
     :timezone (java.util.TimeZone/getDefault)}}))

(defonce __log-time (log-time-format!))

(defn log-add-appender!
  "添加日志记录项"
  [appender]
  (log/merge-config!
   {:appenders appender}))

(defn log-to-file!
  "配置log输出文件"
  ([] (log-to-file! "logs.log"))
  ([file-name]
   (log-add-appender! {:spit (appenders/spit-appender {:fname file-name})})))

(defn extract-resource!
  "提取资源文件到当前目录"
  ([filename] (extract-resource! filename nil))
  ([filename overwrite]
   (let [o-file (io/file filename)]
     (when (or overwrite
               (not (.exists o-file)))
       (log/info :extract-refource filename)
       (with-open [in (io/input-stream (io/resource filename))]
         (io/copy in o-file))))))
