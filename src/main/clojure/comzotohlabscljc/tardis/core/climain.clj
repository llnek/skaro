;; This library is distributed in  the hope that it will be useful but without
;; any  warranty; without  even  the  implied  warranty of  merchantability or
;; fitness for a particular purpose.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0  (http://opensource.org/licenses/eclipse-1.0.php)  which
;; can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any  fashion, you are agreeing to be bound by the
;; terms of this license. You  must not remove this notice, or any other, from
;; this software.
;; Copyright (c) 2013 Cherimoia, LLC. All rights reserved.


(ns ^{ :doc ""
       :author "kenl" }

  comzotohlabscljc.tardis.core.climain )

(use '[clojure.tools.logging :only (info warn error debug)])

(import '(org.jboss.netty.channel
  Channel ChannelFuture ChannelFutureListener))

(import '(com.zotohlabs.gallifrey.core ConfigError))
(import '(com.zotohlabs.frwk.server Component ComponentRegistry))
(import '(com.zotohlabs.frwk.core
  Versioned Identifiable Hierarchial
  Startable ))
(import '(com.zotohlabs.gallifrey.loaders
  AppClassLoader RootClassLoader ExecClassLoader))
(import '(com.zotohlabs.gallifrey.etc CmdHelpError))
(import '(java.util Locale))
(import '(java.io File))
(import '(org.apache.commons.io FileUtils))

(use '[comzotohlabscljc.i18n.resources :only [get-resource] ])
(use '[comzotohlabscljc.util.process :only [pid safe-wait] ])
(use '[comzotohlabscljc.util.meta :only [set-cldr get-cldr] ])
(use '[comzotohlabscljc.util.core :only [test-nonil test-cond conv-long Try! print-mutableObj make-mmap] ])
(use '[comzotohlabscljc.util.str :only [hgl? nsb strim] ])
(use '[comzotohlabscljc.util.ini :only [parse-inifile] ])
(use '[comzotohlabscljc.netty.comms])
(use '[comzotohlabscljc.tardis.impl.exec :only [make-execvisor] ])
(use '[comzotohlabscljc.tardis.core.constants])
(use '[comzotohlabscljc.tardis.core.sys])
(use '[comzotohlabscljc.tardis.impl.defaults])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;(set! *warn-on-reflection* true)

(def CLI-TRIGGER (promise))
(def STOPCLI (atom false))

(defn- inizContext ^comzotohlabscljc.util.core.MuObj [^File baseDir]
  (let [ cfg (File. baseDir ^String DN_CFG)
         home (.getParentFile cfg) ]
    (precondDir home)
    (precondDir cfg)
    (doto (make-context)
          (.setf! K_BASEDIR home)
          (.setf! K_CFGDIR cfg))))

(defn- setupClassLoader [^comzotohlabscljc.util.core.MuObj ctx]
  (let [ root (.getf ctx K_ROOT_CZLR)
         cl (ExecClassLoader. root) ]
    (set-cldr cl)
    (.setf! ctx K_EXEC_CZLR cl)
    ctx))

(defn- setupClassLoaderAsRoot [^comzotohlabscljc.util.core.MuObj ctx]
  (let [ root (RootClassLoader. (get-cldr)) ]
    (.setf! ctx K_ROOT_CZLR root)
    ctx))

(defn- maybeInizLoaders [^comzotohlabscljc.util.core.MuObj ctx]
  (let [ cz (get-cldr) ]
    (cond
      (instance? RootClassLoader cz)
      (do
        (.setf! ctx K_ROOT_CZLR cz)
        (setupClassLoader ctx))
      (instance? ExecClassLoader cz)
      (do
        (.setf! ctx K_ROOT_CZLR (.getParent cz))
        (.setf! ctx K_EXEC_CZLR cz))
      :else
      (setupClassLoader (setupClassLoaderAsRoot ctx)))
    (info "classloaders configured.  using ExecClassLoader.")
    ctx))

(defn- loadConf [^comzotohlabscljc.util.core.MuObj ctx]
  (let [ ^File home (.getf ctx K_BASEDIR)
         cf (File. home  (str DN_CONF "/" (name K_PROPS) ))
        ^comzotohlabscljc.util.ini.IWin32Conf
         w (parse-inifile cf)
         lg (.toLowerCase ^String (.optString w K_LOCALE K_LANG "en"))
         cn (.toUpperCase ^String (.optString w K_LOCALE K_COUNTRY ""))
         loc (if (hgl? cn) (Locale. lg cn) (Locale. lg)) ]
    (info (str "using locale: " loc))
    (doto ctx
      (.setf! K_PROPS w)
      (.setf! K_LOCALE loc))) )

(defn- setupResources [^comzotohlabscljc.util.core.MuObj ctx]
  (let [ rc (get-resource "comzotohlabscljc/tardis/etc/Resources"
                             (.getf ctx K_LOCALE)) ]
    (test-nonil "etc/resouces" rc)
    (.setf! ctx K_RCBUNDLE rc)
    (info "resource bundle found and loaded.")
    ctx))

(defn- pre-parse [^comzotohlabscljc.tardis.core.sys.Element cli args]
  (let [ bh (File. ^String (first args))
         ctx (inizContext bh) ]
    (info "inside pre-parse()")
    ;;(precondDir (File. bh ^String DN_BLOCKS))
    (precondDir (File. bh ^String DN_CFG))
    (precondDir (File. bh ^String DN_BOXX))
    (.setf! ctx K_CLISH cli)
    (.setCtx! cli ctx)
    (info "home directory looks ok.")
    ctx))

(defn- start-exec [^comzotohlabscljc.util.core.MuObj ctx]
  (do
    (info "about to start Skaro...")
    (let [ ^Startable exec (.getf ctx K_EXECV) ]
      (.start exec))
    (info "Skaro started.")
    ctx))

(defn- primodial [^comzotohlabscljc.util.core.MuObj ctx]
  (let [ cl (.getf ctx K_EXEC_CZLR)
         cli (.getf ctx K_CLISH)
        ^comzotohlabscljc.util.ini.IWin32Conf
         wc (.getf ctx K_PROPS)
         cz (.optString wc K_COMPS K_EXECV "") ]
    (test-cond "conf file:exec-visor"
                  (= cz "comzotohlabscljc.tardis.impl.Execvisor"))
    (info "inside primodial()")
    (let [ ^comzotohlabscljc.util.core.MuObj execv (make-execvisor cli) ]
      (.setf! ctx K_EXECV execv)
      (synthesize-component execv { :ctx ctx } )
      (info "Execvisor created and synthesized - OK.")
      ctx)))

(defn- stop-cli [^comzotohlabscljc.util.core.MuObj ctx]
  (let [ ^File pid (.getf ctx K_PIDFILE)
         execv (.getf ctx K_EXECV) ]
    (if-not @STOPCLI
      (do
        (reset! STOPCLI true)
        (when-not (nil? pid) (FileUtils/deleteQuietly pid))
        (info "about to stop Skaro...")
        (info "applications are shutting down...")
        (when-not (nil? execv)
          (.stop ^Startable execv))
        (info "Skaro stopped.")
        (info "Tardis says \"Goodbye\".")
        (deliver CLI-TRIGGER 911)))))

(defn- enableRemoteShutdown [^comzotohlabscljc.util.core.MuObj ctx]
  (let [ port (conv-long (System/getProperty "skaro.kill.port") 4444) ]
    (info "Enabling remote shutdown...")
    ;;TODO - how to clean this up
    (MakeDiscardHTTPD "127.0.0.1" port { :action (fn [] (stop-cli ctx)) })
  ))

(defn- hookShutdown [^comzotohlabscljc.util.core.MuObj ctx]
  (let [ cli (.getf ctx K_CLISH) ]
    (.addShutdownHook (Runtime/getRuntime)
          (Thread. (reify Runnable
                      (run [_] (Try! (stop-cli ctx))))))
    (enableRemoteShutdown ctx)
    (info "added shutdown hook.")
    ctx))

(defn- writePID [^comzotohlabscljc.util.core.MuObj ctx]
  (let [ fp (File. ^File (.getf ctx K_BASEDIR) "skaro.pid") ]
    (FileUtils/writeStringToFile fp (pid) "utf-8")
    (.setf! ctx K_PIDFILE fp)
    (info "wrote skaro.pid - OK.")
    ctx))

(defn- pause-cli [^comzotohlabscljc.util.core.MuObj ctx]
  (do
    (print-mutableObj ctx)
    (info "applications are now running...")
    (info "system thread paused on promise - awaits delivery.")
    (deref CLI-TRIGGER) ;; pause here
    (info "promise delivered!")
    (safe-wait 5000) ;; give some time for stuff to wind-down.
    (System/exit 0)))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn- make-climain [ & args ]
  (let [ impl (make-mmap) ]
    (reify

      Element

      (setCtx! [_ x] (.mm-s impl :ctx x))
      (getCtx [_] (.mm-g impl :ctx))
      (setAttr! [_ a v] (.mm-s impl a v) )
      (clrAttr! [_ a] (.mm-r impl a) )
      (getAttr [_ a] (.mm-g impl a) )

      Hierarchial
      (parent [_] nil)

      Versioned
      (version [_] "1.0")

      Identifiable
      (id [_] K_CLISH)

      Startable

      (start [this]
        (-> (pre-parse this args)
          (maybeInizLoaders)
          (loadConf)
          (setupResources )
          (primodial)
          (start-exec)
          (writePID)
          (hookShutdown)
          (pause-cli)) )

      (stop [this]
        (let [ ^comzotohlabscljc.util.core.MuObj ctx (getCtx this) ]
          (stop-cli ctx))))) )


(defn start-main "" [ & args ]
  (do
    (when (< (count args) 1)
      (throw (CmdHelpError. "Skaro Home not defined.")))
    (info "set skaro-home= " (first args))
    (let [ ^Startable cm  (apply make-climain args) ]
      (.start cm))))







;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



(def ^:private climain-eof nil)

