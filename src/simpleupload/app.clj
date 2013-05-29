(ns simpleupload.app
  (:require [clasp.clasp :refer [defroute wrap-routes]]
            [clojure.data.codec.base64 :as b64]
            [clojure.java.io :as io]
            [clojure.string :refer [split]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.util.response :refer [response]]))


(def KEY "foo")
(def ALPHANUM (map char (mapcat (partial apply range)
                                ['(48 58) '(65 91) '(97 123)])))
(def WWW-DIR-FMT "/home/%s/www")
(def UPLOAD-DIR "uploads")


(defn- rand-str
  ([] (rand-str 6))
  ([n] (apply str (take n (repeatedly #(rand-nth ALPHANUM))))))


(defn- xor-cipher
  [#^String p #^String k]
  (map #(char (bit-xor (int %1) (int %2))) p (cycle k)))


(defn- encode-ciphertext
  [#^String c]
  (String. (b64/encode (.getBytes (apply str c)))))


(defn- decode-ciphertext
  [#^String c #^String k]
  (xor-cipher (map char (b64/decode (.getBytes c))) k))


(defn- decode-address
  [#^String a & {:keys [k] :or {k KEY}}]
  (let [c ((split a #"\@") 0)]
    (apply str (decode-ciphertext c k))))


(defn- safe-filename
  "
  Takes a `filename` and if an exactly-named file already exists at the upload
  location, splits the filename into a name, extension pair. A random string is
  generate and interposed betwee the original filename and extension.
  "
  [{:keys [filename]}]
  (if (.exists (io/file upload-dir filename))
    (let [split-filename (split filename #"\.(?=[^.]*$)")]
      (str (split-filename 0) "-" (rand-str) "." (split-filename 1))))
  filename)


(defn- handle-mail
  [req]
  (let [user (decode-address (get-in req [:multipart-params "recipient"]))
        www-dir (io/file (format WWW-DIR-FMT user))
        upload-dir (io/file www-dir "uploads")
        temp-not-nil? (comp not nil? :tempfile)
        files (filter temp-not-nil? (vals (:multipart-params req)))]
    ;; Ensure that the `www-dir` exists before we proceed.
    ;;
    ;; We want to ignore uploads where the dir is not already preexisting as
    ;; this indicates a given user either doesn't exist or hasn't created a www
    ;; dir. In either case, we shouldn't be attempting any uploads.
    (when (.exists www-dir)
      ;; However if `www-dir` does exists, but the `upload-dir` doesn't, we
      ;; should go ahead and create it.
      (when-not (.exists upload-dir)
        (.mkdir upload-dir))
      (doseq [file files]
        (io/copy (:tempfile file)
                 (io/file upload-dir (safe-filename file)))))
    (response "")))


(defroute mail-receive "/mail/receive" [:post] handle-mail)


(def create-app
  (->
    (partial wrap-routes 'simpleupload.app)
    wrap-multipart-params))
