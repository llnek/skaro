;;
;; Copyright (c) 2013, Ken Leung. All rights reserved.
;;
;; This library is distributed in the hope that it will be useful
;; but without any warranty; without even the implied warranty of
;; merchantability or fitness for a particular purpose.
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;;
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.
;;

(ns ^{:doc ""
      :author "kenl" }

  cmzlabclj.tardis.io.triggers

  (:require [clojure.tools.logging :as log :only [info warn error debug]]
            [clojure.string :as cstr])

  (:use [cmzlabclj.xlib.util.core
         :only
         [ThrowIOE MakeMMap Stringify notnil? Try!]]
        [cmzlabclj.xlib.util.str :only [nsb]]
        [cmzlabclj.tardis.io.core])

  (:import  [com.zotohlab.gallifrey.io HTTPEvent HTTPResult]
            [java.io OutputStream IOException]
            [java.util List Timer TimerTask]
            [com.zotohlab.frwk.netty NettyFW]
            [com.zotohlab.frwk.io XData]
            [com.zotohlab.frwk.core Identifiable]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;(set! *warn-on-reflection* true)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn MakeAsyncWaitHolder

  [^cmzlabclj.tardis.io.core.AsyncWaitTrigger trigger
   ^HTTPEvent event ]

  (let [impl (MakeMMap) ]
    (reify

      Identifiable
      (id [_] (.getId event))

      WaitEventHolder

      (resumeOnResult [this res]
        (let [^Timer tm (.getf impl :timer)
              ^cmzlabclj.tardis.io.core.EmitterAPI
              src (.emitter event) ]
          (when-not (nil? tm) (.cancel tm))
          (.release src this)
          ;;(.mm-s impl :result res)
          (.resumeWithResult trigger res)))

      (timeoutMillis [me millis]
        (let [tm (Timer. true) ]
          (.setf! impl :timer tm)
          (.schedule tm (proxy [TimerTask][]
                          (run [] (.onExpiry me))) ^long millis)))

      (timeoutSecs [this secs]
        (timeoutMillis this (* 1000 secs)))

      (onExpiry [this]
        (let [^cmzlabclj.tardis.io.core.EmitterAPI
              src (.emitter event) ]
          (.release src this)
          (.setf! impl :timer nil)
          (.resumeWithError trigger) ))
  )))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(def ^:private triggers-eof nil)

