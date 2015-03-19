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

  demo.mvc.core

  (:require [clojure.tools.logging :as log :only [info warn error debug]]
            [clojure.string :as cstr])

  (:use [cmzlabclj.xlib.util.process :only [DelayExec]]
        [cmzlabclj.xlib.util.core :only [notnil?]]
        [cmzlabclj.xlib.util.str :only [nsb]]
        [cmzlabclj.tardis.core.wfs :only [DefWFTask]])

  (:import  [com.zotohlab.wflow FlowNode PTask PipelineDelegate]
            [com.zotohlab.gallifrey.io HTTPEvent HTTPResult]
            [com.zotohlab.gallifrey.core Container]
            [com.zotohlab.wflow.core Job]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;(set! *warn-on-reflection* true)

(def ^:private ^String FMTHtml
  (str "  <html><head>"
       "<title>Skaro: Test Web</title>"
       "<link rel=\"shortcut icon\" href=\"/public/media/site/favicon.ico\"/>"
       "<link type=\"text/css\" rel=\"stylesheet\" href=\"/public/styles/site/main.css\"/>"
       "<script type=\"text/javascript\" src=\"/public/scripts/site/test.js\"></script>"
       "</head>"
       "<body><h1>Bonjour!</h1><br/>"
       "<button type=\"button\" onclick=\"pop();\">Click Me!</button>"
       "</body></html>"))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(deftype Demo [] PipelineDelegate

  (getStartActivity [_ pipe]
    (require 'demo.mvc.core)
    (DefWFTask
      (fn [cur ^Job job arg]
        (let [^HTTPEvent ev (.event job)
              res (.getResultObj ev) ]
          (doto res
            (.setContent FMTHtml)
            (.setStatus 200))
          (.replyResult ev)
          nil))))

  (onStop [_ p] )
  (onError [_ e c] nil))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(deftype DemoMain [] cmzlabclj.tardis.impl.ext.CljAppMain

  (contextualize [_ c] )

  (initialize [_]
    (println "Point your browser to http://localhost:8000/test/hello"))

  (configure [_ cfg] )

  (start [_] )

  (stop [_] )

  (dispose [_] ))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(def ^:private core-eof nil)

