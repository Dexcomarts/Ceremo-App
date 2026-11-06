# Product requirements: problem, strategy and release decisions

- Entry ID: product/product-requirements
- Reviewed: 4 November 2026
- Status: researched; PRD version 3.0 revised and locally checked; Grammarly pending
- Question: how to make CEREMO's PRD useful for investor, product and engineering decisions instead of presenting another feature inventory
- Scope: existing document 02 and local research context; no application implementation or change to approved commercial scope

## Authority and revision history

The user requested the PRD rewrite and then rejected its product depth. The current revision addresses that feedback within the already authorised rewrite. It preserves the approved business scope and all 25 requirement references. No new product document, installation, external transfer or application change is authorised by this work.

Version 2.0, written on 4 November 2026, shortened the PRD around nine sections and a requirement table. **Its presentation is superseded by version 3.0.** It preserved scope but did not adequately expose the problem evidence, strategic alternatives, operational metric definitions, validation gates or decision accountability. The user's criticism is not evidence that a particular company format is mandatory.

The PRD owns customer problems, product choices, release outcomes, boundaries and validation. [Document 09](../../../docs/09-functional-requirements.md) owns the complete function catalogue; documents 03/06/07/08 retain architecture, schema, implementation and quality detail. The [functional-specification writing method](../application/customer-lifecycle.md#professional-specification-method-and-rewrite-4-november-2026) remains useful for that document, rather than a compulsory PRD layout.

## Evidence reviewed

Primary sources opened on 4 November 2026:

| Source and date | Finding | Adaptation and limitation |
| --- | --- | --- |
| Robbie Allan, Intercom, [For better products, start with a problem statement](https://www.intercom.com/blog/how-to-write-problem-statements), 5 January 2021 | Frames the desired customer outcome, reason it matters and difficulty with the current approach. Explicitly distinguishes a problem statement from an entire PRD. | Add a specific organiser job and current-workaround hypothesis before capabilities. Historical published team practice; not proof of CEREMO demand or a universal format. |
| GitLab, [Product Development Flow](https://handbook.gitlab.com/handbook/product-development/how-we-work/product-development-flow/), living handbook reviewed 4 November 2026 | Problem validation precedes solution validation; goals, metrics, constraints and engineering feasibility inform design. Build outcomes are measured and used for iteration. | Separate problem, usability, operational readiness and commercial evidence. Propose review gates without importing GitLab's staffing, scheduling or issue labels. Published process does not establish private project execution or CEREMO readiness. |
| Linear, [Projects](https://linear.app/docs/projects), living documentation reviewed 4 November 2026 | Describes projects around outcomes, project leads, collaborative specifications and milestones. | Make responsibilities and completion evidence visible. This is documented functionality and a short statement of Linear's own practice, not a full public PRD template. |
| The Knot Worldwide, [U.S. terms of purchase](https://www.theknotww.com/terms-of-purchase), revised 9 February 2026 | Describes vendor subscription services and no guaranteed minimum enquiries, bookings or revenue. | Supports a comparable provider offer and honest value claims. Do not import its annual term, legal clauses or pricing; it does not validate Ghanaian willingness to pay. |
| The Knot Worldwide, [Terms of use](https://www.theknotww.com/terms-of-use), effective 9 February 2026 | Describes a marketplace helping members identify vendors, with qualifications on identity/capability claims. | Supports distinguishing provider discovery from an endorsement or service guarantee. Product terms do not reveal internal architecture or establish Ghanaian legal requirements. |
| Paystack, [Subscriptions](https://paystack.com/docs/payments/subscriptions/), living documentation reviewed 4 November 2026 | Automatic subscriptions support cards and Nigerian direct debit; this is different from Ghana MoMo collection. | Preserve proposed manual hosted renewal and merchant-approval limits. No live merchant or subscription integration was tested. |

The GitLab handbook's Opportunity Canvas link resolved to [Product Processes](https://handbook.gitlab.com/handbook/product/product-processes/). A guessed standalone opportunity-canvas URL was inaccessible and is not used as evidence. WeddingPro's vendor-visibility page could not be fetched in this investigation; its previously recorded package details were not freshly verified. Use the accessible TKWW terms for the narrower subscription comparison.

Earlier template evidence remains supporting guidance: [Atlassian product requirements template](https://www.atlassian.com/software/confluence/templates/product-requirements) relates objectives, assumptions and scope; [Aha! PRD guidance](https://www.aha.io/roadmapping/guide/templates/create/prd) relates beneficiaries, use cases and capabilities. Both were opened for version 2.0. Neither replaces problem research or proves investor acceptance.

## Skills and method

Reviewed the installed product-lens diagnosis, product-capability and standalone humanizer guidance. Product-lens helps expose the user, pain, workaround, evidence gap, minimum proof and anti-goals. Product-capability separates fixed policy, proposed behaviour and unresolved decisions. Humanizer improves direct professional prose while preserving evidence and qualifications. No PMF/ICE score, author credential, detector claim or interview finding was invented.

The standalone humanizer is MIT-licensed, pinned locally to revision d4b81278e6604e237bf32f0f7ce5c217aee94269 as recorded in its SOURCE.json. Other installed skills were read as guidance, not copied or redistributed. No additional skill, remote script or agent was installed/run; no company draft was uploaded.

## CEREMO adaptation and reuse

Version 3.0 changes the organising logic:

1. Define the organiser's job, desired decision and current-workaround hypothesis.
2. Explain why comparison could help customers and why providers might renew, without claiming either is proven.
3. Record alternatives and deliberate choices, including permitted outside contact, subscription economics and an optional guide.
4. Group existing requirements into coherent product capabilities. Keep all original IDs for traceability; put detailed function behaviour in document 09.
5. State observable trust, privacy, commercial and failure behaviour.
6. Define proposed product metrics, denominators/cohorts where applicable, attribution limits and guardrails.
7. Separate problem, solution, readiness, commercial and expansion gates. Leave numeric thresholds, observation windows and accountable people open for approval.
8. Link delivery dependencies and assign proposed decision roles without inventing a staffed team or launch commitment.

The existing Ghana research already defines qualified enquiries, cohorts, reporting limits, economics and the field plan. Reuse [pilot measures](../../../docs/01-ghana-research.md#15-pilot-measures-and-follow-up) and [field research](../../../docs/01-ghana-research.md#16-field-research-and-pilot) rather than presenting these as new findings. The proposed 20 organisers, 20 providers and 30 qualified enquiries across three occasion groups/four categories remain learning targets.

Current local context was checked through the research index, shared/wire reuse maps, technical requirements, functional catalogue and User Management source. UserService delegates registration to User.register; the current domain factory sets PENDING. Shared/wire are implementation foundations, not new product evidence. No application source or configuration is changed or recommended by this rewrite.

Preserved: Greater Accra and all occasions/festivals; free customer discovery; provider subscriptions only; outside conversations/contracts/service payments; no checkout or commissions; independent entitlement/checks/sponsorship/ratings; unknown/no-review states; editable/skippable preferences; genuine outside-experience reviews; one approved-publication trial; safe billing/history and unresolved commercial terms.

## Verification and remaining work

Version: PRD 3.0, 4 November 2026. Material: the full revised PRD, this entry and the research index. A local PowerShell check passed for 34 relative links/anchors, consistent table columns, ten sequential PRD sections and 25 unique original requirement references. Compared the original and revised reference sets before writing; no ID was lost or added. The approved product-document set remains nine files. Document 09's SHA-256 remained unchanged: 649BC75FC234C9C3BF4190E3C664E5E41FB2D4FF6B9E75F4F0C6B5D98B171C47. Reviewed the preserved commercial/privacy boundaries and confirmed the current User.register factory creates PENDING accounts. No fresh application tests, coverage measurement, Sonar gate, live payment or Keycloak check is claimed.

Grammarly: **pending**. No authorised checker is available in this session. No excerpt or draft was submitted, and no score or actual Grammarly result exists. A later authorised check must record the version, exact public/anonymised material, date, result and limitations; an excerpt check covers only that excerpt.

Remaining product work: founder/product/engineering review of proposed metrics and gates; accountable people; field evidence; offer, trust/privacy and activation policies; numeric service targets and commercial thresholds. The rewrite does not establish product-market fit or approve these unresolved choices.
