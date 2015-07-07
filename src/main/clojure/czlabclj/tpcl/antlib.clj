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

(ns ^{:doc ""
      :author "kenl" }

  czlabclj.tpcl.antlib

  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.string :as cstr])

  (:import [org.apache.commons.exec CommandLine DefaultExecutor]
           [org.apache.commons.io FileUtils]
           [java.util Map HashMap Stack]
           [java.lang.reflect Method]
           [java.beans Introspector PropertyDescriptor]
           [java.io File]
           [org.apache.tools.ant.taskdefs Javadoc Java Copy
            Chmod Concat Move Mkdir Tar Replace ExecuteOn
            Delete Jar Zip ExecTask Javac]
           [org.apache.tools.ant.listener AnsiColorLogger TimestampedLogger]
           [org.apache.tools.ant.types Reference
            Commandline$Argument
            Commandline$Marker
            PatternSet$NameEntry
            Environment$Variable FileSet Path DirSet]
           [org.apache.tools.ant NoBannerLogger Project Target Task]
           [org.apache.tools.ant.taskdefs Javadoc$AccessType
            Replace$Replacefilter Replace$NestedString
            Tar$TarFileSet Tar$TarCompressionMethod
            Javac$ImplementationSpecificArgument]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;(set! *warn-on-reflection* true)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- capstr "Just capitalize the 1st character."
  [^String s]
  (str (.toUpperCase (.substring s 0 1))
       (.substring s 1)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn AntProject "Create a new ant project."
  []
  (let [lg (doto
             ;;(TimestampedLogger.)
             (AnsiColorLogger.)
             ;;(NoBannerLogger.)
             (.setOutputPrintStream System/out)
             (.setErrorPrintStream System/err)
             (.setMessageOutputLevel Project/MSG_INFO)) ]
    (doto (Project.)
      (.init)
      (.setName "project-x")
      (.addBuildListener lg))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn ExecTarget "Run and execute a target."

  [^Target target]

  (.executeTarget (.getProject target) (.getName target)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(def ^:private dftprj (atom (AntProject)))
(def ^:private tasks
  (atom (let [arr (atom [])]
          (doseq [[k v] (.getTaskDefinitions @dftprj)]
            (when (.isAssignableFrom Task v)
              (let [n (str "Ant" (capstr k))]
                (reset! arr (conj @arr n k)))))
          (partition 2 (map #(symbol %) @arr)))))
(def ^:private props
  (atom (let [m {"tarfileset" Tar$TarFileSet
                 "fileset" FileSet }
              arr (atom {})]
          (doseq [[k v] (merge m (.getTaskDefinitions @dftprj))]
            (when (or (.isAssignableFrom Task v)
                      (contains? m k))
              (->> (-> v
                       (Introspector/getBeanInfo)
                       (.getPropertyDescriptors))
                   (reduce (fn [memo pd]
                             (assoc memo
                                    (keyword (.getName pd)) pd))
                           {})
                   (swap! arr assoc v))))
          @arr)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- method? "Find this setter method via best match."
  [^Class cz ^String m]
  (let [arr (make-array java.lang.Class 1)]
    (some
      (fn [^Class z]
        (aset #^"[Ljava.lang.Class;" arr 0 z)
        (try
          [(.getMethod cz m arr) z]
          (catch Exception _)))
      ;;add more types when needed
      [java.lang.String
       java.io.File
       Boolean/TYPE
       java.lang.Boolean
       Integer/TYPE
       java.lang.Integer
       Long/TYPE
       java.lang.Long
       org.apache.tools.ant.types.Path ])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmulti ^:private koerce "Best attempt to convert a value." (fn [_ a b] [a (class b)]))

(defmethod koerce [Integer/TYPE String] [_ _ ^String v] (Integer/parseInt v (int 10)))
(defmethod koerce [Integer String] [_ _ ^String v] (Integer/parseInt v (int 10)))

(defmethod koerce [Integer/TYPE Long] [_ _ ^Long v] (.intValue v))
(defmethod koerce [Integer Long] [_ _ ^Long v] (.intValue v))

(defmethod koerce [Integer/TYPE Integer] [_ _ ^Integer v] v)
(defmethod koerce [Integer Integer] [_ _ ^Integer v] v)

(defmethod koerce [Long/TYPE String] [_ _ ^String v] (Long/parseLong v (int 10)))
(defmethod koerce [Long String] [_ _ ^String v] (Long/parseLong v (int 10)))

(defmethod koerce [Long/TYPE Long] [_ _ ^Long v] v)
(defmethod koerce [Long Long] [_ _ ^Long v] v)

(defmethod koerce [Path File] [^Project pj _ ^File v] (Path. pj (.getCanonicalPath v)))
(defmethod koerce [Path String] [^Project pj _ ^String v] (Path. pj v))

(defmethod koerce [File String] [_ _ ^String v] (io/file v))
(defmethod koerce [File File] [_ _ v] v)

(defmethod koerce :default [_ pz _] (Exception. (str "expected class " pz)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- coerce "Best attempt to convert a given value."

  [^Project pj ^Class pz value]

  (cond
    (or (= Boolean/TYPE pz)
        (= Boolean pz))
    (= "true" (str value))

    (= String pz)
    (str value)

    :else
    (koerce pj pz value)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- setOptions "Use reflection and invoke setters."

  ([pj pojo options]
  (setOptions pj pojo options #{}))

  ([^Project pj ^Object pojo options skips]
  (let [arr (object-array 1)
        cz (.getClass pojo)
        ps (get @props cz) ]
    (doseq [[k v] options]
      (when-not (contains? skips k)
        (if-let [pd (get ps k)]
          (if-let [wm (.getWriteMethod pd)]
            ;;some cases the beaninfo is erroneous
            ;;so fall back to use *best-try*
            (let [pt (.getPropertyType pd)
                  m (.getName wm)]
              (aset arr 0 (coerce pj pt v))
              (.invoke wm pojo arr))
            (let [m (str "set" (capstr (name k)))
                  rc (method? cz m)]
              (when (nil? rc)
                (throw (Exception. (str m " not found in class " pojo))))
              (aset arr 0 (coerce pj (last rc) v))
              (.invoke (first rc) pojo arr)))
          ;;else
          (throw (Exception. (str "unknown property " (name k) ", task " cz)))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(declare maybeCfgNested)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn AntTarFileSet "Configure a TarFileSet Object."

  ^Tar$TarFileSet
  [^Project pj ^Tar$TarFileSet fs & [options nested]]

  (let [options (or options {})
        nested (or nested []) ]

    (setOptions pj fs options)
    (.setProject fs pj)
    (maybeCfgNested pj fs nested)
    fs
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn AntFileSet "Create a FileSet Object."

  ^FileSet
  [^Project pj & [options nested]]

  (let [fs (FileSet.)
        options (or options {})
        nested (or nested []) ]

    (setOptions pj
                fs
                (merge {:errorOnMissingDir false} options))
    (.setProject fs pj)
    (maybeCfgNested pj fs nested)
    fs
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn SetClassPath "Build a nested Path structure for classpath."

  [^Project pj ^Path root paths]

  (doseq [p paths]
    (case (first p)
      :location
      (doto (.createPath root)
        (.setLocation (io/file (str (last p)))))
      :refid
      (throw (Exception. "path:refid not supported."))
      ;;(doto (.createPath root) (.setRefid (last p)))
      :fileset
      (->> (AntFileSet pj
                       (if (> (count p) 1)(nth p 1) {})
                       (if (> (count p) 2)(nth p 2) []))
           (.addFileset root))
      nil)
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- maybeCfgNested ""

  [^Project pj tk nested]

  ;;(println "debug:\n" nested)
  (doseq [p nested]
    (case (first p)

      :compilerarg
      (when-let [^String line (:line (last p))]
        (-> (.createCompilerArg tk)
            (.setLine line)))

      :classpath
      (SetClassPath pj (.createClasspath tk) (last p))

      :sysprops
      (doseq [[k v] (last p)]
        (->> (doto (Environment$Variable.)
                   (.setKey (name k))
                   (.setValue (str v)))
             (.addSysproperty tk)))

      :include
      (-> (.createInclude tk)
          (.setName (str (last p))))

      :exclude
      (-> (.createExclude tk)
          (.setName (str (last p))))

      :fileset
      (->> (AntFileSet pj
                       (if (> (count p) 1)(nth p 1) {})
                       (if (> (count p) 2)(nth p 2) []))
           (.addFileset tk))

      :argvalues
      (doseq [v (last p)]
        (-> (.createArg tk)
            (.setValue (str v))))

      :argpaths
      (doseq [v (last p)]
        (-> (.createArg tk)
            (.setPath (Path. pj (str v)))))

      :arglines
      (doseq [v (last p)]
        (-> (.createArg tk)
            (.setLine (str v))))

      :replacefilter
      (doto (.createReplacefilter tk)
            (.setToken (:token (nth p 1)))
            (.setValue (:value (nth p 1))))

      :replacevalue
      (-> (.createReplaceValue tk)
          (.addText (:text (last p))))

      :replacetoken
      (-> (.createReplaceToken tk)
          (.addText (:text (last p))))

      :tarfileset
      (AntTarFileSet pj
                     (.createTarFileSet tk)
                     (if (> (count p) 1)(nth p 1) {})
                     (if (> (count p) 2)(nth p 2) []))

      nil)
  ))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- xxx-preopts ""
  [tk options]
  [options #{} ])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- delete-pre-opts ""

  [tk options]

  [(merge {:includeEmptyDirs true} options) #{}])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- jdoc-preopts ""

  [tk options]

  (when-let [[k v] (find options :access)]
    (.setAccess tk
                (doto (Javadoc$AccessType.)
                  (.setValue (str v)))))
  [options #{:access}])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- tar-preopts ""
  [tk options]
  (when-let [[k v] (find options :compression)]
    (.setCompression tk
                     (doto (Tar$TarCompressionMethod.)
                       (.setValue (str v)))))
  [options #{:compression}])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- init-task ""

  [^Project pj ^Target target tobj]

  (let [{:keys [pre-options tname
                task options nested] } tobj
        pre-options (or pre-options xxx-preopts)]
    (->> (doto ^Task
           task
           (.setProject pj)
           (.setOwningTarget target))
         (.addTask target))
    (->> (pre-options task options)
         (apply setOptions pj task))
    (maybeCfgNested pj task nested)
    task
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn ProjAntTasks ""

  ^Target
  [^String target tasks]

  (let [;;pj (AntProject)
        pj @dftprj
        tg (Target.)]
    (.setName tg (or target ""))
    (.addOrReplaceTarget pj tg)
    (doseq [t tasks]
      (init-task pj tg t))
    tg
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn ProjAntTasks* ""

  ^Target
  [target & tasks]

  (ProjAntTasks target tasks))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn RunAntTasks "Run ant tasks."

  [target tasks]

  (-> (ProjAntTasks target tasks)
      (ExecTarget)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn RunAntTasks* "Run ant tasks."

  [target & tasks]

  (RunAntTasks target tasks))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmacro ^:private ant-task

  ""
  [pj sym docstr func & [preopt]]

  (let [s (str func)
        tm (cstr/lower-case
             (.substring s (+ 1 (.lastIndexOf s "."))))]
    ;;(println "task---- " s)
    `(defn ~sym ~docstr [& [options# nested#]]
       (let [tk# (doto (.createTask ~pj ~s)
                     (.setTaskName ~tm))
             o# (or options# {})
             n# (or nested# [])
             r#  {:pre-options ~preopt
                  :tname ~tm
                  :task tk#
                  :options o#
                  :nested n#} ]
         (if (nil? ~preopt)
           (->> (case ~s
                  "delete" delete-pre-opts
                  "javadoc" jdoc-preopts
                  "tar" tar-preopts
                  nil)
                (assoc r# :pre-options))
           ;;else
           r#)))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmacro decl-ant-tasks ""
  [pj]
  `(do ~@(map (fn [[a b]] `(ant-task ~pj ~a "" ~b)) (deref tasks))))

(decl-ant-tasks @dftprj)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defmacro ^:private Xant-task

  ""
  [sym docstr func]

  (let [s (str func)
        tm (cstr/lower-case
             (.substring s (+ 1 (.lastIndexOf s "."))))]

    `(defn ~sym ~docstr [pj# & [options# nested#]]
       (let [tk# (doto (new ~func)
                     (.setProject pj#)
                     (.setTaskName ~tm))
             o# (or options# {})
             n# (or nested# [])]
        (setOptions pj# tk# o#)
        (maybeCfgNested pj# tk# n#)
        tk#
      ))
  ))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(comment
(ant-task AntReplace "" Replace)
(ant-task AntApply "" ExecuteOn)
(ant-task AntExec "" ExecTask)
(ant-task AntConcat "" Concat)
(ant-task AntChmod "" Chmod)
(ant-task AntJavac "" Javac)
(ant-task AntJava "" Java)
(ant-task AntMkdir "" Mkdir)
(ant-task AntDelete "" Delete)
(ant-task AntCopy "" Copy)
(ant-task AntMove "" Move)
(ant-task AntJar "" Jar)
(ant-task AntTar "" Tar tar-preopts)
(ant-task AntJavadoc "" Javadoc jdoc-preopts)
)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn CleanDir "Clean an existing dir or create it."

  [^File dir & {:keys [quiet]
                :or {:quiet true}}]

  (if (.exists dir)
    (RunAntTasks* ""
                  (AntDelete
                    {:quiet quiet}
                    [[:fileset {:dir dir}
                               [[:include "**/*"]]]]))
    ;;else
    (.mkdirs dir)
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn DeleteDir "Remove a directory."

  [^File dir & {:keys [quiet]
                :or {:quiet true}}]

  (when (.exists dir)
    (RunAntTasks*
      ""
      (AntDelete {:quiet quiet}
                 [[:fileset {:dir dir} ]]))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn CopyFile ""

  [file toDir]

  (let []
    (.mkdirs (io/file toDir))
    (RunAntTasks*
      ""
      (AntCopy {:file file
                :todir toDir} ))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn MoveFile ""

  [file toDir]

  (let []
    (.mkdirs (io/file toDir))
    (RunAntTasks*
      ""
      (AntMove {:file file
                :todir toDir} ))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;EOF
