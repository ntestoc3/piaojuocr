(ns piaojuocr.ocr
  (:require [camel-snake-kebab.core :refer :all]
            [piaojuocr.config :as config])
  (:import com.baidu.aip.ocr.AipOcr))


(def aip-client (AipOcr.
                 (config/get-config :app-id)
                 (config/get-config :api-key)
                 (config/get-config :sec-key)))

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
  [options]
  (let [key-fn (if (= :caml-case (get options :type))
                 ->camelCaseString
                 ->snake_case_string)]
      (->> (map (fn [[k v]] [(key-fn k) (str v)]) options)
        (into {}))))

(defmacro defapi
  [method args]
  (let [fn-name (->kebab-case-symbol (str method))
        method-name (symbol (str "." method))]
    `(defn ~fn-name
       ([~@args] (~fn-name ~@args {}))
       ([~@args options#]
        (let [opt# (-> (format-options options#)
                       java.util.HashMap.)]
          (-> (~method-name aip-client ~@args opt#)
              json->map))))))

(defapi basicGeneral [file])
(defapi basicAccurateGeneral [file])
(defapi general [file])
(defapi custom [file])

(def options {:language-type, "CHN_ENG"
              :detect-direction, "true"
              :detect-language, "true"
              :probability, "true"})

(comment
  (def file (.getPath (clojure.java.io/resource "test.jpg")))

  (def res2 (basic-general file options))

  (def res3 (basic-accurate-general file options))

  (def res4 (general file options))

  (def res5 (custom file (assoc options
                                :template-sign "eecfbc1a6645c46977ed7c5a49dc5c04"
                                :type :caml-case)))


  )
