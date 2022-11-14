(ns build
  (:refer-clojure :exclude [compile])
  (:require [clojure.tools.build.api :as b]))

(def version "1.0.0")

(def lib 'by.borge/clj-json-pointer)
(def basis (b/create-basis {:project "deps.edn"}))
(def class-dir "target/classes")
(def jar-file (format "target/%s-%s.jar" (name lib) version))

(defn clean [_]
  (b/delete {:path "target"}))

(defn compile [_]
  (clean nil)
  (b/copy-dir    {:src-dirs ["src"] :target-dir class-dir})
  (b/compile-clj {:src-dirs ["src"] :basis basis :class-dir class-dir}))

(defn pom [_]
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :basis basis
                :src-pom "./build/pom.xml"
                :src-dirs ["src"]}))

(defn jar [_]
  (compile nil)
  (pom nil)
  (b/copy-dir {:src-dirs ["src"] :target-dir class-dir})
  (b/jar {:class-dir class-dir :jar-file jar-file}))
