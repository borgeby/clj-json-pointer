(ns borge.by.clj-json-pointer-test
  (:require [clojure.test :refer [deftest is testing]]
            [borge.by.clj-json-pointer :refer [patch]]))

(deftest patch-add-test
  (testing "simple add"
    (is (= (patch {"a" 1} [{"op" "add" "path" "/b" "value" 2}]) {"a" 1 "b" 2})))
  (testing "add with path /"
    (is (= (patch {"a" 1} [{"op" "add" "path" "/" "value" "foo"}]) {"a" 1 "" "foo"})))
  (testing "add nested"
    (is (= (patch {"a" {"b" 2}} [{"op" "add" "path" "/a/c" "value" 3}]) {"a" {"b" 2 "c" 3}})))
  (testing "add to array"
    (is (= (patch {"a" [1]} [{"op" "add" "path" "/a/1" "value" 2}]) {"a" [1 2]})))
  (testing "consecutive add to array"
    (is (= (patch {"a" [1]} [{"op" "add" "path" "/a/1" "value" 2}
                             {"op" "add" "path" "/a/2" "value" 3}]) {"a" [1 2 3]})))
  (testing "add last to array (-)"
    (is (= (patch {"a" [1]} [{"op" "add" "path" "/a/-" "value" 2}]) {"a" [1 2]})))
  (testing "consecutive add last to array (-)"
    (is (= (patch {"a" [1]} [{"op" "add" "path" "/a/-" "value" 2}
                             {"op" "add" "path" "/a/-" "value" 3}]) {"a" [1 2 3]})))
  (testing "mix add to array and add last(-)"
    (is (= (patch {"a" [1]} [{"op" "add" "path" "/a/1" "value" 2}
                             {"op" "add" "path" "/a/-" "value" 3}
                             {"op" "add" "path" "/a/3" "value" 4}]) {"a" [1 2 3 4]})))
  (testing "add nested maps and arrays"
    (is (= (patch {"a" {"b" [1 {"c" 2}]}} [{"op" "add" "path" "/a/b/1/d" "value" 3}]) {"a" {"b" [1 {"c" 2 "d" 3}]}}))))

(deftest patch-remove-test
  (testing "simple remove"
    (is (= (patch {"a" 1} [{"op" "remove" "path" "/a"}]) {})))
  (testing "nested remove"
    (is (= (patch {"a" {"b" {"c" 1}}} [{"op" "remove" "path" "/a/b/c"}]) {"a" {"b" {}}})))
  (testing "nested remove, with array"
    (is (= (patch {"a" {"b" {"c" [1 {"d" 2 "e" 3}]}}} [{"op" "remove" "path" "/a/b/c/1/d"}])
           {"a" {"b" {"c" [1 {"e" 3}]}}}))))

(deftest patch-replace-test
  (testing "simple replace"
    (is (= (patch {"a" 1} [{"op" "replace" "path" "/a" "value" 2}]) {"a" 2})))
  (testing "nested replace"
    (is (= (patch {"a" {"b" {"c" 1}}} [{"op" "replace" "path" "/a/b/c" "value" 2}]) {"a" {"b" {"c" 2}}})))
  (testing "nested replace, with array"
    (is (= (patch {"a" {"b" {"c" [1 {"d" 2 "e" 3}]}}} [{"op" "replace" "path" "/a/b/c/1/d" "value" 3}])
           {"a" {"b" {"c" [1 {"d" 3 "e" 3}]}}}))))

(deftest patch-copy-test
  (testing "simple copy"
    (is (= (patch {"a" 1} [{"op" "copy" "path" "/b" "from" "/a"}]) {"a" 1 "b" 1})))
  (testing "nested copy"
    (is (= (patch {"a" {"b" {"c" 1}}} [{"op" "copy" "path" "/a/b/d" "from" "/a/b/c"}]) {"a" {"b" {"c" 1 "d" 1}}})))
  (testing "nested copy, with array"
    (is (= (patch {"a" {"b" {"c" [1 {"d" 2 "e" 3}]}}} [{"op" "copy" "path" "/a/b/c/-" "from" "/a/b/c/1/d"}])
           {"a" {"b" {"c" [1 {"d" 2 "e" 3} 2]}}}))))

(deftest patch-move-test
  (testing "simple move"
    (is (= (patch {"a" 1} [{"op" "move" "path" "/b" "from" "/a"}]) {"b" 1})))
  (testing "nested move"
    (is (= (patch {"a" {"b" {"c" 1}}} [{"op" "move" "path" "/a/b/d" "from" "/a/b/c"}]) {"a" {"b" {"d" 1}}})))
  (testing "nested move, with array"
    (is (= (patch {"a" {"b" {"c" [1 {"d" 2 "e" 3}]}}} [{"op" "move" "path" "/a/b/c/-" "from" "/a/b/c/1/d"}])
           {"a" {"b" {"c" [1 {"e" 3} 2]}}}))))

(deftest patch-test-test
  (testing "simple test"
    (is (= (patch {"a" 1} [{"op" "test" "path" "/a" "value" 1}]) {"a" 1})))
  (testing "nested test"
    (is (= (patch {"a" {"b" {"c" 1}}} [{"op" "test" "path" "/a/b/c" "value" 1}]) {"a" {"b" {"c" 1}}})))
  (testing "nested test, with array"
    (is (= (patch {"a" {"b" {"c" [1 {"d" 2 "e" 3}]}}} [{"op" "test" "path" "/a/b/c/0" "value" 1}])
           {"a" {"b" {"c" [1 {"d" 2 "e" 3}]}}})))
  (testing "null"
    (is (= (patch {"a" nil} [{"op" "test" "path" "/a" "value" nil}]) {"a" nil}))))

(deftest whole-document-test
  (testing "add whole document"
    (is (= (patch {"a" 1} [{"op" "add" "path" "" "value" "foo"}]) "foo")))
  (testing "copy whole document to path"
    (is (= (patch {"a" 1} [{"op" "copy" "path" "/b" "from" ""}]) {"a" 1 "b" {"a" 1}})))
  (testing "copy from to whole document"
    (is (= (patch {"a" 1} [{"op" "copy" "path" "" "from" "/a"}]) 1)))
  (testing "copy whole document to whole document"
    (is (= (patch {"a" 1} [{"op" "copy" "path" "" "from" ""}]) {"a" 1})))
  (testing "move from to whole document"
    (is (= (patch {"a" 1} [{"op" "move" "from" "/a" "path" ""}]) 1)))
  (testing "remove whole document"
    (is (= (patch {"a" 1} [{"op" "remove" "path" ""}]) nil)))
  (testing "replace whole document"
    (is (= (patch {"a" 1} [{"op" "replace" "path" "" "value" "foo"}]) "foo"))))

(deftest slash->-empty-string-key-test
  (testing "add to /"
    (is (= (patch {"a" 1} [{"op" "add" "path" "/" "value" "foo"}]) {"a" 1 "" "foo"})))
  (testing "copy to /"
    (is (= (patch {"a" 1} [{"op" "copy" "path" "/" "from" "/a"}]) {"a" 1 "" 1})))
  (testing "copy from /"
    (is (= (patch {"a" 1 "" 2} [{"op" "copy" "path" "/a" "from" "/"}]) {"a" 2 "" 2})))
  (testing "move to /"
    (is (= (patch {"a" 1} [{"op" "move" "path" "/" "from" "/a"}]) {"" 1})))
  (testing "move from /"
    (is (= (patch {"a" 1 "" 2} [{"op" "move" "path" "/a" "from" "/"}]) {"a" 2})))
  (testing "remove from /"
    (is (= (patch {"a" 1 "" 2} [{"op" "remove" "path" "/"}]) {"a" 1})))
  (testing "replace value at /"
    (is (= (patch {"a" 1 "" 2} [{"op" "replace" "path" "/" "value" "foo"}]) {"a" 1 "" "foo"})))
  (testing "test value at /"
    (is (= (patch {"a" 1 "" 2} [{"op" "test" "path" "/" "value" 2}]) {"a" 1 "" 2}))))

(deftest leading-hash-is-stripped
  (testing "add with leading hash"
    (is (= (patch {"a" 1} [{"op" "add" "path" "#/b" "value" "foo"}]) {"a" 1 "b" "foo"}))))
