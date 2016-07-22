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


(ns ^:no-doc
    ^{:author "kenl"}

  czlab.skaro.demo.pop3.core

  (:require
    [czlab.xlib.logging :as log]
    [czlab.xlib.process :refer [delayExec]])

  (:import
    [czlab.wflow.dsl WHandler Job FlowDot PTask]
    [org.apache.commons.io IOUtils]
    [java.util.concurrent.atomic AtomicInteger]
    [javax.mail Message Message$RecipientType Multipart]
    [javax.mail.internet MimeMessage]
    [czlab.skaro.runtime AppMain]
    [czlab.skaro.io EmailEvent]
    [czlab.skaro.server Cocoon]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;(set! *warn-on-reflection* true)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(let [ctr (AtomicInteger.)]
  (defn- ncount ""
    []
    (.incrementAndGet ctr)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn demo ""

  ^WHandler
  []

  (reify WHandler
    (run [_  j _]
      (let [^EmailEvent ev (.event ^Job j)
            ^MimeMessage msg (.getMsg ev)
            ^Multipart p (.getContent msg) ]
        (println "######################## (" (ncount) ")" )
        (print "Subj:" (.getSubject msg) "\r\n")
        (print "Fr:" (first (.getFrom msg)) "\r\n")
        (print "To:" (first (.getRecipients msg
                                     Message$RecipientType/TO)))
        (print "\r\n")
        (println (IOUtils/toString (-> (.getBodyPart p 0)
                                       (.getInputStream))
                                   "utf-8"))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn myAppMain ""

  ^AppMain
  []

  (reify AppMain
    (contextualize [_ ctr] )
    (initialize [_] )
    (configure [_ cfg]
      (System/setProperty "skaro.demo.pop3"
                          "czlab.skaro.mock.mail.MockPop3Store"))
    (start [_] )
    (stop [_] )
    (dispose [_] )))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;EOF

