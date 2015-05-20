(ns ^:figwheel-always json-to-edn.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [om.core :as om]
            [om.dom :as dom]
            [cljs.core.async :refer [put! chan <!]]
            [clojure.string]))

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

(def edn->json-str (comp json-str edn->json))
(def json->edn-str (comp edn-str json->edn))

(defonce app-state 
  (atom {:json {:code [sample-json-code]}
         :edn  {:code [(edn-str (json->edn sample-json-code))]}
         :editor [:json]}))

(defn- update-code! [source updated-code]
  (om/transact! source :code #(assoc % 0 updated-code)))

(defn handle-code-change [e source ch]
  (let [updated-code (.. e -target -value)]
    (println "transacting" (.. e -target -id))
    (update-code! source updated-code)
    (put! ch updated-code)))

(defn rows-needed [s]
  (.max js/Math (count (.split s "\n")) 5))

(defn to-title [k] 
  (clojure.string/capitalize (name k)))

(defn source-code 
  [{:keys [source is-editor?]} owner {:keys [source-key translation-fn translation-source translation-target]:as opts}]
  (reify
    om/IWillMount
    (will-mount [_]
          (go-loop []
            (let [new-source (<! translation-source)
                  translated-src (translation-fn new-source)]
              (when translated-src
                (update-code! source translated-src)))
            (recur)))
    om/IRender
    (render [_]
      (let [source-code (first (:code source))]
        (println "Rendering" source-key)
        (dom/div #js {:className "col-xs-6"} 
          (dom/h4 #js {:className "source-title"} (to-title source-key))
          (if is-editor?
            (dom/textarea #js {:id (str "input-" (name source-key))
                     :key (name source-key)
                     :value source-code
                     :className "form-control"
                     :rows (str (rows-needed source-code))
                     :onChange #(handle-code-change % source translation-target)})
            (dom/pre #js {:className "fill"
                          :key (name source-key)} source-code)))))))

(defn- swapTranslation [e app]
  (let [next-editor (if (= (get-in app [:editor 0]) :json) :edn :json)]
    (om/transact! app :editor #(assoc % 0 next-editor))))

(defn controls [app owner]
  (om/component
    (dom/div #js {:className "translation-ctrls"}
      (dom/button #js {:className "btn btn-default btn-block"
                       :onClick #(swapTranslation % app)} "swap!"))))

(defn main [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:json-chan (chan)
       :edn-chan  (chan)})

    om/IRenderState
    (render-state [_ state]
      (let [{:keys [json-chan edn-chan]} state
            editor (first (:editor app))]
        (println "rendering main - editor" editor)
        (dom/div #js {:className "container"}
          (dom/h2 nil "JSON <-> EDN")
          (dom/div #js {:className "row"}

            (om/build source-code {:is-editor? (= editor :json)
                                   :source (:json app)}
              { :opts {
                  :source-key :json
                  :translation-fn edn->json-str
                  :translation-source edn-chan
                  :translation-target json-chan }})

            (om/build source-code {:is-editor? (= editor :edn) 
                                   :source (:edn app)}
              { :opts {
                  :source-key :edn 
                  :translation-fn json->edn-str
                  :translation-source json-chan
                  :translation-target edn-chan }}))
          (dom/div #js {:className "row"}
            (om/build controls app)))))))

(when-let [target (element-by-id "app")]
  (om/root main app-state {:target target}))

(defn on-js-reload []
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)


