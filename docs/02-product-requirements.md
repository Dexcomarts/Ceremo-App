# CEREMO Product Requirements Document

Version 3.4 · 5 November 2026 · Product review draft

## 1. Executive summary

CEREMO is a ceremony and event service marketplace for Ghana, launching in Greater Accra across all occasion types and festivals. It helps organisers find relevant providers, compare useful information and make contact free of charge. Providers prepare listings free, receive one trial when an approved listing is first published and can subscribe to continue active discovery. Customers and providers may discuss, contract and pay for services outside CEREMO. The business depends on customers finding suitable services and providers receiving enough value to renew. The first release must test both outcomes before geographic expansion.

**Current position:** backend development is underway; marketplace demand and subscription economics remain unvalidated. No launch date, budget, subscription price or trial duration has been approved. This document defines expected behaviour, rather than asserting that these capabilities have shipped.

## 2. Customer problem

### 2.1 The job to be done

When organising an occasion, a customer needs to identify providers who offer the required service in the right area, assess their suitability and establish contact with enough information for a useful discussion. A decision may involve relatives, a committee or a business, even when one person does the searching.

A hypothetical organiser seeking catering for 120 guests, photography and decoration needs three separate service decisions. A provider's attractive portfolio alone does not establish coverage, the unit behind a price, the scope of a check or availability for the occasion.

The working problem hypothesis is that collecting and comparing these facts across referrals, social profiles and conversations creates avoidable effort and uncertainty. Its frequency, severity and effect on actual choices still require Greater Accra field research.

### 2.2 Priority users

| User | Situation and desired outcome | Product implication |
| --- | --- | --- |
| Family organiser | Sourcing several services for a wedding, funeral, naming ceremony, birthday or other family occasion. | Browse without commitment; compare service details; save candidates and reuse an optional brief. |
| Community, faith or festival organiser | Sourcing services for a programme involving several decision-makers and local requirements. | Support the occasion and service area without assuming authority, permits or unrestricted availability. |
| Business organiser | Comparing service scope, price basis and evidence before contacting a provider. | Make business information clear and allow direct negotiation. |
| Provider owner or authorised member | Seeking relevant demand while managing limited time and acquisition costs. | Maintain an accurate listing, respond to suitable requests and assess the subscription using attributable evidence. |
| CEREMO operations | Assessing listings, handling abuse, supporting customers and resolving subscription cases. | Provide restricted case access, recorded decisions and escalation paths. |

These are working segments, not interview-validated personas. The organiser, decision-maker and payer may be different people; CEREMO must not infer relationships or sensitive traits from an occasion choice.

## 3. Product strategy and evidence

### 3.1 Why customers and providers would choose CEREMO

The proposed customer advantage is **relevant comparison across several services**, with consistent coverage, price units, portfolios, check descriptions and review information. CEREMO must demonstrate that this helps customers reach suitable candidates more effectively than their usual search.

The provider offer is an approved presence in relevant discovery, a manageable enquiry process and evidence of useful demand. A subscription purchases defined platform benefits. It does not purchase customer trust or a promised number of jobs.

The Knot Worldwide provides a relevant precedent: its published terms describe vendor subscription services and disclaim guaranteed enquiry or booking volumes. This supports the commercial comparison, not Ghanaian willingness to pay or CEREMO's pricing. [U.S. terms of purchase, revised 9 February 2026](https://www.theknotww.com/terms-of-purchase).

### 3.2 Alternatives and deliberate choices

| Alternative or decision | CEREMO's position | Consequence to test |
| --- | --- | --- |
| Social media, referrals and direct conversations | Retain customers' preferred contact channels; improve the information available before contact. | If comparison adds little value, registration and a larger catalogue will not solve the problem. |
| Repeat contact with a known provider | Permit it without penalty. Help the customer find other services and return for later occasions. | Retention must come from continuing usefulness; repeated use of the same contact is not necessarily platform activity. |
| Transaction commissions and compulsory checkout | Use provider subscriptions only; permit outside service agreements and payments. | CEREMO has limited visibility into hires and cannot present contact activity as verified sales. |
| Paid position as a substitute for relevance | Require service/area fit and current publication eligibility first. Sponsorship is a later, labelled enhancement. | A larger subscriber base must not make customer results less useful. |
| A mandatory registration survey | Offer an optional, editable guide alongside ordinary browsing. | Observe whether the guide improves selection sufficiently to justify the extra effort. |
| Nationwide or wedding-only launch | Launch in Greater Accra with all occasion families supported. | Measure actual supply by category and locality; catalogue support does not imply universal coverage. |

[Ghana research and competitor comparisons](01-ghana-research.md) provide context and precedents. No representative demand study, validated price or reliable renewal benchmark has been established. The proposed advantage and marketplace growth cycle remain hypotheses.

## 4. First-release experience and scope

### 4.1 Customer and provider experience

1. A visitor selects a service and area and compares limited eligible-provider summaries without registering. Summaries exclude contact destinations and private tenant data.
2. Opening a full provider profile, detailed reviews or contact requires a free, authenticated ACTIVE account. After registration/verification, return to the selected provider. Saved providers, optional plans and structured enquiries use the same account.
3. A customer may skip the guide, leave an unknown budget/date unset, or approve a brief for one selected provider service.
4. The parties discuss suitability, availability, terms and payment directly. An enquiry acknowledgement records a response; it does not confirm a booking.
5. After the experience, the customer may report an outcome, submit a genuine-experience review, raise a concern or return for another service.
6. A provider prepares a listing, completes assessment and starts one trial on first approved publication. The provider reviews useful demand and chooses whether to subscribe or renew.

The first value milestone is reaching a suitable candidate with a usable contact route. Account creation, guide completion and app opens are intermediate actions.

### 4.2 Must-have capabilities

All capabilities below remain required for the MVP. The order reflects dependencies, rather than permission to omit trust, billing or support work.

| Capability | Required product outcome | Existing requirement references |
| --- | --- | --- |
| Access and accounts | Catalogue/summary discovery stays free. Full profiles, detailed reviews and contacts require a free ACTIVE account; private tenant records require accepted membership and scoped permission. | AC-001, AC-002, AC-003 |
| Provider supply | Free listing preparation; approved publication under a valid trial/paid entitlement; scoped, dated checks; authorised contacts and portfolios. | PR-001, PR-002, PR-003, PR-004 |
| Relevant discovery | Match service and coverage, explain prices and unknown availability, allow optional preferences and saved plans, and show truthful review counts without a default five-star filter. | DS-001, DS-002, DS-003, DS-004, DS-005 |
| Contact and enquiries | Allow outside arrangements. Share only the approved brief with the selected provider. Separate contact actions, enquiries and reported outcomes. | EN-001, EN-002, EN-003 |
| Reviews and accountability | Accept genuine outside-service experiences, distinguish reviewed supporting evidence, and provide replies, reporting, reasoned moderation and appeals. | RV-001, RV-002, RV-003 |
| Trials and subscriptions | Start the trial at approved publication, disclose purchased benefits/expiry, grant paid time once after verification and retain authorised history after expiry. | SB-001, SB-002, SB-003, SB-004 |
| Billing, support and privacy | Reconcile uncertain/repeated subscription events; handle platform complaints, subscription billing cases and personal-data requests. | PY-001, OP-001 |

Provider insights, operational notifications and audit history support these capabilities. [Functional requirements](09-functional-requirements.md) contains the complete module-level functions; [user journeys](04-app-flow-and-user-journeys.md) covers routes and states.

### 4.3 Nice-to-have and excluded work

**Nice-to-have:** labelled relevant sponsored placement (AD-001), shared shortlists, additional Ghanaian languages, advanced recommendations, provider integrations, native mobile apps and supported automatic card renewal. These require separate scope approval and evidence of usefulness.

**Outside the MVP:** booking commissions, customer service checkout, escrow, wallets, provider payouts, quote acceptance, calendar holds, guaranteed availability and outside-service refund guarantees. Ticketing, donations, lending, permits and marriage registration are excluded.

## 5. Commercial operating model

| Stage or condition | Required behaviour |
| --- | --- |
| Registration and preparation | Providers create accounts and prepare listings free. Assessment time does not consume the listing trial. |
| First approved publication | Start one trial. Duration and included benefits require approval before activation of the offer. |
| Purchase | Explain the actual price, period, benefits, limits, cancellation and refund terms before collecting payment. |
| Verified collection | Grant the purchased period once against its invoice. Repeated notifications must not extend it again. |
| Early renewal | Preserve unused trial or paid time under the accepted terms. |
| Expiry | Pause active discovery/contact access. Preserve authorised management, billing, support and history; permitted historical profiles show an inactive state. |
| Suspension | Override publication, trial, paid and promotional eligibility. Retain billing/support rights under the applicable policy. |
| Cancellation | Respect the remaining accepted paid period unless a reviewed refund or revocation applies. |
| Verification and promotion | A tier may fund assessment, but payment cannot buy a passed check, stars or removal of legitimate criticism. Separate verification fees, boosts and lead fees are unapproved. |

The proposed launch collection method is manual monthly renewal through fresh hosted MoMo/card payments, without an automatic trial charge. Billing cadence and commercial terms still need approval. Paystack's automatic subscription API supports cards and Nigerian direct debit; Ghana MoMo auto-debit must not be implied. Merchant access remains subject to confirmation. [Paystack subscriptions](https://paystack.com/docs/payments/subscriptions/).

Provider value must account for additional gross profit and useful time saved, less subscription and enquiry-handling costs. CEREMO must assess earned subscription revenue after collection, assessment, support and acquisition costs. Outside service payments are not CEREMO revenue; provider-reported work is not proof of incremental business. [Financial assessment](01-ghana-research.md#14-financial-assessment).

## 6. Trust, privacy and failure behaviour

| Situation | Customer or provider must experience |
| --- | --- |
| No suitable provider | An honest empty result and control to change criteria. No unrelated paid provider presented as a match or silent widening of the area/budget. |
| Price or availability unknown | A clear need to confirm with the provider. Unknown does not mean free, within budget or available. |
| New or partly assessed provider | Actual review count and check scope/date/expiry. No invented rating, blanket quality badge or guaranteed service performance. |
| Eligibility changes after discovery | Recheck before contact/enquiry; explain the changed status without silently routing the customer elsewhere. |
| Contact or response fails | Distinguish opening a channel, submitting an enquiry and provider acknowledgement. Preserve failed/pending status where observable and allow another choice without claiming delivery. |
| Payment remains uncertain | Keep billing status explicit while verifying/reconciling; do not invent a successful payment or duplicate the entitlement. |
| Outside-service dispute | Provide reporting, evidence handling and moderation. Explain the boundary between platform action, CEREMO subscription refunds and the parties' service dispute. |
| Abusive or disputed content | Restrict access, record moderation reasons and support appeals. Paying providers receive no favourable review treatment. |
| Private information is requested | Share only customer-approved material for the selected enquiry. Keep private verification/review evidence restricted and marketing consent separate. |

Initial discovery must not require an exact private venue address. Budgets belong to a particular service/occasion with GHS currency and a stated unit, rather than a permanent wealth classification. Preferences are editable and removable. Refusing optional tracking or marketing must not block authorised profile/contact access; the free ACTIVE-account requirement still applies.

Protected actions must enforce account status, ownership, provider membership and staff permissions. A successful sign-in or provider subscription alone does not authorise another person's records.

## 7. Outcomes and measurement

The proposed primary product measure is **qualified enquiries to eligible providers**, reported by service category, locality and provider. It is a demand signal, not proof of hires. Pair it with registration/verification completion, contact abandonment, useful connections and repeat service discovery. Authorised contact can occur without a structured enquiry; neither a contact opening nor registration proves retention.

For the pilot, a qualified enquiry is a genuine request to an identified provider whose service and area fit, with enough context to respond. Exclude spam and duplicates; retain qualification reasons and unknown cases. An exact date or budget is not always necessary. A contact click alone does not qualify.

| Measure | Operational definition and interpretation |
| --- | --- |
| Discovery usefulness | In observed tasks, organisers reaching a self-selected suitable candidate and usable contact route ÷ organisers attempting the task. Compare elapsed time, effort and suitability with their usual process; report failures and assistance. |
| Supply coverage | Search attempts returning eligible matching providers ÷ valid attempts, segmented by category/locality. Report candidate counts and empty results; one dominant provider must not disguise weak coverage. |
| Provider response | Acknowledgement and substantive response times for qualified enquiries. Show unanswered enquiries separately rather than calculating only among responders. |
| Trial-to-paid | Providers making a first verified subscription payment ÷ the eligible trial cohort. Fix cohort rules and observation window before measuring; show incomplete observation separately. |
| Paid renewal | Providers paying for their next period ÷ providers whose paid period becomes due in the measured cohort. Show early, late, unpaid and cancelled cases. |
| Continuing customer value | Count relevant enquiries for another category in the same occasion. Measure next-occasion return among customers known to have another suitable occasion; retain unknown eligibility. |
| Provider and CEREMO economics | Source-labelled service outcomes, provider cost/value reports and subscription contribution after defined costs. Report concentration, acquisition costs and attribution limits. |

Guardrails: unsuitable/spam enquiries, empty results, privacy incidents, misleading trust signals, upheld complaints, billing errors/refunds and support effort. Measure by segment and cohort, rather than using aggregate growth to conceal poor experiences.

Follow-up records must identify the reporter, date and evidence status. Preserve conflicting accounts and unknown outcomes. Neither an enquiry closure nor both parties' reports create a CEREMO-verified booking. Do not use daily app opens as the main retention measure for occasional service needs.

Success thresholds, response targets, reporting windows and acceptable guardrail levels are **not yet approved**. Agree them before pilot measurement; do not choose them retrospectively to declare success. [Pilot measurement definitions](01-ghana-research.md#15-pilot-measures-and-follow-up).

## 8. Validation and release decisions

Proposed discovery: **20 organisers and 20 providers** in Greater Accra. Proposed supervised pilot: **30 qualified enquiries**, covering at least **three occasion groups and four service categories**. These are learning targets, not completed research, representative samples or commercial success thresholds.

| Gate | Evidence required | Decision |
| --- | --- | --- |
| Problem validation | Recent real sourcing/acquisition cases, observed workarounds and reasons CEREMO would add value. Include people who prefer existing channels. | Proceed with the proposed experience only if a meaningful problem and credible advantage are demonstrated; otherwise revise it. |
| Solution validation | Organisers can compare candidates, understand check/price information and contact a chosen provider. Providers understand the offer and can manage requests. Test skipping the guide. | Resolve material misunderstanding before pilot exposure. Do not equate a completed survey with a useful match. |
| Pilot readiness | Approved policies/terms, eligible supply in recruited segments, working support and billing recovery, trustworthy measurement and engineering readiness. | Restrict recruitment to supported capacity; show coverage gaps. All occasion families remain in catalogue scope. |
| Commercial validation | Actual provider purchase and later renewal under a disclosed offer, with enquiry quality, costs and non-renewal reasons. | Assess against pre-agreed thresholds. Trial activity or stated willingness alone cannot justify a paid-scale claim. |
| Expansion | Repeatable customer usefulness and provider renewal, sustainable operating costs and acceptable trust/privacy/billing outcomes. | Expand only after review; if evidence is weak, continue learning or revise the offer before adding markets or sponsorship. |

These are proposed decision gates for founder/product/engineering review. The [field research plan](01-ghana-research.md#16-field-research-and-pilot) defines recruitment, consent and observation. A small pilot cannot establish national demand or statistical superiority.

## 9. Delivery dependencies and readiness

Build in dependency order: account/catalogue foundation → provider assessment/publication → discovery and contact → enquiry/outcome/review handling → subscriptions and billing → supervised pilot. Develop support, authorisation and privacy alongside each capability. The complete paid release requires the full Must-have scope.

The backend remains the current implementation focus. Reuse the consolidated account module and shared/wire foundations. Account HTTP operations and verified-email activation are implemented: registration stays PENDING without trusted verification and can become ACTIVE after it. Reads do not activate; suspended/anonymised accounts cannot self-reactivate. Contact and downstream permission policies remain release decisions. Marketplace capabilities must not be presented as implemented merely because account operations exist.

Keycloak is required in development and production. The approved web stack and open client decisions remain in the [UI/UX brief](05-ui-ux-design-brief.md). API, identity, payment integration, storage and security detail belong in the [technical requirements](03-technical-requirements.md) and [database architecture](06-database-architecture.md).

Before live exposure, engineering and operations must demonstrate protected-data isolation, truthful listing/entitlement changes, payment reconciliation, recoverable failures, monitoring, support and restoration procedures. Set performance, capacity and operational service targets against an agreed pilot workload before release. Follow the existing [implementation plan](07-implementation-plan.md) and [quality requirements](08-development-and-quality.md), including SonarQube and the per-class 100% JaCoCo line/branch rule. This document records no fresh test or quality-gate result.

## 10. Open decisions and accountability

The roles below are proposed responsibilities; named accountable people have not been assigned.

| Decision | Proposed accountable role | Required before |
| --- | --- | --- |
| Trial duration, price, benefits, limits and billing cadence | Founder/product, with finance input | Publishing a trial or paid offer |
| Contact checks and downstream action permissions | Product and security lead | Protected customer/provider submissions |
| Verification scope, evidence, expiry and appeals | Trust/operations lead | Publishing check claims |
| Review eligibility, evidence and moderation policy | Trust/operations lead | Accepting and publishing reviews |
| Ranking policy and new-provider exposure | Product lead | Public discovery |
| Cancellation, refunds and payment dispute handling | Finance/operations, with legal review | Collecting subscription charges |
| Privacy, retention, processor arrangements and notices | Privacy/legal lead | Live personal-data processing |
| Pilot thresholds, cohorts, windows and guardrails | Product lead with engineering/operations | Recruiting the pilot cohort |
| Integration access, hosting, capacity and recovery targets | Engineering lead | Production readiness approval |
| Sponsored allocation and disclosures | Product and trust leads | Any later sponsored release |

No decision is supplied by an assumed industry default. Scope changes require approval and corresponding updates to the functional requirements, journeys, architecture, schema and build plan.

Editorial verification: Grammarly **pending** for version 3.4; no external check or score is claimed. Research sources, revisions and local checks are recorded in the [product research entry](../.github/research/product/product-requirements.md).
