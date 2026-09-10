(ns vocational.phase-test
  "The phase table as executable tests. The invariant this repo cannot
  regress on: `:certification/finalize`/`:graduation/finalize` must
  NEVER be members of any phase's `:auto` set."
  (:require [clojure.test :refer [deftest is testing]]
            [vocational.phase :as phase]))

(deftest certification-finalize-never-auto-at-any-phase
  (testing "structural invariant: no phase, now or in the future entries, auto-commits a real certification finalization"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :certification/finalize))
          (str "phase " n " must not auto-commit :certification/finalize")))))

(deftest graduation-finalize-never-auto-at-any-phase
  (testing "structural invariant: no phase, now or in the future entries, auto-commits a real graduation finalization"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :graduation/finalize))
          (str "phase " n " must not auto-commit :graduation/finalize")))))

(deftest academic-integrity-and-safety-screen-never-auto-at-any-phase
  (testing "screening carries no direct capital risk, but is still never auto-eligible, matching every sibling screening op in this fleet"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :academic-integrity/screen))
          (str "phase " n " must not auto-commit :academic-integrity/screen"))
      (is (not (contains? auto :safety/screen))
          (str "phase " n " must not auto-commit :safety/screen")))))

(deftest phase-0-is-fully-read-only
  (is (empty? (:writes (get phase/phases 0)))))

(deftest phase-3-auto-commits-only-no-capital-risk-ops
  (testing ":student/intake carries no direct capital risk -- auto-eligible; it is the ONLY auto-eligible op in this domain"
    (is (= #{:student/intake} (:auto (get phase/phases 3))))))

(deftest gate-hold-always-wins
  (is (= :hold (:disposition (phase/gate 3 {:op :student/intake} :hold)))))

(deftest gate-escalates-a-clean-non-auto-write
  (is (= :escalate (:disposition (phase/gate 3 {:op :certification/finalize} :commit))))
  (is (= :escalate (:disposition (phase/gate 3 {:op :graduation/finalize} :commit)))))

(deftest gate-holds-a-write-disabled-in-this-phase
  (is (= :hold (:disposition (phase/gate 0 {:op :student/intake} :commit)))))
