(ns borge.by.clj-json-pointer
  (:require [clojure.string :as str]))

(defn- numeric? [s]
  (re-find #"^\d+$" s))

(defn escape [s]
  (-> s (str/replace #"~" "~0") (str/replace "/" "~1")))

(defn unescape [s]
  (if (str/includes? s "~") ; replace is fairly expensive, and escaping fairly rare — optimize for the common case
    (-> s (str/replace #"~0" "~") (str/replace #"~1" "/"))
    s))

(defn ->vec [obj pointer]
  (let [parts (mapv unescape (rest (str/split pointer #"/")))]
    (loop [obj* obj parts* parts path* []]
      (cond
        (zero? (count parts*))
        path*
        (nil? obj*)
        (throw (ex-info (str "can't traverse past non-existent node: " (last path*)) {:type :not-found :path path*}))
        :else
        (let [part (first parts*)
              pmod (cond-> part
                           (and (numeric? part) (vector? obj*)) (parse-long)
                           (and (= "-" part)    (vector? obj*)) ((fn [_] (count obj*))))]
          (recur (get obj* pmod) (rest parts*) (conj path* pmod)))))))

(defn- op-add [obj path value]
  (assoc-in  obj (->vec obj path) value))

(defn- op-remove [obj path]
  (let [v (->vec obj path)]
    (if (> (count v) 1)
      (update-in obj (pop v) dissoc (peek v))
      (dissoc obj (first v)))))

(defn- apply-patch [obj {:strs [op path value from] :as patch}]
  (case op
    "add"     (op-add obj path value)
    "remove"  (op-remove obj path)
    "replace" (-> (op-remove obj path) (op-add path value))

    (throw (ex-info (str "unknown operation: " op) {:type "unknown operation" :operation op}))))

(defn patch [obj patches]
  (reduce apply-patch obj patches))
