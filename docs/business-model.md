# Business Model: Technical and vocational secondary education

## Classification

- Repository: `cloud-itonami-isic-8522`
- ISIC Rev.5: `8522`
- Activity: technical and vocational secondary education -- trade and technical-skills instruction at the secondary level
- Social impact: education access, data sovereignty, transparent audit

## Customer

- independent vocational schools
- trade-training cooperatives
- apprenticeship-linked programs

## Offer

- student enrollment intake
- curriculum/placement proposal
- certification/graduation proposal
- immutable audit ledger

## Revenue

- self-host setup: one-time implementation fee
- managed hosting: monthly subscription per school
- support: monthly retainer with SLA
- migration: import from an incumbent vocational-school system
- per-enrollment fee

## Trust Controls

- no certification or graduation decision is finalized without human sign-off
- a fabricated assessment forces a hold, not an override
- a trade/skill certification cannot be finalized without a confirmed
  workplace-safety-training certification on file -- unconfirmed,
  this is a hold, never an override
- every record path is auditable
- student data stays outside Git
- emergency manual override paths remain outside LLM control

## Curriculum Safeguarding Governor: decision rule

This vertical's governor shares its name (`:curriculum-safeguarding-
governor`) with `cloud-itonami-isic-8510`'s (pre-primary/primary
education) and `cloud-itonami-isic-8521`'s (general secondary
education) -- the blueprint authors' own template reuse across
closely related education sub-domains, not a naming error introduced
by this build. The genuinely distinguishing concern this vertical
adds is workplace-safety-training certification: technical and
vocational secondary education characteristically involves hands-on
practicum work with tools, machinery and industrial/workshop
equipment, a concern essentially absent from general secondary
education's own academic-curriculum focus. Unlike some checks this
fleet has recently added elsewhere, this one applies unconditionally
to every student here: hands-on shop/practicum work is the defining
nature of this vertical, not a per-student exception.
