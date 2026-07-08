(ns vocational.sim
  "Demo driver -- `clojure -M:dev:run`. Walks a clean student through
  intake -> jurisdiction assessment -> academic-integrity screening ->
  workplace-safety-training screening -> certification-finalization
  proposal (always escalates) -> human approval -> commit, then through
  graduation-finalization proposal (always escalates) -> human approval
  -> commit, then shows six HARD holds (a jurisdiction with no spec-
  basis, insufficient attendance hours, an unresolved academic-
  integrity flag screened directly via `:academic-integrity/screen`,
  an unconfirmed workplace-safety-training certification screened
  directly via `:safety/screen` [never via an actuation op against an
  unscreened student -- see this actor's own governor ns docstring /
  the lesson `parksafety`'s ADR-2607071922 Decision 5, `eldercare`'s,
  `museum`'s, `conservation`'s, `salon`'s, `entertainment`'s,
  `casework`'s, `hospital`'s, `facility`'s, `school`'s, `association`'s,
  `leasing`'s, `behavioral`'s, `secondary`'s, `card`'s, `water`'s,
  `telecom`'s, `aerospace`'s, `recovery`'s, `consulting`'s, `union`'s,
  `congregation`'s, `fab`'s, `energy`'s, `care`'s, `navigator`'s,
  `learning`'s, `banking`'s, `advertising`'s, `polling`'s, `research`'s,
  `design`'s, `nursing`'s, `sports`'s, `alliedhealth`'s, `laundry`'s,
  `holdco`'s, `photo`'s, `personalservice`'s, `edsupport`'s,
  `headoffice`'s, `residential`'s, `cultural`'s, `reserve`'s,
  `proserv`'s, `sportsevent`'s, `recreation`'s, `sportsclub`'s,
  `partyops`'s, `memberorg`'s, `commrepair`'s, `applianceshop`'s,
  `socialresearch`'s and `bizassoc`'s ADR-0001s already recorded],
  unsatisfied graduation requirements, and a double certification/
  graduation finalization of an already-processed student) that never
  reach a human at all, and prints the audit ledger + the draft
  certification-finalization and graduation-finalization records."
  (:require [langgraph.graph :as g]
            [vocational.store :as store]
            [vocational.operation :as op]))

(def operator {:actor-id "op-1" :actor-role :licensed-educator :phase 3})

(defn- exec! [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn -main [& _]
  (let [db (store/seed-db)
        actor (op/build db)]
    (println "== student/intake student-1 (JPN, clean; sufficient attendance, safety-training certified, all credits earned) ==")
    (println (exec! actor "t1" {:op :student/intake :subject "student-1"
                                :patch {:id "student-1" :student-name "Sakura Tanaka"}} operator))

    (println "== jurisdiction/assess student-1 (escalates -- human approves) ==")
    (println (exec! actor "t2" {:op :jurisdiction/assess :subject "student-1"} operator))
    (println (approve! actor "t2"))

    (println "== academic-integrity/screen student-1 (clean; escalates -- human approves) ==")
    (println (exec! actor "t3" {:op :academic-integrity/screen :subject "student-1"} operator))
    (println (approve! actor "t3"))

    (println "== safety/screen student-1 (clean; escalates -- human approves) ==")
    (println (exec! actor "t3b" {:op :safety/screen :subject "student-1"} operator))
    (println (approve! actor "t3b"))

    (println "== certification/finalize student-1 (always escalates -- actuation/finalize-certification) ==")
    (let [r (exec! actor "t4" {:op :certification/finalize :subject "student-1"} operator)]
      (println r)
      (println "-- human educator approves --")
      (println (approve! actor "t4")))

    (println "== graduation/finalize student-1 (always escalates -- actuation/finalize-graduation) ==")
    (let [r (exec! actor "t5" {:op :graduation/finalize :subject "student-1"} operator)]
      (println r)
      (println "-- human educator approves --")
      (println (approve! actor "t5")))

    (println "== jurisdiction/assess student-2 (no spec-basis -> HARD hold) ==")
    (println (exec! actor "t6" {:op :jurisdiction/assess :subject "student-2" :no-spec? true} operator))

    (println "== jurisdiction/assess student-3 (escalates -- human approves; sets up the attendance test) ==")
    (println (exec! actor "t7" {:op :jurisdiction/assess :subject "student-3"} operator))
    (println (approve! actor "t7"))

    (println "== certification/finalize student-3 (500/800 attendance hours -> HARD hold) ==")
    (println (exec! actor "t8" {:op :certification/finalize :subject "student-3"} operator))

    (println "== academic-integrity/screen student-4 (unresolved -> HARD hold, never reaches a human) ==")
    (println (exec! actor "t9" {:op :academic-integrity/screen :subject "student-4"} operator))

    (println "== jurisdiction/assess student-5 (escalates -- human approves; sets up the graduation-requirements test) ==")
    (println (exec! actor "t10" {:op :jurisdiction/assess :subject "student-5"} operator))
    (println (approve! actor "t10"))

    (println "== graduation/finalize student-5 (missing history1/english1 credits -> HARD hold) ==")
    (println (exec! actor "t11" {:op :graduation/finalize :subject "student-5"} operator))

    (println "== safety/screen student-6 (workplace-safety-training unconfirmed -> HARD hold, never reaches a human) ==")
    (println (exec! actor "t12" {:op :safety/screen :subject "student-6"} operator))

    (println "== certification/finalize student-1 AGAIN (double-finalization -> HARD hold) ==")
    (println (exec! actor "t13" {:op :certification/finalize :subject "student-1"} operator))

    (println "== graduation/finalize student-1 AGAIN (double-finalization -> HARD hold) ==")
    (println (exec! actor "t14" {:op :graduation/finalize :subject "student-1"} operator))

    (println "== audit ledger ==")
    (doseq [f (store/ledger db)] (println f))

    (println "== draft certification-finalization records ==")
    (doseq [r (store/certification-history db)] (println r))

    (println "== draft graduation-finalization records ==")
    (doseq [r (store/graduation-history db)] (println r))))
