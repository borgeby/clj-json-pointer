# cljs-json-pointer
[![Clojars Project](https://img.shields.io/clojars/v/by.borge/clj-json-pointer.svg)](https://clojars.org/by.borge/clj-json-pointer)
[![cljdoc](https://cljdoc.org/badge/by.borge/clj-json-pointer)](https://cljdoc.org/d/by.borge/clj-json-pointer)
![build](https://github.com/borgeby/clj-json-pointer/actions/workflows/check.yml/badge.svg)
[![codecov](https://codecov.io/github/borgeby/clj-json-pointer/branch/main/graph/badge.svg?token=0T30IGULJ2)](https://codecov.io/github/borgeby/clj-json-pointer)

Simple Clojure(Script) library for working with [JSON Pointer](https://www.rfc-editor.org/rfc/rfc6901) and 
[JSON Patch](https://datatracker.ietf.org/doc/html/rfc6902/), with no external dependencies. The JSON Patch function
provided passes all the tests from the JSON patch [conformance test suite](https://github.com/json-patch/json-patch-tests). 

## Usage instructions

At the heart of the library is the `->vec` function, which may be used to transform a JSON pointer into a vector
representing the path of an object or array. This vector is suitable for use with the standard Clojure functions for
nested access or updates, like `get-in`, `assoc-in` and `update-in`:

```clojure
(ns app
  (:require [clj-json-pointer.core :as jp]))

(def org
  {"department"
   {"tech"
    {"users"
     [{"name" "ted"  "roles" ["developer"]}
      {"name" "jane" "roles" ["platform" "devops"]}]}
    "finance"
    {"users"
     [{"name" "joe"  "roles" ["reports-writer"]}]}}})

(let [path (jp/->vec org "/department/tech/users/1/roles") ; => ["department" "tech" 1 "users" "roles"]
      roles (get-in org path)]                             ; => ["platform" "devops"]
  (do (something (with roles))))
```

These simple building blocks are used to implement the various operations of JSON `patch`:

```clojure
(jp/patch {}                                        ; => {}
  [{"op" "add" "path" "/foo" "value" "bar"}         ; => {"foo" "bar"}
   {"op" "add" "path" "/bar" "value" "baz"}         ; => {"foo" "bar" "bar" "baz}
   {"op" "remove" "path" "/foo"}                    ; => {"bar" "baz"}
   {"op" "replace" "path" "/bar" "value" "foo"}     ; => {"bar" "foo"}          
   {"op" "copy" "from" "/bar" "path" "/baz"}        ; => {"bar" "foo" "baz" "foo"}                
   {"op" "move" "from" "/baz" "path" "/foo"}        ; => {"foo" "foo"}
   {"op" "test" "path" "/foo" "value" "foo"}])      ; => {"foo" "foo"}
```

Or if you so prefer, use the `apply-patch` function, which applies a single patch to the provided data structure:

```clojure
(jp/apply-patch {} {"op" "add" "path" "/a" "value" 1}) ; => {"a" 1}

; or, more likely:
(reduce jp/apply-patch {} patches)
```

## Development

### Test

* `clj -X:test` to run the unit and compliance tests
* `shadow-cljs compile test && node target/cljs-test.js` for ClojureScript

### Deploy

* `clj -T:build jar`
* `clj -X:deploy`
