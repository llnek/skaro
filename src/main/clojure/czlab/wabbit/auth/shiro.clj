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

  czlab.wabbit.auth.shiro

  (:require
    [czlab.crypto.codec :refer [passwd<>]])

  (:require
    [czlab.xlib.logging :as log])

  (:import
    [org.apache.shiro.authc.credential CredentialsMatcher]
    [org.apache.shiro.realm AuthorizingRealm]
    [org.apache.shiro.authz
     AuthorizationException
     AuthorizationInfo]
    [org.apache.shiro.authc
     SimpleAccount
     AuthenticationException
     AuthenticationToken
     AuthenticationInfo ]
    [czlab.crypto PasswordAPI]
    [czlab.dbio DBAPI]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;(set! *warn-on-reflection* true)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(deftype PwdMatcher [] CredentialsMatcher

  (doCredentialsMatch [_ token info]
    (let [^AuthenticationToken tkn token
          ^AuthenticationInfo inf info
          pwd (.getCredentials tkn)
          uid (.getPrincipal tkn)
          pc (.getCredentials inf)
          tstPwd (passwd<>
                   (if (string? pwd)
                     pwd (String. ^chars pwd)))
          acc (-> (.getPrincipals inf)
                  (.getPrimaryPrincipal))]
      (and (= (:acctid acc) uid)
           (.validateHash tstPwd pc)))))

(ns-unmap *ns* '->PwdMatcher)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;EOF

