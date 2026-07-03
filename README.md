# cloud-itonami-8522

Open Business Blueprint for **ISIC Rev.5 8522**: Technical and vocational secondary education.

This repository designs a forkable OSS business for technical and vocational secondary education -- trade and technical-skills instruction at the secondary level -- run by a qualified, licensed operator so a community or
independent educator never surrenders student data and ledgers to a
closed SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a workshop-safety monitoring robot supports physical supervision during practical training,
under an actor that proposes actions and an independent **Curriculum Safeguarding Governor**
that gates them. The governor never dispatches hardware itself;
`:high`/`:safety-critical` actions require human sign-off.

## Core Contract

```text
intake + identity + academic records
        |
        v
VocEdOps-LLM -> Curriculum Safeguarding Governor -> hold, proceed, or human approval
        |
        v
academic ledger + evidence record + audit
```

No automated proposal, by itself, can complete the following without governor
approval and audit evidence: finalizing a certification or graduation decision.

## Capability layer

This blueprint resolves its technology stack via
[`kotoba-lang/industry`](https://github.com/kotoba-lang/industry) (ISIC
`8522`). This vertical's academic/case records are practice-specific
rather than a shared cross-operator data contract, so it runs on the generic
identity/forms/dmn/bpmn/audit-ledger stack -- no bespoke domain capability lib.

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## Maturity

`:blueprint` -- this repository is the published business/operator design.
The governed actor implementation (`VocEdOps-LLM` + `Curriculum Safeguarding Governor` as
running code) is a follow-up, same as any other `:blueprint`-tier
`cloud-itonami-*` entry in `kotoba-lang/industry`'s registry.

## License

Code and implementation templates are AGPL-3.0-or-later.
