# cljs-json-pointer

Simple Clojure(Script) library for working with [JSON Pointer](https://www.rfc-editor.org/rfc/rfc6901) and 
[JSON Patch](https://datatracker.ietf.org/doc/html/rfc6902/), with no external dependencies.

## Usage instructions

At the heart of the library is the `->vec` function, which may be used to transform a JSON pointer into a vector
representing the path of an object or array. This vector is suitable for use with the standard Clojure functions for
nested access or updates, like `get-in`, `assoc-in` and `update-in`:

```clojure
(ns app
  (:require [borge.by.clj-json-pointer :as json-pointer]))

(def org
  {"department"
   {"tech"
    {"users"
     [{"name" "ted"  "roles" ["developer"]}
      {"name" "jane" "roles" ["platform" "devops"]}]}
    "finance"
    {"users"
     [{"name" "joe"  "roles" ["reports-writer"]}]}}})

(get-in org (json-pointer/->vec org "/department/tech/users/1/roles")) ; => ["platform" "devops"]
```
