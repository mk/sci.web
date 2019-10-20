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
   [sci.core :refer [eval-string]]))

(def initial-code "(defmacro bindings []
  (zipmap (mapv #(list 'quote %) (keys &env))
          (keys &env)))

(let [x 1] (bindings))
")

(defonce editor-ref (atom nil))

(defn editor [id path]
  (r/create-class
   {:render (fn [] [:textarea
                    {:type "text"
                     :id id
                     :default-value initial-code
                     :auto-complete "off"}])
    :component-did-mount
    (fn [this]
      (let [opts #js {:mode "clojure"
                      :matchBrackets true
                      ;;parinfer does this better
                      ;;:autoCloseBrackets true
                      :lineNumbers true
                      :lint #js {:lintOnChange false}
                      :gutters #js ["CodeMirror-lint-markers"]}
            cm (.fromTextArea js/CodeMirror
                              (r/dom-node this)
                              opts)]
        (js/parinferCodeMirror.init cm)
        (.removeKeyMap cm)
        (.setOption cm "extraKeys" #js {:Shift-Tab false
                                        :Tab false})
        (reset! editor-ref cm)))
    :component-will-unmount
    (fn []
      (let [cm @editor-ref]
        ;; toTextArea will destroy and clean up cm
        (.toTextArea cm)))}))

(defn eval! []
  (try (let [res (eval-string (.getValue @editor-ref) {:bindings {'prn prn
                                                                  'println println}
                                                       :realize-max 10000})
             res-string (pr-str res)]
         (js/CodeMirror.runMode res-string "clojure" (js/document.getElementById "result")))
       (catch ExceptionInfo e
         (set! (.-err1 js/window) e)
         (let [{:keys [:row]} (ex-data e)]
           (if row
             (let [msg (j/get e :message)
                   editor @editor-ref
                   msg-node (js/document.createElement "div")
                   icon-node (.appendChild msg-node (js/document.createElement "span"))
                   _ (set! (.-innerHTML icon-node) "!!")
                   _ (set! (.-className icon-node) "lint-error-icon")
                   _ (.appendChild msg-node (js/document.createTextNode msg))
                   _ (set! (.-className msg-node) "lint-error")]
               (j/call editor :addLineWidget (dec row) msg-node))
             (js/CodeMirror.runMode (str "ERROR: " (j/get e :message))
                                    "clojure"
                                    (js/document.getElementById "result")))))))

(defn controls []
  [:div.buttons
   [:button.btn.btn-sm.btn-outline-primary
    {:on-click eval! #_#(j/call @editor-ref :performLint)}
    "eval!"]
   [:button.btn.btn-sm.btn-outline-primary
    {:on-click #(.setValue @editor-ref "\n\n")}
    "clear!"]
   [:button.btn.btn-sm.btn-outline-primary
    {:on-click #(do (.setValue @editor-ref initial-code))}
    "reset!"]])

(defn app []
  [:div#sci.container
   [:div.row
    [:p.col-12.lead
     [:span [:a {:href "https://github.com/borkdude/sci"
                 :target "_blank"}
             "Small Clojure Interpreter"]
      " playground"]]]
   [:div
    [controls]
    [editor "code" [:code]]
    [controls]
    [:h2 "Result:"]
    [:div#result.cm-s-default.mono.inline]]])

(defn mount [el]
  (r/render-component [app] el))

(defn mount-app-element []
  (when-let [el (js/document.getElementById "app")]
    (mount el)))

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
