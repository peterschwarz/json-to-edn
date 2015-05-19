(ns json-to-edn.core-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [json-to-edn.core :refer [json->edn edn->json rows-needed]]))

(deftest test-json->edn
  (is (= {:foo true} (json->edn "{\"foo\": true}")))
  
  (is (= {:bar "foo"} (json->edn #js { "bar" "foo"})))

  (is (nil? (json->edn "{\"bad-json\": true, }")))) 

(deftest test-edn->json
  ; from string
  (let [json-obj (edn->json "{:one 1 :two [1 2]}")]
    (is (= 1 (.-one json-obj)))
    (is (= 1 (aget (.-two json-obj) 0)))
    (is (= 2 (aget (.-two json-obj) 1))))

  ; From actual edn
  (let [json-obj (edn->json ["a" {:x 1 :y 2}])]

    (is (= "a" (aget json-obj 0)))
    
    (is (= 1 (-> json-obj 
                (aget 1)
                .-x)))

    (is (= 2 (-> json-obj 
                (aget 1)
                .-y))))

   (is (nil? (edn->json "{:bad :edn")))
  )

(deftest test-rows-needed
  (is (= 5 (rows-needed "hello")))
  (is (= 10 (rows-needed "h\ne\nl\nl\no\nW\no\nr\nl\nd"))))