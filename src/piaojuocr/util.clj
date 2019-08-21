(ns piaojuocr.util)

(defn replace-keyword [f kw]
  "修改关键字，f为 str -> str"
  (-> (name kw)
      f
      keyword))

(def ->select-id "转换为id选择器" (partial replace-keyword #(str "#" %1)))

