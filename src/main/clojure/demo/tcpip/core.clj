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

  demo.tcpip.core

  (:require [clojure.tools.logging :as log :only [info warn error debug]]
            [clojure.string :as cstr])

  (:use [czlabclj.xlib.util.process :only [DelayExec]]
        [czlabclj.xlib.util.core :only [Try! notnil?]]
        [czlabclj.xlib.util.str :only [nsb]]
        [czlabclj.xlib.util.wfs :only [SimPTask]])

  (:import  [java.io DataOutputStream DataInputStream BufferedInputStream]
            [com.zotohlab.wflow Job FlowNode PTask Delay PDelegate]
            [com.zotohlab.gallifrey.io SocketEvent]
            [com.zotohlab.gallifrey.core Container]
            [java.net Socket]
            [java.util Date]
            [com.zotohlab.frwk.server Service]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;(set! *warn-on-reflection* true)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(def ^:private ^String TEXTMsg "Hello World, time is ${TS} !")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(deftype DemoClient [] PDelegate

  (onError [_ _ _])
  (onStop [_ _])
  (startWith [_ pipe]
    (require 'demo.tcpip.core)
    ;; wait, then opens a socket and write something to server process.
    (-> (Delay/apply 3000)
        (.chain
          (SimPTask
            (fn [j]
              (with-local-vars [tcp (-> (.container pipe)
                                        (.getService :default-sample))
                                s (.replace TEXTMsg
                                            "${TS}" (.toString (Date.)))
                                ssoc nil]
                (println "TCP Client: about to send message" @s)
                (try
                  (let [bits (.getBytes ^String @s "utf-8")
                        port (.getv ^Service @tcp :port)
                        ^String host (.getv ^Service @tcp :host)
                        soc (Socket. host (int port))
                        os (.getOutputStream soc) ]
                    (var-set ssoc soc)
                    (-> (DataOutputStream. os)
                        (.writeInt (int (alength bits))))
                    (doto os
                      (.write bits)
                      (.flush)))
                  (finally
                    (Try! (when-not (nil? @ssoc)
                            (.close ^Socket @ssoc)))))
                nil)))))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(deftype DemoServer [] PDelegate

  (onError [_ _ _])
  (onStop [_ _])
  (startWith [_ pipe]
    (require 'demo.tcpip.core)
    (-> (SimPTask
          (fn [^Job job]
            (let [^SocketEvent ev (.event job)
                  dis (DataInputStream. (.getSockIn ev))
                  clen (.readInt dis)
                  bf (BufferedInputStream. (.getSockIn ev))
                  ^bytes buf (byte-array clen) ]
              (.read bf buf)
              (.setv job "cmsg" (String. buf "utf-8"))
              ;; add a delay into the workflow before next step
              (Delay/apply 1500))))
        (.chain
          (SimPTask
            (fn [^Job job]
              (println "Socket Server Received: "
                       (.getv job "cmsg"))))))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(deftype DemoMain [] czlabclj.tardis.impl.ext.CljAppMain

  (contextualize [_ c] )

  (initialize [_]
    (println "Demo sending & receiving messages via sockets..." ))

  (configure [_ cfg] )

  (start [_] )
  (stop [_] )

  (dispose [_] ))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(def ^:private core-eof nil)


