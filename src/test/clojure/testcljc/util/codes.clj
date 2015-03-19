;; This library is distributed in  the hope that it will be useful but without
;; any  warranty; without  even  the  implied  warranty of  merchantability or
;; fitness for a particular purpose.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0  (http://opensource.org/licenses/eclipse-1.0.php)  which
;; can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any  fashion, you are agreeing to be bound by the
;; terms of this license. You  must not remove this notice, or any other, from
;; this software.
;; Copyright (c) 2013, Ken Leung. All rights reserved.

(ns

  testcljc.util.codes

  (:require [cmzlabclj.xlib.util.countrycode :as CC]
            [cmzlabclj.xlib.util.usastate :as SC])

  (:use [clojure.test]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(deftest testutil-codes

(is (= (CC/FindCountry "AU") (CC/FindCountry "au")))
(is (= "Australia" (CC/FindCountry "AU")))
(is (= "AU" (CC/FindCode "Australia")))
(is (false? (CC/IsUSA? "aa")))
(is (and (CC/IsUSA? "US") (= (CC/IsUSA? "US") (CC/IsUSA? "us"))))
(is (> (count (CC/ListCodes)) 0))

(is (= (SC/FindState "CA") (SC/FindState "ca")))
(is (= "California" (SC/FindState "ca")))
(is (= "CA" (SC/FindCode "California")))
(is (> (count (SC/ListCodes)) 0))

)

(def ^:private codes-eof nil)

;;(clojure.test/run-tests 'testcljc.util.codes)

