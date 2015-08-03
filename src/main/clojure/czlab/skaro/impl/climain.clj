;; This library is distributed in  the hope that it will be useful but without
;; any  warranty; without  even  the  implied  warranty of  merchantability or
;; fitness for a particular purpose.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0  (http://opensource.org/licenses/eclipse-1.0.php)  which
;; can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any  fashion, you are agreeing to be bound by the
;; terms of this license. You  must not remove this notice, or any other, from
;; this software.
;; Copyright (c) 2013-2015, Ken Leung. All rights reserved.

(ns ^{:doc ""
      :author "kenl" }

  czlab.skaro.impl.climain

  (:require
    [czlab.xlib.netty.discarder :refer [MakeDiscardHTTPD]]
    [czlab.xlib.util.str :refer [lcase hgl? nsb strim]]
    [czlab.xlib.util.ini :refer [ParseInifile]]
    [czlab.xlib.util.io :refer [CloseQ]]
    [czlab.xlib.util.process
    :refer [ProcessPid SafeWait ThreadFunc]]
    [czlab.xlib.i18n.resources :refer [GetResource]]
    [czlab.xlib.util.meta :refer [SetCldr GetCldr]]
    [czlab.xlib.util.format :refer [ReadEdn]]
    [czlab.xlib.util.files
    :refer [ReadOneFile WriteOneFile]]
    [czlab.xlib.util.scheduler :refer [NulScheduler]]
    [czlab.xlib.util.core
    :refer [test-nonil test-cond ConvLong
    Muble FPath PrintMutableObj MakeMMap]]
    [czlab.skaro.impl.exec :refer [MakeExecvisor]]
    [czlab.xlib.netty.io :refer [StopServer]])

  (:require
    [czlab.xlib.util.logging :as log]
    [clojure.java.io :as io])

  (:use [czlab.skaro.core.consts]
        [czlab.xlib.util.consts]
        [czlab.skaro.core.sys]
        [czlab.xlib.util.wfs]
        [czlab.skaro.impl.dfts])

  (:import
    [io.netty.channel Channel ChannelFuture
    ChannelFutureListener]
    [com.zotohlab.skaro.loaders AppClassLoader
    RootClassLoader ExecClassLoader]
    [com.zotohlab.frwk.core Versioned Identifiable
    Disposable Activable
    Hierarchial Startable]
    [com.zotohlab.frwk.util Schedulable]
    [com.zotohlab.frwk.i18n I18N]
    [com.zotohlab.wflow Job WorkFlow
    Activity Nihil]
    [com.zotohlab.skaro.core Context ConfigError]
    [com.zotohlab.skaro.etc CliMain]
    [io.netty.bootstrap ServerBootstrap]
    [com.google.gson JsonObject]
    [com.zotohlab.frwk.server ServerLike
    ServiceHandler
    Component ComponentRegistry]
    [com.zotohlab.skaro.etc CmdHelpError]
    [java.util ResourceBundle Locale]
    [java.io File]
    [org.apache.commons.io FileUtils]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;(set! *warn-on-reflection* true)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(def ^:private STOPCLI (atom false))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- inizContext

  "The context object has a set of properties, such as home dir, which
   is shared with other key components"

  ^czlab.xlib.util.core.Muble
  [^File baseDir]

  (let [etc (io/file baseDir DN_CFG)
        home (.getParentFile etc)]
    ;;(PrecondDir (io/file home DN_PATCH))
    (PrecondDir (io/file home DN_CONF))
    (PrecondDir (io/file home DN_DIST))
    (PrecondDir (io/file home DN_LIB))
    (PrecondDir (io/file home DN_BIN))
    (PrecondDir home)
    (PrecondDir etc)
    (doto (MakeContext)
      (.setf! K_BASEDIR home)
      (.setf! K_CFGDIR etc))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- stopCLI

  "Stop all apps and processors"

  [^czlab.xlib.util.core.Muble ctx]

  (let [^File pid (.getf ctx K_PIDFILE)
        kp (.getf ctx K_KILLPORT)
        execv (.getf ctx K_EXECV) ]

    (when-not @STOPCLI
      (reset! STOPCLI true)
      (print "\n\n")
      (log/info "closing the http discarder...")
      (StopServer (:bootstrap kp)
                  (:channel kp))
      (log/info "http discarder closed. OK")
      ;;(when-not (nil? pid) (io/delete-file pid true))
      (log/info "containers are shutting down...")
      (log/info "about to stop Skaro...")
      (when (some? execv)
        (.stop ^Startable execv))
      (log/info "skaro stopped")
      (log/info "vm shut down")
      (log/info "\"goodbye\""))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- enableRemoteShutdown

  "Listen on a port for remote kill command"

  [^czlab.xlib.util.core.Muble ctx]

  (log/info "enabling remote shutdown")
  (let [port (-> (System/getProperty "skaro.kill.port")
                 (ConvLong  4444))
        rc (MakeDiscardHTTPD "127.0.0.1"
                             port
                             #(stopCLI ctx) {}) ]
    (.setf! ctx K_KILLPORT rc)
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- cserver

  ^ServerLike
  [^File home]

  (let [cpu (NulScheduler)
        impl (MakeMMap)
        ctxt (atom (MakeMMap)) ]
    (-> ^Activable
        cpu
        (.activate {:threads 1}))
    (reify

      ServiceHandler

      (handle [this arg options]
        (let [w (ToWorkFlow arg)
              j (NewJob this w)]
          (doseq [[k v] options]
            (.setv j k v))
          (.run cpu (.reify (.startWith w)
                            (-> (Nihil/apply)
                                (.reify j))))))
      (handleError [_ e] )

      Disposable
      (dispose [_] (.dispose cpu))

      ServerLike

      (core [_] cpu)

      Context

      (setx [_ x] (reset! ctxt x))
      (getx [_] @ctxt)

      Muble

      (setf! [_ a v] (.setf! impl a v) )
      (clrf! [_ a] (.clrf! impl a) )
      (getf [_ a] (.getf impl a) )
      (seq* [_] )
      (clear! [_] (.clear! impl))
      (toEDN [_ ] (.toEDN impl))

      Hierarchial
      (parent [_] nil)

      Versioned
      (version [_]
        (str (System/getProperty "skaro.version")))

      Identifiable
      (id [_] K_CLISH))

  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- pauseCLI ""

  ^Activity
  []

  (SimPTask "PauseCLI"
    (fn [^Job j]
      (let [^czlab.xlib.util.core.Muble
            ctx (.getLastResult j)
            s (.container j)]
        (log/debug "#### sys loader = "
                   (-> (ClassLoader/getSystemClassLoader)
                       (.getClass)
                       (.getName)))
        (PrintMutableObj ctx)
        (log/info "container(s) are now running...")))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- hookShutdown ""

  ^Activity
  []

  (SimPTask "HookShutDown"
    (fn [^Job j]
      (let [^czlab.xlib.util.core.Muble
            ctx (.getLastResult j)
            cli (.getf ctx K_CLISH)]
        (.addShutdownHook (Runtime/getRuntime)
                          (ThreadFunc #(stopCLI ctx) false))
        (enableRemoteShutdown ctx)
        (log/info "added shutdown hook")))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- writePID ""

  ^Activity
  []

  (SimPTask "WritePID"
    (fn [^Job j]
      (let [^czlab.xlib.util.core.Muble
            ctx (.getLastResult j)
            home (.getf ctx K_BASEDIR)
            fp (io/file home "skaro.pid")]
        (WriteOneFile fp (ProcessPid))
        (.setf! ctx K_PIDFILE fp)
        (.deleteOnExit fp)
        (log/info "wrote skaro.pid - ok")))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- primodial

  "Create and synthesize Execvisor"

  ^Activity
  []

  (SimPTask "Primodial"
    (fn [^Job j]
      (let [^czlab.xlib.util.core.Muble
            ctx (.getLastResult j)
            cl (.getf ctx K_EXEC_CZLR)
            cli (.getf ctx K_CLISH)
            wc (.getf ctx K_PROPS)
            cz (str (get-in wc [K_COMPS K_EXECV])) ]
        ;;(test-cond "conf file:exec-visor" (= cz "czlab.skaro.impl.Execvisor"))
        (log/info "inside primodial() ---------------------------------------------->")
        (log/info "execvisor = %s" cz)
        (let [^czlab.xlib.util.core.Muble
              execv (MakeExecvisor cli)]
          (.setf! ctx K_EXECV execv)
          (SynthesizeComponent execv {:ctx ctx})
          (log/info "execvisor created and synthesized - ok")
          (log/info "*********************************************************")
          (log/info "*")
          (log/info "about to start Skaro...")
          (log/info "*")
          (log/info "*********************************************************")
          (.start ^Startable execv)
          (log/info "skaro started!"))))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- loadRes

  "Look for and load the resource bundle"

  ^Activity
  []

  (SimPTask "LoadResource"
    (fn [^Job j]
      (let [^czlab.xlib.util.core.Muble
            ctx (.getLastResult j)
            rc (GetResource "czlab.skaro.etc/Resources"
                            (.getf ctx K_LOCALE))]
        (test-nonil "etc/resouces" rc)
        (.setf! ctx K_RCBUNDLE rc)
        (I18N/setBase rc)
        (log/info "resource bundle found and loaded")))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- loadConf

  "Parse skaro.conf"

  ^Activity
  []

  (SimPTask "LoadConf"
    (fn [^Job j]
      (let [^czlab.xlib.util.core.Muble
            ctx (.getLastResult j)
            home (.getf ctx K_BASEDIR)
            cf (io/file home DN_CONF (name K_PROPS))]
        (log/info "about to parse config file %s" cf)
        (let [w (ReadEdn cf)
              cn (lcase (str (K_COUNTRY (K_LOCALE w)) ))
              lg (lcase (or (K_LANG (K_LOCALE w)) "en"))
              loc (if (hgl? cn)
                    (Locale. lg cn)
                    (Locale. lg))]
          (log/info "using locale: %s" loc)
          (doto ctx
            (.setf! K_LOCALE loc)
            (.setf! K_PROPS w)))))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- setupLoaders

  "Prepare class loaders.  The root class loader loads all the core libs
  The exec class loader inherits from the root and is the class loader
  that runs skaro"

  ^Activity
  []

  (SimPTask "SetupLoaders"
    (fn [^Job j]
      (let [^czlab.xlib.util.core.Muble
            x (.getLastResult j)
            cz (GetCldr)
            p (.getParent cz)
            pp (.getParent p)]
        (test-cond "bad classloaders" (and (instance? RootClassLoader pp)
                                           (instance? ExecClassLoader p)))
        (.setf! x K_ROOT_CZLR (.getParent p))
        (.setf! x K_EXEC_CZLR p)
        (log/info "classloaders configured: using %s" (type cz))))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- rtStart ""

  ^Activity
  []

  (SimPTask "RtStart"
    (fn [^Job j]
      (let [^czlab.xlib.util.core.Muble
            c (.container j)
            home (.getv j :home)
            x (inizContext home)]
        (log/info "skaro.home %s" (FPath home))
        (log/info "skaro.version= %s" (.version ^Versioned c))
        ;; a bit of circular referencing here.  the climain object refers to context
        ;; and the context refers back to the climain object.
        (.setf! x K_CLISH c)
        (-> ^Context c (.setx x))
        (log/info "home directory looks ok")
        (.setLastResult j x)))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn StartViaCLI ""

  [home]

  (let [cs (cserver home)
        a (-> (rtStart)
              (.chain (setupLoaders))
              (.chain (loadConf))
              (.chain (loadRes))
              (.chain (primodial))
              (.chain (writePID))
              (.chain (hookShutdown))
              (.chain (pauseCLI)))]
    (-> ^ServiceHandler
        cs
        (.handle a {:home home}))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;EOF
