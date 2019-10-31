(ns sci.gist
  (:require
   [ajax.core :as ajax]
   [ajax.formats :as formats]
   [cljs.reader :as edn]))

(defn path [id]
  (str "https://gist.githubusercontent.com/" id "/raw?" (rand-int 1000)))

(defn load-gist [id callback]
  (let [url (path id)]
    (ajax/GET url
              {:response-format (formats/text-response-format)
               :handler (fn [resp]
                          (let [edn (edn/read-string resp)]
                            (callback edn)))})))
