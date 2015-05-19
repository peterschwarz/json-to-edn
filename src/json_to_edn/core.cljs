(ns ^:figwheel-always json-to-edn.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [om.core :as om]
            [om.dom :as dom]
            [cljs.core.async :refer [put! chan <!]]
            [cljs.pprint :as pp]))

(enable-console-print!)

(defn element-by-id [id]
  (. js/document (getElementById id)))

(def ^:private sample-json-code "
{
  \"json\": true,
  \"converted\": {
    \"edn\": true
  }
}
")

(defn- safe-parse [json-str]
  (try
    (.parse js/JSON json-str)
    (catch js/SyntaxError e
      nil)))

(defn json->edn [json]
  (let [obj (if (string? json) (safe-parse json) json)]
    (js->clj obj :keywordize-keys true)))


(defn json-str [json]
  (if json
    (.stringify js/JSON json nil 2)) )

(defn- safe-read [edn-str]
  (try 
    (cljs.reader/read-string edn-str)
    (catch :default e
      nil)))

(defn edn->json [edn]
  (let [c (if (string? edn) (safe-read edn) edn)]
    (clj->js c)))

(defn edn-str [edn]
  (if edn
    (str edn)))

(defn rows-needed [s]
  (.max js/Math (count (.split s "\n")) 5))

(defonce app-state 
  (atom {:json {:code sample-json-code}
         :edn  {:code (str (json->edn sample-json-code))
                :is-output? true}}))

(defn handle-code-change [e source ch]
  (when (not (:is-output? source))
    (let [updated-code (.. e -target -value)]
      (om/transact! source #(assoc % :code updated-code))
      (put! ch updated-code))))

(defn- to-title [k] (.toUpperCase (name k)))

(defmulti display-code (fn [_ {:keys [is-output?]} _] (if is-output? :read-only :editor)))

(defmethod display-code :read-only
  [source-key source _]
  (println "rendering, read-only" source-key)
  (dom/pre #js {:className "fill"} (:code source)))

(defmethod display-code :editor
  [source-key source {:keys [translation-target]}]
  (println "rendering, editor" source-key)
  (dom/textarea #js {:id (str "input-" (name source-key))
                     :key source-key
                     :value (:code source)
                     :className "form-control"
                     :rows (str (rows-needed (:code source)))
                     :onChange #(handle-code-change % source translation-target)}))

(defmethod display-code :default [_ _ _] 
  (dom/p #js {:className "bg-danger"} "Unknown display type"))

(defn source-code 
  [source-key tranlation-fn translation-source translation-target]
  (fn [source owner]
    (reify
      om/IWillMount
      (will-mount [_]
            (go-loop []
              (let [new-source (<! translation-source)
                    translated-src (tranlation-fn new-source)]
                (when translated-src
                  (om/transact! source #(assoc % :code translated-src))))
              (recur)))
      om/IRender
      (render [_]
        (dom/div #js {:className "col-xs-6"} 
          (dom/h4 nil (to-title source-key))
          (display-code source-key source {:translation-target translation-target}))))))

(defn- swapTranslation [e app]
  (let [current-editor (if (get-in app [:json :is-output?]) :edn :json)
        next-editor (if (= :json current-editor) :edn :json)]
    (om/transact! app (fn [a]
      (-> a
        (assoc-in [current-editor :is-output?] true)
        (assoc-in [next-editor :is-output?] false))))))

(defn controls [app owner]
  (om/component
    (dom/div #js {:className "translation-ctrls"}
      (dom/button #js {:className "btn btn-default btn-block"
                       :onClick #(swapTranslation % app)} "<->"))))

(defn main [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:json-chan (chan)
       :edn-chan  (chan)})

    om/IRenderState
    (render-state [_ state]
      (let [{:keys [json-chan edn-chan]} state
            {:keys [json edn]} app]
        (dom/div #js {:className "container"}
          (dom/h2 nil "JSON <-> EDN")
          (dom/div #js {:className "row"}
            (om/build (source-code :json #(json-str (edn->json %)) 
                                   edn-chan json-chan) json)
            (om/build (source-code :edn #(edn-str (json->edn %)) 
                                   json-chan edn-chan) (:edn app)))
          (dom/div #js {:className "row"}
            (om/build controls app)))))))

(when-let [target (element-by-id "app")]
  (om/root main app-state {:target target}))

(defn on-js-reload []
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)


