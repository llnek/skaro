;; This library is distributed in  the hope that it will be useful but without
;; any  warranty; without  even  the  implied  warranty of  merchantability or
;; fitness for a particular purpose.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0  (http://opensource.org/licenses/eclipse-1.0.php)  which
;; can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any  fashion, you are agreeing to be bound by the
;; terms of this license. You  must not remove this notice, or any other, from
;; this software.
;; Copyright (c) 2013 Ken Leung. All rights reserved.


(ns ^{:doc "A class that maps the state-code to the state-name."
      :author "kenl" }

  cmzlabclj.nucleus.util.usastate

  (:require [clojure.tools.logging :as log :only [info warn error debug] ]
            [clojure.string :as cstr]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;(set! *warn-on-reflection* true)

(def ^:private _CCODES {
    "AL"  "Alabama"
    "AK"  "Alaska"
    "AZ"  "Arizona"
    "AR"  "Arkansas"
    "CA"  "California"
    "CO"  "Colorado"
    "CT"  "Connecticut"
    "DE"  "Delaware"
    "FL"  "Florida"
    "GA"  "Georgia"
    "HI"  "Hawaii"
    "ID"  "Idaho"
    "IL"  "Illinois"
    "IN"  "Indiana"
    "IA"  "Iowa"
    "KS"  "Kansas"
    "KY"  "Kentucky"
    "LA"  "Louisiana"
    "ME"  "Maine"
    "MD"  "Maryland"
    "MA"  "Massachusetts"
    "MI"  "Michigan"
    "MN"  "Minnesota"
    "MS"  "Mississippi"
    "MO"  "Missouri"
    "MT"  "Montana"
    "NE"  "Nebraska"
    "NV"  "Nevada"
    "NH"  "New Hampshire"
    "NJ"  "New Jersey"
    "NM"  "New Mexico"
    "NY"  "New York"
    "NC"  "North Carolina"
    "ND"  "North Dakota"
    "OH"  "Ohio"
    "OK"  "Oklahoma"
    "OR"  "Oregon"
    "PA"  "Pennsylvania"
    "RI"  "Rhode Island"
    "SC"  "South Carolina"
    "SD"  "South Dakota"
    "TN"  "Tennessee"
    "TX"  "Texas"
    "UT"  "Utah"
    "VT"  "Vermont"
    "VA"  "Virginia"
    "WA"  "Washington"
    "WV"  "West Virginia"
    "WI"  "Wisconsin"
    "WY"  "Wyoming"
})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(def ^:private _CCODESEQ (seq _CCODES))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn ListCodes "List all the abbreviated states."

  []

  (keys _CCODES))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn FindState "Return the full state name."

  ^String
  [^String code]

  (_CCODES (cstr/upper-case code)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn FindCode "Return the abbreviated state code."

  ^String
  [^String state]

  (when-let [rs (filter #(= (nth % 1) state) _CCODESEQ) ]
    (nth (first rs) 0)
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def ^:private usastate-eof nil)

