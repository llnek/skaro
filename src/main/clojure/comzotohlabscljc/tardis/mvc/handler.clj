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

  comzotohlabscljc.tardis.mvc.handler

  (:use [comzotohlabscljc.util.core :only [spos? ToJavaInt MubleAPI Try! NiceFPath] ])
  (:require [clojure.tools.logging :as log :only [info warn error debug] ])
  (:require [clojure.string :as cstr])
  (:use [comzotohlabscljc.tardis.io.triggers])
  (:use [comzotohlabscljc.tardis.io.http :only [HttpBasicConfig] ])
  (:use [comzotohlabscljc.tardis.io.netty])
  (:use [comzotohlabscljc.tardis.io.core])
  (:use [comzotohlabscljc.tardis.core.sys])
  (:use [comzotohlabscljc.tardis.core.constants])

  (:use [comzotohlabscljc.tardis.mvc.templates :only [GetLocalFile ReplyFileAsset] ])
  (:use [comzotohlabscljc.tardis.mvc.comms])
  (:use [comzotohlabscljc.util.str :only [hgl? nsb strim] ])
  (:use [comzotohlabscljc.util.meta :only [MakeObj] ])
  (:use [comzotohlabscljc.net.routes])

  (:import [com.zotohlabs.frwk.netty NettyFW])
  (:import (org.apache.commons.lang3 StringUtils))
  (:import (java.util Date))
  (:import (java.io File))
  (:import (com.zotohlabs.frwk.io XData))
  (:import (com.google.gson JsonObject))
  (:import (com.zotohlabs.frwk.core Hierarchial Identifiable))
  (:import (com.zotohlabs.gallifrey.io HTTPEvent Emitter))
  (:import (com.zotohlabs.gallifrey.mvc HTTPErrorHandler MVCUtils WebAsset WebContent))
  (:import (io.netty.handler.codec.http HttpRequest HttpResponse
                                        CookieDecoder ServerCookieEncoder
                                        DefaultHttpResponse HttpVersion
                                        HttpServerCodec
                                        HttpHeaders LastHttpContent
                                        HttpHeaders Cookie QueryStringDecoder))
  (:import (io.netty.bootstrap ServerBootstrap))
  (:import (io.netty.channel Channel ChannelHandler
                             SimpleChannelInboundHandler
                             ChannelPipeline ChannelHandlerContext))
  (:import (io.netty.handler.stream ChunkedWriteHandler))

  (:import (com.zotohlabs.frwk.netty NettyFW ErrorCatcher
                                     DemuxedMsg PipelineConfigurator
                                     HttpDemux
                                     SSLServerHShake ServerSide))
  (:import (jregex Matcher Pattern)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- routeFilter ""

  ^ChannelHandler
  [^comzotohlabscljc.tardis.core.sys.Element co]

  (proxy [SimpleChannelInboundHandler] []
    (channelRead0 [c msg]
      (if (instance? HttpRequest msg)
        (let [ ^comzotohlabscljc.net.routes.RouteCracker
               ck (.getAttr co :cracker)
               ^ChannelHandlerContext ctx c
               ^HttpRequest req msg
               ch (.channel ctx)
               json (doto (JsonObject.)
                          (.addProperty "method" (NettyFW/getMethod req))
                          (.addProperty "uri" (NettyFW/getUriPath req)))
               [r1 r2 r3 r4] (.crack ck json) ]
          (cond
            (and r1 (hgl? r4))
            (NettyFW/sendRedirect ch false ^String r4)

            (= r1 true)
            (.fireChannelRead ctx msg)

            :else
            (do
              (log/debug "failed to match uri: " (.getUri req))
              (NettyFW/replyXXX ch 404 false)))
        )))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- msgDispatcher ""

  ^ChannelHandler
  [^comzotohlabscljc.tardis.io.core.EmitterAPI em
   ^comzotohlabscljc.tardis.core.sys.Element co]

  (proxy [SimpleChannelInboundHandler] []
    (channelRead0 [ctx msg]
      (let [ ^comzotohlabscljc.net.routes.RouteCracker
             rcc (.getAttr co :cracker)
             ch (.channel ^ChannelHandlerContext ctx)
             ^HTTPEvent evt (IOESReifyEvent co ch msg)
             info (.info ^DemuxedMsg msg)
             [r1 ^comzotohlabscljc.net.routes.RouteInfo r2 r3 r4]
             (.crack rcc info) ]
        (cond
          (= r1 true)
          (do
            (log/debug "matched one route: " (.getPath r2)
                       " , and static = " (.isStatic? r2))
            (if (.isStatic? r2)
                (ServeStatic co r2 r3 ch info evt)
                (ServeRoute co r2 r3 ch evt)))
          :else
          (do
            (log/debug "failed to match uri: " (.getUri evt))
            (ServeError co ch 404)) )))
  ))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- mvcInitor ""

  ^PipelineConfigurator
  [^comzotohlabscljc.tardis.core.sys.Element co]

  (proxy [PipelineConfigurator] []
    (assemble [p o]
      (let [ ^ChannelPipeline pipe p
             ^JsonObject options o
             ssl (SSLServerHShake/getInstance options) ]
        (when-not (nil? ssl) (.addLast pipe "ssl" ssl))
        (-> pipe
            ;;(.addLast "ssl" (SSLServerHShake/getInstance options))
            (.addLast "codec" (HttpServerCodec.))
            (.addLast "filter" (routeFilter co))
            (.addLast "demux" (HttpDemux/getInstance))
            (.addLast "chunker" (ChunkedWriteHandler.))
            (.addLast "disp" (msgDispatcher co co))
            (.addLast "error" (ErrorCatcher/getInstance)))
        pipe))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- init-netty ""

  [^comzotohlabscljc.tardis.core.sys.Element co]

  (let [ ^comzotohlabscljc.tardis.core.sys.Element
         ctr (.parent ^Hierarchial co)
         rts (.getAttr ctr :routes)
         ^JsonObject options (.getAttr co :emcfg)
         bs (ServerSide/initServerSide (mvcInitor co)
                                       options) ]
    (.setAttr! co :cracker (MakeRouteCracker rts))
    (.setAttr! co :netty  { :bootstrap bs })
    co
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmethod CompConfigure :czc.tardis.io/NettyMVC

  [^comzotohlabscljc.tardis.core.sys.Element co cfg]

  (HttpBasicConfig co cfg)
  (let [ ^JsonObject json (.getAttr co :emcfg)
         c (nsb (:context cfg)) ]

    ;;(.setAttr! co :welcomeFiles (:welcomeFiles cfg))

    (let [ xxx (strim c) ]
      (.addProperty json "contextPath" xxx)
      (.setAttr! co :contextPath xxx))

    (let [ n (:cacheMaxAgeSecs cfg)
           xxx (if (spos? n) n 3600) ]
      (.addProperty json "cacheMaxAgeSecs" (ToJavaInt xxx))
      (.setAttr! co :cacheMaxAgeSecs xxx))

    (let [ xxx (:useETags cfg) ]
      (.addProperty json "useETags" (true? xxx))
      (.setAttr! co :useETags xxx))

    (let [ xxx (strim (:handler cfg)) ]
      (.addProperty json "router" xxx)
      (.setAttr! co :router xxx))

    (let [ xxx (strim (:errorHandler cfg)) ]
      (.addProperty json "errorRouter" xxx)
      (.setAttr! co :errorRouter xxx))

    co
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmethod CompInitialize :czc.tardis.io/NettyMVC

  [^comzotohlabscljc.tardis.core.sys.Element co]

  (init-netty co))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(def ^:private handler-eof nil)

