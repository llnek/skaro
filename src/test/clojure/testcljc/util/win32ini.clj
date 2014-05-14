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

(ns

  testcljc.util.win32ini

  (:use [clojure.test])
  (:require [cmzlabsclj.nucleus.util.core :as CU])
  (:require [cmzlabsclj.nucleus.util.ini :as WI]))

(def ^:private INIFILE (WI/ParseInifile (CU/ResUrl "com/zotohlabs/frwk/util/sample.ini")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(deftest testutil-wi32ini

(is (= (count (.sectionKeys ^cmzlabsclj.nucleus.util.ini.IWin32Conf INIFILE)) 2))

(is (map? (.getSection ^cmzlabsclj.nucleus.util.ini.IWin32Conf  INIFILE "operating systems")))
(is (map? (.getSection ^cmzlabsclj.nucleus.util.ini.IWin32Conf  INIFILE "boot loader")))

(is (true? (.endsWith ^String (.getString ^cmzlabsclj.nucleus.util.ini.IWin32Conf  INIFILE "boot loader" "default") "WINDOWS")))

(is (true? (= (.getLong ^cmzlabsclj.nucleus.util.ini.IWin32Conf  INIFILE "boot loader" "timeout") 30)))



)

(def ^:private win32ini-eof nil)

;;(clojure.test/run-tests 'testcljc.util.win32ini)

