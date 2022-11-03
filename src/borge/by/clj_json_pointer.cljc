(ns borge.by.clj-json-pointer
  (:require [clojure.string :as str]))

(defn escape [s]
  (-> s (str/replace #"~" "~0") (str/replace "/" "~1")))

(defn unescape [s]
  (if (str/includes? s "~") ; replace is fairly expensive, and escaping fairly rare — optimize for the common case
    (-> s (str/replace #"~1" "/") (str/replace #"~0" "~"))
    s))

(defn ->vec [obj pointer]
  (let [sp (str/split pointer #"/")
        parts (if (and (= 1 (count sp)) (= "" (first sp)))
                [""]
                (mapv unescape (rest sp)))]
    (loop [obj* obj parts* parts path* []]
      (cond
        (zero? (count parts*))
        path*
        (nil? obj*)
        (throw (ex-info (str "can't traverse past non-existent node: " (last path*)) {:type :not-found :path path*}))
        :else
        (let [part (first parts*)
              pmod (cond-> part
                           (and (re-find #"^\d+$" part) (vector? obj*)) (parse-long)
                           (and (= "-" part)    (vector? obj*))         ((fn [_] (count obj*))))]
          (recur (get obj* pmod) (rest parts*) (conj path* pmod)))))))

(defn strip-hash [s]
  (if (str/starts-with? s "#") (subs s 1) s))

(defn slash->empty [s]
  (if (= "/" s) "" s))

(defn- op-add [obj path value]
  (if (= "" path) ; whole document
    value         ; replaced by value
    (assoc-in obj (->vec obj (slash->empty path)) value)))

(defn- op-remove [obj path]
  (if (= "" path) ; whole document
    nil
    (let [v (->vec obj (slash->empty path))]
      (if (> (count v) 1)
        (update-in obj (pop v) dissoc (peek v))
        (dissoc obj (first v))))))

(defn- op-copy [obj path from]
  (if (and (= "" path) (= "" from)) ; whole document copied to itself
    obj
    (if (= "" path) ; 'from' copied to entire document
      (get-in obj (->vec obj (slash->empty from)))
      (if (= "" from) ; whole document copied to 'path'
        (op-add obj path obj)
        (op-add obj path (get-in obj (->vec obj (slash->empty from))))))))

(defn op-move [obj path from]
  (if (and (= "" path) (= "" from)) ; whole document moved to itself
    obj
    (if (= "" path) ; move to whole document
      (get-in obj (->vec obj (slash->empty from)))
      (if (= "" from)
        (throw (ex-info "can't move from entire document to path" {:type "illegal operation" :operation "move"}))
        (-> (op-copy obj path from) (op-remove from))))))

(defn- op-test [obj path value]
  (if (= value (get-in obj (->vec obj (slash->empty path))))
    obj
    (throw (ex-info (str "test operation failure for path " path " and value " value) {:type "test operation failure"
                                                                                       :operation "test"}))))

(defn- apply-patch [obj {:strs [op path value from]}]
  (let [path (strip-hash path)]
    (case op
      "add"     (op-add obj path value)
      "remove"  (op-remove obj path)
      "replace" (-> (op-remove obj path) (op-add path value))
      "copy"    (op-copy obj path from)
      "move"    (op-move obj path from)
      "test"    (-> (op-test obj path value))

      (throw (ex-info (str "unknown operation: " op) {:type "unknown operation" :operation op})))))

(defn patch [obj patches]
  (reduce apply-patch obj patches))
