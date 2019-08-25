(ns piaojuocr.ocr-api
  (:require [camel-snake-kebab.core :refer :all]
            [piaojuocr.config :as config]
            [taoensso.timbre :as log])
  (:import com.baidu.aip.ocr.AipOcr))


(def aip-client (AipOcr.
                 (config/get-config :app-id)
                 (config/get-config :api-key)
                 (config/get-config :api-secret-key)))

(doto aip-client
  (.setConnectionTimeoutInMillis 2000)
  (.setSocketTimeoutInMillis 60000))

(defn json->map
  [json]
  (condp instance? json
    org.json.JSONObject
    (->> (map (fn [k] [(->kebab-case-keyword k)
                       (-> (.get json k)
                           json->map)])
              (.keySet json))
         (into {}))

    org.json.JSONArray
    (->> (.iterator json)
         iterator-seq
         (map json->map))

    json))

(defn format-options
  [option]
  (let [key-fn (if (= :caml-case (get option :type))
                 ->camelCaseString
                 ->snake_case_string)]
    (->> (map (fn [[k v]] [(key-fn k) (str v)]) option)
         (into {})
         java.util.HashMap.)))

(defmacro defapi
  [method doc args]
  (let [fn-name (->kebab-case-symbol (str method))
        str-method (str method)
        method-name (symbol (str "." method))]
    `(defn ~fn-name
       ~doc
       ([~@args] (~fn-name ~@args {}))
       ([~@args options#]
        (let [opt#  (format-options options#)]
          (log/info "baidu-ocr api:" ~str-method "options:" opt#)
          (-> (~method-name aip-client ~@args opt#)
              json->map))))))

(defapi basicGeneral "通用识别" [file])
(defapi basicAccurateGeneral "通用高精度识别" [file])
(defapi accurateGeneral "通用高精度识别(位置信息)" [file])
(defapi general "通用识别(位置信息)" [file])
(defapi custom "自定义模板识别" [file])
(defapi receipt "通用票据识别" [file])
(defapi trainTicket "火车票识别" [file])
(defapi taxiReceipt "出租车票识别" [file])
(defapi form "表单识别" [file])
(defapi tableRecognitionAsync "表格异步识别" [file])
(defapi tableResultGet "表格异步识别结果" [req-id])
(defapi vatInvoice "增值税发票识别" [file])
(defapi passport "护照识别" [file])
(defapi businessCard "名片识别" [file])
(defapi handwriting "手写中文识别" [file])
(defapi bankcard "银行卡识别" [file])
(defapi idcard "身份证识别" [file card-side])
(defapi drivingLicense "行驶证识别" [file])
(defapi plateLicense "车牌识别" [file])
(defapi businessLicense "营业执照识别" [file])

(defn table-recognize-to-json
  "表格识别"
  ([file] (table-recognize-to-json file 30000))
  ([file ^java.lang.Long timeout]
   (json->map
    (. aip-client tableRecognizeToJson file timeout))))

(def options {:language-type, "CHN_ENG"
              :detect-direction, "true"
              :detect-language, "true"
              :probability, "true"})


(comment
  (def file (.getPath (clojure.java.io/resource "test2.jpg")))


  (def res2 (basic-general file options))

  (def res3 (basic-accurate-general file options))

  (def res4 (general file options))

  (def opt2 (merge options {"recognize_granularity" "small"}))

  (def res6 (general file opt2))

  (def res7 (accurate-general file options))

  (def res5 (custom file (assoc options
                                ;;:template-sign "eecfbc1a6645c46977ed7c5a49dc5c04"
                                :template-sign "b50b3a7a70cf1f85ed9c07679dd3f20e"
                                :type :caml-case)))


  (def file2 (.getPath (clojure.java.io/resource "fapiao1.jpg")))

  (def res8 (vat-invoice file2))

  (def res9 (table-recognize-to-json file))

  )
