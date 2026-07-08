(ns vocational.governor-contract-test
  "The governor contract as executable tests -- the technical-and-
  vocational-secondary-education analog of `cloud-itonami-isic-8521`'s
  `secondary.governor-contract-test`. The single invariant under test:

    VocEdOps-LLM never finalizes a certification or graduation the
    Curriculum Safeguarding Governor would reject, `:certification/
    finalize`/`:graduation/finalize` NEVER auto-commit at any phase,
    `:student/intake` (no direct capital risk) MAY auto-commit when
    clean, and every decision (commit OR hold) leaves exactly one
    ledger fact."
  (:require [clojure.test :refer [deftest is testing]]
            [langgraph.graph :as g]
            [vocational.store :as store]
            [vocational.operation :as op]))

(defn- fresh []
  (let [db (store/seed-db)]
    [db (op/build db)]))

(def operator {:actor-id "op-1" :actor-role :licensed-educator :phase 3})

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn- assess!
  "Walks `subject` through assess -> approve, leaving an assessment on
  file. Uses distinct thread-ids per call site by suffixing
  `tid-prefix`."
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-assess") {:op :jurisdiction/assess :subject subject} operator)
  (approve! actor (str tid-prefix "-assess")))

(deftest clean-intake-auto-commits
  (let [[db actor] (fresh)
        res (exec-op actor "t1"
                  {:op :student/intake :subject "student-1"
                   :patch {:id "student-1" :student-name "Sakura Tanaka"}} operator)]
    (is (= :commit (get-in res [:state :disposition])))
    (is (= "Sakura Tanaka" (:student-name (store/student db "student-1"))) "SSoT actually updated")
    (is (= 1 (count (store/ledger db))))))

(deftest jurisdiction-assess-always-needs-approval
  (testing "assess is never in any phase's :auto set -- always human approval, even when clean"
    (let [[db actor] (fresh)
          res (exec-op actor "t2" {:op :jurisdiction/assess :subject "student-1"} operator)]
      (is (= :interrupted (:status res)))
      (let [r2 (approve! actor "t2")]
        (is (= :commit (get-in r2 [:state :disposition])))
        (is (some? (store/assessment-of db "student-1")))))))

(deftest fabricated-jurisdiction-is-held
  (testing "a jurisdiction/assess proposal with no official spec-basis -> HOLD, never reaches a human"
    (let [[db actor] (fresh)
          res (exec-op actor "t3"
                    {:op :jurisdiction/assess :subject "student-1" :no-spec? true} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:no-spec-basis} (-> (store/ledger db) first :basis)))
      (is (nil? (store/assessment-of db "student-1")) "no assessment written"))))

(deftest certification-finalize-without-assessment-is-held
  (testing "certification/finalize before any jurisdiction assessment -> HOLD (evidence incomplete)"
    (let [[db actor] (fresh)
          res (exec-op actor "t4" {:op :certification/finalize :subject "student-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:evidence-incomplete} (-> (store/ledger db) first :basis))))))

(deftest attendance-hours-insufficient-is-held
  (testing "a student whose attendance hours fall short of the required minimum -> HOLD (honest reuse of secondary/8521's own check)"
    (let [[db actor] (fresh)
          _ (assess! actor "t5pre" "student-3")
          res (exec-op actor "t5" {:op :certification/finalize :subject "student-3"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:attendance-hours-insufficient} (-> (store/ledger db) last :basis)))
      (is (empty? (store/certification-history db))))))

(deftest academic-integrity-flag-is-held-and-unoverridable
  (testing "an unresolved academic-integrity flag on a student -> HOLD, and never reaches request-approval -- exercised via :academic-integrity/screen DIRECTLY, not via the actuation op against an unscreened student (honest reuse of secondary/8521's own check; see this actor's governor ns docstring / parksafety's ADR-2607071922 Decision 5 / eldercare's, museum's, conservation's, salon's, entertainment's, casework's, hospital's, facility's, school's, association's, leasing's, behavioral's, secondary's, card's, water's, telecom's, aerospace's, recovery's, consulting's, union's, congregation's, fab's, energy's, care's, navigator's, learning's, banking's, advertising's, polling's, research's, design's, nursing's, sports's, alliedhealth's, laundry's, holdco's, photo's, personalservice's, edsupport's, headoffice's, residential's, cultural's, reserve's, proserv's, sportsevent's, recreation's, sportsclub's, partyops's, memberorg's, commrepair's, applianceshop's, socialresearch's and bizassoc's ADR-0001s)"
    (let [[db actor] (fresh)
          res (exec-op actor "t6" {:op :academic-integrity/screen :subject "student-4"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:academic-integrity-flag-unresolved} (-> (store/ledger db) first :basis)))
      (is (nil? (store/integrity-screen-of db "student-4")) "no clearance written"))))

(deftest workplace-safety-training-unconfirmed-is-held-and-unoverridable
  (testing "a student whose workplace-safety-training certification is unconfirmed -> HOLD, and never reaches request-approval -- exercised via :safety/screen DIRECTLY, not via the actuation op against an unscreened student -- the genuinely NEW check this vertical adds, the 65th unconditional-evaluation-discipline grounding overall, and the FIRST fully-unconditional variant since bizassoc/9411's and socialresearch/7220's own conditional variants"
    (let [[db actor] (fresh)
          res (exec-op actor "t6b" {:op :safety/screen :subject "student-6"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:workplace-safety-training-unconfirmed} (-> (store/ledger db) first :basis)))
      (is (nil? (store/safety-screen-of db "student-6")) "no clearance written"))))

(deftest graduation-requirements-unsatisfied-is-held
  (testing "a student whose completed credits don't cover every required credit -> HOLD (honest reuse of secondary/8521's own check)"
    (let [[db actor] (fresh)
          _ (assess! actor "t7pre" "student-5")
          res (exec-op actor "t7" {:op :graduation/finalize :subject "student-5"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:graduation-requirements-unsatisfied} (-> (store/ledger db) last :basis)))
      (is (empty? (store/graduation-history db))))))

(deftest certification-finalize-always-escalates-then-human-decides
  (testing "a clean, fully-assessed, attendance-sufficient, safety-certified student still ALWAYS interrupts for human approval -- actuation/finalize-certification is never auto"
    (let [[db actor] (fresh)
          _ (assess! actor "t8pre" "student-1")
          r1 (exec-op actor "t8" {:op :certification/finalize :subject "student-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, certification record drafted"
        (let [r2 (approve! actor "t8")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:certification-finalized? (store/student db "student-1"))))
          (is (= 1 (count (store/certification-history db))) "one draft certification record"))))))

(deftest graduation-finalize-always-escalates-then-human-decides
  (testing "a clean, fully-assessed, requirements-satisfied student still ALWAYS interrupts for human approval -- actuation/finalize-graduation is never auto"
    (let [[db actor] (fresh)
          _ (assess! actor "t9pre" "student-1")
          r1 (exec-op actor "t9" {:op :graduation/finalize :subject "student-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, graduation record drafted"
        (let [r2 (approve! actor "t9")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:graduation-finalized? (store/student db "student-1"))))
          (is (= 1 (count (store/graduation-history db))) "one draft graduation record"))))))

(deftest certification-finalize-double-finalization-is-held
  (testing "finalizing the same student's certification twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (assess! actor "t10pre" "student-1")
          _ (exec-op actor "t10a" {:op :certification/finalize :subject "student-1"} operator)
          _ (approve! actor "t10a")
          res (exec-op actor "t10" {:op :certification/finalize :subject "student-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-certified} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/certification-history db))) "still only the one earlier certification"))))

(deftest graduation-finalize-double-finalization-is-held
  (testing "finalizing the same student's graduation twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (assess! actor "t11pre" "student-1")
          _ (exec-op actor "t11a" {:op :graduation/finalize :subject "student-1"} operator)
          _ (approve! actor "t11a")
          res (exec-op actor "t11" {:op :graduation/finalize :subject "student-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-graduated} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/graduation-history db))) "still only the one earlier graduation"))))

(deftest every-decision-leaves-one-ledger-fact
  (testing "write-only-through-ledger: N operations -> N ledger facts"
    (let [[db actor] (fresh)]
      (exec-op actor "a" {:op :student/intake :subject "student-1"
                          :patch {:id "student-1" :student-name "Sakura Tanaka"}} operator)
      (exec-op actor "b" {:op :jurisdiction/assess :subject "student-1" :no-spec? true} operator)
      (is (= 2 (count (store/ledger db)))
          "one commit + one hold, both recorded"))))
