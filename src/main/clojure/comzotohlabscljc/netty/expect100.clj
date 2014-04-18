;; This library is distributed in  the hope that it will be useful but without
;; any  warranty; without  even  the  implied  warranty of  merchantability or
;; fitness for a particular purpose.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0  (http://opensource.org/licenses/eclipse-1.0.php)  which
;; can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any  fashion, you are agreeing to be bound by the
;; terms of this license. You  must not remove this notice, or any other, from
;; this software.
;; Copyright (c) 2013-2014 Cherimoia, LLC. All rights reserved.


(ns ^{ :doc ""
       :author "kenl" }

  comzotohlabscljc.netty.expect100 )

(use '[clojure.tools.logging :only [info warn error debug] ])

(import '(io.netty.channel ChannelFutureListener ChannelFuture
  ChannelHandler ChannelHandlerContext
  ChannelInboundHandlerAdapter))

(import '(io.netty.buffer ByteBuf ByteBufHolder Unpooled))

(import '(io.netty.handler.codec.http
  HttpMessage HttpResponseStatus
  HttpHeaders HttpVersion
  DefaultFullHttpResponse))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(def ^:private CONTINUE (DefaultFullHttpResponse. (HttpVersion/HTTP_1_1)
                                                  (HttpResponseStatus/CONTINUE)
                                                  (Unpooled/EMPTY_BUFFER)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn HandleExpect100 ""

  ^ChannelHandler
  []

  (proxy [ChannelInboundHandlerAdapter] []
    (channelRead [c msg]
      (let [^ChannelHandlerContext ctx c]
        (when (and (instance?  HttpMessage msg)
                   (HttpHeaders/is100ContinueExpected msg))
          (-> (.writeAndFlush ctx CONTINUE)
              (.addListener (reify ChannelFutureListener
                              (operationComplete [_ ^ChannelFuture f]
                                (when-not (.isSuccess f)
                                          (.fireExceptionCaught ctx (.cause f))))))))
        (.fireChannelRead ctx msg)

      ))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private expect100-eof nil)
