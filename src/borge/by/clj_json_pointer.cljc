(ns borge.by.clj-json-pointer
  (:require [clojure.string :as str]))

(defn- numeric?
  [s]
  (re-find #"^\d+$" s))

(defn ->vec
  [obj pointer]
  (let [parts (rest (str/split pointer #"/"))]
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

(defn- apply-patch [obj {:strs [op path value from] :as patch}]
  ;(println obj patch)
  ;(println (->vec obj path))
  (case op
    "add" (assoc-in obj (->vec obj path) value)
    ))

(defn patch [obj patches]
  (reduce apply-patch obj patches))

(def org
  {"department"
   {"tech"
    {"users"
     [{"name" "ted"  "roles" ["developer"]}
      {"name" "jane" "roles" ["platform" "devops"]}]}
    "finance"
    {"users"
     [{"name" "joe"  "roles" ["reports-writer"]}]}}})

(defn -main []
  (println (get-in org (->vec org "/department/tech/users/1/roles"))))
