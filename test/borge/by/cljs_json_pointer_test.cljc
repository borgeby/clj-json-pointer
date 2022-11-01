(ns borge.by.cljs-json-pointer-test
  (:require [clojure.test :refer [deftest is testing]]
            [borge.by.clj-json-pointer :refer [escape patch unescape]]))

(deftest escape-test
  (testing "escape string"
    (is (= (escape "b~a/r") "b~0a~1r"))))

(deftest unescape-test
  (testing "unescape string"
    (is (= (unescape "b~0a~1r") "b~a/r"))))

(deftest patch-add-test
  (testing "simple add"
    (is (= (patch {"a" 1} [{"op" "add" "path" "/b" "value" 2}]) {"a" 1 "b" 2})))
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
