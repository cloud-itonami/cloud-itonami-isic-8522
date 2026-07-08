(ns vocational.governor
  "Curriculum Safeguarding Governor -- the independent compliance layer
  that earns the VocEdOps-LLM the right to commit. The LLM has no
  notion of jurisdictional vocational-education licensing law, whether
  a student's own attendance hours have actually reached the
  jurisdiction's own required minimum, whether the student's own
  completed-credits set actually contains every required credit,
  whether an academic-integrity concern or a workplace-safety-training
  gap has actually stayed unresolved, or when an act stops being a
  draft and becomes a real-world certification finalization or
  graduation finalization, so this MUST be a separate system able to
  *reject* a proposal and fall back to HOLD -- the technical-and-
  vocational-secondary-education analog of `cloud-itonami-isic-6512`'s
  CasualtyGovernor.

  This is the THIRD sibling in this fleet to share the EXACT SAME
  governor keyword, `:curriculum-safeguarding-governor` -- `school`/
  8510 (pre-primary/primary education) and `secondary`/8521 (general
  secondary education) already share it (the blueprint authors' own
  template reuse across closely related general-education sub-
  domains, per `secondary.governor`'s own docstring, the FIRST time
  two verticals shared an identical governor+advisor name pair). This
  build is the FIFTH confirmation of the fleet-wide governor-name-
  reuse precedent `commrepair`/9512's own ADR-0001 established (1st:
  commrepair/9512; 2nd: applianceshop/9522; 3rd: socialresearch/7220,
  first on a different family; 4th: bizassoc/9411, second on a
  different family) -- and, notably, the FIRST time the precedent
  applies within a family that ALREADY had two members before this
  build even started (unlike the other families, which started as a
  single already-implemented sibling). This actor's own advisor name,
  VocEdOps-LLM, is DIFFERENT from `school`/8510's and `secondary`/
  8521's shared 'SchoolOps-LLM' -- a distinction already present in
  this blueprint's own published README text, not introduced by this
  build.

  Eight checks, in priority order, ALL HARD violations: a human
  approver CANNOT override them (you don't get to approve your way
  past a fabricated jurisdiction spec-basis, incomplete evidence,
  insufficient attendance hours, an unresolved academic-integrity
  flag, an unconfirmed workplace-safety-training certification,
  unsatisfied graduation requirements, or a double certification/
  graduation finalization). The confidence/actuation gate is SOFT: it
  asks a human to look (low confidence / actuation), and the human may
  approve -- but see `vocational.phase`: for `:stake :actuation/
  finalize-certification`/`:actuation/finalize-graduation` (a real
  academic-record act) NO phase ever allows auto-commit either. Two
  independent layers agree that actuation is always a human call.

    1. Spec-basis                  -- did the jurisdiction proposal cite
                                       an OFFICIAL source (`vocational.
                                       facts`), or invent one?
    2. Evidence incomplete         -- for `:certification/finalize`/
                                       `:graduation/finalize`, has the
                                       jurisdiction actually been
                                       assessed with a full enrollment/
                                       curriculum-approval/attendance/
                                       workshop-safety-training/
                                       transcript evidence checklist on
                                       file?
    3. Attendance hours
       insufficient                  -- for `:certification/finalize`,
                                       INDEPENDENTLY recompute whether
                                       the student's own attendance
                                       hours reach the jurisdiction's
                                       own recorded minimum
                                       (`vocational.registry/
                                       attendance-hours-insufficient?`)
                                       -- an HONEST, LITERAL reuse of
                                       `secondary.registry`'s own
                                       SECOND-non-temporal-instance
                                       check, NOT claimed as new.
    4. Academic-integrity flag
       unresolved                     -- reported by THIS proposal
                                       itself (an `:academic-
                                       integrity/screen` that just
                                       found an unresolved concern), or
                                       already on file for the student
                                       (`:academic-integrity/screen`/
                                       `:certification/finalize`). An
                                       HONEST, LITERAL reuse of
                                       `secondary.governor`'s own
                                       TWENTY-THIRD-instance
                                       unconditional-evaluation
                                       grounding, NOT claimed as new.
    5. Workplace-safety-training
       unconfirmed                    -- for `:certification/finalize`,
                                       INDEPENDENTLY check whether the
                                       student's own `:workplace-
                                       safety-training-certified?` is
                                       true. A GENUINELY NEW concept
                                       (grep-verified absent fleet-wide
                                       -- zero hits for any governor
                                       check function named 'osha'/
                                       'workplace-safety'/
                                       'apprenticeship-safety'), the
                                       65th distinct application of
                                       the unconditional-evaluation
                                       discipline overall (most
                                       recently `bizassoc.governor/
                                       lobbying-registration-
                                       unconfirmed-violations` at 64th,
                                       the second CONDITIONAL variant).
                                       UNLIKE that check and
                                       `socialresearch`/7220's own
                                       (the first two conditional
                                       variants), this check is
                                       deliberately UNCONDITIONAL:
                                       every student in a technical/
                                       vocational secondary program is,
                                       by ISIC 8522's own definition,
                                       engaged in hands-on shop/
                                       practicum work with tools,
                                       machinery or industrial
                                       equipment, so the requirement
                                       applies to every student here,
                                       not merely a subset determined
                                       by some other ground-truth flag.
                                       Grounded in real workplace/
                                       workshop-safety-training law: US
                                       OSHA (29 C.F.R. Part 1910/1926),
                                       UK Health and Safety at Work
                                       etc. Act 1974 (HSE), Germany's
                                       DGUV Vorschriften under SGB VII,
                                       Japan's 労働安全衛生法.
    6. Graduation requirements
       unsatisfied                    -- for `:graduation/finalize`,
                                       INDEPENDENTLY recompute whether
                                       the student's own completed-
                                       credits set actually contains
                                       every required credit
                                       (`vocational.registry/
                                       graduation-requirements-
                                       unsatisfied?`) -- an HONEST,
                                       LITERAL reuse of `secondary.
                                       registry`'s own THIRD-instance
                                       set-containment/subset check,
                                       NOT claimed as new.
    7. Confidence floor / actuation
       gate                          -- LLM confidence below threshold,
                                       OR the op is `:certification/
                                       finalize`/`:graduation/finalize`
                                       (REAL academic-record acts) ->
                                       escalate.

  Two more guards, double-certification/double-graduation prevention,
  are enforced but NOT listed as numbered HARD checks above because
  they need no upstream comparison at all -- `already-certified-
  violations`/`already-graduated-violations` refuse to finalize a
  certification/graduation for the SAME student twice, off dedicated
  `:certification-finalized?`/`:graduation-finalized?` facts (never a
  `:status` value) -- the SAME 'check a dedicated boolean, not status'
  discipline every prior sibling governor's guards establish, informed
  by `cloud-itonami-isic-6492`'s status-lifecycle bug
  (ADR-2607071320)."
  (:require [vocational.facts :as facts]
            [vocational.registry :as registry]
            [vocational.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Finalizing a real trade/skill certification and finalizing a real
  graduation are the two real-world actuation events this actor
  performs -- a two-member set, matching every prior dual-actuation
  sibling's shape, grounded directly in this blueprint's own README
  ('No automated proposal, by itself, can complete the following
  without governor approval and audit evidence: finalizing a
  certification or graduation decision')."
  #{:actuation/finalize-certification :actuation/finalize-graduation})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "A `:jurisdiction/assess` (or `:certification/finalize`/`:graduation/
  finalize`) proposal with no spec-basis citation is a HARD violation
  -- never invent a jurisdiction's vocational-education licensing
  requirements."
  [{:keys [op]} proposal]
  (when (contains? #{:jurisdiction/assess :certification/finalize :graduation/finalize} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "公式spec-basisの引用が無い提案は法域要件として扱えない"}]))))

(defn- evidence-incomplete-violations
  "For `:certification/finalize`/`:graduation/finalize`, the
  jurisdiction's required enrollment/curriculum-approval/attendance/
  workshop-safety-training/transcript evidence must actually be
  satisfied -- do not trust the advisor's self-reported confidence
  alone."
  [{:keys [op subject]} st]
  (when (contains? #{:certification/finalize :graduation/finalize} op)
    (let [s (store/student st subject)
          assessment (store/assessment-of st subject)]
      (when-not (and assessment
                     (facts/required-evidence-satisfied?
                      (:jurisdiction s) (:checklist assessment)))
        [{:rule :evidence-incomplete
          :detail "法域の必要書類(生徒在籍記録/教育課程編成届/出席記録/実習安全訓練記録/成績・単位修得証明書等)が充足していない状態での提案"}]))))

(defn- attendance-hours-insufficient-violations
  "For `:certification/finalize`, INDEPENDENTLY recompute whether the
  student's own attendance hours reach the jurisdiction's own recorded
  minimum via `vocational.registry/attendance-hours-insufficient?` --
  an HONEST, LITERAL reuse of `secondary.registry`'s own check, NOT
  claimed as new."
  [{:keys [op subject]} st]
  (when (= op :certification/finalize)
    (let [s (store/student st subject)]
      (when (registry/attendance-hours-insufficient? s)
        [{:rule :attendance-hours-insufficient
          :detail (str subject " の出席時間(" (:attendance-hours-completed s)
                      ")が必要時間(" (:attendance-hours-required s) ")に満たない")}]))))

(defn- academic-integrity-flag-unresolved-violations
  "An unresolved academic-integrity flag -- reported by THIS proposal
  (e.g. an `:academic-integrity/screen` that itself just found one),
  or already on file in the store for the student (`:academic-
  integrity/screen`/`:certification/finalize`) -- is a HARD, un-
  overridable hold. An HONEST, LITERAL reuse of `secondary.governor`'s
  own check, NOT claimed as new."
  [{:keys [op subject]} proposal st]
  (let [hit-in-proposal? (= :unresolved (get-in proposal [:value :verdict]))
        student-id (when (contains? #{:academic-integrity/screen :certification/finalize} op) subject)
        hit-on-file? (and student-id (= :unresolved (:verdict (store/integrity-screen-of st student-id))))]
    (when (or hit-in-proposal? hit-on-file?)
      [{:rule :academic-integrity-flag-unresolved
        :detail "未解決の学業不正行為に関する懸念がある状態での認定確定提案は進められない"}])))

(defn- workplace-safety-training-unconfirmed-violations
  "For `:certification/finalize`, INDEPENDENTLY check whether the
  student's own `:workplace-safety-training-certified?` is true -- a
  genuinely new concept (see ns docstring), UNCONDITIONALLY applied
  (every student in this domain is engaged in hands-on shop/practicum
  work, unlike `socialresearch`'s/`bizassoc`'s own CONDITIONAL
  variants). Scoped to `:safety/screen` and `:certification/finalize`,
  so the screening op itself can HARD-hold on its own finding,
  matching every prior unconditional-evaluation check's scoping
  shape."
  [{:keys [op subject]} st]
  (when (contains? #{:safety/screen :certification/finalize} op)
    (let [s (store/student st subject)]
      (when-not (true? (:workplace-safety-training-certified? s))
        [{:rule :workplace-safety-training-unconfirmed
          :detail (str subject " は実習安全訓練の認定が未確認 -- 認定確定提案は進められない")}]))))

(defn- graduation-requirements-unsatisfied-violations
  "For `:graduation/finalize`, INDEPENDENTLY recompute whether the
  student's own completed-credits set actually contains every
  required credit via `vocational.registry/graduation-requirements-
  unsatisfied?` -- an HONEST, LITERAL reuse of `secondary.registry`'s
  own check, NOT claimed as new."
  [{:keys [op subject]} st]
  (when (= op :graduation/finalize)
    (let [s (store/student st subject)]
      (when (registry/graduation-requirements-unsatisfied? s)
        [{:rule :graduation-requirements-unsatisfied
          :detail (str subject " の修得単位(" (pr-str (:credits-earned s))
                      ")が卒業要件(" (pr-str (:credits-required s)) ")を充足していない")}]))))

(defn- already-certified-violations
  "For `:certification/finalize`, refuses to finalize a certification
  for the SAME student twice, off a dedicated `:certification-
  finalized?` fact (never a `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :certification/finalize)
    (when (store/student-already-certified? st subject)
      [{:rule :already-certified
        :detail (str subject " は既に認定確定済み")}])))

(defn- already-graduated-violations
  "For `:graduation/finalize`, refuses to finalize a graduation for
  the SAME student twice, off a dedicated `:graduation-finalized?`
  fact (never a `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :graduation/finalize)
    (when (store/student-already-graduated? st subject)
      [{:rule :already-graduated
        :detail (str subject " は既に卒業確定済み")}])))

(defn check
  "Censors a VocEdOps-LLM proposal against the governor rules. Returns
  {:ok? bool :violations [..] :confidence c :escalate? bool
  :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (evidence-incomplete-violations request st)
                           (attendance-hours-insufficient-violations request st)
                           (academic-integrity-flag-unresolved-violations request proposal st)
                           (workplace-safety-training-unconfirmed-violations request st)
                           (graduation-requirements-unsatisfied-violations request st)
                           (already-certified-violations request st)
                           (already-graduated-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
