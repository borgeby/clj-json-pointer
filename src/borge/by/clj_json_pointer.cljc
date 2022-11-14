(ns borge.by.clj-json-pointer
  (:require [clojure.set :as set]
            [clojure.string :as str]))

(defn- unescape [s]
  (if (str/includes? s "~") ; replace is fairly expensive, and escaping fairly rare — optimize for the common case
    (-> s (str/replace #"~1" "/") (str/replace #"~0" "~"))
    s))

(defn- ->unescaped-parts [pointer]
  (if (= "" pointer)
    [""]
    (let [parts (mapv unescape (rest (str/split pointer #"/")))]
      (if (str/ends-with? pointer "/") ; turn /foo/ into ["foo" ""]
        (conj parts "")
        parts))))

(defn- valid-number? [s]
  (when-not (and (> (count s) 1) (str/starts-with? s "0"))
    (re-find #"^\d+$" s)))

(defn- valid-path? [s]
  (and (some? s)
       (or (= s "") (str/starts-with? s "/") (str/starts-with? s "#/"))))

(defn- must-get-in [obj path]
  (if (= ::not-found (get-in obj path ::not-found))
    (throw (ex-info "nonexistent attribute" {:type :not-found :path path}))
    obj)) ; we never have use for existing values, so return object instead for chaining

(defn ->vec
  "Convert JSON pointer to vector in the format accepted by get-in, assoc-in and update-in. Uses provided
  obj to ensure that any path (minus the last elements, as we need to allow adding new ones) traversed actually exists
  in the data structure, and will throw if not"
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
                   (and (vector? obj*) (valid-number? part)) (parse-long part)
                   (and (vector? obj*) (= "-" part))         (count obj*)
                   (vector? obj*) (throw (ex-info (str "only numbers and '-' allowed to access array, got: " part)
                                                  {:type "invalid pointer" :path path*}))
                   :else part)]
        (if (and (number? pmod) (> pmod (count obj*)))
          (throw (ex-info (str "can't traverse past size of vector: " (last path*)) {:type :not-found :path path*}))
          (recur (get obj* pmod) (subvec parts* 1) (conj path* pmod)))))))

(defn- strip-hash [s]
  (if (str/starts-with? s "#") (subs s 1) s))

(defn- slash->empty [s]
  (if (= "/" s) "" s))

(defn- insert-at [v pos value]
  (into (conj (subvec v 0 pos) value) (subvec v pos)))

(defn- add [obj v value]
  (if (vector? obj)
    (insert-at obj (peek v) value)
    (assoc-in obj v value)))

(defn- op-add [obj path value]
  (if (= "" path) ; whole document
    value         ; replaced by value
    (let [v (->vec obj (slash->empty path))]
      (if (> (count v) 1)
        (let [vp (pop v) next-last (get-in obj vp)] ; special case for inserting into vector nested under obj
          (if (vector? next-last)
            (assoc-in obj vp (insert-at next-last (peek v) value))
            (add obj v value)))
        (add obj v value)))))

(defn- op-remove [obj path]
  (when (not= "" path)
    (let [v (->vec obj (slash->empty path))
          _ (must-get-in obj v)]
      (if (vector? obj)
        (into (subvec obj 0 (peek v)) (subvec obj (inc (peek v))))
        (if (> (count v) 1)
          (update-in obj (pop v) dissoc (peek v))
          (dissoc obj (first v)))))))

(defn- op-copy [obj path from]
  (if (= from path)
    obj
    (if (= "" path) ; 'from' copied to entire document
      (get-in obj (->vec obj (slash->empty from)))
      (if (= "" from) ; whole document copied to 'path'
        (op-add obj path obj)
        (let [val (get-in obj (->vec obj (slash->empty from)) ::not-found)]
          (if (= val ::not-found)
            (throw (ex-info "nonexistent attribute" {:type "not found" :op "copy" :path path}))
            (op-add obj path val)))))))

(defn- op-move [obj path from]
  (if (= from path)
    obj
    (if (= "" path) ; move to whole document
      (get-in obj (->vec obj (slash->empty from)))
      (if (= "" from)
        (throw (ex-info "can't move from entire document to path" {:type "illegal operation" :op "move"}))
        (-> obj (op-copy path from) (op-remove from))))))

(defn- op-test [obj path value]
  (when-not (and (= "" path) (= obj value)) ; whole document — why should this return nil?
    (if (= value (get-in obj (->vec obj (slash->empty path))))
      obj
      (throw (ex-info (str "test failure for path " path " and value " value) {:type "test failure" :op "test"})))))

(def ^:private required-keys
  {"add"     #{"path" "value"}
   "remove"  #{"path"}
   "replace" #{"path" "value"}
   "copy"    #{"path" "from"}
   "move"    #{"path" "from"}
   "test"    #{"path" "value"}})

(defn- require-keys [{:strs [op] :as patch}]
  (if (nil? op)
    (throw (ex-info "missing op attribute" {:type "invalid patch" :op nil}))
  (let [required (get required-keys op)
        missing  (set/difference required (set (keys patch)))]
    (if (pos? (count missing))
      (throw (ex-info (str "missing keys " (str/join ", " missing)) {:type "invalid patch" :op op}))
      patch))))

(defn apply-patch [obj {:strs [op path value from] :as patch}]
  (require-keys patch)
  (when-not (valid-path? path) (throw (ex-info "invalid path" {:type "invalid path" :op op :path path})))
  (let [path (strip-hash path)]
    (case op
      "add"     (op-add obj path value)
      "remove"  (op-remove obj path)
      "replace" (-> obj (op-remove path) (op-add path value))
      "copy"    (op-copy obj path from)
      "move"    (op-move obj path from)
      "test"    (op-test obj path value)
      (throw    (ex-info (str "unknown operation: " op) {:type "unknown operation" :operation op})))))

(defn patch
  "Applies a series of JSON patch operations on obj"
  [obj patches]
  (reduce apply-patch obj patches))
