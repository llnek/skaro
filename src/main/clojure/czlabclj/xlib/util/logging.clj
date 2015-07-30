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

  czlabclj.xlib.util.logging

  (:require [clojure.tools.logging :as log]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;(set! *warn-on-reflection* true)

(defmacro trace "" [& args]
  `(when (log/enabled? :trace) (log/logf :trace ~@args)))

(defmacro debug "" [& args]
  `(when (log/enabled? :debug) (log/logf :debug ~@args)))

(defmacro info "" [& args]
  `(when (log/enabled? :info) (log/logf :info ~@args)))

(defmacro warn "" [& args]
  `(when (log/enabled? :warn) (log/logf :warn ~@args)))

(defmacro error "" [& args]
  `(when (log/enabled? :error) (log/logf :error ~@args)))

(defmacro fatal "" [& args]
  `(when (log/enabled? :fatal) (log/logf :fatal ~@args)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;EOF

