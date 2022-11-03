(ns borge.by.clj-json-pointer
  (:require [clojure.string :as str]))

(defn- unescape
  [s]
  (if (str/includes? s "~") ; replace is fairly expensive, and escaping fairly rare — optimize for the common case
    (-> s (str/replace #"~1" "/") (str/replace #"~0" "~"))
    s))

(defn- ->unescaped-parts [pointer]
  (if (= "" pointer)
    [""]
    (mapv unescape (rest (str/split pointer #"/")))))

(defn ->vec
  "Convert JSON pointer to vector in the format accepted by get-in, assoc-in and update-in. Uses provided
  obj to ensure that any path traversed actually exists, and will throw if not"
  [obj pointer]
  (loop [obj* obj parts* (->unescaped-parts pointer) path* []]
    (cond
      (zero? (count parts*))
      path*
      (nil? obj*)
      (throw (ex-info (str "can't traverse past non-existent node: " (last path*)) {:type :not-found :path path*}))
      :else
      (let [part (first parts*)
            pmod (cond
                   (and (vector? obj*) (re-find #"^\d+$" part)) (parse-long part)
                   (and (vector? obj*) (= "-" part))            (count obj*)
                   :else part)]
        (recur (get obj* pmod) (subvec parts* 1) (conj path* pmod))))))

(defn- strip-hash [s]
  (if (str/starts-with? s "#") (subs s 1) s))

(defn- slash->empty [s]
  (if (= "/" s) "" s))

(defn- op-add [obj path value]
  (if (= "" path) ; whole document
    value         ; replaced by value
    (assoc-in obj (->vec obj (slash->empty path)) value)))

(defn- op-remove [obj path]
  (when-not (= "" path) ; whole document
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

(defn- op-move [obj path from]
  (if (and (= "" path) (= "" from)) ; whole document moved to itself
    obj
    (if (= "" path) ; move to whole document
      (get-in obj (->vec obj (slash->empty from)))
      (if (= "" from)
        (throw (ex-info "can't move from entire document to path" {:type "illegal operation" :operation "move"}))
        (-> obj (op-copy path from) (op-remove from))))))

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
      "replace" (-> obj (op-remove path) (op-add path value))
      "copy"    (op-copy obj path from)
      "move"    (op-move obj path from)
      "test"    (op-test obj path value)
      (throw (ex-info (str "unknown operation: " op) {:type "unknown operation" :operation op})))))

(defn patch
  "Applies a series of JSON patch operations on obj"
  [obj patches]
  (reduce apply-patch obj patches))
