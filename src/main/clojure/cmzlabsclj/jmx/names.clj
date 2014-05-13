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

  cmzlabsclj.jmx.names

  (:require [clojure.tools.logging :as log :only [info warn error debug] ])
  (:require [clojure.string :as cstr])
  (:import (org.apache.commons.lang3 StringUtils))
  (:import (javax.management ObjectName)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn MakeObjectName "domain: com.acme
                      beanName: mybean
                      paths: [ \"a=b\" \"c=d\" ]"
  (^ObjectName [^String domain ^String beanName paths]
    (let [ sb (StringBuilder.)
           cs (seq paths) ]
      (doto sb
            (.append domain)
            (.append ":")
            (.append (cstr/join "," cs)))
      (when-not (empty? cs) (.append sb ","))
      (doto sb
            (.append "name=")
            (.append beanName))
      (ObjectName. (.toString sb))))

  (^ObjectName [domain beanName] (MakeObjectName domain beanName [])) )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(def ^:private names-eof nil)
