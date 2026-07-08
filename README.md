# cloud-itonami-isic-8522

Open Business Blueprint for **ISIC Rev.5 8522**: Technical and
vocational secondary education.

This repository publishes a technical-and-vocational-secondary-
education actor -- student intake, jurisdiction assessment, academic-
integrity screening, workplace-safety-training screening,
certification finalization and graduation finalization -- as an OSS
business that any qualified, licensed vocational-school operator can
fork, deploy, run, improve and sell, so a community or independent
educator never surrenders student data and ledgers to a closed SaaS.

Built on this workspace's
[`langgraph`](https://github.com/kotoba-lang/langgraph)
StateGraph runtime (portable `.cljc`, supervised superstep loop,
interrupts, Datomic/in-mem checkpoints) -- the same actor pattern as
every prior actor in this fleet
([`cloud-itonami-isic-6511`](https://github.com/cloud-itonami/cloud-itonami-isic-6511),
[`6512`](https://github.com/cloud-itonami/cloud-itonami-isic-6512),
[`6621`](https://github.com/cloud-itonami/cloud-itonami-isic-6621),
[`6622`](https://github.com/cloud-itonami/cloud-itonami-isic-6622),
[`6629`](https://github.com/cloud-itonami/cloud-itonami-isic-6629),
[`6520`](https://github.com/cloud-itonami/cloud-itonami-isic-6520),
[`6530`](https://github.com/cloud-itonami/cloud-itonami-isic-6530),
[`6820`](https://github.com/cloud-itonami/cloud-itonami-isic-6820),
[`6612`](https://github.com/cloud-itonami/cloud-itonami-isic-6612),
[`6492`](https://github.com/cloud-itonami/cloud-itonami-isic-6492),
[`6920`](https://github.com/cloud-itonami/cloud-itonami-isic-6920),
[`6611`](https://github.com/cloud-itonami/cloud-itonami-isic-6611),
[`7120`](https://github.com/cloud-itonami/cloud-itonami-isic-7120),
[`8620`](https://github.com/cloud-itonami/cloud-itonami-isic-8620),
[`8530`](https://github.com/cloud-itonami/cloud-itonami-isic-8530),
[`9200`](https://github.com/cloud-itonami/cloud-itonami-isic-9200),
[`7500`](https://github.com/cloud-itonami/cloud-itonami-isic-7500),
[`9603`](https://github.com/cloud-itonami/cloud-itonami-isic-9603),
[`9521`](https://github.com/cloud-itonami/cloud-itonami-isic-9521),
[`9321`](https://github.com/cloud-itonami/cloud-itonami-isic-9321),
[`8730`](https://github.com/cloud-itonami/cloud-itonami-isic-8730),
[`9102`](https://github.com/cloud-itonami/cloud-itonami-isic-9102),
[`9103`](https://github.com/cloud-itonami/cloud-itonami-isic-9103),
[`9602`](https://github.com/cloud-itonami/cloud-itonami-isic-9602),
[`9000`](https://github.com/cloud-itonami/cloud-itonami-isic-9000),
[`8890`](https://github.com/cloud-itonami/cloud-itonami-isic-8890),
[`8610`](https://github.com/cloud-itonami/cloud-itonami-isic-8610),
[`9311`](https://github.com/cloud-itonami/cloud-itonami-isic-9311),
[`8510`](https://github.com/cloud-itonami/cloud-itonami-isic-8510),
[`9412`](https://github.com/cloud-itonami/cloud-itonami-isic-9412),
[`6491`](https://github.com/cloud-itonami/cloud-itonami-isic-6491),
[`8720`](https://github.com/cloud-itonami/cloud-itonami-isic-8720),
[`8521`](https://github.com/cloud-itonami/cloud-itonami-isic-8521),
[`6619`](https://github.com/cloud-itonami/cloud-itonami-isic-6619),
[`3600`](https://github.com/cloud-itonami/cloud-itonami-isic-3600),
[`6190`](https://github.com/cloud-itonami/cloud-itonami-isic-6190),
[`3030`](https://github.com/cloud-itonami/cloud-itonami-isic-3030),
[`3830`](https://github.com/cloud-itonami/cloud-itonami-isic-3830),
[`7020`](https://github.com/cloud-itonami/cloud-itonami-isic-7020),
[`9420`](https://github.com/cloud-itonami/cloud-itonami-isic-9420),
[`9491`](https://github.com/cloud-itonami/cloud-itonami-isic-9491),
[`2610`](https://github.com/cloud-itonami/cloud-itonami-isic-2610),
[`3512`](https://github.com/cloud-itonami/cloud-itonami-isic-3512),
[`8810`](https://github.com/cloud-itonami/cloud-itonami-isic-8810),
[`8691`](https://github.com/cloud-itonami/cloud-itonami-isic-8691),
[`8569`](https://github.com/cloud-itonami/cloud-itonami-isic-8569),
[`6419`](https://github.com/cloud-itonami/cloud-itonami-isic-6419),
[`7310`](https://github.com/cloud-itonami/cloud-itonami-isic-7310),
[`7320`](https://github.com/cloud-itonami/cloud-itonami-isic-7320),
[`7210`](https://github.com/cloud-itonami/cloud-itonami-isic-7210),
[`7410`](https://github.com/cloud-itonami/cloud-itonami-isic-7410),
[`8710`](https://github.com/cloud-itonami/cloud-itonami-isic-8710),
[`8541`](https://github.com/cloud-itonami/cloud-itonami-isic-8541),
[`8690`](https://github.com/cloud-itonami/cloud-itonami-isic-8690),
[`9601`](https://github.com/cloud-itonami/cloud-itonami-isic-9601),
[`6420`](https://github.com/cloud-itonami/cloud-itonami-isic-6420),
[`7420`](https://github.com/cloud-itonami/cloud-itonami-isic-7420),
[`9609`](https://github.com/cloud-itonami/cloud-itonami-isic-9609),
[`8550`](https://github.com/cloud-itonami/cloud-itonami-isic-8550),
[`7010`](https://github.com/cloud-itonami/cloud-itonami-isic-7010),
[`8790`](https://github.com/cloud-itonami/cloud-itonami-isic-8790),
[`8542`](https://github.com/cloud-itonami/cloud-itonami-isic-8542),
[`6411`](https://github.com/cloud-itonami/cloud-itonami-isic-6411),
[`7490`](https://github.com/cloud-itonami/cloud-itonami-isic-7490),
[`9319`](https://github.com/cloud-itonami/cloud-itonami-isic-9319),
[`9329`](https://github.com/cloud-itonami/cloud-itonami-isic-9329),
[`9312`](https://github.com/cloud-itonami/cloud-itonami-isic-9312),
[`9492`](https://github.com/cloud-itonami/cloud-itonami-isic-9492),
[`9499`](https://github.com/cloud-itonami/cloud-itonami-isic-9499),
[`9512`](https://github.com/cloud-itonami/cloud-itonami-isic-9512),
[`9522`](https://github.com/cloud-itonami/cloud-itonami-isic-9522),
[`7220`](https://github.com/cloud-itonami/cloud-itonami-isic-7220),
[`9411`](https://github.com/cloud-itonami/cloud-itonami-isic-9411)) --
here it is **VocEdOps-LLM ⊣ Curriculum Safeguarding Governor** -- the
SAME governor keyword `school`/8510 (pre-primary/primary education)
and `secondary`/8521 (general secondary education) already share (the
blueprint authors' own template reuse across closely related
education sub-domains, the FIRST time two verticals in this fleet
shared an identical governor+advisor name pair, per `secondary.
governor`'s own docstring). This build is the THIRD sibling to share
this exact governor keyword, and the FIFTH confirmation of the fleet-
wide governor-name-reuse precedent overall (see `docs/adr/0001-
architecture.md` Decision 1). This actor's own advisor name,
VocEdOps-LLM, is DIFFERENT from `school`/8510's and `secondary`/8521's
shared "SchoolOps-LLM" -- a distinction already present in this
blueprint's own published README text, not introduced by this build.

> **Why an actor layer at all?** An LLM is great at drafting a
> student-intake summary, normalizing records, and checking whether a
> student's own completed-credits set actually contains every credit
> a jurisdiction requires for graduation -- but it has **no notion of
> which jurisdiction's vocational-education requirements are official,
> no license to finalize a real certification or a real graduation
> decision, and no way to know on its own whether a student has
> actually completed the workplace-safety training required before
> earning a trade certification**. Letting it finalize a certification
> or graduation directly invites fabricated jurisdiction citations, a
> certification finalized on insufficient attendance, a graduation
> finalized on incomplete credits, and a student sent into hands-on
> shop/practicum work (or certified as having completed it) without
> ever having cleared workplace-safety training -- and liability, and
> real physical-safety risk, for whoever runs it. This project seals
> the VocEdOps-LLM into a single node and wraps it with an independent
> **Curriculum Safeguarding Governor**, a human **approval workflow**,
> and an immutable **audit ledger**.

## Scope: what this actor does and does not do

This actor covers student intake through jurisdiction assessment,
academic-integrity screening, workplace-safety-training screening,
certification finalization and graduation finalization. It does
**not**, by itself, hold any license required to operate a vocational
school in a given jurisdiction, and it does not claim to. It also does
**not** model a full curriculum-design/pedagogical-assessment engine --
no subject-by-subject grading rubric, no trade-examination-board
integration, no full student-information-system feature set (see
`vocational.facts`'s own docstring for the honest simplification this
makes). Whoever deploys and operates a live instance (a licensed
vocational-school operator) supplies any jurisdiction-specific
license, the real pedagogical/workshop-instruction expertise and the
real school-information-system integrations, and bears that
jurisdiction's liability -- the software supplies the governed, spec-
cited, audited execution scaffold so that operator does not have to
build the compliance layer from scratch for every new market.

### Actuation

**Finalizing a real trade/skill certification or a real graduation is
never autonomous, at any phase, by construction.** Two independent
layers enforce this (`vocational.governor`'s `:actuation/finalize-
certification`/`:actuation/finalize-graduation` high-stakes gate and
`vocational.phase`'s phase table, which never puts either op in any
phase's `:auto` set) -- see `vocational.phase`'s docstring and
`test/vocational/phase_test.clj`'s `certification-finalize-never-
auto-at-any-phase`/`graduation-finalize-never-auto-at-any-phase`. The
actor may draft, check and recommend; a human licensed educator is
always the one who actually finalizes a certification or graduation.
Grounded directly in this blueprint's own README text ("No automated
proposal, by itself, can complete the following without governor
approval and audit evidence: finalizing a certification or graduation
decision") -- a genuine DUAL-actuation shape (two distinct real-world
acts on the same student), structurally the closest sibling to
`secondary`/8521's own `:actuation/finalize-grading`/`:actuation/
finalize-graduation` shape (the same general dual-actuation-education
archetype, applied to trade/skill certification rather than general
academic grading).

## The core contract

```
student intake + jurisdiction facts (vocational.facts, spec-cited)
        |
        v
   ┌───────────────────────┐   proposal      ┌───────────────────────┐
   │ VocEdOps-LLM          │ ─────────────▶ │ Curriculum                     │  (independent system)
   │ (sealed)              │  + citations    │ Safeguarding Governor:        │
   └───────────────────────┘                 │ spec-basis · evidence-       │
          │                 commit ◀┼ incomplete · attendance-         │
          │                         │ hours-insufficient (honest             │
    record + ledger        escalate ┼ reuse) · academic-integrity-             │
          │              (ALWAYS for│ flag-unresolved (honest reuse) ·          │
          │       :actuation/finalize│ workplace-safety-training-                │
          │       -certification/    │ unconfirmed (unconditional, NEW) ·         │
          │       :actuation/finalize│ graduation-requirements-                    │
          │       -graduation)       │ unsatisfied (honest reuse) ·                 │
          │                          │ already-certified · already-graduated        │
          ▼                          └───────────────────────┘
      human approval
```

**The VocEdOps-LLM never finalizes a certification or a graduation the
Curriculum Safeguarding Governor would reject, and never does so
without a human sign-off.** Hard violations (fabricated jurisdiction
requirements; unsupported evidence; insufficient attendance hours; an
unresolved academic-integrity flag; an unconfirmed workplace-safety-
training certification; unsatisfied graduation requirements; a double
certification or graduation finalization) force **hold** and *cannot*
be approved past; a clean certification/graduation proposal still
always routes to a human.

## Run

```bash
clojure -M:dev:run     # walk one clean dual-actuation lifecycle + six HARD-hold cases through the actor
clojure -M:dev:test    # governor contract · phase invariants · store parity · registry conformance · facts coverage
clojure -M:lint        # clj-kondo (errors fail; CI mirrors this)
```

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical domain work**. Here a workshop-safety monitoring
robot supports physical supervision during practical training, under
the actor, gated by the independent **Curriculum Safeguarding
Governor**. The governor never dispatches hardware itself;
`:high`/`:safety-critical` actions require human sign-off.

## Open business

This repository is not only source code. It is a public, forkable
business model:

| Layer | What is open |
|---|---|
| OSS core | Actor runtime, Curriculum Safeguarding Governor, certification-finalization + graduation-finalization draft records, audit ledger |
| Business blueprint | Customer, offer, pricing, unit economics, sales motion |
| Operator playbook | How to fork, license, deploy and support the service in a jurisdiction |
| Trust controls | Governance, security reporting, actuation invariant, audit requirements |

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md) to start this as an
open business on itonami.cloud, and
[`docs/adr/0001-architecture.md`](docs/adr/0001-architecture.md) for the
full architecture and decision record.

## Capability layer

This blueprint resolves its technology stack via
[`kotoba-lang/industry`](https://github.com/kotoba-lang/industry) (ISIC
`8522`). This vertical's academic/case records are practice-specific
rather than a shared cross-operator data contract, so `vocational.*`
runs on the generic robotics/identity/forms/dmn/bpmn/audit-ledger
stack only -- no bespoke domain capability lib to reference at all.

## Layout

| File | Role |
|---|---|
| `src/vocational/store.cljc` | **Store** protocol -- `MemStore` ‖ `DatomicStore` (`langchain.db`) + append-only audit ledger + separate certification-finalization/graduation-finalization history. No dynamically-filed sub-record -- both actuation ops act directly on a pre-seeded student, and the double-finalization guards check dedicated `:certification-finalized?`/`:graduation-finalized?` booleans rather than a `:status` value |
| `src/vocational/registry.cljc` | Certification-finalization + graduation-finalization draft records, plus `attendance-hours-insufficient?`/`graduation-requirements-unsatisfied?` -- HONEST, literal reuses of `secondary.registry`'s own checks for the SAME real-world concerns, not claimed as new |
| `src/vocational/facts.cljc` | Per-jurisdiction vocational-education licensing catalog AND a SEPARATE workplace-safety-training citation per jurisdiction (a genuine extension beyond `secondary.facts`'s own general-academic-curriculum-only catalog) with an official spec-basis citation per entry, honest coverage reporting |
| `src/vocational/vocedopsllm.cljc` | **VocEdOps-LLM** -- `mock-advisor` ‖ `llm-advisor`; intake/jurisdiction-assessment/academic-integrity-screening/workplace-safety-training-screening/certification-finalization/graduation-finalization proposals |
| `src/vocational/governor.cljc` | **Curriculum Safeguarding Governor** -- 8 checks: spec-basis · evidence-incomplete · attendance-hours-insufficient (honest reuse) · academic-integrity-flag-unresolved (honest reuse) · workplace-safety-training-unconfirmed (UNCONDITIONAL evaluation, GENUINELY NEW, the 65th grounding of this discipline) · graduation-requirements-unsatisfied (honest reuse) · already-certified guard · already-graduated guard, + 1 soft (confidence/actuation gate) |
| `src/vocational/phase.cljc` | **Phase 0→3** -- read-only → assisted intake → assisted assess → supervised (both certification and graduation finalization always human; student intake is the ONLY auto-eligible op, no direct capital risk) |
| `src/vocational/operation.cljc` | **OperationActor** -- langgraph StateGraph |
| `src/vocational/sim.cljc` | demo driver |
| `test/vocational/*_test.clj` | governor contract · phase invariants · store parity · registry conformance · facts coverage |

## Business-process coverage (honest)

This actor covers student intake through jurisdiction assessment,
academic-integrity screening, workplace-safety-training screening,
certification finalization and graduation finalization -- the core
governed lifecycle this blueprint's own `docs/business-model.md` names
as its Offer:

| Covered | Not covered (out of scope for this R0) |
|---|---|
| Student intake + per-jurisdiction evidence checklisting, HARD-gated on an official spec-basis citation (`:student/intake`/`:jurisdiction/assess`) | A full curriculum-design/pedagogical-assessment engine (subject-by-subject grading rubrics, trade-examination-board integration -- see `vocational.facts`'s docstring) |
| Academic-integrity screening + workplace-safety-training screening, each evaluated so the screening op itself can HARD-hold on its own finding (`:academic-integrity/screen`/`:safety/screen`) | Real school-information-system integration, billing/tuition workflows |
| Certification finalization, HARD-gated on full evidence, attendance-hours sufficiency and a confirmed workplace-safety-training certification, plus a double-finalization guard (`:certification/finalize`) | Ongoing workshop-instruction workflows themselves |
| Graduation finalization, HARD-gated on full evidence and graduation-requirement completeness, plus a double-finalization guard (`:graduation/finalize`) | |
| Immutable audit ledger for every intake/assessment/screening/certification/graduation decision | |

Extending coverage is additive: add the next gate (e.g. an
apprenticeship-placement-employer-vetting check) as its own governed
op with its own HARD checks and tests, following the SAME "an
independent governor re-verifies against the actor's own records
before any real-world act" pattern this repo's flagship ops already
establish.

## Jurisdiction coverage (honest)

`vocational.facts/coverage` reports how many requested jurisdictions
actually have an official spec-basis in `vocational.facts/catalog` --
currently 4 seeded (JPN, USA, GBR, DEU) out of ~194 jurisdictions
worldwide. This is a starting catalog to prove the governor contract
end-to-end, not a claim of global coverage. Adding a jurisdiction is
additive: one map entry in `vocational.facts/catalog`, citing a real
official source -- never fabricate a jurisdiction's requirements to
make coverage look bigger.

## Maturity

`:implemented` -- `VocEdOps-LLM` + `Curriculum Safeguarding Governor`
run as real, tested code (see `Run` above), promoted from the
originally-published `:blueprint`-tier scaffold, modeled closely on
`secondary`/8521's own architecture and the seventy-nine other prior
actors' architecture across this fleet. See
`docs/adr/0001-architecture.md` for the history and design.

## License

Code and implementation templates are AGPL-3.0-or-later.
