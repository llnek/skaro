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

  comzotohlabscljc.tardis.mvc.templates

  (:require [clojure.tools.logging :as log :only [info warn error debug] ])
  (:require [clojure.string :as cstr])
  (:use [comzotohlabscljc.util.core :only [Try! notnil? NiceFPath] ])
  (:use [comzotohlabscljc.util.mime :only [GuessContentType] ])
  (:use [comzotohlabscljc.util.io :only [Streamify] ])

  (:import (org.jboss.netty.handler.codec.http HttpMethod HttpHeaders
                                               HttpResponseStatus
                                               HttpResponse))
  (:import (org.jboss.netty.handler.stream ChunkedFile ChunkedStream ChunkedInput ))
  (:import (org.jboss.netty.channel ChannelFuture ChannelFutureListener Channel))
  (:import [com.zotohlabs.frwk.netty NettyFW])
  (:import (org.apache.commons.io FileUtils))
  (:import (com.zotohlabs.gallifrey.mvc WebContent WebAsset
                                        HTTPRangeInput AssetCache))
  (:import (java.io RandomAccessFile File))
  (:import (java.util Map HashMap)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- make-webcontent ""

  [^String cType bits]

  (reify
    WebContent
    (contentType [_] cType)
    (body [_] bits)
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn GetLocalFile ""

  [^File appDir ^String fname]

  (let [ f (File. appDir fname) ]
    (if (.canRead f)
      (make-webcontent
        (GuessContentType f "utf-8")
        (FileUtils/readFileToByteArray f))
      nil)
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- maybeCache ""

  [^File fp]

  (let [ ^String fpath (cstr/lower-case (NiceFPath fp)) ]
    (or (.endsWith fpath ".css")
        (.endsWith fpath ".gif")
        (.endsWith fpath ".jpg")
        (.endsWith fpath ".jpeg")
        (.endsWith fpath ".png")
        (.endsWith fpath ".js"))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- make-web-asset ""

  [^File file]

  (let [ ct (GuessContentType file "utf-8" "text/plain")
         ts (.lastModified file)
         bits (FileUtils/readFileToByteArray file) ]
    (reify
      WebAsset

      (contentType [_] ct)
      (getFile [_] file)
      (getTS [_] ts)
      (size [_] (alength bits))
      (getBytes [_] bits) )
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- fetchAndSetAsset ""

  [^Map cache fp ^File file]

  (if-let [ wa (if (and (.exists file)
                        (.canRead file))
                   (make-web-asset file)) ]
    (do
      (log/debug "asset-cache: cached new file: " fp)
      (.put cache fp wa)
      wa)
    (do
      (log/warn "asset-cache: failed to read/find file: " fp)
      nil)
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- getAsset ""

  [^File file]

  (let [ cache (AssetCache/get)
         fp (NiceFPath file)
         ^WebAsset wa (.get cache fp)
         ^File cf (if (nil? wa) nil (.getFile wa)) ]
    (if (or (nil? cf)
            (> (.lastModified file)
               (.getTS wa)))
      (fetchAndSetAsset cache fp file)
      wa)
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- getFileInput ""

  [ ^RandomAccessFile raf
    ^String ct
    ^JsonObject info
    ^HttpResponse rsp ]

  (let [ ^JsonObject h (.getAsJsonObject info "headers")
         ^JsonArray r (if (.has h "range")
                          (.getAsJsonArray h "range")
                          nil)
         s (if (or (nil? r)(< (.size r) 1))
               ""
               (.get r 0)) ]
    (if (HTTPRangeInput/accepts s)
        (doto (HTTPRangeInput. raf ct s)
              (.prepareNettyResponse rsp))
        (ChunkedFile. raf))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn ReplyFileAsset ""

  [ src ^Channel ch ^JsonObject info ^HttpResponse rsp ^File file]

  (let [ ^WebAsset asset (if (not (maybeCache file))
                             nil
                             (getAsset file)) ]
    (with-local-vars [raf nil clen 0 inp nil ct "" wf nil]
      (if (nil? asset)
        (do
          (var-set ct (GuessContentType file "utf-8" "text/plain"))
          (var-set raf (RandomAccessFile. file "r"))
          (var-set clen (.length ^RandomAccessFile @raf))
          (var-set inp (getFileInput @raf @ct info rsp)))
        (do
          (var-set ct (.contentType asset))
          (var-set clen (.size asset))
          (var-set inp (ChunkedStream. (Streamify (.getBytes asset))))) )
      (log/debug "serving file: " (.getName file) " with clen= " @clen ", ctype= " @ct)
      (try
        (when (= (.getStatus rsp) HttpResponseStatus/NOT_MODIFIED)
              (var-set clen 0))
        (HttpHeaders/addHeader rsp "Accept-Ranges" "bytes")
        (HttpHeaders/setHeader rsp "Content-Type" @ct)
        (HttpHeaders/setContentLength rsp @clen)
        (var-set wf (.write ch rsp))
        (when-not (= (-> (.get info "method")(.getAsString)) "HEAD")
                  (var-set wf (.write ch @inp)))
        (.addListener ^ChannelFuture @wf
                      (reify ChannelFutureListener
                        (operationComplete [_ ff]
                          (Try! (when (notnil? @raf) (.close ^RandomAccessFile @raf)))
                          (when-not (HttpHeaders/isKeepAlive req)
                                    (NettyFW/closeChannel ch)))))
        (catch Throwable e#
          (Try! (when (notnil? @raf)(.close ^RandomAccessFile @raf)))
          (log/error e# "")
          (Try! (NettyFW/closeChannel ch))) )
  )))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(def ^:private templates-eof nil)

