(ns borge.by.cljs-json-pointer-test
  (:require [clojure.test :refer [deftest is testing]]
            [borge.by.clj-json-pointer :refer [patch]]))

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
