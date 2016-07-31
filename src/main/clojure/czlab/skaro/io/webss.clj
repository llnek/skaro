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


(ns ^{:doc ""
      :author "Kenneth Leung" }

  czlab.skaro.io.webss

  (:require
    [czlab.xlib.core
     :refer [convLong
             spos?
             juid
             trap!
             muble<>
             stringify
             bytesify]]
    [czlab.xlib.str :refer [hgl? addDelim!]]
    [czlab.xlib.io :refer [hexify]]
    [czlab.crypto.core :refer [genMac]]
    [czlab.xlib.logging :as log])

  (:import
    [czlab.skaro.runtime ExpiredError AuthError]
    [czlab.server EventEmitter]
    [java.net HttpCookie]
    [czlab.xlib Muble CU]
    [czlab.skaro.io
     HTTPResult
     HTTPEvent
     WebSS
     IOSession]
    [czlab.skaro.server Container]
    [czlab.net ULFormItems ULFileItem]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;(set! *warn-on-reflection* true)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(def ^:private SESSION_COOKIE "__ss117")
(def ^:private SSID_FLAG :__f01es)
(def ^:private CS_FLAG :__f184n ) ;; creation time
(def ^:private LS_FLAG :__f384n ) ;; last access time
(def ^:private ES_FLAG :__f484n ) ;; expiry time
(def ^String ^:private NV_SEP "\u0000")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- resetFlags

  "A negative value means that the cookie is not stored persistently and will be deleted when the Web browser exits. A zero value causes the cookie to be deleted."
  [^WebSS mvs maxAgeSecs]

  (let [now (System/currentTimeMillis)
        maxAgeSecs (or maxAgeSecs 0)]
    (doto mvs
      (.setAttr SSID_FLAG
                (hexify (bytesify (juid))))
      (.setAttr ES_FLAG
                (if (spos? maxAgeSecs)
                  (+ now (* maxAgeSecs 1000))
                  maxAgeSecs))
      (.setAttr CS_FLAG now)
      (.setAttr LS_FLAG now))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- maybeMacIt

  ""
  [^HTTPEvent evt ^String data
   ^Container ctr]

  (if
    (.checkAuthenticity evt)
    (str (-> (.getAppKeyBits ctr)
             (genMac data))
         "-" data)
    data))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- downstream

  ""
  [^HTTPEvent evt ^HTTPResult res ]

  (let [^WebSS mvs (.session evt)
        src (.emitter evt)
        {:keys [sessionAgeSecs
                domainPath
                domain
                hidden
                maxIdleSecs]}
        (-> (.getx ^Context src)
            (.getv :emcfg))]
    (when-not (.isNull mvs)
      (log/debug "session ok, about to set-cookie!")
      (when (.isNew mvs)
        (->> ^long (or sessionAgeSecs 3600)
             (resetFlags mvs )))
      (let
        [ck (->> (.server src)
                 (maybeMacIt evt (str mvs))
                 (HttpCookie. SESSION_COOKIE ))
         est (.expiryTime mvs)]
        (->> ^long (or maxIdleSecs 0)
             (.setMaxIdleSecs mvs))
        (doto ck
          (.setMaxAge (if (spos? est)
                        (/ (- est (now<>)) 1000) est))
          (.setDomain (str domain))
          (.setSecure (.isSSL mvs))
          (.setHttpOnly (true? hidden))
          (.setPath (str domainPath)))
        (.addCookie res ck)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- testCookie

  ""
  [^HTTPEvent evt p1 p2 ^Container ctr]

  (when-some
    [pkey (when (.checkAuthenticity evt)
            (.getAppKeyBits ctr))]
    (when (not= (genMac pkey p2) p1)
      (log/error "session cookie - broken")
      (trap! AuthError "Bad Session Cookie"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- upstream

  ""
  [^HTTPEvent evt]

  (let
    [ck (.cookie evt SESSION_COOKIE)
     ^WebSS mvs (.session evt)
     src (.emitter evt)]
    (if (nil? ck)
      (do
        (log/warn "no s-cookie found, invalidate!")
        (.invalidate mvs))
      (let
        [cookie (str (.getValue ck))
         cfg (-> ^Muble
                    netty
                    (.getv :emcfg))
         pos (.indexOf cookie (int \-))
         [p1 p2]
         (if (< pos 0)
           ["" cookie]
           [(.substring cookie 0 pos)
            (.substring cookie (inc pos1))])]
        (testCookie evt p1 p2 (.server src))
        (log/debug "session attrs= %s" p2)
        (try
          (doseq [nv (.split p2 NV_SEP)
                 :let [ss (.split nv ":" 2)
                       ^String s1 (aget ss 0)
                       ^String s2 (aget ss 1)]]
            (log/debug "s-attr n=%s, v=%s" s1 s2)
            (if (and (.startsWith s1 "__f")
                     (.endsWith s1 "n"))
              (.setAttr mvs
                        (keyword s1)
                        (convLong s2 0))
              (.setAttr mvs (keyword s1) s2)))
          (catch Throwable _
            (trap! ExpiredError "malformed cookie")))
        (.setNew mvs false 0)
        (let [ts (or (.attr mvs LS_FLAG) -1)
              es (or (.attr mvs ES_FLAG) -1)
              now (System/currentTimeMillis)
              mi (or (maxIdleSecs 0))]
          (when (or (< es now)
                    (and (spos? mi)
                         (< (+ ts (* 1000 mi)) now)))
            (trap! ExpiredError "Session has expired"))
          (.setAttr mvs LS_FLAG now))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn wsession

  ""
  ^WebSS
  [co ssl?]

  (let [impl (muble<> {:maxIdleSecs 0
                       :newOne true})
        _attrs (muble<>)]
    (with-meta
      (reify WebSS

        (removeAttr [_ k] (.unsetv _attrs k))
        (setAttr [_ k v] (.setv _attrs k v))
        (attr [_ k] (.getv _attrs k))
        (clear [_] (.clear _attrs))
        (attrs [_] (.seq _attrs))

        (setMaxIdleSecs [_ idleSecs]
          (when (number? idleSecs)
            (.setv impl :maxIdleSecs idleSecs)))

        (isNull [_] (empty? (.impl impl)))
        (isNew [_] (.getv impl :newOne))
        (isSSL [_] ssl?)

        (invalidate [_]
          (.clear attrs)
          (.clear impl))

        (setXref [_ csrf]
          (.setv _attrs :csrf csrf))

        (setNew [this flag maxAge]
          (when flag
            (.clear _attrs)
            (.clear impl)
            (resetFlags this maxAge)
            (.setv impl :maxIdleSecs 0))
          (.setv impl :newOne flag))

        (maxIdleSecs [_] (or (.getv impl :maxIdleSecs) 0))
        (creationTime [_] (or (.getv _attrs CS_FLAG) 0))
        (expiryTime [_] (or (.getv _attrs ES_FLAG) 0))
        (xref [_] (.getv _attrs :csrf))
        (id [_] (.getv _attrs SSID_FLAG))

        (lastError [_] (.getv impl :error))

        (lastAccessedTime [_]
          (or (.getv _attrs LS_FLAG) 0))

        Object

        (toString [this]
          (str
            (reduce
              #(addDelim!
                 %1 NV_SEP
                 (str (name (first %2))
                      ":"
                      (last %2)))
              (strbf<>)
              (.seq _attrs))))

        IOSession

        (handleResult [_ evt res] (downstream evt res))
        (handleEvent [_ evt] (upstream evt))
        (impl [_] nil))

        {:typeid ::WebSS })))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;EOF


