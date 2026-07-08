(ns vocational.registry-test
  (:require [clojure.test :refer [deftest is]]
            [vocational.registry :as r]))

;; ----------------------------- attendance-hours-insufficient? -----------------------------

(deftest not-insufficient-when-meets-minimum
  (is (not (r/attendance-hours-insufficient? {:attendance-hours-completed 900 :attendance-hours-required 800})))
  (is (not (r/attendance-hours-insufficient? {:attendance-hours-completed 800 :attendance-hours-required 800}))))

(deftest insufficient-when-below-minimum
  (is (r/attendance-hours-insufficient? {:attendance-hours-completed 500 :attendance-hours-required 800})))

(deftest insufficient-is-false-on-missing-fields
  (is (not (r/attendance-hours-insufficient? {})))
  (is (not (r/attendance-hours-insufficient? {:attendance-hours-completed 500}))))

;; ----------------------------- graduation-requirements-unsatisfied? -----------------------------

(deftest not-unsatisfied-when-all-credits-earned
  (is (not (r/graduation-requirements-unsatisfied? {:credits-earned #{:math1 :science1} :credits-required #{:math1 :science1}})))
  (is (not (r/graduation-requirements-unsatisfied? {:credits-earned #{:math1 :science1 :extra} :credits-required #{:math1 :science1}}))))

(deftest unsatisfied-when-missing-a-required-credit
  (is (r/graduation-requirements-unsatisfied? {:credits-earned #{:math1} :credits-required #{:math1 :science1}})))

;; ----------------------------- register-certification-finalization -----------------------------

(deftest certification-is-a-draft-not-a-real-finalization
  (let [result (r/register-certification-finalization "student-1" "JPN" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest certification-assigns-certification-number
  (let [result (r/register-certification-finalization "student-1" "JPN" 7)]
    (is (= (get result "certification_number") "JPN-CRT-000007"))
    (is (= (get-in result ["record" "student_id"]) "student-1"))
    (is (= (get-in result ["record" "kind"]) "certification-finalization-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest certification-validation-rules
  (is (thrown? Exception (r/register-certification-finalization "" "JPN" 0)))
  (is (thrown? Exception (r/register-certification-finalization "student-1" "" 0)))
  (is (thrown? Exception (r/register-certification-finalization "student-1" "JPN" -1))))

;; ----------------------------- register-graduation-finalization -----------------------------

(deftest graduation-is-a-draft-not-a-real-finalization
  (let [result (r/register-graduation-finalization "student-1" "JPN" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest graduation-assigns-graduation-number
  (let [result (r/register-graduation-finalization "student-1" "JPN" 7)]
    (is (= (get result "graduation_number") "JPN-GRA-000007"))
    (is (= (get-in result ["record" "student_id"]) "student-1"))
    (is (= (get-in result ["record" "kind"]) "graduation-finalization-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest graduation-validation-rules
  (is (thrown? Exception (r/register-graduation-finalization "" "JPN" 0)))
  (is (thrown? Exception (r/register-graduation-finalization "student-1" "" 0)))
  (is (thrown? Exception (r/register-graduation-finalization "student-1" "JPN" -1))))

(deftest history-is-append-only
  (let [c1 (r/register-certification-finalization "student-1" "JPN" 0)
        hist (r/append [] c1)
        c2 (r/register-certification-finalization "student-2" "JPN" 1)
        hist2 (r/append hist c2)]
    (is (= 2 (count hist2)))
    (is (= "JPN-CRT-000000" (get-in hist2 [0 "record_id"])))
    (is (= "JPN-CRT-000001" (get-in hist2 [1 "record_id"])))))
