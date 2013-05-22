(ns simpleupload.app
  (:require [clasp.clasp :refer [defroute wrap-routes]]
            [clojure.data.codec.base64 :as b64]
            [clojure.java.io :as io]
            [clojure.string :refer [split]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.util.response :refer [response]]))


(def KEY "foo")
(def WWW-DIR-FMT "/Users/%s/Desktop")
(def UPLOAD-DIR "uploads")


(defn- xor-cipher
  [p k]
  (map #(char (bit-xor (int %1) (int %2))) p (cycle k)))


(defn- encode-ciphertext
  [c]
  (String. (b64/encode (.getBytes (apply str c)))))


(defn- decode-ciphertext
  [c k]
  (xor-cipher (map char (b64/decode (.getBytes c))) k))


(defn- decode-address
  [a & {:keys [k] :or {k KEY}}]
  (let [c ((split a #"\@") 0)]
    (apply str (decode-ciphertext c k))))


(defn- format-filename
  [file]
  (let [filename (:filename file)]
    filename))


(defn- handle-mail
  [req]
  (let [user (decode-address (get-in req [:multipart-params "recipient"]))
        www-dir (io/file (format WWW-DIR-FMT user))
        upload-dir (io/file www-dir "uploads")
        files (filter #(not (nil? (:tempfile %)))
                      (vals (:multipart-params req)))]
    (when (.exists www-dir)  ;; make sure the dir exists
      (when-not (.exists upload-dir)
        (.mkdir upload-dir))  ;; mkdir if not exists
      (doseq [file files]
        (io/copy (:tempfile file)
                 (io/file upload-dir (format-filename file)))))
  (response "")))


(defroute mail-receive "/mail/receive" [:post]
  handle-mail)


(def create-app
  (->
    (partial wrap-routes 'simpleupload.app)
    wrap-multipart-params))
