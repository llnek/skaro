;; Copyright (c) 2013-2017, Kenneth Leung. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns ^{:doc ""
      :author "Kenneth Leung"}

  czlab.test.wabbit.mock

  (:require [czlab.xlib.scheduler :refer [scheduler<>]]
            [czlab.xlib.meta :refer [getCldr]]
            [czlab.xlib.logging :as log]
            [clojure.string :as cs]
            [clojure.java.io :as io])

  (:use [czlab.wabbit.common.core]
        [czlab.wabbit.etc.core]
        [czlab.xlib.core]
        [czlab.xlib.str]
        [czlab.xlib.io]
        [czlab.wabbit.io.core]
        [czlab.wabbit.io.loops]
        [czlab.wabbit.io.mails]
        [czlab.wabbit.io.files]
        [czlab.wabbit.io.jms]
        [czlab.wabbit.io.http]
        [czlab.wabbit.io.socket])

  (:import [czlab.wabbit.etc Gist]
           [czlab.wabbit.server
            Execvisor
            Cljshim
            Container]
           [czlab.xlib
            Activable
            Schedulable]
           [czlab.wabbit.io IoGist IoEvent]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;(set! *warn-on-reflection* true)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- mkctr
  ""
  ^Container
  [^Execvisor parObj ^Gist gist]
  (let
    [rts (Cljshim/newrt (getCldr) "mock")
     ctx (.getx gist)
     impl (muble<> {:services {}})]
    (with-meta
      (reify
        Container
        (podKeyBits [this] (bytesify (.podKey this)))
        (podKey [_] "hello world")
        (podDir [this] (getCwd))
        (cljrt [_] rts)
        (getx [_] impl)
        (version [_] "1.0")
        (id [_] "007")
        (name [_] "mock")

        (acquireDbPool [this gid] nil)
        (acquireDbAPI [this gid] nil)
        (acquireDbPool [this] nil)
        (acquireDbAPI [this] nil)

        (parent [_] parObj)
        (setParent [_ x])

        (loadTemplate [_ tpath ctx])
        (isEnabled [_] true)

        (service [_ sid])
        (hasService [_ sid])

        (core [_]
          (.getv impl :core))

        (podConfig [_] {})

        (start [this _] )
        (stop [this] )
        (dispose [this]
          (.dispose (.core this))
          (.close rts)))

      {:typeid ::Container})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- init<c>
  ""
  ^Container
  [^Container co ^Execvisor execv]
  (let
    [cpu (scheduler<> (.id co))
     rts (.cljrt co)
     pid (.id co)]
    (.setv (.getx co) :core cpu)
    (.activate ^Activable cpu {})
    co))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- mkexe
  ^Execvisor
  []
  (let
    [impl (muble<> {:container nil
                    :pod nil
                    :services {}})]
    (with-meta
      (reify
        Execvisor
        (uptimeInMillis [_] 0)
        (id [_] "001")
        (homeDir [_] (getCwd))
        (locale [_] nil)
        (version [_] "1.0")
        (getx [_] impl)
        (startTime [_] 0)
        (kill9 [_] )
        (start [this _] )
        (stop [this] ))
      {:typeid ::Execvisor})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- mkgist
  ""
  ^Gist
  [^Execvisor ec]
  (let [impl (muble<>)]
    (with-meta
      (reify
        Gist
        (setParent [_ p] )
        (parent [_] ec)
        (version [_] "1.0")
        (id [_] "005")
        (getx [_] impl))
      {:typeid  ::PodGist})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn mock
  ""
  [kind]
  (case kind
    :execvisor nil
    :pod nil
    :container
    (let [e (mkexe)
          p (mkgist e)
          c (mkctr e p)]
      (init<c> c e))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;EOF

