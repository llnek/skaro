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

  czlab.skaro.io.files

  (:require
    [czlab.xlib.files :refer [mkdirs]]
    [czlab.xlib.core
     :refer [test-nestr
             seqint2
             trylet!
             muble<>
             try!]]
    [czlab.skaro.io.loops
     :refer [loopableSchedule
             loopableOneLoop]]
    [czlab.xlib.logging :as log]
    [clojure.java.io :as io]
    [czlab.xlib.str :refer [hgl? nsn]])

  (:use
    [czlab.skaro.sys.core]
    [czlab.skaro.io.core])

  (:import
    [java.io FileFilter File IOException]
    [java.util Properties ResourceBundle]
    [org.apache.commons.io.filefilter
     SuffixFileFilter
     PrefixFileFilter
     RegexFileFilter
     FileFileFilter]
    [org.apache.commons.io.monitor
     FileAlterationListener
     FileAlterationMonitor
     FileAlterationObserver
     FileAlterationListenerAdaptor]
    [org.apache.commons.io FileUtils]
    [czlab.skaro.server Service]
    [czlab.skaro.io FileEvent]
    [czlab.xlib Muble Identifiable]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;(set! *warn-on-reflection* false)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Order of args must match
(defmethod ioevent<>

  ::FilePicker
  [^Service co & [fnm f]]

  (let
    [eeid (seqint2)
     impl (muble<>)]
    (with-meta
      (reify FileEvent

        (checkAuthenticity [_] false)
        (bindSession [_ s] )
        (session [_] )
        (emitter [_] co)
        (originalFileName [_] fnm)
        (file [_] f)
        (id [_] eeid))

      {:typeid ::FileEvent})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- postPoll

  "Only look for new files"
  [^Service co ^File f action]

  (let
    [{:keys [recvFolder]}
     (.config co)
     orig (.getName f)
     cf (if (and (= action :FP-CREATED)
                 (some? recvFolder))
          (trylet!
            [r (expandVars recvFolder)
             d (io/file r orig)]
            (FileUtils/moveFile f d)
            d))]
    (when (some? cf)
      (->> (ioevent<> co orig cf action)
           (.dispatch co)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- toFMask

  ""
  ^FileFilter
  [^String mask]

  (cond
    (.startsWith mask "*.")
    (SuffixFileFilter. (.substring mask 1))
    (.endsWith mask "*")
    (PrefixFileFilter.
      (.substring mask
                  0 (dec (.length mask))))
    (> (.length mask) 0)
    (RegexFileFilter. mask)
    :else
    FileFileFilter/FILE))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmethod comp->initialize

  ::FilePicker
  [^Service co & [cfg0]]

  (log/info "comp->initialize: FilePicker: %s" (.id co))
  (let
    [c2 (merge (.config co) cfg0)
     {:keys [recvFolder
             fmask
             targetFolder]} c2
     root (expandVars targetFolder)
     dest (expandVars recvFolder)
     ff (toFMask (str fmask))]
    (log/info "monitoring folder: %s" root)
    (log/info "rcv folder: %s" (nsn dest))
    (test-nestr "file-root-folder" root)
    (->> (merge c2 {:targetFolder root
               :recvFolder dest
               :fmask ff})
         (.setv (.getx co) :emcfg))
    (let
      [obs (FileAlterationObserver. (io/file root) ff)
       mon (-> (s2ms (:intervalSecs c2))
               (FileAlterationMonitor. ))]
      (->>
        (proxy [FileAlterationListenerAdaptor][]
          (onFileCreate [f]
            (postPoll co f :FP-CREATED))
          (onFileChange [f]
            (postPoll co f :FP-CHANGED))
          (onFileDelete [f]
            (postPoll co f :FP-DELETED)))
        (.addListener obs ))
      (.addObserver mon obs)
      (doto (.getx co)
        (.setv :monitor mon)))
    co))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmethod loopableSchedule

  ::FilePicker
  [^Service co & args]

  (when-some
    [mon (.getv (.getx co) :monitor)]
    (log/info "apache io monitor starting...")
    (.start ^FileAlterationMonitor mon)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;EOF


