(ns ^:figwheel-hooks sci.web
  (:require
   [applied-science.js-interop :as j]
   [cljsjs.codemirror]
   [cljsjs.codemirror.addon.edit.matchbrackets]
   [cljsjs.codemirror.addon.lint.lint]
   [cljsjs.codemirror.addon.runmode.runmode]
   [cljsjs.codemirror.mode.clojure]
   [cljsjs.parinfer]
   [cljsjs.parinfer-codemirror]
   [reagent.core :as r]
   [sci.core :refer [eval-string]]
   [sci.gist :as gist])
  (:import [goog Uri]))

(defonce initial-code (atom "(defmacro bindings []
  (zipmap (mapv #(list 'quote %) (keys &env))
          (keys &env)))

(let [x 1] (bindings))
"))

(defonce initial-opts (atom "{:realize-max 100}\n"))

(defonce gist-ref (r/atom ""))
(defonce title-ref (r/atom ""))

(defonce editor-ref (atom nil))
(defonce options-ref (atom nil))

(defonce warnings-ref (atom []))

(defn state-from-query-params [cb]
  (let [uri (-> js/window .-location .-href)
        uri (.parse Uri uri)
        qd (.getQueryData uri)
        gist (first (.getValues qd "gist"))]
    (if gist
      (do (reset! gist-ref gist)
          (gist/load-gist gist (fn [{:keys [:title :options :code]}]
                                 (reset! title-ref title)
                                 (reset! initial-code code)
                                 (reset! initial-opts options)
                                 (cb))))
      (cb))))

(defn eval! []
  (let [editor @editor-ref
        opts @options-ref]
    (try
      (doseq [node @warnings-ref]
        ;; see https://github.com/codemirror/CodeMirror/blob/75b12befaadff25de537f4117f13c38fce0c6895/demo/widget.html#L38
        (j/call editor :removeLineWidget node))
      (reset! warnings-ref [])
      (let [opts (eval-string (.getValue opts) {:realize-max 10000})
            res (eval-string (.getValue editor) (assoc-in opts [:namespaces 'clojure.core 'prn] prn))
            res-string (pr-str res)]
        (j/call js/CodeMirror :runMode res-string "clojure" (js/document.getElementById "result")))
      (catch js/Error e
        (let [{:keys [:row]} (ex-data e)]
          (if row
            (let [msg (j/get e :message)
                  msg-node (js/document.createElement "div")
                  icon-node (.appendChild msg-node (js/document.createElement "span"))]
              (set! (.-innerHTML icon-node) "!!")
              (set! (.-className icon-node) "lint-error-icon")
              (.appendChild msg-node (js/document.createTextNode msg))
              (set! (.-className msg-node) "lint-error")
              ;; see https://github.com/codemirror/CodeMirror/blob/75b12befaadff25de537f4117f13c38fce0c6895/demo/widget.html#L51
              (let [lw (j/call editor :addLineWidget (dec row) msg-node)]
                (swap! warnings-ref conj lw)))
            (j/call js/CodeMirror :runMode (str "ERROR: " (j/get e :message))
                    "clojure"
                    (js/document.getElementById "result"))))))))

(defn editor [id]
  (r/create-class
   {:render (fn [] [:textarea
                    {:type "text"
                     :id id
                     :default-value (case id
                                      "code" @initial-code
                                      "opts" @initial-opts)
                     :auto-complete "off"}])
    :component-did-mount
    (fn [this]
      (let [node (r/dom-node this)
            opts #js {:mode "clojure"
                      :matchBrackets true
                      ;;parinfer does this better
                      ;;:autoCloseBrackets true
                      :lineNumbers true
                      :lint #js {:lintOnChange false}
                      :gutters #js ["CodeMirror-lint-markers"]}
            cm (.fromTextArea js/CodeMirror
                              node
                              opts)]
        (js/parinferCodeMirror.init cm)
        (.removeKeyMap cm)
        (.setOption cm "extraKeys" #js {:Shift-Tab false
                                        :Tab false})
        (reset! (case id
                  "code" editor-ref
                  "opts"  options-ref) cm)
        (eval!)))
    :component-will-unmount
    (fn []
      (let [cm (case id
                      "code" @editor-ref
                      "opts"  @options-ref)]
        ;; toTextArea will destroy and clean up cm
        (j/call cm :toTextArea cm)))}))

(defn controls []
  [:div.buttons
   [:button.btn.btn-sm.btn-outline-primary
    {:on-click eval!}
    "eval!"]
   [:button.btn.btn-sm.btn-outline-primary
    {:on-click #(do (j/call @editor-ref :setValue "\n\n")
                    (j/call @options-ref :setValue "\n\n"))}
    "clear!"]
   [:button.btn.btn-sm.btn-outline-primary
    {:on-click #(do (j/call @editor-ref :setValue @initial-code)
                    (j/call @options-ref :setValue @initial-opts))}
    "reset!"]])

(defn new-address [gist]
  (let [uri (.parse Uri (.. js/window -location -href))
        qd (-> (Uri.QueryData.)
               (.add "gist" gist))]
    (.setQueryData uri qd)
    (str uri)))

(defn load-example [event gist]
  (.preventDefault event)
  (let [new-url (new-address gist)]
    (when (not= new-url (.. js/window -location -href))
      (.pushState js/window.history nil "" new-url)
      (state-from-query-params #(do (j/call @editor-ref :setValue @initial-code)
                                    (j/call @options-ref :setValue @initial-opts))))))

(def example-data
  [{:gist "borkdude/33d757d5080eb61051c5db9c597d0b38" :title "Reader conditionals"}
   {:gist "borkdude/66a5a4614985f7de30f849650c05ed71" :title "Realize max"}])

(defn examples []
  [:div
   [:h3 "Examples:"]
   [:ul
    (for [{:keys [:gist :title]} example-data]
      ^{:key gist}
      [:li [:a {:href ""
                :on-click #(load-example % gist)} title]])]])

(defn app []
  [:div#sci.container
   [:div.row
    [:p.col-12.lead
     [:span [:a {:href "https://github.com/borkdude/sci"}
             "Small Clojure Interpreter"]
      " playground"]]]
   [:div
    (when-let [t (not-empty @title-ref)]
      [:div
       [:h2 t]])
    [controls]
    [:h3 "Code"]
    [editor "code"]
    [:h3 "Options"]
    [editor "opts"]
    [controls]
    [:h3 "Result:"]
    [:div#result.cm-s-default.mono.inline]
    [examples]]])

(defn mount [el]
  (r/render-component [app] el))

(defn mount-app-element []
  (when-let [el (js/document.getElementById "app")]
    (state-from-query-params #(mount el))))

;; conditionally start your application based on the presence of an "app" element
;; this is particularly helpful for testing this ns without launching the app
(mount-app-element)

;; specify reload hook with ^;after-load metadata
(defn ^:after-load on-reload []
  (mount-app-element)
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )
