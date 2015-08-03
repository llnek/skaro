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

  czlab.skaro.io.jetty

  (:require
    [czlab.xlib.util.str :refer [lcase ucase hgl? strim]]
    [czlab.xlib.util.core
    :refer [Muble notnil? juid tryc spos?
    NextLong ToJavaInt try! MakeMMap test-cond Stringify]]
    [czlab.xlib.crypto.codec :refer [Pwdify]])

  (:require
    [czlab.xlib.util.logging :as log]
    [clojure.java.io :as io])

  (:use [czlab.xlib.crypto.ssl]
        [czlab.xlib.net.routes]
        [czlab.skaro.core.consts]
        [czlab.skaro.core.sys]
        [czlab.skaro.io.core]
        [czlab.skaro.io.http]
        [czlab.skaro.io.webss]
        [czlab.skaro.io.triggers])

  (:import
    [org.eclipse.jetty.server Server Connector ConnectionFactory]
    [java.util.concurrent ConcurrentHashMap]
    [java.net URL]
    [jregex Matcher Pattern]
    [org.apache.commons.io IOUtils]
    [java.util List Map HashMap ArrayList]
    [java.io File]
    [com.zotohlab.frwk.util NCMap]
    [javax.servlet.http Cookie HttpServletRequest]
    [java.net HttpCookie]
    [com.google.gson JsonObject]
    [org.eclipse.jetty.continuation Continuation
    ContinuationSupport]
    [com.zotohlab.frwk.server Component Emitter]
    [com.zotohlab.frwk.io XData]
    [com.zotohlab.frwk.core Versioned Hierarchial
    Identifiable Disposable Startable]
    [org.apache.commons.codec.binary Base64]
    [org.eclipse.jetty.server Connector HttpConfiguration
    HttpConnectionFactory SecureRequestCustomizer
    Server ServerConnector Handler
    SslConnectionFactory]
    [org.eclipse.jetty.util.ssl SslContextFactory]
    [org.eclipse.jetty.util.thread QueuedThreadPool]
    [org.eclipse.jetty.util.resource Resource]
    [org.eclipse.jetty.server.handler AbstractHandler
    ContextHandler
    ContextHandlerCollection
    ResourceHandler]
    [com.zotohlab.skaro.io IOSession ServletEmitter]
    [org.eclipse.jetty.webapp WebAppContext]
    [javax.servlet.http HttpServletRequest HttpServletResponse]
    [com.zotohlab.skaro.io WebSockResult
    HTTPResult
    HTTPEvent JettyUtils]
    [com.zotohlab.skaro.core Container]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;(set! *warn-on-reflection* true)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- isServletKeepAlive ""

  [^HttpServletRequest req]

  (if-let [v (.getHeader req "connection") ]
    (>= (.indexOf (lcase v)
                  "keep-alive") 0)
    false
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- cookieToServlet ""

  ^Cookie
  [^HttpCookie c]

  (doto (Cookie. (.getName c) (.getValue c))
    (.setDomain (str (.getDomain c)))
    (.setHttpOnly (.isHttpOnly c))
    (.setMaxAge (.getMaxAge c))
    (.setPath (str (.getPath c)))
    (.setSecure (.getSecure c))
    (.setVersion (.getVersion c))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- replyServlet ""

  [^czlab.xlib.util.core.Muble res
   ^HttpServletRequest req
   ^HttpServletResponse rsp
   src]

  (let [^URL url (.getf res :redirect)
        os (.getOutputStream rsp)
        cks (.getf res :cookies)
        hds (.getf res :hds)
        code (.getf res :code)
        data (.getf res :data) ]
    (try
      (.setStatus rsp code)
      (doseq [[nm vs] hds]
        (when (not= "content-length"
                    (lcase nm))
          (doseq [vv vs]
            (.addHeader rsp ^String nm ^String vv))))
      (doseq [c cks ]
        (.addCookie rsp (cookieToServlet c)))
      (cond
        (and (>= code 300)
             (< code 400))
        (.sendRedirect rsp
                       (.encodeRedirectURL rsp
                                           (str url)))
        :else
        (let [^XData dd (cond
                          (instance? XData data)
                          data
                          (some? data)
                          (XData. data)
                          :else nil)
              clen (if (and (some? dd)
                            (.hasContent dd))
                     (.size dd)
                     0) ]
            (.setContentLength rsp clen)
            (.flushBuffer rsp)
            (when (> clen 0)
              (IOUtils/copyLarge (.stream dd) os 0 clen)
              (.flush os) )))
      (catch Throwable e#
        (log/error e# ""))
      (finally
        (try! (when-not (isServletKeepAlive req) (.close os)))
        (-> (ContinuationSupport/getContinuation req)
            (.complete))) )
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- makeServletTrigger ""

  [^HttpServletRequest req
   ^HttpServletResponse rsp src]

  (reify AsyncWaitTrigger

    (resumeWithResult [_ res]
      (replyServlet res req rsp src) )

    (resumeWithError [_]
      (try
        (.sendError rsp 500)
        (catch Throwable e#
          (log/error e# ""))
        (finally
          (-> (ContinuationSupport/getContinuation req)
              (.complete)))) )
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmethod CompConfigure :czc.skaro.io/JettyIO

  [^czlab.xlib.util.core.Muble co cfg0]

  (log/info "compConfigure: JettyIO: %s" (.id ^Identifiable co))
  (let [cfg (merge (.getf co :dftOptions) cfg0)]
    (.setf! co :emcfg
               (HttpBasicConfig co (dissoc cfg K_APP_CZLR)))
    (.setf! co K_APP_CZLR (get cfg K_APP_CZLR))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- cfgHTTPS ""

  ^ServerConnector
  [^Server server port
   ^URL keyfile ^String pwd conf]

  ;; SSL Context Factory for HTTPS and SPDY
  (let [sslxf (doto (SslContextFactory.)
                (.setKeyStorePath (-> keyfile
                                      (.toString )))
                (.setKeyStorePassword pwd)
                (.setKeyManagerPassword pwd))
        config (doto (HttpConfiguration. conf)
                 (.addCustomizer (SecureRequestCustomizer.)))
        https (doto (ServerConnector. server)
                (.addConnectionFactory (SslConnectionFactory. sslxf "HTTP/1.1"))
                (.addConnectionFactory (HttpConnectionFactory. config))) ]
    (doto https
      (.setPort port)
      (.setIdleTimeout (int 500000)))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmethod CompInitialize :czc.skaro.io/JettyIO

  [^czlab.xlib.util.core.Muble co]

  (let [conf (doto (HttpConfiguration.)
               (.setRequestHeaderSize 8192)  ;; from jetty examples
               (.setOutputBufferSize (int 32768)))

        ^czlab.xlib.util.core.Muble
        ctr (.parent ^Hierarchial co)
        rts (.getf ctr :routes)

        cfg (.getf co :emcfg)
        keyfile (:serverKey cfg)
        ^String host (:host cfg)
        port (:port cfg)
        pwdObj (:passwd cfg)
        ws (:workers cfg)
         ;;q (QueuedThreadPool. (if (pos? ws) ws 8))
        svr (Server.)
        cc  (if (nil? keyfile)
              (doto (JettyUtils/makeConnector svr conf)
                (.setPort port)
                (.setIdleTimeout (int 30000)))
              (cfgHTTPS svr port keyfile
                        (if (nil? pwdObj) nil (str pwdObj))
                        (doto conf
                          (.setSecureScheme "https")
                          (.setSecurePort port)))) ]
    (when (hgl? host) (.setHost cc host))
    (.setName cc (juid))
    (doto svr
      (.setConnectors (into-array Connector [cc])))
    (.setf! co :jetty svr)
    (.setf! co :cracker (MakeRouteCracker rts))
    co
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- dispREQ ""

  [^czlab.xlib.util.core.Muble co
   ^Continuation ct
   ^HttpServletRequest req rsp]

  (let [^czlab.xlib.net.routes.RouteCracker
        ck (.getf co :cracker)
        cfg {:method (ucase (.getMethod req))
             :uri (.getRequestURI req)}
        [r1 r2 r3 r4]
        (.crack ck cfg)]
    (cond
      (and r1
           (hgl? r4))
      (JettyUtils/replyRedirect req rsp r4)

      (= r1 true)
      (let [^czlab.xlib.net.routes.RouteInfo ri r2
            ^HTTPEvent evt (IOESReifyEvent co req)
            ssl (= "https" (.getScheme req))
            wss (MakeWSSession co ssl)
            cfg (.getf co :emcfg)
            wm (:waitMillis cfg)
            pms (.collect ri ^Matcher r3) ]
        ;;(log/debug "mvc route filter MATCHED with uri = " (.getRequestURI req))
        (.bindSession evt wss)
        ;;(.setTimeout ct wm)
        (let [^czlab.skaro.io.core.WaitEventHolder
              w (MakeAsyncWaitHolder (makeServletTrigger req
                                                         rsp co)
                                     evt)
              ^czlab.skaro.io.core.EmitAPI src co]
          (.timeoutMillis w wm)
          (.hold src w)
          (.dispatch src evt {:router (.getHandler ri)
                              :params (merge {} pms)
                              :template (.getTemplate ri) })))

      :else
      (do
        (log/debug "failed to match uri: %s" (.getRequestURI req))
        (JettyUtils/replyXXX req rsp 404)))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- serviceJetty ""

  [co ^HttpServletRequest req ^HttpServletResponse rsp]

  (when-let [c (ContinuationSupport/getContinuation req) ]
    (when (.isInitial c)
      (tryc
        (.suspend c rsp)
        (dispREQ co c req rsp) ))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmethod IOESStart :czc.skaro.io/JettyIO

  [^czlab.xlib.util.core.Muble co]

  (log/info "IOESStart: JettyIO: %s" (.id ^Identifiable co))
  (let [^czlab.xlib.util.core.Muble
        ctr (.parent ^Hierarchial co)
        ^Server jetty (.getf co :jetty)
        ^File app (.getf ctr K_APPDIR)
        ^File rcpath (io/file app DN_PUBLIC)
        rcpathStr (io/as-url  rcpath)
        cfg (.getf co :emcfg)
        cp (:contextPath cfg)
        ctxs (ContextHandlerCollection.)
        c2 (ContextHandler.)
        c1 (ContextHandler.)
        r1 (ResourceHandler.)
        myHandler (proxy [AbstractHandler] []
                    (handle [target baseReq req rsp]
                      (serviceJetty co req rsp))) ]
    ;; static resources are based from resBase, regardless of context
    (-> r1
        (.setBaseResource (Resource/newResource rcpathStr)))
    (.setContextPath c1 (str "/" DN_PUBLIC))
    (.setHandler c1 r1)
    (.setClassLoader c2 ^ClassLoader (.getf co K_APP_CZLR))
    (.setContextPath c2 (strim cp))
    (.setHandler c2 myHandler)
    (.setHandlers ctxs (into-array Handler [c1 c2]))
    (.setHandler jetty ctxs)
    (.start jetty)
    (IOESStarted co)
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmethod IOESStop :czc.skaro.io/JettyIO

  [^czlab.xlib.util.core.Muble co]

  (log/info "IOESStop: JettyIO: %s" (.id ^Identifiable co))
  (let [^Server svr (.getf co :jetty) ]
    (when (some? svr)
      (tryc
          (.stop svr) ))
    (IOESStopped co)
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- cookie-to-javaCookie  ""

  [^Cookie c]

  (doto (HttpCookie. (.getName c) (.getValue c))
    (.setDomain (.getDomain c))
    (.setHttpOnly (.isHttpOnly c))
    (.setMaxAge (.getMaxAge c))
    (.setPath (.getPath c))
    (.setSecure (.getSecure c))
    (.setVersion (.getVersion c))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- maybeGetCookies ""

  [^HttpServletRequest req]

  (with-local-vars [rc (transient {})]
    (if-let [cs (.getCookies req) ]
      (doseq [^Cookie c (seq cs) ]
        (var-set rc (assoc! @rc
                            (.getName c)
                            (cookie-to-javaCookie c)))))
    (persistent! @rc)
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmethod IOESReifyEvent :czc.skaro.io/JettyIO

  [co & args]

  (log/debug "OPESReifyEvent: JettyIO: %s" (.id ^Identifiable co))
  (let [^HTTPResult result (MakeHttpResult co)
        ^HttpServletRequest req (first args)
        impl (MakeMMap {:cookies (maybeGetCookies req)})
        ssl (= "https" (.getScheme req))
        eid (NextLong) ]
    (reify

      Identifiable
      (id [_] eid)

      HTTPEvent

      (getCookies [_] (vals (.getf impl :cookies)))
      (getCookie [_ nm]
        (when-let [cs (.getf impl :cookies)]
          (get cs nm)))

      (checkAuthenticity [_] false)
      (getId [_] eid)

      (bindSession [this s]
        (.setf! impl :ios s)
        (.handleEvent ^IOSession s this))

      (isKeepAlive [_] (isServletKeepAlive req))
      (getSession [_] (.getf impl :ios))
      (emitter [_] co)

      (hasData [_] false)
      (data [_] nil)

      (contentLength [_] (.getContentLength req))
      (contentType [_] (.getContentType req))
      (encoding [_] (.getCharacterEncoding req))
      (contextPath [_] (.getContextPath req))

      (getHeaderValues [this nm]
        (if (.hasHeader this nm)
          (vec (seq (.getHeaders req nm)))
          []))

      (hasHeader [_ nm] (notnil? (.getHeader req nm)))
      (getHeaderValue [_ nm] (.getHeader req nm))
      (getHeaders [_] (vec (seq (.getHeaderNames req))))

      (getParameterValue [_ nm] (.getParameter req nm))
      (hasParameter [_ nm]
        (.containsKey (.getParameterMap req) nm))

      (getParameterValues [this nm]
        (if (.hasParameter this nm)
          (vec (seq (.getParameterValues req nm)))
          []))

      (getParameters [_]
        (vec (seq (.getParameterNames req))))

      (localAddr [_] (.getLocalAddr req))
      (localHost [_] (.getLocalName req))
      (localPort [_] (.getLocalPort req))

      (queryString [_] (.getQueryString req))
      (method [_] (.getMethod req))
      (protocol [_] (.getProtocol req))

      (remoteAddr [_] (.getRemoteAddr req))
      (remoteHost [_] (.getRemoteHost req))
      (remotePort [_] (.getRemotePort req))

      (scheme [_] (.getScheme req))

      (serverName [_] (.getServerName req))
      (serverPort [_] (.getServerPort req))

      (host [_] (.getHeader req "host"))

      (isSSL [_] (= "https" (.getScheme req)))

      (getUri [_] (.getRequestURI req))

      (getRequestURL [_] (.getRequestURL req))

      (getResultObj [_] result)
      (replyResult [this]
        (let [^czlab.skaro.io.core.WaitEventHolder
              wevt
              (-> ^czlab.skaro.io.core.EmitAPI co
                  (.release this))
              ^IOSession mvs (.getSession this)
              code (.getStatus result) ]
          (cond
            (and (>= code 200)
                 (< code 400))
            (.handleResult mvs this result)
            :else nil)
          (when (some? wevt)
            (.resumeOnResult wevt result))))
  )))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;EOF
