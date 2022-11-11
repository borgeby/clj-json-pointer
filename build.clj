(ns build
  (:refer-clojure :exclude [compile])
  (:require [clojure.tools.build.api :as b]))

(def basis (b/create-basis {:project "deps.edn"}))
(def class-dir "target/classes")

(defn clean [_]
  (b/delete {:path "target"}))

(defn compile [_]
  (clean nil)
  (b/copy-dir {:src-dirs ["src" "test"]
               :target-dir class-dir})
  (b/compile-clj {:basis basis
                  :src-dirs ["src" "test"]
                  :class-dir class-dir}))

; :build     {:deps       {org.clojure/tools.build {:mvn/version "0.8.4"}}
;              :ns-default build}
