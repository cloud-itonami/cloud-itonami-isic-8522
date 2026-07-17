# Operator Quick Start

## Prerequisites

1. **Clojure**: Install the Clojure CLI (https://clojure.org/guides/install_clojure)
2. **This repository**: Fork and clone `cloud-itonami-isic-8522` to your machine
3. **Optional (monorepo context)**: If you're working inside the full cloud-itonami workspace, the dependencies resolve automatically; otherwise, update `deps.edn` to use git coordinates for `langgraph` and `langchain` instead of `:local/root` paths

## Run Tests

The test suite validates the Curriculum Safeguarding Governor contract, phase invariants, store parity, registry conformance, and jurisdiction facts coverage.

```bash
cd /path/to/cloud-itonami-isic-8522
clojure -M:dev:test
```

Expected output includes passing tests for:
- Governor decision rules (8 hard checks + 1 soft gate)
- Phase state invariants (phases 0→3, no autonomous certification/graduation finalization)
- Store protocol compliance (MemStore and DatomicStore parity)
- Registry completeness (certification-finalization and graduation-finalization records)
- Facts coverage (jurisdiction catalog with spec-basis citations)

## Run the Demo

Walk through a complete, clean student lifecycle and six hard-hold cases:

```bash
clojure -M:dev:run
```

This drives the `OperationActor` through:
1. Student intake
2. Jurisdiction assessment
3. Academic-integrity screening
4. Workplace-safety-training screening
5. Certification finalization proposal
6. Graduation finalization proposal
7. Six governor-hold scenarios (evidence incomplete, attendance hours insufficient, academic-integrity flag unresolved, workplace-safety-training unconfirmed, graduation requirements unsatisfied, double-finalization guards)

The demo logs every decision, governor hold, escalation, and audit record.

## Where the Governor Lives

The **Curriculum Safeguarding Governor** is the core independent decision engine:

**File**: `src/vocational/governor.cljc`

**Key decision gates**:
- `:spec-basis` — jurisdiction requirements are in the official catalog
- `:evidence-incomplete` — required documents/evidence are present
- `:attendance-hours-insufficient` — hours meet minimum thresholds (shared check with general secondary education)
- `:academic-integrity-flag-unresolved` — no unresolved integrity flags (shared check)
- `:workplace-safety-training-unconfirmed` — trade/skill certification requires confirmed workplace-safety-training certification (unique to this vertical)
- `:graduation-requirements-unsatisfied` — all required credits/competencies met (shared check)
- `:already-certified` — prevents double-finalization of certification
- `:already-graduated` — prevents double-finalization of graduation
- `:confidence` — soft gate for actuation (LLM confidence threshold for proposals)

The governor **never finalizes a certification or graduation without a human sign-off**, enforced at two layers:
1. Governor's `:actuation/finalize-certification` and `:actuation/finalize-graduation` gates (reject if hard checks fail)
2. Phase table (neither finalization op appears in any phase's `:auto` set)

See `test/vocational/phase_test.clj` for invariant tests.

## Demo Driver

**File**: `src/vocational/sim.cljc`

The simulation loads the actor, seeds a test student, and runs through the complete lifecycle, demonstrating:
- Intake proposal and record creation
- Jurisdiction assessment proposal
- Screening (academic-integrity and workplace-safety-training)
- Finalization proposals with governor holds and escalations
- Audit ledger entries for every decision

## Other Key Files

| File | Role |
|---|---|
| `src/vocational/store.cljc` | Append-only audit ledger + student record store (in-memory and Datomic) |
| `src/vocational/operation.cljc` | `OperationActor` — langgraph StateGraph runtime |
| `src/vocational/phase.cljc` | Phase 0→3 state machine and phase table |
| `src/vocational/vocedopsllm.cljc` | VocEdOps-LLM proposal engine (mock and real) |
| `src/vocational/registry.cljc` | Certification/graduation draft records and validation |
| `src/vocational/facts.cljc` | Jurisdiction vocational-education catalog + workplace-safety-training citations |

## Lint

Run static analysis with clj-kondo:

```bash
clojure -M:lint
```

Errors fail the lint step; warnings are reported. CI mirrors this check.

## Next: First Deployment

See `operator-guide.md` for:
1. License registration
2. Historical record import
3. Governor hold/escalation policy configuration
4. Production control setup
5. Audit export and dry-run validation

See `business-model.md` for revenue models and customer profiles.
