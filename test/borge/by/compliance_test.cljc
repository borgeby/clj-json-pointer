(ns borge.by.compliance-test
  "Implementation of the JSON patch compliance test suite from https://github.com/json-patch/json-patch-tests"
  (:require [clojure.test :refer [deftest is testing]]
            [borge.by.clj-json-pointer :as json-pointer])
  #?(:clj (:import (clojure.lang ExceptionInfo))))

(deftest json-patch-compliance-tests
  (testing "empty list, empty docs"
    (is (= (json-pointer/patch {} []) {})))
  (testing "empty patch list"
    (is (= (json-pointer/patch {"foo" 1} []) {"foo" 1})))
  (testing "rearrangements OK?"
    (is (= (json-pointer/patch {"foo" 1, "bar" 2} []) {"bar" 2, "foo" 1})))
  (testing "rearrangements OK?  How about one level down ... array"
    (is (= (json-pointer/patch [{"foo" 1, "bar" 2}] []) [{"bar" 2, "foo" 1}])))
  (testing "rearrangements OK?  How about one level down..."
    (is (= (json-pointer/patch {"foo" {"foo" 1, "bar" 2}} []) {"foo" {"bar" 2, "foo" 1}})))
  (testing "add replaces any existing field"
    (is (= (json-pointer/patch {"foo" nil} [{"op" "add", "path" "/foo", "value" 1}]) {"foo" 1})))
  (testing "toplevel array"
    (is (= (json-pointer/patch [] [{"op" "add", "path" "/0", "value" "foo"}]) ["foo"])))
  (testing "toplevel array, no change"
    (is (= (json-pointer/patch ["foo"] []) ["foo"])))
  (testing "toplevel object, numeric string"
    (is (= (json-pointer/patch {} [{"op" "add", "path" "/foo", "value" "1"}]) {"foo" "1"})))
  (testing "toplevel object, integer"
    (is (= (json-pointer/patch {} [{"op" "add", "path" "/foo", "value" 1}]) {"foo" 1})))
  (testing "Toplevel scalar values OK?"
    (is (= (json-pointer/patch "foo" [{"op" "replace", "path" "", "value" "bar"}]) "bar")))
  (testing "replace object document with array document?"
    (is (= (json-pointer/patch {} [{"op" "add", "path" "", "value" []}]) [])))
  (testing "replace array document with object document?"
    (is (= (json-pointer/patch [] [{"op" "add", "path" "", "value" {}}]) {})))
  (testing "append to root array document?"
    (is (= (json-pointer/patch [] [{"op" "add", "path" "/-", "value" "hi"}]) ["hi"])))
  (testing "Add, / target"
    (is (= (json-pointer/patch {} [{"op" "add", "path" "/", "value" 1}]) {"" 1})))
  (testing "Add, /foo/ deep target (trailing slash)"
    (is (= (json-pointer/patch {"foo" {}} [{"op" "add", "path" "/foo/", "value" 1}]) {"foo" {"" 1}})))
  (testing "Add composite value at top level"
    (is (= (json-pointer/patch {"foo" 1} [{"op" "add", "path" "/bar", "value" [1 2]}]) {"foo" 1, "bar" [1 2]})))
  (testing "Add into composite value"
    (is (= (json-pointer/patch {"foo" 1, "baz" [{"qux" "hello"}]} [{"op" "add", "path" "/baz/0/foo", "value" "world"}])
           {"foo" 1, "baz" [{"qux" "hello", "foo" "world"}]})))
  (testing "unnamed test"
    (is (thrown? ExceptionInfo (json-pointer/patch {"bar" [1 2]} [{"op" "add", "path" "/bar/8", "value" "5"}]))))
  (testing "unnamed test"
    (is (thrown? ExceptionInfo (json-pointer/patch {"bar" [1 2]} [{"op" "add", "path" "/bar/-1", "value" "5"}]))))
  (testing "unnamed test"
    (is (= (json-pointer/patch {"foo" 1} [{"op" "add", "path" "/bar", "value" true}]) {"foo" 1, "bar" true})))
  (testing "unnamed test"
    (is (= (json-pointer/patch {"foo" 1} [{"op" "add", "path" "/bar", "value" false}]) {"foo" 1, "bar" false})))
  (testing "unnamed test"
    (is (= (json-pointer/patch {"foo" 1} [{"op" "add", "path" "/bar", "value" nil}]) {"foo" 1, "bar" nil})))
  (testing "0 can be an array index or object element name"
    (is (= (json-pointer/patch {"foo" 1} [{"op" "add", "path" "/0", "value" "bar"}]) {"foo" 1, "0" "bar"})))
  (testing "unnamed test"
    (is (= (json-pointer/patch ["foo"] [{"op" "add", "path" "/1", "value" "bar"}]) ["foo" "bar"])))
  (testing "unnamed test"
    (is (= (json-pointer/patch ["foo" "sil"] [{"op" "add", "path" "/1", "value" "bar"}]) ["foo" "bar" "sil"])))
  (testing "unnamed test"
    (is (= (json-pointer/patch ["foo" "sil"] [{"op" "add", "path" "/0", "value" "bar"}]) ["bar" "foo" "sil"])))
  (testing "push item to array via last index + 1"
    (is (= (json-pointer/patch ["foo" "sil"] [{"op" "add", "path" "/2", "value" "bar"}]) ["foo" "sil" "bar"])))
  (testing "add item to array at index > length should fail"
    (is (thrown? ExceptionInfo (json-pointer/patch ["foo" "sil"] [{"op" "add", "path" "/3", "value" "bar"}]))))
  (testing "test against implementation-specific numeric parsing"
    (is (= (json-pointer/patch {"1e0" "foo"} [{"op" "test", "path" "/1e0", "value" "foo"}]) {"1e0" "foo"})))
  (testing "test with bad number should fail"
    (is (thrown? ExceptionInfo (json-pointer/patch ["foo" "bar"] [{"op" "test", "path" "/1e0", "value" "bar"}]))))
  (testing "unnamed test"
    (is (thrown? ExceptionInfo (json-pointer/patch ["foo" "sil"] [{"op" "add", "path" "/bar", "value" 42}]))))
  (testing "value in array add not flattened"
    (is (= (json-pointer/patch ["foo" "sil"] [{"op" "add", "path" "/1", "value" ["bar" "baz"]}])
           ["foo" ["bar" "baz"] "sil"])))
  (testing "unnamed test"
    (is (= (json-pointer/patch {"foo" 1, "bar" [1 2 3 4]} [{"op" "remove", "path" "/bar"}]) {"foo" 1})))
  (testing "unnamed test"
    (is (= (json-pointer/patch {"foo" 1, "baz" [{"qux" "hello"}]} [{"op" "remove", "path" "/baz/0/qux"}])
           {"foo" 1, "baz" [{}]})))
  (testing "unnamed test"
    (is (= (json-pointer/patch {"foo" 1, "baz" [{"qux" "hello"}]} [{"op" "replace", "path" "/foo", "value" [1 2 3 4]}])
           {"foo" [1 2 3 4], "baz" [{"qux" "hello"}]})))
  (testing "unnamed test"
    (is (= (json-pointer/patch {"foo" [1 2 3 4], "baz" [{"qux" "hello"}]}
                               [{"op" "replace", "path" "/baz/0/qux", "value" "world"}])
           {"foo" [1 2 3 4], "baz" [{"qux" "world"}]})))
  (testing "unnamed test"
    (is (= (json-pointer/patch ["foo"] [{"op" "replace", "path" "/0", "value" "bar"}]) ["bar"])))
  (testing "unnamed test"
    (is (= (json-pointer/patch [""] [{"op" "replace", "path" "/0", "value" 0}]) [0])))
  (testing "unnamed test"
    (is (= (json-pointer/patch [""] [{"op" "replace", "path" "/0", "value" true}]) [true])))
  (testing "unnamed test"
    (is (= (json-pointer/patch [""] [{"op" "replace", "path" "/0", "value" false}]) [false])))
  (testing "unnamed test"
    (is (= (json-pointer/patch [""] [{"op" "replace", "path" "/0", "value" nil}]) [nil])))
  (testing "value in array replace not flattened"
    (is (= (json-pointer/patch ["foo" "sil"] [{"op" "replace", "path" "/1", "value" ["bar" "baz"]}])
           ["foo" ["bar" "baz"]])))
  (testing "replace whole document"
    (is (= (json-pointer/patch {"foo" "bar"} [{"op" "replace", "path" "", "value" {"baz" "qux"}}]) {"baz" "qux"})))
  (testing "test replace with missing parent key should fail"
    (is (thrown? ExceptionInfo
                 (json-pointer/patch {"bar" "baz"} [{"op" "replace", "path" "/foo/bar", "value" false}]))))
  (testing "spurious patch properties"
    (is (= (json-pointer/patch {"foo" 1} [{"op" "test", "path" "/foo", "value" 1, "spurious" 1}]) {"foo" 1})))
  (testing "nil value should be valid obj property"
    (is (= (json-pointer/patch {"foo" nil} [{"op" "test", "path" "/foo", "value" nil}]) {"foo" nil})))
  (testing "nil value should be valid obj property to be replaced with something truthy"
    (is (= (json-pointer/patch {"foo" nil} [{"op" "replace", "path" "/foo", "value" "truthy"}]) {"foo" "truthy"})))
  (testing "nil value should be valid obj property to be moved"
    (is (= (json-pointer/patch {"foo" nil} [{"op" "move", "from" "/foo", "path" "/bar"}]) {"bar" nil})))
  (testing "nil value should be valid obj property to be copied"
    (is (= (json-pointer/patch {"foo" nil} [{"op" "copy", "from" "/foo", "path" "/bar"}]) {"foo" nil, "bar" nil})))
  (testing "nil value should be valid obj property to be removed"
    (is (= (json-pointer/patch {"foo" nil} [{"op" "remove", "path" "/foo"}]) {})))
  (testing "nil value should still be valid obj property replace other value"
    (is (= (json-pointer/patch {"foo" "bar"} [{"op" "replace", "path" "/foo", "value" nil}]) {"foo" nil})))
  (testing "test should pass despite rearrangement"
    (is (= (json-pointer/patch {"foo" {"foo" 1, "bar" 2}} [{"op" "test", "path" "/foo", "value" {"bar" 2, "foo" 1}}])
           {"foo" {"foo" 1, "bar" 2}})))
  (testing "test should pass despite (nested) rearrangement"
    (is (= (json-pointer/patch {"foo" [{"foo" 1, "bar" 2}]}
                               [{"op" "test", "path" "/foo", "value" [{"bar" 2, "foo" 1}]}])
           {"foo" [{"foo" 1, "bar" 2}]})))
  (testing "test should pass - no error"
    (is (= (json-pointer/patch {"foo" {"bar" [1 2 5 4]}} [{"op" "test", "path" "/foo", "value" {"bar" [1 2 5 4]}}])
           {"foo" {"bar" [1 2 5 4]}})))
  (testing "unnamed test"
    (is (thrown? ExceptionInfo
                 (json-pointer/patch {"foo" {"bar" [1 2 5 4]}} [{"op" "test", "path" "/foo", "value" [1 2]}]))))
  (testing "Whole document"
    (is (= (json-pointer/patch {"foo" 1} [{"op" "test", "path" "", "value" {"foo" 1}}]) nil)))
  (testing "Empty-string element"
    (is (= (json-pointer/patch {"" 1} [{"op" "test", "path" "/", "value" 1}]) {"" 1})))
  (testing "unnamed test"
    (is (= (json-pointer/patch
            {"" 0, "m~n" 8, "foo" ["bar" "baz"], "k\"l" 6, "a/b" 1, "i\\j" 5, "c%d" 2, "e^f" 3, "g|h" 4, " " 7}
            [{"op" "test", "path" "/foo", "value" ["bar" "baz"]}
             {"op" "test", "path" "/foo/0", "value" "bar"}
             {"op" "test", "path" "/", "value" 0}
             {"op" "test", "path" "/a~1b", "value" 1}
             {"op" "test", "path" "/c%d", "value" 2}
             {"op" "test", "path" "/e^f", "value" 3}
             {"op" "test", "path" "/g|h", "value" 4}
             {"op" "test", "path" "/i\\j", "value" 5}
             {"op" "test", "path" "/k\"l", "value" 6}
             {"op" "test", "path" "/ ", "value" 7}
             {"op" "test", "path" "/m~0n", "value" 8}])
           {"" 0, "m~n" 8, "foo" ["bar" "baz"], "k\"l" 6, "a/b" 1, "i\\j" 5, "c%d" 2, "e^f" 3, "g|h" 4, " " 7})))
  (testing "Move to same location has no effect"
    (is (= (json-pointer/patch {"foo" 1} [{"op" "move", "from" "/foo", "path" "/foo"}]) {"foo" 1})))
  (testing "unnamed test"
    (is (= (json-pointer/patch {"foo" 1, "baz" [{"qux" "hello"}]} [{"op" "move", "from" "/foo", "path" "/bar"}])
           {"baz" [{"qux" "hello"}], "bar" 1})))
  (testing "unnamed test"
    (is (= (json-pointer/patch {"baz" [{"qux" "hello"}], "bar" 1} [{"op" "move", "from" "/baz/0/qux", "path" "/baz/1"}])
           {"baz" [{} "hello"], "bar" 1})))
  (testing "unnamed test"
    (is (= (json-pointer/patch {"baz" [{"qux" "hello"}], "bar" 1} [{"op" "copy", "from" "/baz/0", "path" "/boo"}])
           {"baz" [{"qux" "hello"}], "bar" 1, "boo" {"qux" "hello"}})))
  (testing "replacing the root of the document is possible with add"
    (is (= (json-pointer/patch {"foo" "bar"} [{"op" "add", "path" "", "value" {"baz" "qux"}}]) {"baz" "qux"})))
  (testing "Adding to \"/-\" adds to the end of the array"
    (is (= (json-pointer/patch [1 2] [{"op" "add", "path" "/-", "value" {"foo" ["bar" "baz"]}}])
           [1 2 {"foo" ["bar" "baz"]}])))
  (testing "Adding to \"/-\" adds to the end of the array, even n levels down"
    (is (= (json-pointer/patch [1 2 [3 [4 5]]] [{"op" "add", "path" "/2/1/-", "value" {"foo" ["bar" "baz"]}}])
           [1 2 [3 [4 5 {"foo" ["bar" "baz"]}]]])))
  (testing "test remove with bad number should fail"
    (is (thrown? ExceptionInfo
                 (json-pointer/patch {"foo" 1, "baz" [{"qux" "hello"}]} [{"op" "remove", "path" "/baz/1e0/qux"}]))))
  (testing "test remove on array"
    (is (= (json-pointer/patch [1 2 3 4] [{"op" "remove", "path" "/0"}]) [2 3 4])))
  (testing "test repeated removes"
    (is (= (json-pointer/patch [1 2 3 4] [{"op" "remove", "path" "/1"} {"op" "remove", "path" "/2"}]) [1 3])))
  (testing "test remove with bad index should fail"
    (is (thrown? ExceptionInfo (json-pointer/patch [1 2 3 4] [{"op" "remove", "path" "/1e0"}]))))
  (testing "test replace with bad number should fail"
    (is (thrown? ExceptionInfo (json-pointer/patch [""] [{"op" "replace", "path" "/1e0", "value" false}]))))
  (testing "test copy with bad number should fail"
    (is (thrown? ExceptionInfo
                 (json-pointer/patch {"baz" [1 2 3], "bar" 1} [{"op" "copy", "from" "/baz/1e0", "path" "/boo"}]))))
  (testing "test move with bad number should fail"
    (is (thrown? ExceptionInfo
                 (json-pointer/patch {"foo" 1, "baz" [1 2 3 4]} [{"op" "move", "from" "/baz/1e0", "path" "/foo"}]))))
  (testing "test add with bad number should fail"
    (is (thrown? ExceptionInfo (json-pointer/patch ["foo" "sil"] [{"op" "add", "path" "/1e0", "value" "bar"}]))))
  (testing "missing 'path' parameter"
    (is (thrown? ExceptionInfo (json-pointer/patch {} [{"op" "add", "value" "bar"}]))))
  (testing "'path' parameter with nil value"
    (is (thrown? ExceptionInfo (json-pointer/patch {} [{"op" "add", "path" nil, "value" "bar"}]))))
  (testing "invalid JSON Pointer token"
    (is (thrown? ExceptionInfo (json-pointer/patch {} [{"op" "add", "path" "foo", "value" "bar"}]))))
  (testing "missing 'value' parameter to add"
    (is (thrown? ExceptionInfo (json-pointer/patch [1] [{"op" "add", "path" "/-"}]))))
  (testing "missing 'value' parameter to replace"
    (is (thrown? ExceptionInfo (json-pointer/patch [1] [{"op" "replace", "path" "/0"}]))))
  (testing "missing 'value' parameter to test"
    (is (thrown? ExceptionInfo (json-pointer/patch [nil] [{"op" "test", "path" "/0"}]))))
  (testing "missing value parameter to test - where undef is falsy"
    (is (thrown? ExceptionInfo (json-pointer/patch [false] [{"op" "test", "path" "/0"}]))))
  (testing "missing from parameter to copy"
    (is (thrown? ExceptionInfo (json-pointer/patch [1] [{"op" "copy", "path" "/-"}]))))
  (testing "missing from location to copy"
    (is (thrown? ExceptionInfo (json-pointer/patch {"foo" 1} [{"op" "copy", "from" "/bar", "path" "/foo"}]))))
  (testing "missing from parameter to move"
    (is (thrown? ExceptionInfo (json-pointer/patch {"foo" 1} [{"op" "move", "path" ""}]))))
  (testing "missing from location to move"
    (is (thrown? ExceptionInfo (json-pointer/patch {"foo" 1} [{"op" "move", "from" "/bar", "path" "/foo"}]))))
  (testing "unrecognized op should fail"
    (is (thrown? ExceptionInfo (json-pointer/patch {"foo" 1} [{"op" "spam", "path" "/foo", "value" 1}]))))
  (testing "test with bad array number that has leading zeros"
    (is (thrown? ExceptionInfo (json-pointer/patch ["foo" "bar"] [{"op" "test", "path" "/00", "value" "foo"}]))))
  (testing "test with bad array number that has leading zeros"
    (is (thrown? ExceptionInfo (json-pointer/patch ["foo" "bar"] [{"op" "test", "path" "/01", "value" "bar"}]))))
  (testing "Removing nonexistent field"
    (is (thrown? ExceptionInfo (json-pointer/patch {"foo" "bar"} [{"op" "remove", "path" "/baz"}]))))
  (testing "Removing deep nonexistent path"
    (is (thrown? ExceptionInfo (json-pointer/patch {"foo" "bar"} [{"op" "remove", "path" "/missing1/missing2"}]))))
  (testing "Removing nonexistent index"
    (is (thrown? ExceptionInfo (json-pointer/patch ["foo" "bar"] [{"op" "remove", "path" "/2"}]))))
  (testing "Patch with different capitalisation than doc"
    (is (= (json-pointer/patch {"foo" "bar"}
                               [{"op" "add", "path" "/FOO", "value" "BAR"}]) {"foo" "bar", "FOO" "BAR"}))))
