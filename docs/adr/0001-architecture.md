# ADR-0001: VocEdOps-LLM ⊣ Curriculum Safeguarding Governor architecture

## Status

Accepted. `cloud-itonami-isic-8522` promoted from `:blueprint` to
`:implemented` in the `kotoba-lang/industry` registry.

## Context

`cloud-itonami-isic-8522` publishes an OSS business blueprint for
technical and vocational secondary education: trade and technical-
skills instruction at the secondary level. Like every prior actor in
this fleet, the blueprint alone is not an implementation: this ADR
records the governed-actor architecture that promotes it to real,
tested code, following the same langgraph StateGraph + independent
Governor + Phase 0→3 rollout pattern established by `cloud-itonami-
isic-6511` (life insurance) and applied across seventy-nine prior
siblings, most recently `cloud-itonami-isic-9411` (business and
employers membership organizations).

This blueprint's own `:itonami.blueprint/governor` keyword,
`:curriculum-safeguarding-governor`, is IDENTICAL to `school`/8510's
(pre-primary/primary education) AND `secondary`/8521's (general
secondary education) -- `secondary.governor`'s own docstring already
documented that `school`/8510 and `secondary`/8521 share this name as
the blueprint authors' own template reuse across closely related
general-education sub-domains, the FIRST time two verticals in this
fleet shared an identical governor+advisor name pair. This build is
the THIRD sibling to share the exact same governor keyword, and the
FIFTH confirmation of the fleet-wide governor-name-reuse precedent
`commrepair`/9512's own ADR-0001 established (1st: commrepair/9512;
2nd: applianceshop/9522; 3rd: socialresearch/7220, first on a
different family; 4th: bizassoc/9411, second on a different family).
Notably, this is the FIRST time the precedent applies within a family
that already had TWO members before this build even started (every
other family started as a single already-implemented sibling).

## Decision

### Decision 1: governor-name reuse -- third sibling in an existing family, fifth confirmation overall

`school`/8510, `secondary`/8521 and `vocational`/8522 all perform
curriculum-safeguarding oversight of a school finalizing high-stakes
academic-record acts on a student's behalf. This build's own advisor
name, VocEdOps-LLM, is already DIFFERENT from `school`/8510's and
`secondary`/8521's shared "SchoolOps-LLM" in this blueprint's own
published README text -- a distinction present before this build
started, not introduced by it. Reusing the governor keyword is an
honest reflection of the shared curriculum-safeguarding archetype, the
same reasoning `commrepair`/9512's, `applianceshop`/9522's,
`socialresearch`/7220's and `bizassoc`/9411's own ADR-0001s applied to
their own governor-name families.

### Decision 2: dual-actuation shape, entity/op shape borrowed from `secondary`/8521

This blueprint's own README, business-model.md and operator-guide.md
consistently name TWO real-world acts: "finalizing a certification or
graduation decision." Matching `secondary`/8521's own dual-actuation
shape, `high-stakes` here is a two-member set, `#{:actuation/finalize-
certification :actuation/finalize-graduation}`. The primary entity is
a `student`, closely modeled on `secondary`/8521's own architecture,
with `:certification/finalize` replacing `secondary`/8521's own
`:grading/finalize` (the blueprint's own text uses "certification"
rather than "grading," reflecting the trade/skill-certification focus
of vocational education specifically).

### Decision 3: `attendance-hours-insufficient?` and `graduation-requirements-unsatisfied?` -- honest, literal reuses

`vocational.registry/attendance-hours-insufficient?` and
`vocational.registry/graduation-requirements-unsatisfied?` are
HONEST, LITERAL reuses of `secondary.registry`'s own checks (the
SECOND non-temporal MINIMUM-threshold-family instance and the THIRD
set-containment/subset-family instance, respectively) -- NOT claimed
as new. A vocational-school student's attendance and overall-
graduation-credit concerns are the SAME real-world concerns as a
general-secondary-school student's.

### Decision 4: `academic-integrity-flag-unresolved?` -- an honest, literal reuse

`vocational.governor/academic-integrity-flag-unresolved-violations` is
an HONEST, LITERAL reuse of `secondary.governor`'s own TWENTY-THIRD-
instance unconditional-evaluation grounding, NOT claimed as new.
Academic-integrity concerns apply identically to vocational and
general-secondary students alike.

### Decision 5: `workplace-safety-training-unconfirmed?` -- the 65th unconditional-evaluation grounding, a genuinely new concept, deliberately UNCONDITIONAL

Before writing this check, every prior sibling's governor namespace
across the entire fleet was grepped for any check function named
`osha`, `workplace-safety` or `apprenticeship-safety` -- zero hits,
confirming this is a genuinely new concept.
`workplace-safety-training-unconfirmed-violations` reuses the
unconditional-evaluation-screening DISCIPLINE (`casualty.governor/
sanctions-violations`'s original fix) for the 65th distinct
application overall (most recently `bizassoc.governor/lobbying-
registration-unconfirmed-violations` at 64th, the second CONDITIONAL
variant). UNLIKE that check and `socialresearch`/7220's own (the
first two conditional variants introduced immediately prior in this
fleet), this check is deliberately UNCONDITIONAL: every student in a
technical/vocational secondary program is, by ISIC 8522's own
definition, engaged in hands-on shop/practicum work with tools,
machinery or industrial equipment, so the requirement applies to
every student here, not merely a subset determined by some other
ground-truth flag (unlike `socialresearch`'s human-subjects-review,
where not every study involves human subjects, or `bizassoc`'s
lobbying-registration, where not every jurisdiction has a registration
regime). Grounded in real workplace/workshop-safety-training law: US
OSHA (29 C.F.R. Part 1910/1926), UK Health and Safety at Work etc. Act
1974 (HSE), Germany's DGUV Vorschriften under SGB VII, Japan's
労働安全衛生法. Gates `:safety/screen` and `:certification/finalize`.

### Decision 6: dedicated double-actuation-guard booleans

`:certification-finalized?`/`:graduation-finalized?` are dedicated
booleans on the `student` record, never a single `:status` value --
an honest, literal reuse of `secondary.governor`'s own guards, informed
by `cloud-itonami-isic-6492`'s real status-lifecycle bug
(ADR-2607071320).

### Decision 7: Store protocol, MemStore + DatomicStore parity

`vocational.store/Store` is implemented by both `MemStore` (atom-
backed, default for dev/tests/demo) and `DatomicStore` (`langchain.
db`-backed), proven to satisfy the same contract in
`test/vocational/store_contract_test.clj` -- the same seam every
sibling actor uses so swapping the SSoT backend is a configuration
change, not a rewrite.

### Decision 8: Phase 0→3 rollout

Phase 3's `:auto` set has exactly one member, `:student/intake` (no
capital risk). `:jurisdiction/assess`, `:academic-integrity/screen`
and `:safety/screen` are never auto-eligible at any phase (matching
every sibling's screening-op posture), and `:certification/finalize`/
`:graduation/finalize` are permanently excluded from every phase's
`:auto` set -- a structural fact, not a rollout milestone, enforced by
BOTH `vocational.phase` and `vocational.governor`'s `high-stakes` set
independently.

### Decision 9: no bespoke domain capability lib, and no `blueprint.edn` field-sync fixes needed

This blueprint's own `:itonami.blueprint/required-technologies` names
no domain-specific capability beyond the generic robotics/identity/
forms/dmn/bpmn/audit-ledger stack -- there was no capability-lib
decision to make at all. This repo's `blueprint.edn` already had the
correct `isic-` prefixed `:id` and correctly populated `:required-
technologies`/`:optional-technologies` matching the `kotoba-lang/
industry` registry's own entry for `"8522"` exactly -- only the
`:maturity` field itself needed adding.

### Decision 10: mock + LLM advisor pair

`vocational.vocedopsllm` provides `mock-advisor` (deterministic,
default everywhere -- the actor graph and governor contract run
offline) and `llm-advisor` (backed by `langchain.model/ChatModel`,
with a defensive EDN-proposal parser so a malformed LLM response
degrades to a safe low-confidence noop rather than ever auto-
finalizing a certification or graduation).

## Alternatives considered

- **A CONDITIONAL workplace-safety-training check** (applying only to
  students in specific trade tracks, e.g. welding/electrical vs.
  culinary/business). Rejected: ISIC 8522 itself defines this entire
  vertical as involving hands-on shop/practicum work -- unlike
  `socialresearch`'s or `bizassoc`'s own genuinely varying ground
  truths, there is no honest per-student exception to carve out here.
  An unconditional check is the more accurate model of this domain.
- **Renaming `:grading/finalize` to keep parity with `secondary`/
  8521's own op name.** Rejected: this blueprint's own published text
  consistently uses "certification," not "grading" -- the op name
  must match the blueprint's own actual text (`:certification/
  finalize`), not merely mirror the sibling's naming for its own sake.
- **Declining the build because this is the fleet's first "three-way"
  governor-name collision.** Rejected: `secondary.governor`'s own
  docstring already established that `school`/8510 and `secondary`/
  8521 sharing a name was the blueprint authors' own deliberate
  template reuse, not an accidental collision -- extending that
  reasoning to a third sibling is the same precedent this fleet has
  already applied four times elsewhere.

## Consequences

- Eighty-first actor in this fleet (80 implemented before this
  build).
- Confirms the fleet-wide governor-name-reuse precedent a fifth time,
  and demonstrates it generalizes even to a governor-name family that
  already had two members before a new sibling joined.
- Establishes a genuinely NEW unconditional-evaluation-screening
  concept (workplace-safety-training-unconfirmed?), grep-verified
  absent from every prior sibling before the claim was finalized, and
  the FIRST fully-unconditional variant since the two most recent
  conditional-variant additions.
- `MemStore` ‖ `DatomicStore` parity is proven by
  `test/vocational/store_contract_test.clj`, the same `:db-api`-driven
  swap pattern every sibling actor uses.
- 41 tests / 208 assertions pass; lint is clean; the demo
  (`clojure -M:dev:run`) walks one clean dual-actuation lifecycle plus
  six HARD-hold scenarios end-to-end.
- `blueprint.edn` required no field-sync fixes this time (already
  correct) -- only the `:maturity` flip itself.
