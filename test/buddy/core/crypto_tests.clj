;; Copyright 2014-2015 Andrey Antukh <niwi@niwi.be>
;;
;; Licensed under the Apache License, Version 2.0 (the "License")
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

(ns buddy.core.crypto-tests
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer :all]
            [buddy.core.codecs :as codecs :refer :all]
            [buddy.core.bytes :as bytes]
            [buddy.core.keys :refer :all]
            [buddy.core.nonce :as nonce]
            [buddy.core.hash :as hash]
            [buddy.core.crypto :as cr]
            [clojure.java.io :as io]))

(deftest buddy-core-crypto
  (let [key     (nonce/random-bytes 32)
        iv      (nonce/random-bytes 16)
        key     (hex->bytes "0000000000000000000000000000000000000000000000000000000000000000")
        iv16    (hex->bytes "00000000000000000000000000000000")
        iv8     (hex->bytes "0011001100110011")
        block16 (hex->bytes "000000000000000000000000000000AA")
        block3  (hex->bytes "121314")
        block6  (hex->bytes "221122112211")]

    (testing "Twofish in CRT mode."
      (let [engine    (cr/block-cipher :twofish :ctr)
            expected1 (into-array Byte/TYPE [87 -1 115 -99 77 -55 44 27 -41 -4 1 112 12 -56 33 -59])
            expected2 (into-array Byte/TYPE [35 -47 36 126 -1 76 -88 -53 -77 120 -33 17 -125 105 -126 -76])]

        ;; Encrypt
        (cr/initialize! engine {:iv iv16 :key key :op :encrypt})
        (let [result1 (cr/process-block! engine block16)
              result2 (cr/process-block! engine block16)]
          (is (bytes/bytes? result1))
          (is (bytes/bytes? result2))
          (is (bytes/equals? expected1 result1))
          (is (bytes/equals? expected2 result2)))

        ;; Decrypt
        (cr/initialize! engine {:iv iv16 :key key :op :decrypt})
        (let [result1 (cr/process-block! engine expected1)
              result2 (cr/process-block! engine expected2)]
          (is (bytes/equals? result1 block16))
          (is (bytes/equals? result2 block16)))))

    (testing "Aes in :cbc mode"
      (let [engine   (cr/block-cipher :aes :cbc)
            expected (into-array Byte/TYPE [-121 104 86 98 109 -110 53 104 119 -94
                                            -124 -105 92 39 -30 -30])]
        ;; Encrypt
        (cr/initialize! engine {:iv iv16 :key key :op :encrypt})
        (let [result (cr/process-block! engine block16)]
          (is (bytes/equals? result expected)))

        ;; Decrypt
        (cr/initialize! engine {:iv iv16 :key key :op :decrypt})
        (let [result (cr/process-block! engine expected)]
          (is (bytes/equals? result block16)))))

    (testing "ChaCha Streaming Cipher"
      (let [engine    (cr/stream-cipher :chacha)
            expected1 (into-array Byte/TYPE [14, 37, 45])
            expected2 (into-array Byte/TYPE [-5, 46, -80, -91, 19, -12])]
        (cr/initialize! engine {:iv iv8 :key key :op :encrypt})
        (let [result1 (cr/process-block! engine block3)
              result2 (cr/process-block! engine block6)]
          (is (bytes/equals? result1 expected1))
          (is (bytes/equals? result2 expected2)))

        (cr/initialize! engine {:iv iv8 :key key :op :decrypt})
        (let [result1 (cr/process-block! engine expected1)
              result2 (cr/process-block! engine expected2)]
          (is (bytes/equals? result1 block3))
          (is (bytes/equals? result2 block6)))))
))

;; (deftest buddy-high-level-crypto
;;   (testing "Split by blocksize"
;;     (let [input1 (codecs/str->bytes "aaaabbbb")
;;           input2 (codecs/str->bytes "aaaabbb")
;;           input3 (codecs/str->bytes "aaaabbbbc")
;;           result1 (cr/split-by-blocksize input1 4)
;;           result2 (cr/split-by-blocksize input2 4)
;;           result3 (cr/split-by-blocksize input3 4)]
;;       (is (= (count result1) 2))
;;       (is (= (count result2) 2))
;;       (is (= (count result3) 3))))

;;   ;; (testing "Encrypt/Decript using :aes"
;;   ;;   (let [key     (nonce/random-bytes 32)
;;   ;;         iv16    (hex->bytes "00000000000000000000000000000000")
;;   ;;         iv8     (hex->bytes "0011001100110011")
;;   ;;         block16 (hex->bytes "000000000000000000000000000000AA")
;;   ;;         block3  (hex->bytes "121314")
;;   ;;         block6  (hex->bytes "221122112211")]
;;   ;;     (println (vec (cr/encrypt block3 :key key :iv iv16)))
;;   ;;     (println (vec (cr/encrypt block3 :key key :iv iv16)))))
;; )

