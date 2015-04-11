;; This library is distributed in  the hope that it will be useful but without
;; any  warranty; without  even  the  implied  warranty of  merchantability or
;; fitness for a particular purpose.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0  (http://opensource.org/licenses/eclipse-1.0.php)  which
;; can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any  fashion, you are agreeing to be bound by the
;; terms of this license. You  must not remove this notice, or any other, from
;; this software.
;; Copyright (c) 2013, Ken Leung. All rights reserved.

(ns ^{:doc ""
      :author "kenl"}

  demo.http.formpost

  (:require [clojure.tools.logging :as log :only [info warn error debug]]
            [clojure.string :as cstr])

  (:use [czlabclj.xlib.util.process :only [DelayExec]]
        [czlabclj.xlib.util.core :only [notnil?]]
        [czlabclj.xlib.util.str :only [nsb]]
        [czlabclj.xlib.util.wfs :only [SimPTask]])

  (:import  [com.zotohlab.wflow Job FlowNode PTask PDelegate]
            [com.zotohlab.gallifrey.io HTTPEvent HTTPResult]
            [java.util ListIterator]
            [com.zotohlab.frwk.io XData]
            [com.zotohlab.frwk.net ULFileItem ULFormItems]
            [com.zotohlab.gallifrey.core Container]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;(set! *warn-on-reflection* true)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(deftype Demo [] PDelegate

  (onError [_ _ _])
  (onStop [_ _])
  (startWith [_ pipe]
    (require 'demo.http.formpost)
    (SimPTask
      (fn [^Job job]
        (let [^HTTPEvent ev (.event job)
              res (.getResultObj ev)
              data (.data ev)
              stuff (if (and (notnil? data)
                             (.hasContent data))
                      (.content data)
                      nil) ]
          (cond
            (instance? ULFormItems stuff)
            (doseq [^ULFileItem fi (seq (.intern ^ULFormItems stuff))]
              (println "Fieldname : " (.getFieldName fi))
              (println "Name : " (.getName fi))
              (println "Formfield : " (.isFormField fi))
              (if (.isFormField fi)
                (println "Field value: " (.getString fi))
                (when-let [xs (.fileData fi)]
                  (println "Field file = " (.filePath xs)))))
            :else
            (println "Error: data is not ULFormItems."))
          (.setStatus res 200)
          ;; associate this result with the orignal event
          ;; this will trigger the http response
          (.replyResult ev)
          nil)))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(def ^:private core-eof nil)

