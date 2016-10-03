;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.
;;
;; Copyright (c) 2013-2016, Kenneth Leung. All rights reserved.

(ns ^{:doc "Implementation for HTTP/MVC service."
      :author "Kenneth Leung" }

  czlab.skaro.io.http

  (:require
    [czlab.xlib.meta :refer [bytesClass isString? isBytes?]]
    [czlab.net.util :refer [parseBasicAuth]]
    [czlab.crypto.codec :refer [passwd<>]]
    [czlab.net.mime :refer [getCharset]]
    [czlab.xlib.io :refer [xdata<>]]
    [czlab.xlib.logging :as log]
    [clojure.java.io :as io]
    [clojure.string :as cs])

  (:use [czlab.netty.routes]
        [czlab.netty.server]
        [czlab.netty.core]
        [czlab.skaro.sys.core]
        [czlab.skaro.io.core]
        [czlab.crypto.ssl]
        [czlab.xlib.core]
        [czlab.xlib.str]
        [czlab.skaro.io.webss])

  (:import
    [io.netty.handler.codec.http.cookie ServerCookieDecoder ServerCookieEncoder]
    [java.nio.channels ClosedChannelException]
    [io.netty.handler.codec.http.websocketx
     WebSocketFrame
     BinaryWebSocketFrame
     TextWebSocketFrame]
    [czlab.netty CPDecorator TcpPipeline]
    [czlab.skaro.net WebAsset RangeInput]
    [io.netty.bootstrap ServerBootstrap]
    [io.netty.buffer ByteBuf Unpooled]
    [io.netty.handler.ssl SslHandler]
    [czlab.net RouteCracker RouteInfo]
    [clojure.lang APersistentMap]
    [czlab.crypto PasswordAPI]
    [java.util Timer TimerTask]
    [czlab.netty InboundFilter]
    [io.netty.handler.codec.http
     HttpResponseStatus
     HttpRequest
     HttpUtil
     HttpResponse
     DefaultHttpResponse
     HttpVersion
     HttpRequestDecoder
     HttpResponseEncoder
     DefaultCookie
     HttpHeaderValues
     HttpHeaderNames
     LastHttpContent
     HttpHeaders
     Cookie
     QueryStringDecoder]
    [czlab.skaro.server
     Container
     Service
     EventTrigger]
    [java.io
     Closeable
     File
     IOException
     RandomAccessFile]
    [java.net
     HttpCookie
     URI
     URL
     InetSocketAddress
     InetAddress
     SocketAddress]
    [czlab.skaro.io
     HttpEvent
     HttpResult
     IoSession
     WebSockEvent
     WebSockResult]
    [io.netty.channel
     Channel
     ChannelHandler
     ChannelFuture
     ChannelFutureListener
     ChannelPipeline
     ChannelHandlerContext
     SimpleChannelInboundHandler]
    [io.netty.handler.stream
     ChunkedStream
     ChunkedFile
     ChunkedInput
     ChunkedWriteHandler]
    [czlab.xlib
     XData
     Muble
     Hierarchial
     Identifiable]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;(set! *warn-on-reflection* true)
(derive ::HTTP :czlab.skaro.io.core/Service)
(derive ::WebMVC ::HTTP)
(def ^:private ^String AUTH "authorization")
(def ^:private ^String BASIC "basic")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn scanBasicAuth

  "Scan and parse if exists basic authentication"
  ^APersistentMap
  [^HttpEvent evt]

  (if-some+ [v (-> (.msgGist evt)
                   (gistHeader AUTH))]
    (parseBasicAuth v)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- httpBasicConfig

  "Basic http config"
  [^Service co cfg0]

  (let [{:keys [serverKey port passwd]
         :as cfg}
        (merge (.config co) cfg0)
        kfile (expandVars serverKey)
        ^Container ctr (.server co)
        ssl? (hgl? kfile)]
    (if ssl?
      (test-cond "server-key file url"
                 (.startsWith kfile "file:")))
    (->>
      {:port (if-not (spos? port)
               (if ssl? 443 80) port)
       :passwd (->> (.appKey ctr)
                    (passwd<> passwd) (.text))
       :serverKey (if ssl? (URL. kfile))}
      (merge cfg ))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn wsockResult<>

  "Create a WebSocket result object"
  ^WebSockResult
  [^Service co]

  (let [impl (muble<>)]
    (reify WebSockResult
      (isEmpty [this] (nil? (.content this)))
      (setContent [_ c] (.setv impl :body c))
      (content [_] (.getv impl :body))
      (isText [_]
        (isString? (class (.getv impl :body))))
      (isBinary [_]
        (isBytes? (class (.getv impl :body))))
      (getx [_] impl)
      (source [_] co))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn httpResult<>

  "Create a HttpResult object"
  ^HttpResult
  [^Service co]

  (let [impl (muble<> {:version "HTTP/1.1"
                       :cookies []
                       :code -1
                       :headers {}})]
    (reify HttpResult

      (setRedirect [_ url] (.setv impl :redirect url))
      (setVersion [_ ver]  (.setv impl :version ver))
      (setStatus [_ code] (.setv impl :code code))
      (status [_] (.getv impl :code))
      (source [_] co)

      (addCookie [_ c]
        (if (some? c)
          (let [a (.getv impl :cookies)]
            (.setv impl :cookies (conj a c)))))

      (containsHeader [_ nm]
        (let [m (.getv impl :headers)
              a (get m (lcase nm))]
          (and (some? a)
               (> (count a) 0))))

      (removeHeader [_ nm]
        (let [m (.getv impl :headers)]
          (->> (dissoc m (lcase nm))
               (.setv impl :headers))))

      (clearHeaders [_]
        (.setv impl :headers {}))

      (addHeader [_ nm v]
        (let [m (.getv impl :headers)
              a (or (get m (lcase nm))
                         [])]
          (.setv impl
                 :headers
                 (assoc m (lcase nm) (conj a v)))))

      (setHeader [_ nm v]
        (let [m (.getv impl :headers)]
          (.setv impl
                 :headers
                 (assoc m (lcase nm) [v]))))

      (setChunked [_ b] (.setv impl :chunked? b))

      (isEmpty [_] (nil? (.getv impl :body)))

      (content [_] (.getv impl :body))

      (setContent [_ data]
          (.setv impl :body data)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- maybeLoadRoutes

  ^APersistentMap
  [^Service co]

  (let [^Container ctr (.server co)
        ctx (.getx co)
        f (io/file (.appDir ctr)
                   DN_CONF
                   "routes.conf")]
    (doto->>
      (if (.exists f)
        (loadRoutes f) [])
      (.setv ctx :routes ))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- javaToCookie

  ""
  ^Cookie
  [^HttpCookie c]

  ;; stick with version 0, Java's HttpCookie defaults to 1 but that
  ;; screws up the Path attribute on the wire => it's quoted but
  ;; browser seems to not like it and mis-interpret it.
  ;; Netty's cookie defaults to 0, which is cool with me.
  (doto (DefaultCookie. (.getName c)
                        (.getValue c))
    ;;(.setComment (.getComment c))
    (.setDomain (.getDomain c))
    (.setMaxAge (.getMaxAge c))
    (.setPath (.getPath c))
    ;;(.setDiscard (.getDiscard c))
    (.setVersion 0)
    (.setHttpOnly (.isHttpOnly c))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- maybeClose

  ""
  [^HttpEvent evt ^ChannelFuture cf]

  (->> (:keepAlive? (.msgGist evt))
       (closeCF cf )))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- csToNetty

  ""
  [cookies]

  (pcoll!
    (reduce
      #(->> (.encode ServerCookieEncoder/STRICT ^Cookie %2)
            (conj! %1 ))
      (transient [])
      (map #(javaToCookie %) (seq cookies)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- nettyWSReply

  ""
  [^WebSockEvent evt]

  (let [^WebSockResult res (.resultObj evt)
        ^Channel ch (.socket evt)
        c (.content res)
        ^WebSocketFrame
        f (condp instance? c
            (bytesClass)
            (->> (Unpooled/wrappedBuffer ^bytes c)
                 (BinaryWebSocketFrame. ))
            String
            (TextWebSocketFrame. ^String c))]
    (if (some? f)
      (.writeAndFlush ch f))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- replyOneFile

  ""
  ^ChunkedInput
  [^RandomAccessFile raf
   ^HttpEvent evt ^HttpResponse rsp]

  (-> (.msgGist evt)
      (gistHeader "range")
      str
      (RangeInput/fileRange rsp raf)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- nettyReplyMore

  ""

  ([^ChannelHandlerContext ctx
    ^HttpResponse rsp
    ^HttpEvent evt]
   (nettyReplyMore ctx rsp evt nil))

  ([^ChannelHandlerContext ctx
    ^HttpResponse rsp
    ^HttpEvent evt
    body]
   (let
     [gist (.msgGist evt)
      [clen body]
      (condp instance? body
        ChunkedInput
        [(.length ^ChunkedInput body) body]
        XData
        [(.size ^XData body)
         (->> (.stream ^XData body)
              (ChunkedStream.))]
        (if (nil? body)
          0
          (throwBadArg "rogue payload type %s"
                       (class body))))]
     (log/debug "writing out %d bytes to client" clen)
     (if (:keepAlive? gist)
       (setHeader rsp
                  HttpHeaderNames/CONNECTION
                  HttpHeaderValues/KEEP_ALIVE))
     (if-not
       (hgl? (getHeader rsp
                        HttpHeaderNames/CONTENT_LENGTH))
       (HttpUtil/setContentLength rsp clen))
     (let [f1 (.write ctx rsp)
           f2 (if (spos? clen)
                (.write ctx body))]
       (if (some? f1)
         (log/debug "wrote rsp-headers out to client"))
       (if (some? f2)
         (log/debug "wrote rsp-body out to client")
         (.close ^ChunkedInput body))
       (-> (or f2 f1)
           (closeCF (:keepAlive? gist)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- nettyReply

  ""
  [^ChannelHandlerContext ctx ^HttpEvent evt]

  (let [res (.resultObj evt)
        {:keys [redirect cookies
                code body headers]}
        (.impl (.getx res))
        gist (.msgGist evt)
        rsp (httpReply<> code)]
    ;;headers
    (doseq [[nm vs]  headers
            :when (not= "content-length" (lcase nm))]
      (doseq [vv (seq vs)]
        (addHeader rsp nm vv)))
    ;;cookies
    (doseq [s (csToNetty cookies)]
      (addHeader rsp
                 HttpHeaderNames/SET_COOKIE s))
    (cond
      ;;redirect?
      (and (>= code 300)
           (< code 400))
      (do
        (if (hgl? redirect)
          (setHeader rsp
                     HttpHeaderNames/LOCATION redirect))
        (nettyReplyMore ctx rsp evt))
      ;;ok?
      (and (>= code 200)
           (< code 300))
      (->>
        (when (not= "HEAD" (:method gist))
          (condp instance? body
            WebAsset
            (let [raf (RandomAccessFile.
                        (.file ^WebAsset body) "r")]
              (setHeader rsp
                         HttpHeaderNames/CONTENT_TYPE
                         (.contentType ^WebAsset body))
              (replyOneFile raf evt rsp))
            File
            (let [raf (RandomAccessFile.
                        ^File body "r")]
              (replyOneFile raf evt rsp))
            ;;else
            (xdata<> body)))
        (nettyReplyMore ctx rsp evt ))

      :else
      (nettyReplyMore ctx rsp evt))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn nettyTrigger<>

  "Create a Netty Async Trigger"
  ^EventTrigger
  [^ChannelHandlerContext ctx evt]

  (let
    [eeid (str "trigger#" (seqint2))
     impl (muble<>)
     t
     (reify EventTrigger
       (resumeWithResult [_ res]
         (some-> ^TimerTask
                 (.getv impl :ttask)
                 (.cancel))
         (.unsetv impl :ttask)
         (if (inst? WebSockEvent evt)
           (try! (nettyWSReply evt))
           (try! (nettyReply ctx evt))))
       (getx [_] impl)
       (id [_] eeid)
       (resumeOnExpiry [_]
         (.unsetv impl :ttask)
         (let [rsp (httpFullReply<> 500)]
           (try
             (maybeClose evt (.writeAndFlush ctx rsp))
             (catch ClosedChannelException _
               (log/warn "closedChannelEx thrown"))
             (catch Throwable t# (log/error t# "") )) )))]
    (.setTrigger ^HttpEvent evt t)
    t))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- cookieToJava

  ""
  [^Cookie c]

  (doto (HttpCookie. (.getName c)
                     (.getValue c))
    (.setComment (.getComment c))
    (.setDomain (.getDomain c))
    (.setMaxAge (.getMaxAge c))
    (.setPath (.getPath c))
    (.setVersion (.getVersion c))
    (.setHttpOnly (.isHttpOnly c))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- makeWEBSockEvent

  ""
  [^Service co ^Channel ch ssl? msg]

  (let
    [text? (inst? TextWebSocketFrame msg)
     _body
     (cond
       (inst? BinaryWebSocketFrame msg)
       (toByteArray (.content
                     ^BinaryWebSocketFrame msg))
       text?
       (.text ^TextWebSocketFrame msg)
       :else nil)
     res (wsockResult<> co)
     eeid (seqint2) ]
    (with-meta
      (reify WebSockEvent

        (checkAuthenticity [_] false)
        (bindSession [_ s] )
        (session [_] )
        (socket [_] ch)
        (id [_] eeid)
        (isSSL [_] ssl?)
        (isBinary [_] (not text?))
        (isText [_] text?)
        (body [_] _body)
        (resultObj [_] res)
        (replyResult [this]
          (nettyWSReply this))
        (source [_] co))

      {:typeid ::WebSockEvent })))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- crackCookies

  ""
  ^APersistentMap
  [gist]

  (let
    [^String v (gistHeader gist HttpHeaderNames/COOKIE)
     cks (if (hgl? v)
           (.decode ServerCookieDecoder/STRICT v))]
    (persistent!
      (reduce
        #(assoc! %1
                 (.getName ^Cookie %2)
                 (cookieToJava %2))
        (transient {})
        (seq cks)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- makeHttpEvent

  ""
  ^HttpEvent
  [^Service co ^Channel ch
   ssl? _body gist wantSecure?]

  (let
    [^InetSocketAddress laddr (.localAddress ch)
     cookieJar (crackCookies gist)
     res (httpResult<> co)
     impl (muble<>)
     eeid (str "event#" (seqint2))
     evt
     (reify HttpEvent

       (bindSession [_ s] (.setv impl :ios s))
       (session [_] (.getv impl :ios))
       (id [_] eeid)
       (source [_] co)
       (checkAuthenticity [_] wantSecure?)

       (cookie [_ n] (get cookieJar n))
       (cookies [_] (vals cookieJar))
       (msgGist [_] gist)
       (body [_] _body)

       (localAddr [_] (.getHostAddress (.getAddress laddr)))
       (localHost [_] (.getHostName laddr))
       (localPort [_] (.getPort laddr))

       (remotePort [_]
         (convLong (gistHeader gist "remote_port") 0))
       (remoteAddr [_]
         (str (gistHeader gist "remote_addr")))
       (remoteHost [_]
         (str (gistHeader gist "remote_host")))

       (serverPort [_]
         (convLong (gistHeader gist "server_port") 0))
       (serverName [_]
         (str (gistHeader gist "server_name")))

       (scheme [_] (if ssl? "https" "http"))
       (isSSL [_] ssl?)

       (setTrigger [_ t] (.setv impl :trigger t))

       (getx [_] impl)

       (resultObj [_] res)
       (replyResult [this]
         (let [^IoSession mvs (.session this)
               code (.status res)
               ^EventTrigger
               t (.getv impl :trigger)]
           (when
             (and (>= code 200)
                  (< code 400))
             (.handleResult mvs this res))
           (some->
             t
             (.resumeWithResult res)))))]
    (doto evt
      (.bindSession (wsession<> co ssl?))
      (with-meta {:typeid ::HTTPEvent}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmethod ioevent<>

  ::HTTP
  [^Service co {:keys [^Channel ch
                       msg
                       ^RouteInfo route]}]

  (log/info "ioevent: '%s': '%s'" (gtid co) (.id co))
  (let [ssl? (maybeSSL? ch)]
    (if
      (inst? WebSocketFrame msg)
      (makeWEBSockEvent co ch ssl? msg)
      ;else
      (makeHttpEvent
        co ch ssl?
        (:body msg)
        (:gist msg)
        (if (nil? route) false (.isSecure route))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- h1Handler

  ""
  ^ChannelHandler
  [^Service co args]

  (log/debug "dispatcher for %s : %s" (gtid co) (.id co))
  (proxy [InboundFilter] []
    (channelRead0 [c msg]
      (let [ch (.channel ^ChannelHandlerContext c)
            {:keys [waitMillis]}
            (.config co)
            evt (ioevent<> co {:ch ch
                               :msg msg
                               :route nil})]
        (if (inst? HttpEvent evt)
          (let [w (nettyTrigger<> ch evt)]
            (.hold co w waitMillis)))
        (.dispatch co evt )))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmethod io->start

  ::HTTP
  [^Service co & _]

  (log/info "io->start: %s: %s" (gtid co) (.id co))
  (let [bs (.getv (.getx co) :bootstrap)
        cfg (.config co)
        ch (startServer bs cfg)]
    (.setv (.getx co) :channel ch)
    (io<started> co)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmethod io->stop

  ::HTTP
  [^Service co & _]

  (log/info "io->stop %s: %s" (gtid co) (.id co))
  (let [{:keys [bootstrap channel]}
        (.impl (.getx co)) ]
    (stopServer bootstrap channel)
    (doto (.getx co)
      (.unsetv :bootstrap)
      (.unsetv :channel))
    (io<stopped> co)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmethod comp->init

  ::HTTP
  [^Service co cfg0]

  (log/info "comp->init: '%s': '%s'" (gtid co) (.id co))
  (let [^Container ctr (.server co)
        cfg (httpBasicConfig co cfg0)]
    (.setv (.getx co) :emcfg cfg)
    (->>
      (httpServer<>
        (reify CPDecorator
          (forH1 [_ ops] (h1Handler co ops))
          (forH2H1 [_ _])
          (forH2 [_ _])) cfg)
      (.setv (.getx co) :bootstrap ))
    co))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmethod comp->init

  ::WebMVC
  [^Service co cfg0]

  (log/info "comp->init: '%s': '%s'" (gtid co) (.id co))
  (let [^Container ctr (.server co)
        cfg (httpBasicConfig co cfg0)
        bs (httpServer<>
             (reify CPDecorator
               (forH1 [_ ops] (h1Handler co ops))
               (forH2H1 [_ _])
               (forH2 [_ _]))
             cfg)]
    (doto (.getx co)
      (.setv :emcfg cfg)
      (.setv :bootstrap bs))
    co))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;EOF


