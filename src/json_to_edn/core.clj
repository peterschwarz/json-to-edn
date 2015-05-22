(ns json-to-edn.core
  (:require [endophile.core :refer [mp to-clj]]
            [clojure.string :as s]))

(def cljs->js (symbol "clj->js"))

(defn- to-om [part]
  (if (string? part)
    part
    (let [tag (name (:tag part))
        tag-sym (symbol (str "dom/" tag))
        content (:content part)
        attrs (:attrs part)
        rendered (if (seq? content) (mapv to-om content) content)]
    `(apply ~tag-sym (~cljs->js ~attrs) ~rendered))))

(defn- parse-md [filename]
  (try
    (-> filename slurp mp to-clj)
    (catch java.io.FileNotFoundException e
      [(str "Unable to find " filename)])))

(defmacro markdown->om [filename & {:keys [container container-opts]}]
  (let [parsed (parse-md filename)]
    `(apply dom/div ~container-opts ~(mapv to-om parsed))))
