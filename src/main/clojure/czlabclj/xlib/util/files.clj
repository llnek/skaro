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

(ns ^{:doc "General file related utilities."
      :author "kenl" }

  czlabclj.xlib.util.files

  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.string :as cstr])

  (:use [czlabclj.xlib.util.core :only [notnil?]]
        [czlabclj.xlib.util.str :only [nsb]]
        [czlabclj.xlib.util.meta :only [IsBytes?]])

  (:import  [org.apache.commons.io.filefilter FileFileFilter
                                              FileFilterUtils]
            [org.apache.commons.lang3 StringUtils]
            [java.io File FileInputStream
             FileOutputStream InputStream OutputStream]
            [java.util ArrayList]
            [java.net URL URI]
            [org.apache.commons.io IOUtils  FileUtils]
            [java.util.zip ZipFile ZipEntry]
            [com.zotohlab.frwk.io XData]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;(set! *warn-on-reflection* true)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn FileReadWrite? "Returns true if file is readable & writable."

  [^File fp]

  (and (notnil? fp)
       (.exists fp)
       (.isFile fp)
       (.canRead fp)
       (.canWrite fp)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn FileOK? "Returns true if file exists."

  [^File fp]

  (and (notnil? fp)
       (.exists fp)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn FileRead? "Returns true if file is readable."

  [^File fp]

  (and (notnil? fp)
       (.exists fp)
       (.isFile fp)
       (.canRead fp)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn DirReadWrite? "Returns true if directory is readable and writable."

  [^File dir]

  (and (notnil? dir)
       (.exists dir)
       (.isDirectory dir)
       (.canRead dir)
       (.canWrite dir) ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn DirRead? "Returns true if directory is readable."

  [^File dir]

  (and (notnil? dir)
       (.exists dir)
       (.isDirectory dir)
       (.canRead dir) ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn CanExec? "Returns true if file or directory is executable."

  [^File fp]

  (and (notnil? fp)
       (.exists fp)
       (.canExecute fp)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn ParentPath "Get the path to the parent directory."

  ^String
  [^String path]

  (if (cstr/blank? path)
    path
    (.getParent (File. path))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- jiggleZipEntryName ""

  ^String
  [^ZipEntry en]

  (.replaceAll (.getName en) "^[\\/]+",""))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn- doOneEntry ""

  [^ZipFile src ^File des ^ZipEntry en]

  (let [f (io/file des (jiggleZipEntryName en)) ]
    (if (.isDirectory en)
      (.mkdirs f)
      (do
        (.mkdirs (.getParentFile f))
        (with-open [inp (.getInputStream src en)
                    os (FileOutputStream. f) ]
          (IOUtils/copy inp os))
      ))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn Unzip "Unzip contents of zip file to a target folder."

  [^File src ^File des]

  (let [fpz (ZipFile. src)
        ents (.entries fpz) ]
    (.mkdirs des)
    (while (.hasMoreElements ents)
      (doOneEntry fpz des (.nextElement ents)))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn CopyFiles "Copy all files with *ext* to the destination folder."

  [^File srcDir ^File destDir ext]

  (FileUtils/copyDirectory
    srcDir
    destDir
    (FileFilterUtils/andFileFilter
      FileFileFilter/FILE
      (FileFilterUtils/suffixFileFilter (str "." ext)))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn CopyFileToDir "Copy a file to the target folder."

  [^File fp ^File dir]

  (FileUtils/copyFileToDirectory fp dir))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn CopyFile "Copy a file."

  [^File fp ^File target]

  (FileUtils/copyFile fp target))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn CopyToDir "Copy source folder to be a subfolder of target folder."

  [^File dir ^File targetDir]

  (FileUtils/copyDirectoryToDirectory dir targetDir))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn CopyDirFiles "Copy all contents in source folder to target folder."

  [^File dir ^File targetDir]

  (FileUtils/copyDirectory dir targetDir))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn DeleteDir "Erase the folder."

  [^File dir]

  (FileUtils/deleteDirectory dir))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn CleanDir "Remove contents in this folder."

  [^File dir]

  (FileUtils/cleanDirectory dir))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn WriteOneFile "Write data to file."

  ([^File fout ^Object data ^String enc]
   (if (IsBytes? (class data))
     (FileUtils/writeByteArrayToFile fout ^bytes data)
     (FileUtils/writeStringToFile fout
                                  (nsb data) enc)))

  ([^File fout ^Object data] (WriteOneFile fout data "utf-8")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn ReadOneFile "Read data from a file."

  (^String [^File fp] (ReadOneFile fp "utf-8"))

  (^String [^File fp ^String enc] (slurp fp :encoding enc)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn ReadOneUrl "Read data from a URL."

  (^String [^URL url] (ReadOneUrl url "utf-8"))

  (^String [^URL url ^String enc] (slurp url :encoding enc)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn SaveFile "Save a file to a directory."

  [^File dir ^String fname ^XData xdata]

  ;;(log/debug "Saving file: " fname)
  (let [fp (io/file dir fname) ]
    (io/delete-file fp true)
    (if-not (.isDiskFile xdata)
      (WriteOneFile fp (.javaBytes xdata))
      (FileUtils/moveFile (.fileRef xdata) fp))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(defn GetFile "Get a file from a directory."

  ^XData
  [^File dir ^String fname]

  ;;(log/debug "Getting file: " fname)
  (let [fp (io/file dir fname)
        xs (XData.) ]
    (if (FileRead? fp)
      (doto xs
        (.setDeleteFile false)
        (.resetContent fp))
      nil)
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn Mkdirs ""

  ^File
  [^File f]

  (doto f (.mkdirs)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(def ^:private files-eof nil)

