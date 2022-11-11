# cljs-json-pointer
![build](https://github.com/borgeby/clj-json-pointer/actions/workflows/check.yml/badge.svg)
[![codecov](https://codecov.io/github/borgeby/clj-json-pointer/branch/main/graph/badge.svg?token=0T30IGULJ2)](https://codecov.io/github/borgeby/clj-json-pointer)

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

(let [path (json-pointer/->vec org "/department/tech/users/1/roles") ; => ["department" "tech" 1 "users" "roles"]
      roles (get-in org path)]                                       ; => ["platform" "devops"]
  (do (something (with roles))))
```

These simple building blocks are used to implement the various operations of JSON `patch`:

```clojure
(json-pointer/patch {}                                ; => {}
  [{"op" "add" "path" "/foo" "value" "bar"}           ; => {"foo" "bar"}
   {"op" "add" "path" "/bar" "value" "baz"}           ; => {"foo" "bar" "bar" "baz}
   {"op" "remove" "path" "/foo"}                      ; => {"bar" "baz"}
   {"op" "replace" "path" "/bar" "value" "foo"}       ; => {"bar" "foo"}          
   {"op" "copy" "from" "/bar" "path" "/baz"}          ; => {"bar" "foo" "baz" "foo"}                
   {"op" "move" "from" "/baz" "path" "/foo"}          ; => {"foo" "foo"}
   {"op" "test" "path" "/foo" "value" "foo"}])        ; => {"foo" "foo"}
```

Or if you so prefer, use the `apply-patch` function, which applies a single patch to the provided data structure:

```clojure
(reduce json-pointer/apply-patch obj patches)
```

## Development

`clj -X:test` to run the unit tests, or `shadow-cljs compile test && node target/cljs-test.js` for ClojureScript
