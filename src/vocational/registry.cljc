(ns vocational.registry
  "Pure-function certification-finalization + graduation-finalization
  record construction -- an append-only vocational-school book-of-
  record draft, closely modeled on `cloud-itonami-isic-8521`'s
  `secondary.registry`.

  Like every sibling actor's registry, there is no single
  international check-digit standard for a certification-finalization
  or graduation-finalization reference number -- every school/
  jurisdiction assigns its own reference format. This namespace does
  NOT invent one; it builds a jurisdiction-scoped sequence number and
  validates the record's required fields, the same honest, non-
  fabricating discipline `vocational.facts` uses.

  `attendance-hours-insufficient?` and `graduation-requirements-
  unsatisfied?` are HONEST, LITERAL reuses of `secondary.registry`'s
  own checks (the SECOND non-temporal MINIMUM-threshold-family
  instance and the THIRD set-containment/subset-family instance,
  respectively) -- NOT claimed as new. A vocational-school student's
  attendance and overall-graduation-credit concerns are the SAME real-
  world concerns as a general-secondary-school student's, unrelated to
  the trade-certification-specific workplace-safety concern this
  vertical newly adds (see `vocational.governor`'s docstring).

  This namespace is pure data + pure functions -- no I/O, no network
  call to any real school-information system. It builds the RECORD a
  vocational school would keep, not the act of finalizing the
  certification or graduation decision itself (that is `vocational.
  operation`'s `:certification/finalize`/`:graduation/finalize`,
  always human-gated -- see README `Actuation`)."
  (:require [clojure.set :as set]
            [clojure.string :as str]))

(defn- unsigned-certificate
  "Every certificate this actor produces is UNSIGNED -- signature is the
  school's own act, not this actor's. See README `Actuation`."
  [kind subject record-id]
  {"@context" ["https://www.w3.org/ns/credentials/v2"]
   "type" ["VerifiableCredential" kind]
   "credentialSubject" {"id" subject "record" record-id}
   "proof" nil
   "issued_by_registry" false
   "status" "draft-unsigned"})

(defn- zero-pad [n w]
  (let [s (str n)]
    (str (apply str (repeat (max 0 (- w (count s))) "0")) s)))

(defn attendance-hours-insufficient?
  "Does `student`'s own `:attendance-hours-completed` fall short of the
  jurisdiction's own recorded `:attendance-hours-required` minimum? An
  honest, literal reuse of `secondary.registry`'s own shape -- see ns
  docstring."
  [{:keys [attendance-hours-completed attendance-hours-required]}]
  (and (number? attendance-hours-completed) (number? attendance-hours-required)
       (< attendance-hours-completed attendance-hours-required)))

(defn graduation-requirements-unsatisfied?
  "Does `student`'s own `:credits-earned` set fail to contain EVERY
  credit in its own `:credits-required` set? An honest, literal reuse
  of `secondary.registry`'s own shape -- see ns docstring."
  [{:keys [credits-earned credits-required]}]
  (not (set/subset? (set credits-required) (set credits-earned))))

(defn register-certification-finalization
  "Validate + construct the CERTIFICATION-FINALIZATION registration
  DRAFT -- the vocational school's own legal act of finalizing a real
  student's trade/skill certification. Pure function -- does not
  touch any real school-information system; it builds the RECORD a
  school would keep. `vocational.governor` independently re-verifies
  the student's own attendance sufficiency, unresolved academic-
  integrity status and workplace-safety-training certification, and
  blocks a double-finalization of the same student's certification,
  before this is ever allowed to commit."
  [student-id jurisdiction sequence]
  (when-not (and student-id (not= student-id ""))
    (throw (ex-info "certification-finalization: student_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "certification-finalization: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "certification-finalization: sequence must be >= 0" {})))
  (let [certification-number (str (str/upper-case jurisdiction) "-CRT-" (zero-pad sequence 6))
        record {"record_id" certification-number
                "kind" "certification-finalization-draft"
                "student_id" student-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "certification_number" certification-number
     "certificate" (unsigned-certificate "CertificationFinalization" certification-number certification-number)}))

(defn register-graduation-finalization
  "Validate + construct the GRADUATION-FINALIZATION registration
  DRAFT -- the vocational school's own legal act of finalizing a real
  student's graduation. Pure function -- does not touch any real
  school-information system; it builds the RECORD a school would
  keep. `vocational.governor` independently re-verifies the student's
  own graduation-requirement completeness, and blocks a double-
  finalization of the same student's graduation, before this is ever
  allowed to commit."
  [student-id jurisdiction sequence]
  (when-not (and student-id (not= student-id ""))
    (throw (ex-info "graduation-finalization: student_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "graduation-finalization: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "graduation-finalization: sequence must be >= 0" {})))
  (let [graduation-number (str (str/upper-case jurisdiction) "-GRA-" (zero-pad sequence 6))
        record {"record_id" graduation-number
                "kind" "graduation-finalization-draft"
                "student_id" student-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "graduation_number" graduation-number
     "certificate" (unsigned-certificate "GraduationFinalization" graduation-number graduation-number)}))

(defn append [history result]
  (conj (vec history) (get result "record")))
