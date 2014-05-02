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

  comzotohlabscljc.crypto.core

  (:require [clojure.tools.logging :as log :only [info warn error debug] ])
  (:require [clojure.string :as cstr])
  (:require [clojure.math.numeric-tower :as math])
  (:use [comzotohlabscljc.util.io :only [Streamify MakeBitOS ResetStream!] ])
  (:use [comzotohlabscljc.util.seqnum :only [NextInt] ])
  (:use [comzotohlabscljc.util.core
         :only
         [ThrowIOE ThrowBadArg ternary NewRandom Bytesify Try! notnil? juid GetClassname] ])
  (:use [comzotohlabscljc.util.dates :only [PlusMonths] ])
  (:require [comzotohlabscljc.util.mime
             :as mime
             :only [MaybeStream IsCompressed? IsEncrypted? IsSigned? ] ])
  (:use [comzotohlabscljc.util.str :only [strim nsb hgl?] ])

  (:import (java.io PrintStream File InputStream IOException
                    ByteArrayOutputStream ByteArrayInputStream
                    FileInputStream InputStreamReader))
  (:import (java.security GeneralSecurityException))
  (:import (java.math BigInteger))
  (:import (java.net URL))
  (:import (java.util Random Date))
  (:import (javax.activation DataHandler CommandMap MailcapCommandMap))
  (:import (javax.mail BodyPart MessagingException Multipart Session ))
  (:import (javax.mail.internet ContentType
                                MimeBodyPart MimeMessage MimeMultipart MimeUtility ))
  (:import (org.bouncycastle.asn1 ASN1ObjectIdentifier))
  (:import (org.bouncycastle.cms CMSAlgorithm))
  (:import (org.bouncycastle.cert X509CertificateHolder))
  (:import (java.security KeyStore$PasswordProtection))
  (:import (java.security KeyStore$PrivateKeyEntry KeyStore$TrustedCertificateEntry))
  (:import (java.security Policy PermissionCollection CodeSource
                          Permissions KeyPair KeyPairGenerator KeyStore
                          MessageDigest PrivateKey Provider PublicKey
                          AllPermission SecureRandom Security))
  (:import (java.security.cert CertificateFactory Certificate X509Certificate))
  (:import (org.bouncycastle.jce.provider BouncyCastleProvider))
  (:import (org.bouncycastle.asn1.x509 X509Extension))
  (:import (org.bouncycastle.asn1 ASN1EncodableVector))
  (:import (org.bouncycastle.asn1.cms AttributeTable IssuerAndSerialNumber))
  (:import (org.bouncycastle.asn1.smime SMIMECapabilitiesAttribute
                                        SMIMECapability
                                        SMIMECapabilityVector
                                        SMIMEEncryptionKeyPreferenceAttribute))
  (:import (org.bouncycastle.asn1.x500 X500Name))
  (:import (org.bouncycastle.cms CMSCompressedDataParser CMSException CMSProcessable
                                 CMSProcessableByteArray CMSProcessableFile
                                 CMSSignedData CMSSignedDataGenerator
                                 CMSTypedData CMSTypedStream
                                 DefaultSignedAttributeTableGenerator
                                 Recipient RecipientInfoGenerator
                                 RecipientInformation SignerInformation))
  (:import (org.bouncycastle.cms.jcajce JcaSignerInfoGeneratorBuilder
                                        JcaSimpleSignerInfoVerifierBuilder
                                        JceCMSContentEncryptorBuilder
                                        JceKeyTransEnvelopedRecipient
                                        JceKeyTransRecipientInfoGenerator
                                        ZlibExpanderProvider))
  (:import (org.bouncycastle.mail.smime SMIMECompressedGenerator SMIMEEnveloped
                                        SMIMEEnvelopedGenerator SMIMEException
                                        SMIMESigned SMIMESignedGenerator
                                        SMIMESignedParser))
  (:import (org.bouncycastle.operator OperatorCreationException ContentSigner))
  (:import (org.bouncycastle.operator.jcajce JcaDigestCalculatorProviderBuilder JcaContentSignerBuilder))
  (:import (org.bouncycastle.util Store))
  (:import (org.bouncycastle.operator.bc BcDigestCalculatorProvider))
  (:import (javax.security.auth.x500 X500Principal))
  (:import (org.bouncycastle.cms.jcajce JceKeyTransRecipientId))
  (:import (org.bouncycastle.mail.smime SMIMEEnvelopedParser))
  (:import (org.apache.commons.mail DefaultAuthenticator))
  (:import (org.bouncycastle.cert.jcajce JcaCertStore
                                         JcaX509CertificateConverter
                                         JcaX509ExtensionUtils
                                         JcaX509v1CertificateBuilder
                                         JcaX509v3CertificateBuilder))
  (:import (org.bouncycastle.cms CMSProcessableByteArray
                                 CMSSignedDataGenerator CMSSignedGenerator))
  (:import (org.bouncycastle.cms.jcajce ZlibCompressor JcaSignerInfoGeneratorBuilder))
  (:import (org.bouncycastle.openssl PEMParser))
  (:import (org.bouncycastle.operator DigestCalculatorProvider ContentSigner))
  (:import (org.bouncycastle.operator.jcajce JcaDigestCalculatorProviderBuilder JcaContentSignerBuilder))
  (:import (org.bouncycastle.pkcs PKCS10CertificationRequestBuilder PKCS10CertificationRequest))
  (:import (org.bouncycastle.pkcs.jcajce JcaPKCS10CertificationRequestBuilder))
  (:import (javax.crypto Cipher KeyGenerator Mac SecretKey))
  (:import (javax.crypto.spec SecretKeySpec))
  (:import (javax.net.ssl X509TrustManager TrustManager))
  (:import (javax.security.auth.x500 X500Principal))
  (:import (org.apache.commons.codec.binary Hex Base64))
  (:import (org.apache.commons.lang3 StringUtils))
  (:import (org.apache.commons.io FileUtils IOUtils))
  (:import (com.zotohlabs.frwk.crypto CryptoUtils SDataSource))
  (:import (com.zotohlabs.frwk.io XData))
  (:import (com.zotohlabs.frwk.net SSLTrustMgrFactory))
  (:import (java.lang Math)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(def DES_EDE3_CBC CMSAlgorithm/DES_EDE3_CBC)
(def RC2_CBC CMSAlgorithm/RC2_CBC)
(def IDEA_CBC CMSAlgorithm/IDEA_CBC)
(def CAST5_CBC CMSAlgorithm/CAST5_CBC)
(def AES128_CBC CMSAlgorithm/AES128_CBC)
(def AES192_CBC CMSAlgorithm/AES192_CBC)
(def AES256_CBC CMSAlgorithm/AES256_CBC)
(def CAMELLIA128_CBC CMSAlgorithm/CAMELLIA128_CBC)
(def CAMELLIA192_CBC CMSAlgorithm/CAMELLIA192_CBC)
(def CAMELLIA256_CBC CMSAlgorithm/CAMELLIA256_CBC)
(def SEED_CBC CMSAlgorithm/SEED_CBC)
(def DES_EDE3_WRAP CMSAlgorithm/DES_EDE3_WRAP)
(def AES128_WRAP CMSAlgorithm/AES128_WRAP)
(def AES256_WRAP CMSAlgorithm/AES256_WRAP)
(def CAMELLIA128_WRAP CMSAlgorithm/CAMELLIA128_WRAP)
(def CAMELLIA192_WRAP CMSAlgorithm/CAMELLIA192_WRAP)
(def CAMELLIA256_WRAP CMSAlgorithm/CAMELLIA256_WRAP)
(def SEED_WRAP CMSAlgorithm/SEED_WRAP)
(def ECDH_SHA1KDF CMSAlgorithm/ECDH_SHA1KDF)

(def EXPLICIT_SIGNING :EXPLICIT)
(def IMPLICIT_SIGNING :IMPLICIT)
(def DER_CERT :DER)
(def PEM_CERT :PEM)

(def SHA512 "SHA512withRSA")
(def SHA256 "SHA256withRSA")
(def SHA1 "SHA1withRSA")
(def SHA_512 "SHA-512")
(def SHA_1 "SHA-1")
(def SHA_256 "SHA-256")
(def MD_5 "MD5")
(def MD5 "MD5withRSA")

(def AES256_CBC  "AES256_CBC")
(def BFISH "BlowFish")
(def PKCS12 "PKCS12")
(def JKS "JKS")
(def SHA1 "SHA1")
(def MD5 "MD5")
(def RAS  "RAS")
(def DES  "DES")
(def RSA  "RSA")
(def DSA  "DSA")

(def ^:private DEF_ALGO "SHA1WithRSAEncryption")
(def ^:private DEF_MAC "HmacSHA512")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;(set! *warn-on-reflection* true)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn AssertJce "This function should fail if the non-restricted (unlimited-strength) jce files are not placed in jre-home"

  []

  (let [ kgen (doto (KeyGenerator/getInstance BFISH) (.init 256))
         cipher (doto (Cipher/getInstance BFISH)
                      (.init (Cipher/ENCRYPT_MODE)
                             (SecretKeySpec. (.. kgen generateKey getEncoded) BFISH))) ]
    (.doFinal cipher (Bytesify "This is just an example"))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(def ^Provider _BCProvider (let [ bcp (BouncyCastleProvider.) ] (Security/addProvider bcp) bcp))
(AssertJce)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(doto ^MailcapCommandMap (CommandMap/getDefaultCommandMap)
  (.addMailcap (str "application/pkcs7-signature;; "
            "x-java-content-handler="
            "org.bouncycastle.mail.smime.handlers.pkcs7_signature"))
  (.addMailcap (str "application/pkcs7-mime;; "
            "x-java-content-handler="
            "org.bouncycastle.mail.smime.handlers.pkcs7_mime"))
  (.addMailcap (str "application/x-pkcs7-signature;; "
          "x-java-content-handler="
          "org.bouncycastle.mail.smime.handlers.x_pkcs7_signature") )
  (.addMailcap (str "application/x-pkcs7-mime;; "
            "x-java-content-handler="
            "org.bouncycastle.mail.smime.handlers.x_pkcs7_mime"))
  (.addMailcap (str "multipart/signed;; "
          "x-java-content-handler="
          "org.bouncycastle.mail.smime.handlers.multipart_signed") ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn PkcsFile? ""

  [^URL keyUrl]

  (not (-> keyUrl (.getFile) (cstr/lower-case ) (.endsWith ".jks"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn MakeMsgDigest "Get a message digest instance."

  ^MessageDigest
  [algo]

  (MessageDigest/getInstance (nsb algo) _BCProvider))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn NextSerial "Get a random Big Integer."

  ^BigInteger
  []

  (let [ r (Random. (.getTime (Date.))) ]
    (BigInteger/valueOf (Math/abs (.nextLong r)))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn DbgProvider ""

  [^PrintStream os]

  (Try!  (.list _BCProvider os)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- getsrand "Get a secure random."
  ^SecureRandom
  []
  (SecureRandom/getInstance "SHA1PRNG" ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn NewAlias "Generate a new name."

  ^String
  []

  (str "" (System/currentTimeMillis) (NextInt)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- findAliases ""

  [^KeyStore keystore predicate]

  (loop [ en (.aliases keystore)
          rc (transient []) ]
    (if (not (.hasMoreElements en))
      (persistent! rc)
      (let [ n (.nextElement en) ]
        (if (predicate keystore n)
          (recur en (conj! rc n))
          (recur en rc))))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn CertAliases "Enumerate all cert aliases in the key-store."

  [^KeyStore keystore]

  (findAliases keystore (fn [^KeyStore ks ^String n] (.isCertificateEntry ks n))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn PKeyAliases "Enumerate all key aliases in the key-store."

  [^KeyStore keystore]

  (findAliases keystore (fn [^KeyStore ks ^String n] (.isKeyEntry ks n))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- regoCerts ""

  [ ^KeyStore ks
    ^comzotohlabscljc.crypto.codec.Password pwdObj]

  (let [ ^chars ca (if-not (nil? pwdObj)(.toCharArray pwdObj)) ]
    (doseq [ ^String a (PKeyAliases ks) ]
      (let [ cs (-> (CryptoUtils/getPKey ks a ca) (.getCertificateChain )) ]
        (doseq [ c (seq cs) ]
          (.setCertificateEntry ks (NewAlias) c))
      ))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn GetPkcsStore "Create a PKCS12 key-store."

  (^KeyStore
   [^InputStream inp ^comzotohlabscljc.crypto.codec.Password pwdObj]
     (let [ ca (if-not (nil? pwdObj)(.toCharArray pwdObj))
            ks (doto (KeyStore/getInstance "PKCS12" _BCProvider)
                     (.load inp ca)) ]
       (when-not (nil? inp) (regoCerts ks pwdObj))
       ks))

  (^KeyStore
   []
   (GetPkcsStore nil nil)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn GetJksStore "Create a JKS key-store."

  (^KeyStore
   [^InputStream inp ^comzotohlabscljc.crypto.codec.Password pwdObj]
     (let [ ca (if-not (nil? pwdObj)(.toCharArray pwdObj))
            ks (doto (KeyStore/getInstance "JKS" (Security/getProvider "SUN"))
                     (.load inp ca)) ]
       (when-not (nil? inp) (regoCerts ks pwdObj))
       ks))

  (^KeyStore
   []
   (GetJksStore nil nil)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmulti InitStore! "Initialize the key-store."
  (fn [a b c]
    (cond
      (instance? InputStream b)
      :stream

      (instance? File b)
      :file

      :else
      :bytes)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmethod InitStore! :stream

  ^KeyStore
  [ ^KeyStore store
    ^InputStream inp
    ^comzotohlabscljc.crypto.codec.Password pwdObj]

  (doto store (.load inp (if (nil? pwdObj) nil (.toCharArray pwdObj)))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmethod InitStore! :bytes

  ^KeyStore
  [ ^KeyStore store
    ^bytes bits
    ^comzotohlabscljc.crypto.codec.Password pwdObj]

  (InitStore! store (Streamify bits) pwdObj))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmethod InitStore! :file

  ^KeyStore
  [ ^KeyStore store
    ^File f
    ^comzotohlabscljc.crypto.codec.Password pwdObj]

  (with-open [ inp (FileInputStream. f) ]
    (InitStore! store inp pwdObj)
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn ConvCert "Returns a KeyStore$TrustedCertificateEntry."

  ^KeyStore$TrustedCertificateEntry
  [^bytes bits]

  (let [ ks (GetPkcsStore)
         nm (NewAlias)
         c (-> (CertificateFactory/getInstance "X.509")
               (.generateCertificate (Streamify bits))) ]
    (.setCertificateEntry ks nm c)
    (.getEntry ks nm nil)
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn ConvPKey "Returns a KeyStore$PrivateKeyEntry."

  ^KeyStore$PrivateKeyEntry
  [ ^bytes bits
    ^comzotohlabscljc.crypto.codec.Password pwdObj]

  (let [ ca (if (nil? pwdObj) nil (.toCharArray pwdObj))
         ks (GetPkcsStore) ]
    (.load ks (Streamify bits) ca)
    (.getEntry ks
               (nsb (first (PKeyAliases ks)))
               (KeyStore$PasswordProtection. ca))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn MakeEasyPolicy "Make a Policy that enables all permissions."

  ^Policy
  []

  (proxy [Policy] []
    (getPermissions [_ cs]
      (doto (Permissions.) (.add (AllPermission.))))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn GenMac "Generate a Message Auth Code."

  (^String
   [^bytes skey ^String data]
   (GenMac skey data DEF_MAC))

  (^String
   [^bytes skey ^String data ^String algo]
   (let [ mac (doto (Mac/getInstance algo _BCProvider)
                    (.init (SecretKeySpec. skey algo))
                    (.update (Bytesify data))) ]
      (Hex/encodeHexString (.doFinal mac)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn GenHash "Generate a Message Digest."

  (^String
    [^String data]
    (GenHash data SHA_512))

  (^String
    [^String data ^String algo]
    (let [ dig (MessageDigest/getInstance algo)
           b (.digest dig (Bytesify data)) ]
      (Base64/encodeBase64String b))) )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn MakeKeypair "Make a Asymmetric key-pair."

  ^KeyPair
  [^String algo keylen]

  (let [ kpg (doto (KeyPairGenerator/getInstance algo _BCProvider)
                   (.initialize ^long keylen (NewRandom))) ]
    (log/debug "Generating keypair for algo " algo ", length " keylen)
    (.generateKeyPair kpg)
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- loadPKCS12Key ""

  ^KeyStore$PrivateKeyEntry
  [ ^URL p12File
    ^comzotohlabscljc.crypto.codec.Password pwdObj]

  (with-open [ inp (.openStream p12File) ]
    (let [ ca (if (nil? pwdObj) nil (.toCharArray pwdObj))
           ks (doto (GetPkcsStore) (.load inp ca)) ]
      (CryptoUtils/getPKey ks (str (.nextElement (.aliases ks))) ca))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- fmtPEM ""

  ^bytes
  [^String top ^String end ^bytes bits]

  (let [ bs (Base64/encodeBase64 bits)
         baos (MakeBitOS)
         nl (Bytesify "\n")
         len (alength bs)
         bb (byte-array 1) ]
    (.write baos (Bytesify top))
    (loop [pos 0]
      (if (= pos len)
        (do (.write baos (Bytesify end)) (.toByteArray baos))
        (do
          (when (and (> pos 0) (= (mod pos 64) 0)) (.write baos nl))
          (aset bb 0 (aget ^bytes bs pos))
          (.write baos bb)
          (recur (inc pos)))
      ))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn ExportPKey "Export Private Key."

  ^bytes
  [^PrivateKey pkey ^clojure.lang.Keyword fmt]

  (let [ bits (.getEncoded pkey) ]
    (if (= fmt PEM_CERT)
      (fmtPEM "-----BEGIN RSA PRIVATE KEY-----\n"
              "\n-----END RSA PRIVATE KEY-----\n"
              bits)
      bits)
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn ExportCert "Export Certificate."

  ^bytes
  [^X509Certificate cert ^clojure.lang.Keyword fmt]

  (let [ bits (.getEncoded cert) ]
    (if (= fmt PEM_CERT)
      (fmtPEM "-----BEGIN CERTIFICATE-----\n"
              "-----END CERTIFICATE-----\n"
              bits)
      bits)
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn MakeCsrReq "Make a PKCS10 - csr-request."

  [keylen ^String dnStr ^clojure.lang.Keyword fmt]

  (log/debug "make-csrreq: dnStr= " dnStr ", key-len= " keylen)
  (let [ csb (JcaContentSignerBuilder. (nsb DEF_ALGO))
         ^KeyPair
         kp (MakeKeypair (nsb RSA) keylen)
         rbr (JcaPKCS10CertificationRequestBuilder. (X500Principal. dnStr)
                                                    (.getPublic kp))
         k (.getPrivate kp)
         cs (-> (.setProvider csb _BCProvider)
                (.build k))
         bits (-> (.build rbr cs) (.getEncoded)) ]
    [ (if (= fmt PEM_CERT)
              (fmtPEM "-----BEGIN CERTIFICATE REQUEST-----\n"
                      "\n-----END CERTIFICATE REQUEST-----\n"
                      bits)
              bits)
      (ExportPKey k fmt) ]
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; generate self-signed cert
;; self signed-> issuer is self
;;
(defn- mkSSV1Cert ""

  [^Provider pv ^KeyPair kp options]

  (let [ ^String dnStr (:dnStr options)
         ^Date start (:start options)
         ^Date end (:end options)
         ^String algo (:algo options)
         prv (.getPrivate kp)
         pub (.getPublic kp)
         bdr (JcaX509v1CertificateBuilder. (X500Principal. dnStr)
                                           (NextSerial)
                                           start
                                           end
                                           (X500Principal. dnStr)
                                           pub)
         cs (-> (.setProvider (JcaContentSignerBuilder. algo) pv)
                (.build prv))
         cert (-> (.setProvider (JcaX509CertificateConverter.) pv)
                  (.getCertificate (.build bdr cs))) ]
    (.checkValidity cert (Date.))
    (.verify cert pub)
    (log/debug "mkSSV1Cert: dn= " dnStr ", algo= " algo ", start=" start ", end=" end )
    [cert prv]
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- mkSSV1 ""

  ^bytes
  [^KeyStore ks ^KeyPair kp
   ^comzotohlabscljc.crypto.codec.Password pwdObj options]

  (let [ [^Certificate cert ^PrivateKey pkey]
         (mkSSV1Cert (.getProvider ks) kp options)
         ca (if (nil? pwdObj) nil (.toCharArray pwdObj))
         baos (MakeBitOS) ]
    (.setKeyEntry ks (juid) pkey ca (into-array Certificate [cert] ))
    (.store ks baos ca)
    (.toByteArray baos)
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn MakePkcs12 "Make a PKCS12 object from key and cert."

  [^bytes keyPEM ^bytes certPEM
   ^comzotohlabscljc.crypto.codec.Password pwdObj ^File out]

  (let [ ct (.getTrustedCertificate (ConvCert certPEM))
         rdr (InputStreamReader. (Streamify keyPEM))
         ca (if (nil? pwdObj) nil (.toCharArray pwdObj))
         baos (MakeBitOS)
         ss (GetPkcsStore)
         ^KeyPair kp (.readObject (PEMParser. rdr)) ]
    (.setKeyEntry ss (juid) (.getPrivate kp) ca (into-array Certificate [ct]))
    (.store ss baos ca)
    (FileUtils/writeByteArrayToFile out (.toByteArray baos))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn MakeSSv1PKCS12 "Make a SSV1 (root level) type PKCS12 object."

  [^String dnStr ^comzotohlabscljc.crypto.codec.Password pwdObj
   ^File out options]

  (let [ dft { :keylen 1024 :start (Date.) :end (PlusMonths 12) :algo DEF_ALGO }
         opts (assoc (merge dft options) :dnStr dnStr)
         ^long keylen (:keylen opts) ]
    (FileUtils/writeByteArrayToFile
      out
      (mkSSV1 (GetPkcsStore)
              (MakeKeypair (nsb RSA) keylen)
              pwdObj opts))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn MakeSSv1JKS "Make a SSV1 (root level) type JKS object."

  [^String dnStr ^comzotohlabscljc.crypto.codec.Password pwdObj
   ^File out options]

  (let [ dft { :keylen 1024 :start (Date.) :end (PlusMonths 12) :algo "SHA1withDSA" }
         opts (assoc (merge dft options) :dnStr dnStr)
         ^long keylen (:keylen opts) ]
    (FileUtils/writeByteArrayToFile
      out
      (mkSSV1 (GetJksStore)
              (MakeKeypair (nsb DSA) keylen)
              pwdObj
              opts))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- mkSSV3Cert ""

  [^Provider pv ^KeyPair kp  issuerObjs options ]

  (let [ subject (X500Principal. ^String (:dnStr options))
         ^X509Certificate issuer (first issuerObjs)
         ^PrivateKey issuerKey (last issuerObjs)
         exUte (JcaX509ExtensionUtils.)
         bdr (JcaX509v3CertificateBuilder. issuer
                                           (NextSerial)
                                           ^Date (:start options)
                                           ^Date (:end options)
                                           subject
                                           (.getPublic kp))
         cs (-> (JcaContentSignerBuilder. (str (:algo options)))
                (.setProvider pv)
                (.build issuerKey)) ]

    (-> bdr (.addExtension X509Extension/authorityKeyIdentifier false
                   (.createAuthorityKeyIdentifier exUte issuer)))
    (-> bdr (.addExtension X509Extension/subjectKeyIdentifier false
                   (.createSubjectKeyIdentifier exUte (.getPublic kp))))

    (let [ ct (-> (JcaX509CertificateConverter.)
                  (.setProvider pv)
                  (.getCertificate (.build bdr cs))) ]
      (.checkValidity ct (Date.))
      (.verify ct (.getPublicKey issuer))
      [ct (.getPrivate kp) ] )
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- mkSSV3 ""

  [ ^KeyStore ks ^comzotohlabscljc.crypto.codec.Password pwdObj
    issuerObjs options ]

  (let [ ^PrivateKey issuerKey (last issuerObjs)
         issuerCerts (vec (first issuerObjs))
         [^Certificate cert ^PrivateKey pkey]
         (mkSSV3Cert (.getProvider ks)
                     (MakeKeypair (.getAlgorithm issuerKey)
                                  (:keylen options))
                     [ (first issuerCerts) issuerKey ]
                     options)
         ca (if (nil? pwdObj) nil (.toCharArray pwdObj))
         baos (MakeBitOS)
         cs (cons cert issuerCerts) ]

    (.setKeyEntry ks (juid) pkey ca (into-array Certificate cs))
    (.store ks baos ca)
    (.toByteArray baos)
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- make-ssv3XXX ""

  [^String dnStr ^comzotohlabscljc.crypto.codec.Password pwdObj
   ^File out options]

  (let [ hack (:hack options)
         dft { :keylen 1024 :start (Date.) :end (PlusMonths 12) :algo (:algo hack) }
         issuerObjs [ (:issuerCerts options) (:issuerKey options) ]
         opts (assoc (merge dft options) :dnStr dnStr)
         ks (:ks hack)
         opts2 (-> opts (dissoc hack) (dissoc :issuerCerts) (dissoc :issuerKey)) ]
    (FileUtils/writeByteArrayToFile
      out
      (mkSSV3 ks pwdObj issuerObjs opts2))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn MakeSSv3PKCS12 "Make a SSV3 type PKCS12 object."

  [^String dnStr ^comzotohlabscljc.crypto.codec.Password pwdObj
   ^File out options]

  (make-ssv3XXX dnStr pwdObj out
                (-> options (assoc :hack { :algo DEF_ALGO :ks (GetPkcsStore) } ))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; JKS uses SUN and hence needs to use DSA
;;
(defn MakeSSv3JKS "Make a SSV3 JKS object."

  [^String dnStr ^comzotohlabscljc.crypto.codec.Password pwdObj
   ^File out options]

  (make-ssv3XXX dnStr pwdObj out
                (-> options (assoc :hack { :algo "SHA1withDSA" :ks (GetJksStore) } ))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn ExportPkcs7 "Extract and export PKCS7 info from a PKCS12 object."

  [^URL p12File ^comzotohlabscljc.crypto.codec.Password pwdObj
   ^File fileOut]

  (let [ pkey (loadPKCS12Key p12File pwdObj)
         cl (vec (.getCertificateChain pkey))
         gen (CMSSignedDataGenerator.)
         bdr (JcaSignerInfoGeneratorBuilder.  (-> (JcaDigestCalculatorProviderBuilder.)
                                              (.setProvider _BCProvider)
                                              (.build)))
;;    "SHA1withRSA"
         ;;cs (.build (.setProvider (JcaContentSignerBuilder. (CMSSignedGenerator/DIGEST_SHA512)) _BCProvider) k)
         cs (-> (JcaContentSignerBuilder. (nsb SHA512))
                (.setProvider _BCProvider)
                (.build (.getPrivateKey pkey)))
         ^X509Certificate x509 (first cl) ]

    (.addSignerInfoGenerator gen (.build bdr cs x509))
    (.addCertificates gen (JcaCertStore. cl))

    (let [ dummy (CMSProcessableByteArray. (Bytesify "Hello")) ]
      (FileUtils/writeByteArrayToFile
        fileOut
        (-> (.generate gen dummy)(.getEncoded)) ))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn NewSession "Creates a new java-mail session."

  (^Session
    []
    (NewSession "" nil))

  (^Session
    [^String user ^comzotohlabscljc.crypto.codec.Password pwdObj]
    (Session/getInstance (System/getProperties)
      (if (cstr/blank? user)
        nil
        (DefaultAuthenticator. user (nsb pwdObj)) ))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn NewMimeMsg "Create a new MIME Message."

  (^MimeMessage
    [^String user ^comzotohlabscljc.crypto.codec.Password pwdObj]
    (NewMimeMsg user pwdObj nil))

  (^MimeMessage
    []
    (NewMimeMsg "" nil nil))

  (^MimeMessage
    [^InputStream inp]
    (NewMimeMsg "" nil inp))

  (^MimeMessage
    [^String user ^comzotohlabscljc.crypto.codec.Password pwdObj
     ^InputStream inp]
    (let [ s (NewSession user pwdObj) ]
      (if (nil? inp) (MimeMessage. s) (MimeMessage. s inp)))) )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn IsSigned? "Check if this stream-like object/message-part is signed."

  [^Object obj]

  (let [ inp (mime/MaybeStream obj) ]
    (if (nil? inp)
      (if (instance? Multipart obj)
        (let [ ^Multipart mp obj ] (mime/IsSigned? (.getContentType mp)))
        (ThrowIOE (str "Invalid content: " (GetClassname obj))))
      (try
        (mime/IsSigned? (.getContentType (NewMimeMsg "" "" inp)))
        (finally (ResetStream! inp))))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn IsCompressed? "Check if this stream-like object/message-part is compressed."

  [^Object obj]

  (let [ inp (mime/MaybeStream obj) ]
    (if (nil? inp)
      (cond
        (instance? Multipart obj)
        (let [ ^Multipart mp obj ]
          (mime/IsCompressed? (.getContentType mp)))

        (instance? BodyPart obj)
        (let [ ^BodyPart bp obj ]
          (mime/IsCompressed? (.getContentType bp)))

        :else
        (ThrowIOE (str "Invalid content: " (GetClassname obj))))
      (try
        (mime/IsCompressed? (.getContentType (NewMimeMsg "" "" inp)))
        (finally (ResetStream! inp))))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn IsEncrypted? "Check if this stream-like object/message-part is encrypted."

  [^Object obj]

  (let [ inp (mime/MaybeStream obj) ]
    (if (nil? inp)
      (cond
        (instance? Multipart obj)
        (let [ ^Multipart mp obj ]
          (mime/IsEncrypted? (.getContentType mp)))

        (instance? BodyPart obj)
        (let [ ^BodyPart bp obj ]
          (mime/IsEncrypted? (.getContentType bp)))

        :else
        (ThrowIOE (str "Invalid content: " (GetClassname obj))))
      (try
        (mime/IsEncrypted? (.getContentType (NewMimeMsg "" "" inp)))
        (finally (ResetStream! inp))))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn GetCharset "Deduce the char-set from content-type."

  (^String
    [^String cType]
    (if (hgl? cType)
      (try
        (nsb (MimeUtility/javaCharset (-> (ContentType. cType) (.getParameter "charset"))))
        (catch Throwable e# (do (log/warn e# "") "")))
      ""))

  (^String
    [^String cType ^String dft]
    (let [ cs (GetCharset cType) ]
      (nsb (if (hgl? cs) cs (MimeUtility/javaCharset dft))))) )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- makeSignerGentor

  ^SMIMESignedGenerator
  [^PrivateKey pkey certs ^String algo]

  (let [ gen (SMIMESignedGenerator. "base64")
         lst (vec certs)
         caps (doto (SMIMECapabilityVector.)
                    (.addCapability SMIMECapability/dES_EDE3_CBC)
                    (.addCapability SMIMECapability/rC2_CBC, 128)
                    (.addCapability SMIMECapability/dES_CBC) )
         signedAttrs (doto (ASN1EncodableVector.)
                           (.add (SMIMECapabilitiesAttribute. caps)))
         ^X509Certificate x0 (first lst)
         ^X509Certificate issuer (if (> (count lst) 1) (nth lst 1) x0)
         issuerDN (.getSubjectX500Principal issuer)
         ;;
         ;; add an encryption key preference for encrypted responses -
         ;; normally this would be different from the signing certificate...
         ;;
         issAndSer (IssuerAndSerialNumber.
                     (X500Name/getInstance (.getEncoded issuerDN))
                     (.getSerialNumber x0))
         dm1 (.add signedAttrs (SMIMEEncryptionKeyPreferenceAttribute. issAndSer))
         bdr (doto (JcaSignerInfoGeneratorBuilder.
                          (-> (JcaDigestCalculatorProviderBuilder.)
                              (.setProvider _BCProvider)
                              (.build)))
                   (.setDirectSignature true))
         cs (-> (JcaContentSignerBuilder. (nsb algo))
                (.setProvider _BCProvider)
                (.build pkey)) ]

    (-> bdr
        (.setSignedAttributeGenerator (DefaultSignedAttributeTableGenerator.
                                        (AttributeTable. signedAttrs))))
    (.addSignerInfoGenerator gen (.build bdr cs x0))
    (.addCertificates gen (JcaCertStore. lst))
    gen
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmulti SmimeDigSig "Generates a MimeMultipart."
  (fn [a b c d]
    (cond
      (instance? MimeMessage d)
      :mimemessage

      (instance? Multipart d)
      :multipart

      (instance? BodyPart d)
      :bodypart

      :else
      (ThrowBadArg "wrong type"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmethod SmimeDigSig :mimemessage

  [^PrivateKey pkey certs ^String algo ^MimeMessage mmsg]

  ;; force internal processing, just in case
  (.getContent mmsg)
  (-> (makeSignerGentor pkey certs algo) (.generate mmsg )))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmethod SmimeDigSig :multipart

  [^PrivateKey pkey certs ^String algo ^Multipart mp]

  (let [ mm (NewMimeMsg) ]
    (.setContent mm mp)
    (-> (makeSignerGentor pkey certs algo) (.generate mm ))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmethod SmimeDigSig :bodypart

  [^PrivateKey pkey certs ^String algo ^BodyPart bp]

  (let [ ^MimeBodyPart mbp bp ]
    (-> (makeSignerGentor pkey certs algo)
        (.generate mbp ))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- smimeDec

  ^CMSTypedStream
  [^PrivateKey pkey ^SMIMEEnveloped env]

    ;;var  recId = new JceKeyTransRecipientId(cert.asInstanceOf[XCert])
  (loop [ rec (-> (JceKeyTransEnvelopedRecipient. pkey)
                 (.setProvider _BCProvider))
          it (-> (.getRecipientInfos env) (.getRecipients) (.iterator))
          rc nil ]
    (if (or (notnil? rc) (not (.hasNext it)))
      rc
      (recur rec it
             (-> (let [ ^RecipientInformation ri (.next it) ] ri)
                 (.getContentStream rec))))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmulti SmimeDecrypt "SMIME decrypt this object."
  (fn [a b]
    (cond
      (instance? MimeMessage b)
      :mimemsg

      (instance? BodyPart b)
      :bodypart

      :else
      (ThrowBadArg "wrong type"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- smimeLoopDec ""

  ^bytes
  [pkeys ^SMIMEEnveloped ev]

  (let [ rc (some (fn [ ^PrivateKey k & args ]
                      (if-let [ cms (smimeDec k ev) ]
                        (IOUtils/toByteArray (.getContentStream cms))
                        false))
                  pkeys) ]
    (when (nil? rc) (throw (GeneralSecurityException. "No matching decryption key")))
    rc
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmethod SmimeDecrypt :mimemsg

  ^bytes
  [pkeys ^MimeMessage mimemsg]

  (smimeLoopDec pkeys (SMIMEEnveloped. mimemsg)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmethod SmimeDecrypt :bodypart

  ^bytes
  [pkeys ^BodyPart part]

  (let [ ^MimeBodyPart bbp part ]
    (smimeLoopDec pkeys (SMIMEEnveloped. bbp))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn PeekSmimeSignedContent "Get the content ignoring the signing stuff."

  ^Object
  [^Multipart mp]

  (let [ ^MimeMultipart  mmp mp ]
    (-> (SMIMESignedParser. (BcDigestCalculatorProvider.)
                            mmp
                            (GetCharset (.getContentType mp) "binary"))
        (.getContent)
        (.getContent))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn TestSmimeDigSig "Verify the signature and return content if ok."

  ( [^Multipart mp certs]
    (TestSmimeDigSig mp certs ""))

  ( [^Multipart mp certs ^String cte]
    (let [ sc (if (hgl? cte)
                  (SMIMESigned. ^MimeMultipart mp cte)
                  (SMIMESigned. ^MimeMultipart mp))
           sns (-> (.getSignerInfos sc) (.getSigners) )
           s (JcaCertStore. (vec certs))
           rc (some (fn [^SignerInformation si]
                      (loop [ c (.getMatches s (.getSID si))
                              it (.iterator c)
                              ret nil
                              stop false]
                        (if (or stop (not (.hasNext it)))
                          ret
                          (let [ ci (-> (JcaSimpleSignerInfoVerifierBuilder.)
                                        (.setProvider _BCProvider)
                                        (.build ^X509CertificateHolder (.next it))) ]
                            (if (.verify si ci)
                              (if-let [ digest (.getContentDigest si) ]
                                (recur c it [sc digest] true)
                                (recur c it nil false))
                              (recur c it nil false))))))
                    (seq sns)) ]
      (when (nil? rc) (throw (GeneralSecurityException. "Verify signature: no matching cert.")) )
      [ (-> (.getContentAsMimeMessage sc (NewSession)) (.getContent)) (nth rc 1) ] )))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmulti SmimeDecompress "Inflate the compressed content."
  (fn [a]
    (cond
      (instance? InputStream a)
      :stream

      (instance? BodyPart a)
      :bodypart

      :else
      (ThrowBadArg "wrong type"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmethod SmimeDecompress :bodypart

  ^XData
  [^BodyPart bp]

  (if (nil? bp)
      (XData.)
      (SmimeDecompress (.getInputStream bp))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmethod SmimeDecompress :stream

  ^XData
  [^InputStream inp]

  (if (nil? inp)
    (XData.)
    (let [ cms (-> (CMSCompressedDataParser. inp)
                   (.getContent (ZlibExpanderProvider.))) ]
      (when (nil? cms) (throw (GeneralSecurityException. "Decompress stream: corrupted content.")))
      (XData. (IOUtils/toByteArray (.getContentStream cms))))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmulti SmimeEncrypt "Generates a MimeBodyPart."
  (fn [a b c]
    (cond
      (instance? MimeMessage c)
      :mimemsg

      (instance? Multipart c)
      :multipart

      (instance? BodyPart c)
      :bodypart

      :else
      (ThrowBadArg "wrong type"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmethod SmimeEncrypt :bodypart

  ^MimeBodyPart
  [^Certificate cert ^String algo ^BodyPart bp]

  (let [ gen (SMIMEEnvelopedGenerator.) ]
    (.addRecipientInfoGenerator gen
        (-> (JceKeyTransRecipientInfoGenerator. ^X509Certificate cert)
            (.setProvider _BCProvider)))
    (.generate gen ^MimeBodyPart bp
        (-> (JceCMSContentEncryptorBuilder. algo)
            (.setProvider _BCProvider)
            (.build)))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmethod SmimeEncrypt :mimemsg

  ^MimeBodyPart
  [^Certificate cert ^String algo ^MimeMessage msg]

  (let [ gen (SMIMEEnvelopedGenerator.) ]
    ;; force message to be processed, just in case.
    (.getContent msg)
    (.addRecipientInfoGenerator gen
      (-> (JceKeyTransRecipientInfoGenerator. ^X509Certificate cert)
          (.setProvider _BCProvider)))
    (.generate gen msg
      (-> (JceCMSContentEncryptorBuilder. algo)
          (.setProvider _BCProvider)
          (.build)))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmethod SmimeEncrypt :multipart

  ^MimeBodyPart
  [^Certificate cert ^String algo ^Multipart mp]

  (let [ gen (SMIMEEnvelopedGenerator.) ]
    (.addRecipientInfoGenerator gen
      (-> (JceKeyTransRecipientInfoGenerator. ^X509Certificate cert)
          (.setProvider _BCProvider)))
    (.generate gen (doto (NewMimeMsg)(.setContent mp))
      (-> (JceCMSContentEncryptorBuilder. algo)
          (.setProvider _BCProvider)
          (.build)))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn SmimeCompress "Generates a MimeBodyPart."

  (^MimeBodyPart
    [^String cType ^XData xs]
    (let [ ds (if (.isDiskFile xs)
                  (SDataSource. (.fileRef xs) cType)
                  (SDataSource. (.javaBytes xs) cType))
           bp (MimeBodyPart.) ]
      (.setDataHandler bp (DataHandler. ds))
      (.generate (SMIMECompressedGenerator.) bp (ZlibCompressor.))))

  (^MimeBodyPart
    [^MimeMessage msg]
    (.getContent msg) ;; make sure it's processed, just in case
    (-> (SMIMECompressedGenerator.) (.generate msg (ZlibCompressor.))))

  (^MimeBodyPart
    [^String cType ^String contentLoc ^String cid ^XData xs]
    (let [ ds (if (.isDiskFile xs)
                  (SDataSource. (.fileRef xs) cType)
                  (SDataSource. (.javaBytes xs) cType))
           bp (MimeBodyPart.) ]
      (when (hgl? contentLoc) (.setHeader bp "content-location" contentLoc))
      (when (hgl? cid) (.setHeader bp "content-id" cid))
      (.setDataHandler bp (DataHandler. ds))
      (let [ zbp (.generate (SMIMECompressedGenerator.) bp (ZlibCompressor.))
             pos (.lastIndexOf cid (int \>))
             cID (if (>= pos 0) (str (.substring cid 0 pos) "--z>") (str cid "--z")) ]
        (when (hgl? contentLoc) (.setHeader zbp "content-location" contentLoc))
        (.setHeader zbp "content-id" cID)
        ;; always base64
        ;;cte="base64"
        (.setHeader zbp "content-transfer-encoding" "base64")
        zbp))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn PkcsDigSig "SMIME sign some data."

  ^bytes
  [^PrivateKey pkey certs ^String algo ^XData xs]

  (let [ bdr (JcaSignerInfoGeneratorBuilder.
                (-> (JcaDigestCalculatorProviderBuilder.)
                    (.setProvider _BCProvider)
                    (.build)))
         cs (-> (JcaContentSignerBuilder. (nsb algo))
                (.setProvider _BCProvider)
                (.build pkey))
         gen (CMSSignedDataGenerator.)
         cl (vec certs)
         cert (first cl) ]
    (.setDirectSignature bdr true)
    (.addSignerInfoGenerator gen (.build bdr cs ^X509Certificate cert))
    (.addCertificates gen (JcaCertStore. cl))
    (-> (.generate gen
                   (if (.isDiskFile xs)
                       (CMSProcessableFile. (.fileRef xs))
                       (CMSProcessableByteArray. (.javaBytes xs)))
                   false)
        (.getEncoded))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn TestPkcsDigSig "Verify the signed object with the signature."

  ^bytes
  [^Certificate cert ^XData xdata ^bytes signature]

  (let [ cproc (if (.isDiskFile xdata)
                   (CMSProcessableFile. (.fileRef xdata))
                   (CMSProcessableByteArray. (.javaBytes xdata)))
         cms (CMSSignedData. ^CMSProcessable cproc signature)
         s (JcaCertStore. [cert])
         sls (-> cms (.getSignerInfos) (.getSigners))
         rc (some (fn [^SignerInformation si]
                     (loop [ c (.getMatches s (.getSID si))
                             it (.iterator c)
                             digest nil
                             stop false ]
                       (if (or stop (not (.hasNext it)))
                         digest
                         (let [ bdr (-> (JcaSimpleSignerInfoVerifierBuilder.)
                                        (.setProvider _BCProvider))
                                ok (.verify si (.build bdr ^X509CertificateHolder (.next it)))
                                dg (if ok (.getContentDigest si) nil) ]
                           (if (nil? dg)
                               (recur c it nil false)
                               (recur c it dg true))))))
                  (seq sls)) ]
    (when (nil? rc) (throw (GeneralSecurityException. "Decode signature: no matching cert.")))
    rc
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- str-signingAlgo ""

  [algo]

  (case algo
    "SHA-512" SMIMESignedGenerator/DIGEST_SHA512
    "SHA-1" SMIMESignedGenerator/DIGEST_SHA1
    "MD5" SMIMESignedGenerator/DIGEST_MD5
    (ThrowBadArg (str "Unsupported signing algo: " algo))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- finger-print ""

  ^String
  [^bytes data ^String algo]

  (let [ hv (-> (MessageDigest/getInstance (nsb algo))
                (.digest data))
         hlen (alength hv)
         tail (dec hlen) ]
    (loop [ ret (StringBuilder.)
            i 0 ]
      (if (>= i hlen)
        (nsb ret)
        (let [ n (cstr/upper-case (Integer/toString (bit-and (aget ^bytes hv i) 0xff) 16)) ]
          (-> ret
              (.append (if (= (.length n) 1) (str "0" n) n))
              (.append (if (= i tail) "" ":")))
          (recur ret (inc i)))
      ))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn FingerprintSHA1 "Generate a fingerprint using SHA-1."

  ^String
  [^bytes data]

  (finger-print data SHA_1))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn FingerprintMD5 "Generate a fingerprint using MD5."

  ^String
  [^bytes data]

  (finger-print data MD_5))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defrecord CertDesc
  [ ^X500Principal subj ^X500Principal issuer ^Date notBefore ^Date notAfter ])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn DescCertificate "Get some basic info from Certificate."

  ^comzotohlabscljc.crypto.core.CertDesc
  [^X509Certificate x509]

  (if (nil? x509)
      (->CertDesc nil nil nil nil)
      (->CertDesc (.getSubjectX500Principal x509)
                  (.getIssuerX500Principal x509)
                  (.getNotBefore x509)
                  (.getNotAfter x509))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn DescCert "Return a object"

  ( ^comzotohlabscljc.crypto.core.CertDesc
    [^bytes privateKeyBits ^comzotohlabscljc.crypto.codec.Password pwdObj]
      (if-let [ pkey (ConvPKey privateKeyBits pwdObj) ]
              (DescCertificate (.getCertificate pkey))
              (->CertDesc nil nil nil nil)))

  ( ^comzotohlabscljc.crypto.core.CertDesc
    [^bytes certBits]
      (if-let [ cert (ConvCert certBits) ]
              (DescCertificate (.getTrustedCertificate cert))
              (->CertDesc nil nil nil nil))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn ValidCertificate? "Validate this Certificate."

  [^X509Certificate x509]

  (try
    (.checkValidity x509 (Date.))
    (catch Throwable e# false)
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn ValidPKey? "Validate this Private Key."

  [^bytes keyBits ^comzotohlabscljc.crypto.codec.Password pwdObj]

  (if-let [ pkey (ConvPKey keyBits pwdObj) ]
          (ValidCertificate? (.getCertificate pkey))
          false
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn ValidCert? "Validate this Certificate."

  [^bytes certBits]

  (if-let [ cert (ConvCert certBits) ]
          (ValidCertificate? (.getTrustedCertificate cert))
          false
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn IntoArrayCerts "From a list of TrustedCertificateEntry(s)."

  [certs]

  (if (empty? certs)
      []
      (map (fn [ ^KeyStore$TrustedCertificateEntry x] (.getTrustedCertificate x))
           (seq certs))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn IntoArrayPKeys "From a list of PrivateKeyEntry(s)."

  [pkeys]

  (if (empty? pkeys)
      []
      (map (fn [^KeyStore$PrivateKeyEntry x] (.getPrivateKey x))
           (seq pkeys))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn MakeSimpleTrustMgr ""

  ^X509TrustManager
  []

  (nth (SSLTrustMgrFactory/getTrustManagers) 0))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(def ^:private core-eof nil)


