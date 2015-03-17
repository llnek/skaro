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

  testcljc.tardis.auth

  (:use [cmzlabclj.tardis.auth.plugin]
        [cmzlabclj.tardis.auth.model]
        [cmzlabclj.nucleus.crypto.codec]
        [cmzlabclj.nucleus.util.core]
        [cmzlabclj.nucleus.dbio.drivers]
        [cmzlabclj.nucleus.dbio.connect]
        [cmzlabclj.nucleus.dbio.core]
        [cmzlabclj.nucleus.dbio.h2]
        [clojure.test])

  (:import  [com.zotohlab.gallifrey.runtime AuthError UnknownUser]
            [java.io File]
            [com.zotohlab.frwk.crypto PasswordAPI]
            [com.zotohlab.frwk.dbio Transactable SQLr MetaCache DBAPI]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def METAC (atom (MakeMetaCache (cmzlabclj.tardis.auth.model.AuthPluginSchema.))))
(def JDBC (atom nil))
(def DB (atom nil))
(def ROLES (atom nil))

(defn init-test [f]
  (let [ dir (File. (System/getProperty "java.io.tmpdir"))
         db (str "" (System/currentTimeMillis))
         url (MakeH2Db dir db "sa" (Pwdify ""))
        jdbc (MakeJdbc (juid)
               { :d H2-DRIVER :url url :user "sa" :passwd "" }
               (Pwdify "")) ]
    (reset! JDBC jdbc)
    (ApplyAuthPluginDDL jdbc)
    (reset! DB (DbioConnect jdbc @METAC {})))
  (if (nil? f) nil (f))
    )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- create-roles []
  (binding [ *META-CACHE* (.getMetaCache ^DBAPI @DB) ]
    (let [ ^Transactable sql (.newCompositeSQLr ^DBAPI @DB) ]
      (.execWith
        sql
        (fn [^SQLr tx]
          (CreateAuthRole tx "Admin" "???")
          (CreateAuthRole tx "User" "???")
          (CreateAuthRole tx "Developer" "???")
          (CreateAuthRole tx "Tester" "???")))
      (let [ rs (.execWith
                  sql
                  (fn [^SQLr tx]
                    (.findAll tx
                              :czc.tardis.auth/AuthRole
                              {}))) ]
                              ;;TODO
                              ;;"order by role_name desc"))) ]
        (== (count rs) 4)))))

(defn- fetch-roles []
  (binding [ *META-CACHE* (.getMetaCache ^DBAPI @DB) ]
    (let [ ^Transactable sql (.newCompositeSQLr ^DBAPI @DB)
           rs (.execWith
                sql
                (fn [^SQLr tx] (.findAll tx :czc.tardis.auth/AuthRole ))) ]
      (reduce (fn [sum r]
                (assoc sum (:name r) r))
              {}
              (seq rs)))))

(defn- create-acct []
  (binding [ *META-CACHE* (.getMetaCache ^DBAPI @DB) ]
    (let [ ^Transactable sql (.newCompositeSQLr ^DBAPI @DB)
           ros (fetch-roles)
           u (.execWith
               sql
               (fn [^SQLr tx]
                 (CreateLoginAccount tx "joeb" (Pwdify "hi")
                                     {}
                                     [ (get ros "User") ] )))
           rc (.execWith
                sql
                (fn [^SQLr tx]
                  (DbioGetM2M {:as :roles :with tx} u))) ]
      (== (count rc) 1))))

(defn- load-acct-nouser []
  (binding [ *META-CACHE* (.getMetaCache ^DBAPI @DB) ]
    (let [ ^Transactable sql (.newCompositeSQLr ^DBAPI @DB) ]
      (try
        (.execWith
          sql
          (fn [^SQLr tx]
            (GetLoginAccount tx "xxxxx" "7soiwqhfasfhals")))
        false
        (catch UnknownUser e#
          true)))))

(defn- load-acct-badpwd [user]
  (binding [ *META-CACHE* (.getMetaCache ^DBAPI @DB) ]
    (let [ ^Transactable sql (.newCompositeSQLr ^DBAPI @DB) ]
      (try
        (.execWith
          sql
          (fn [^SQLr tx]
            (GetLoginAccount tx user "7soiwqhfasfhals")))
        false
        (catch AuthError e#
          true)))))

(defn- load-acct-ok [user pwd]
  (binding [ *META-CACHE* (.getMetaCache ^DBAPI @DB) ]
    (let [ ^Transactable sql (.newCompositeSQLr ^DBAPI @DB)
           u (.execWith
               sql
               (fn [^SQLr tx]
                 (GetLoginAccount tx user pwd))) ]
      (not (nil? u)))))

(deftest testtardis-auth

  (is (do (init-test nil) true))

  (is (create-roles))
  (is (create-acct))
  (is (load-acct-ok "joeb" "hi"))
  (is (load-acct-nouser))
  (is (load-acct-badpwd "joeb"))
)



;;(use-fixtures :each init-test)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def ^:private auth-eof nil)

;;(clojure.test/run-tests 'testcljc.tardis.auth)


