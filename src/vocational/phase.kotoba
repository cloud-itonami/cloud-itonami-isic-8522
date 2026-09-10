(ns vocational.phase
  "Phase 0->3 staged rollout -- the technical-and-vocational-secondary-
  education analog of `cloud-itonami-isic-8521`'s `secondary.phase`.

    Phase 0  read-only        -- no writes, still governor-gated.
    Phase 1  assisted-intake  -- student intake allowed, every write
                                 needs human approval.
    Phase 2  assisted-assess  -- adds jurisdiction assessment +
                                 academic-integrity screening +
                                 workplace-safety-training screening
                                 writes, still approval.
    Phase 3  supervised auto  -- governor-clean, high-confidence
                                 `:student/intake` (no capital risk
                                 yet) may auto-commit. `:certification/
                                 finalize`/`:graduation/finalize` NEVER
                                 auto-commit, at any phase.

  `:certification/finalize`/`:graduation/finalize` are deliberately
  ABSENT from every phase's `:auto` set, including phase 3 -- a
  permanent structural fact, not a rollout milestone still to come.
  Finalizing a real trade/skill certification and finalizing a real
  graduation are the two real-world legal acts this actor performs;
  both are always a human licensed educator's call. `vocational.
  governor`'s `:actuation/finalize-certification`/`:actuation/
  finalize-graduation` high-stakes gate enforces the same invariant
  independently -- two layers, not one, agree on this.
  `:academic-integrity/screen`/`:safety/screen` are likewise never
  auto-eligible, at any phase -- the same posture every sibling's
  screening op has. Phase 3's `:auto` set here has only ONE member
  (`:student/intake`) -- this domain has no separate no-capital-risk
  'file' lifecycle distinct from the student record itself.")

(def read-ops  #{})
(def write-ops #{:student/intake :jurisdiction/assess :academic-integrity/screen :safety/screen
                 :certification/finalize :graduation/finalize})

;; NOTE the invariant: `:certification/finalize`/`:graduation/
;; finalize` are members of `write-ops` (governor-gated like any
;; write) but are NEVER members of any phase's `:auto` set below. Do
;; not add them there.
(def phases
  "phase -> {:label .. :writes <ops allowed to write> :auto <ops allowed to
  auto-commit when governor-clean>}."
  {0 {:label "read-only"       :writes #{}                                                                             :auto #{}}
   1 {:label "assisted-intake" :writes #{:student/intake}                                                              :auto #{}}
   2 {:label "assisted-assess" :writes #{:student/intake :jurisdiction/assess :academic-integrity/screen :safety/screen} :auto #{}}
   3 {:label "supervised-auto" :writes write-ops
      :auto #{:student/intake}}})

(def default-phase 3)

(defn gate
  "Adjust a governor disposition for the rollout phase. Returns
  {:disposition kw :reason kw|nil}.

  - a governor HOLD always stays HOLD (compliance wins).
  - a write op not yet enabled in this phase -> HOLD (:phase-disabled).
  - a write op enabled but not auto-eligible -> ESCALATE (:phase-approval),
    even if the governor was clean.
  - `:certification/finalize`/`:graduation/finalize` are never auto-
    eligible at any phase, so they always escalate once the governor
    clears them (or hold if the governor doesn't)."
  [phase {:keys [op]} governor-disposition]
  (let [{:keys [writes auto]} (get phases phase (get phases default-phase))]
    (cond
      (= :hold governor-disposition)       {:disposition :hold :reason nil}
      (contains? read-ops op)              {:disposition governor-disposition :reason nil}
      (not (contains? writes op))          {:disposition :hold :reason :phase-disabled}
      (and (= :commit governor-disposition)
           (not (contains? auto op)))      {:disposition :escalate :reason :phase-approval}
      :else                                {:disposition governor-disposition :reason nil})))

(defn verdict->disposition
  "Map a Curriculum Safeguarding Governor verdict to a base disposition
  before the phase gate."
  [verdict]
  (cond (:hard? verdict) :hold
        (:escalate? verdict) :escalate
        :else :commit))
