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

  czlab.skaro.etc.core

  (:require
    [czlab.xlib.resources :refer [getResource rstr rstr*]]
    [czlab.xlib.core
     :refer [test-cond
             inst?
             trap!
             prtStk
             muble<>]]
    [czlab.xlib.str :refer [str<> strim]]
    [clojure.java.io :as io]
    [czlab.table.core :as tbl]
    [czlab.xlib.logging :as log]
    [czlab.xlib.files :refer [dirRead?]])

  (:use [czlab.skaro.etc.cmd2]
        [czlab.xlib.format]
        [czlab.xlib.consts]
        [czlab.skaro.etc.cmd1])

  (:import
    [czlab.skaro.etc CmdHelpError]
    [java.io File]
    [czlab.xlib I18N]
    [java.util ResourceBundle List Locale]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;(set! *warn-on-reflection* false)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- getCmdInfo

  ""
  [rcb]

  (doall
    (partition 2
      (rstr*
        rcb
        ["usage.new"] ["usage.new.desc"]
        ["usage.svc"] ["usage.svc.desc"]
        ["usage.podify"] ["usage.podify.desc"]
        ["usage.ide"] [ "usage.ide.desc"]
        ["usage.build"] [ "usage.build.desc"]
        ["usage.test"] [ "usage.test.desc"]

        ["usage.debug"] ["usage.debug.desc"]
        ["usage.start"] ["usage.start.desc"]

        ["usage.gen"] [ "usage.gen.desc"]
        ["usage.demo"] [ "usage.demo.desc"]
        ["usage.version"] [ "usage.version.desc"]

        ["usage.testjce"] ["usage.testjce.desc"]
        ["usage.help"] ["usage.help.desc"]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- drawHelp

  ""
  [fmt arr]

  (doseq [a arr]
    (print (apply format fmt a))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn usage

  ""
  []

  (let
    [walls ["" "   " ""]
     style {:top ["" "" ""], :middle ["" "" ""] :bottom ["" "" ""]
            :dash " " :header-walls walls :body-walls walls }
     rcb (I18N/base)
     strs (getCmdInfo rcb)]
    (printf "%s\n\n" (rstr rcb "skaro.desc"))
    (printf "%s\n" (rstr rcb "cmds.header"))
    ;; prepend blanks to act as headers
    (printf "%s\n\n"
            (strim
              (with-out-str
                (-> '(("" ""))
                    (concat strs)
                    (tbl/table :style style)))))
    (printf "%s\n" (rstr rcb "cmds.trailer"))
    (println)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- execArgs

  ""
  [args]

  (let [cmd (keyword (first args))
        args (vec (drop 1 args))
        [f h]
        (*skaro-tasks* cmd)]
    (if (fn? f)
      (f args)
      (trap! CmdHelpError))
    args))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- parseArgs
  "Do nothing right now"
  [args]
  args)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- cmdStart

  "Make sure cmdline args are ok"
  [args]

  (when (< (count args) 1) (trap! CmdHelpError ))
  args)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn bootAndRun

  ""
  [home rcb & args]

  (binding [*skaro-home* (io/file home)
            *skaro-rb* rcb]
    (try
      ((comp execArgs
             parseArgs
             cmdStart) (vec args))
      (catch Throwable e
        (if (inst? CmdHelpError e)
          (usage)
          (prtStk e))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;EOF


