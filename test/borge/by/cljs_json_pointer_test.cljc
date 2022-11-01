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
