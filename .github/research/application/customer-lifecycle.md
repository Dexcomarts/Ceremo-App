# Complete customer cycle and functional requirements

## Registration timing review, 5 November 2026

Superseded in part: the founder challenged unrestricted public contacts. The later [contact-access and retention investigation](../product/customer-contact-retention.md) finds a direct Jiji Ghana precedent and recommends a free active-account gate before contact. The no-timer/public-browsing recommendation remains; public-contact document requirements are under review rather than silently rewritten here.

- Question: how long may a customer use CEREMO before registering?
- Status: existing public-discovery requirement reaffirmed; action-based prompts recommended, no new product policy or runtime change applied.
- Local evidence: document 04 section 1 permits public browsing and approved business contacts; document 09 sections 1–2 and contact functions require accounts for protected submissions. Current shared SecurityConfiguration permits listed public GET routes and authenticates other API routes. This configuration is not proof that planned marketplace controllers exist.
- Reviewed primary sources: [Airbnb wishlists](https://www.airbnb.com/help/article/1236) describes logged-in collaboration and public view-only links; [Houzz professionals](https://www.houzz.com/professionals) exposes a professional directory in the unauthenticated research fetch; [Bark customer workflow](https://www.bark.com/en/gb/how-it-works/) describes comparison and shows request phone verification/account login. These support separating discovery from account-linked actions; they establish no optimal registration delay for Ghana. No interactive signup, code import or private-data submission occurred. Dynamic flows may differ by region/device; source publication dates are not provided for these live pages.
- Recommendation: no elapsed-time or profile-count registration limit. Keep public search, profiles, reviews and approved contacts accessible. Request sign-in when saving to an account, persisting an event plan, submitting an enquiry or review, or managing a provider business. Use a specific benefit in the prompt and return to the intended action after authentication; obtain confirmation before sharing an enquiry. Provider trial duration is a separate unapproved commercial decision.
- Reuse: retain the current account/Keycloak boundary and shared security composition; no new timer, identity store or duplicate authentication helper. Review actual route ownership and active-account policy when implementing the relevant feature.
- Verification: read-only source/document review and public-page research; no application code/configuration or tests changed. Current journeys already state the public-access rule, so no public-document rewrite is required. Registration conversion and successful discovery/contact need local organiser/provider validation; no improvement percentage is claimed. Local prose reviewed with humanizer; Grammarly unavailable/pending; no company prose uploaded.

- Entry ID: application/customer-lifecycle
- Researched: 4 November 2026
- Status: version 2.0 module-level rewrite applied and locally verified; Grammarly pending
- Question: which functions support an organiser from first visit through service discovery, outside contact, reported experience, repeat use and account exit?
- Scope: functional specification only, including the provider/staff operations needed to support that cycle; no application code or runtime configuration changed

Current presentation and writing rules: [approved professional specification method](#professional-specification-method-and-rewrite-4-november-2026). Earlier clause counts and layout descriptions below are version 1.0 history; the current document has 14 functional modules, 110 function rows and two scope/reference sections.

## Authority, reference and local context

The user rejected expanding document 08 and explicitly requested a separate file for every function in the complete customer cycle. This authorises [document 09](../../../docs/09-functional-requirements.md) as an exception to the older eight-file rule. Preserve the original eight documents. AGENTS.md, Copilot instructions and the README index record that exception.

The local writing reference was `PAYWELL_BAST_SYSTEM_REQUIREMENTS.md` in the user-specified Paywell POS/inventory project. Only that requirements file was read. Its short action lists grouped by module informed the presentation; repair, inventory and financial-sector operations were not imported. No Salesync source was accessed.

Read the CEREMO PRD, technical requirements, journeys, schema, research index, shared/wire reuse skill/maps and security rules. Checked current `UserAccounts` and `UserStatus`; registration/current-account lookup exist as core contracts, and PENDING activation remains an unresolved product/implementation dependency. Requirements are not labelled as implemented features.

## Primary evidence and adaptation

All external sources below were reopened on 4 November 2026. No company material was included in public queries or uploaded to another service.

| Source | Finding and application | Limit |
| --- | --- | --- |
| [NASA requirements checklist](https://www.nasa.gov/reference/appendix-c-how-to-write-a-good-requirement/) | Clear, independently verifiable requirements and explicit unresolved decisions support function IDs and acceptance scenarios | Writing convention, not marketplace architecture; no source implementation copied |
| [Bark customer journey](https://www.bark.com/en/us/how-it-works/) | Public need-description, comparison, contact and review workflow is relevant to discovery | Does not establish CEREMO pricing, selected-provider-only enquiries, internal code or Ghanaian demand |
| [Paystack subscriptions](https://paystack.com/docs/payments/subscriptions/) | Automatic subscriptions support card and Nigerian direct debit; keep Ghana MoMo manual renewal separate | Confirm merchant/channel availability; manual renewal remains CEREMO's planned launch behaviour |
| [Paystack webhooks](https://paystack.com/docs/payments/webhooks/) | Raw-body authentication, retry handling and current newer/legacy signature modes | The live guide describes timestamped `Paystack-Signature` and legacy `x-paystack-signature`. Actual merchant mode must be verified before implementation; do not silently copy the older sole-header assumption from cached notes |
| [Paystack verification](https://paystack.com/docs/payments/verify-payments/) | Server-side transaction checks and preventing repeated value delivery support one verified invoice grant | Does not implement CEREMO invoice locks, refund authority or commercial terms |
| [Ghana Data Protection Commission](https://dpc.gov.gh/for-individuals/) | Authorised personal-data rights handling supports privacy requests and corrections | No invented legal deadline, blanket exemption or compliance certification |

The installed local `humanizer` skill was reviewed and applied for plain professional wording, subject to preserving factual/technical meaning. Read `SOURCE.json`: it records `blader/humanizer` revision `d4b81278e6604e237bf32f0f7ce5c217aee94269`, MIT licence and instructions-only installation. No authorship/banking-adoption claim is made. The specification skill was evaluated in the preceding planning review; its multiple-file convention does not override the user's explicit single functional catalogue. The user confirmed the catalogue's scope and separate-file requirement before construction. No skill installation, external scripts or subagents were used.

## Specification coverage

Document 09 contains the customer cycle first: public browsing; Keycloak/local account registration; verification; optional preferences/occasion plans; search; profile comparison; saved providers; direct contact; selected enquiries; outside agreements/payments; outcome reports; experience reviews; support; notifications; repeat use; privacy/logout/closure.

Supporting sections cover provider teams/services, assessment, trial/paid entitlements, subscription payments, moderation, catalogues, reporting and technical operations. Preserve no booking commission, no service checkout, no default five-star filter and no pay-to-pass verification. Thirty acceptance scenarios include failures, ownership, retries, expiry, disputed reports, rollback and recovery. Proposed policy details remain in the decision register rather than becoming silent defaults. Seven later-feature entries are separated from launch requirements.

## Verification and remaining work

Verified with local PowerShell/regex/file checks on 4 November 2026: 373 distinct function IDs across 26 responsibility prefixes, including seven later-feature entries; no duplicate IDs, undefined function references or numbering gaps; 31 sequential numbered sections, 30 acceptance scenarios and one balanced Mermaid code block. Relative file links in all six affected files resolve. Documents 01–08 remain present, and `docs/` now has the expressly authorised nine files. Reviewed scope, consent, public-contact, entitlement and external-payment wording against the existing documents. These are documentation checks, not execution of 373 functions or application tests. No new code/test/coverage/Sonar result is claimed.

Grammarly record: document version 1.0, full text not submitted, 4 November 2026, **pending**. No Grammarly tool/checker is available in this session and no company draft or excerpt was uploaded. No score or full language-check pass is claimed. Local editorial checks do not satisfy the Grammarly requirement. A later authorised check must identify exactly what anonymised/public material was submitted and its actual result.

Next work: user review of the requirement catalogue and unresolved activation/commercial/moderation/privacy policies; function-by-function engineering research and implementation planning as authorised. Do not treat this draft as approval of unselected commercial policies or newly implemented endpoints.

## Requested introduction removal, 4 November 2026

The user removed the need for the opening version/status line and the entire purpose/operating-rules section, including its requirements-status explanation, permissions table and customer-cycle diagram. Removed that block from document 09. The file now begins with First visit and public browsing. Renumbered the remaining sections/subsections and corrected section references and the evidence anchor. All 373 function identifiers remain unchanged. The earlier 31-section/Mermaid inventory is historical; the current document has 30 numbered sections and no diagram. Grammarly remains pending; no external text submission or application code/test change was made.

## Professional specification method and rewrite, 4 November 2026

Status: approved presentation rewrite applied; detailed scope retained for engineering review; final local checks recorded below; Grammarly pending.

### Approved decision

The user rejected the 373-statement presentation as unsuitable for investors and asked for concise, professionally organised functions for every application section. After the public-platform/skill review, the user explicitly approved saving the researched rules in local context and rewriting document 09. This authorises the document, AGENTS.md, Copilot guidance, this research entry/index and README navigation updates. It does not approve new commercial policy, install a skill, change application code or transfer company text externally.

Document 09 version 2.0 uses numbered business modules and Function / Required behaviour / Essential business rule tables. Customer discovery through repeat use and account exit precedes supporting provider, commercial and platform operations. It begins directly with functions; the previously removed purpose/status opening, permissions matrix and Mermaid diagram remain removed. Current, later and excluded scope is explicit. Superseded presentation claims and counts above are historical.

### Public-template analysis

Method: inspect public templates or examples, identify their audience and information hierarchy, distinguish functional behaviour from quality/implementation detail, then adapt the useful structure to CEREMO. This is analysis of public evidence, not access to a vendor's private client workflow. No scraping scripts, account creation, purchases, private portals or external document uploads were used.

All sources were reviewed on 4 November 2026 and the five principal URLs were reopened before applying the rewrite.

| Primary reference | Observed method and local adaptation | Limits |
| --- | --- | --- |
| [Atlassian product requirements](https://www.atlassian.com/software/confluence/templates/product-requirements) | Associates requirements with user stories, importance and explicit scope. Organise CEREMO functions by user task and business area. | A PRD template, not a mandatory FRD format. Do not copy release dates, metrics or assumptions as facts. The separately attempted legacy PDF timed out. |
| [Aha! feature requirements](https://www.aha.io/roadmapping/guide/templates/create/featurerequirement) | Connects a feature's functionality, user challenge, value, affected areas and open questions. Keep useful behaviour and governing rules together. | Vendor guidance and public preview, not access to a private implementation or evidence of investor acceptance. |
| [Jama functional requirements](https://www.jamasoftware.com/requirements-management-guide/writing-requirements/functional-requirements-examples-and-templates/) | Recommends function/subfunction hierarchy, concise observable requirements and separation from rationale/non-functional detail. Use short functional rows; retain technical detail elsewhere. | Do not replace all business precision with generic feature names. The linked legacy PRD page could not be retrieved; findings depend on the accessible functional-requirements guide. |
| [ReqView example project](https://www.reqview.com/doc/example-project/) | Separates NEEDS, SRS, TESTS and ARCH with satisfaction/verification/implementation links. Keep investor-readable functions linked to developer requirements and verification. | Maintainer demo is explicitly incomplete. Its [published SRS PDF](https://www.reqview.com/papers/ReqView-Example_Software_Requirements_Specification_SRS_Document.pdf) dates to 3 September 2021 and models older product functionality; use as structural evidence only. |
| [Asana SRS template](https://assets.asana.biz/m/6ac2683dd6006280/original/software-requirement-document-template.pdf) | Seven-page template separates functional, interface and non-functional sections. Supports separating documentation responsibilities. | A skeleton does not supply CEREMO business functions. Text was retrieved; a later screenshot request had a cache failure, so no complete visual audit is claimed. |
| [Anthropic write-spec](https://github.com/anthropics/knowledge-work-plugins/blob/main/product-management/skills/write-spec/SKILL.md) | Distinguishes user outcomes, scope, priorities, acceptance and unresolved decisions. Use these as analysis checks, not a reason to add another long PRD introduction. | Reviewed on main; no immutable commit was established through the blocked commit-API request. It was not downloaded or installed. Pin/review a revision before any future installation. |
 
The [Anthropic repository licence](https://github.com/anthropics/knowledge-work-plugins/blob/main/LICENSE) was inspected as Apache-2.0. Its connector instructions describe optional tools and a local-context fallback; no connector was enabled or used to access company content. Maintainer identity and a public licence are verifiable; no claim of universal adoption, elite status, author experience or investor approval follows from them. The IEEE Computer Society article returned 403 and is not relied on.

### Skills and persistent writing rules

Reused installed product-capability for the distinction between user-visible promises and implementation constraints, research-ops/skill-scout for evidence and source selection, and the standalone humanizer for professional prose. The humanizer provenance remains the reviewed MIT revision recorded above. The community business-analyst candidate was examined as a reference, but its marketing claims of senior experience were not treated as credentials and it was not installed. Broad competitive benchmarking, code-style inheritance and new governance machinery were not needed.

Saved the following rules in AGENTS.md and linked them from Copilot instructions:

- Write functional specifications at the business-module level for the intended reader. Keep the complete customer cycle and supporting provider/staff responsibilities.
- Use concise function tables; each row names the operation, observable result and essential business condition. Avoid repeated actor paragraphs and implementation checklists.
- Consolidate repetition without dropping meaningful policy, ownership, trust, privacy or financial integrity.
- Keep API names, database mechanisms, security configuration, job schedules, test scenarios and coverage rules in developer documentation/internal traceability, with links.
- Distinguish required, proposed/unapproved, later and excluded scope. Never invent prices, trial duration, metrics, dates or guarantees.
- Respect explicitly removed content and the nine-document limit.
- Preserve a coverage mapping and source record when compressing a specification.
- Apply local editorial review; Grammarly is a separate pending check until its actual authorised result is recorded. No authorship-certification or company-adoption claims.

### Coverage mapping

The original 373 numbered clauses are retained below for developer review; 30 acceptance scenarios and the unresolved policy register are also preserved. These records retain detail rather than serving as the investor-facing layout. Retention of a proposed v1 clause does not make its unsettled policy approved. The SYS group remains engineering detail governed by documents 03, 06, 07 and 08; document 09 links those owners through section 16.

| Previous clauses | Count | Current functional sections |
| --- | --- | --- |
| PUB-001–PUB-011 | 11 | [1. Service discovery](../../../docs/09-functional-requirements.md#1-service-discovery); [4. Provider profiles and shortlists](../../../docs/09-functional-requirements.md#4-provider-profiles-and-shortlists); [13. Administration and platform controls](../../../docs/09-functional-requirements.md#13-administration-and-platform-controls) |
| ACC-001–ACC-016 | 16 | [2. Customer accounts and access](../../../docs/09-functional-requirements.md#2-customer-accounts-and-access); [7. Customer support and privacy](../../../docs/09-functional-requirements.md#7-customer-support-and-privacy); [15. Release scope and decisions](../../../docs/09-functional-requirements.md#15-release-scope-and-decisions) |
| PRO-001–PRO-012 | 12 | [2. Customer accounts and access](../../../docs/09-functional-requirements.md#2-customer-accounts-and-access); [7. Customer support and privacy](../../../docs/09-functional-requirements.md#7-customer-support-and-privacy) |
| PREF-001–PREF-014 | 14 | [3. Preferences and occasion planning](../../../docs/09-functional-requirements.md#3-preferences-and-occasion-planning); [8. Notifications and repeat use](../../../docs/09-functional-requirements.md#8-notifications-and-repeat-use) |
| EVT-001–EVT-013 | 13 | [3. Preferences and occasion planning](../../../docs/09-functional-requirements.md#3-preferences-and-occasion-planning); [8. Notifications and repeat use](../../../docs/09-functional-requirements.md#8-notifications-and-repeat-use) |
| SRC-001–SRC-016 | 16 | [1. Service discovery](../../../docs/09-functional-requirements.md#1-service-discovery); [4. Provider profiles and shortlists](../../../docs/09-functional-requirements.md#4-provider-profiles-and-shortlists); [10. Listing approval and verification](../../../docs/09-functional-requirements.md#10-listing-approval-and-verification); [11. Provider trials and subscriptions](../../../docs/09-functional-requirements.md#11-provider-trials-and-subscriptions) |
| VIEW-001–VIEW-012 | 12 | [4. Provider profiles and shortlists](../../../docs/09-functional-requirements.md#4-provider-profiles-and-shortlists); [10. Listing approval and verification](../../../docs/09-functional-requirements.md#10-listing-approval-and-verification) |
| SAVE-001–SAVE-008 | 8 | [4. Provider profiles and shortlists](../../../docs/09-functional-requirements.md#4-provider-profiles-and-shortlists); [8. Notifications and repeat use](../../../docs/09-functional-requirements.md#8-notifications-and-repeat-use) |
| CON-001–CON-012 | 12 | [5. Contact and enquiry management](../../../docs/09-functional-requirements.md#5-contact-and-enquiry-management); [14. Reporting and operational oversight](../../../docs/09-functional-requirements.md#14-reporting-and-operational-oversight) |
| ENQ-001–ENQ-015 | 15 | [5. Contact and enquiry management](../../../docs/09-functional-requirements.md#5-contact-and-enquiry-management) |
| TRACK-001–TRACK-012 | 12 | [5. Contact and enquiry management](../../../docs/09-functional-requirements.md#5-contact-and-enquiry-management); [11. Provider trials and subscriptions](../../../docs/09-functional-requirements.md#11-provider-trials-and-subscriptions) |
| EXT-001–EXT-008 | 8 | [5. Contact and enquiry management](../../../docs/09-functional-requirements.md#5-contact-and-enquiry-management); [7. Customer support and privacy](../../../docs/09-functional-requirements.md#7-customer-support-and-privacy) |
| OUT-001–OUT-009 | 9 | [6. Outcomes, reviews and trust](../../../docs/09-functional-requirements.md#6-outcomes-reviews-and-trust); [8. Notifications and repeat use](../../../docs/09-functional-requirements.md#8-notifications-and-repeat-use) |
| REV-001–REV-015 | 15 | [6. Outcomes, reviews and trust](../../../docs/09-functional-requirements.md#6-outcomes-reviews-and-trust); [10. Listing approval and verification](../../../docs/09-functional-requirements.md#10-listing-approval-and-verification); [13. Administration and platform controls](../../../docs/09-functional-requirements.md#13-administration-and-platform-controls) |
| CASE-001–CASE-012 | 12 | [7. Customer support and privacy](../../../docs/09-functional-requirements.md#7-customer-support-and-privacy); [13. Administration and platform controls](../../../docs/09-functional-requirements.md#13-administration-and-platform-controls) |
| NOT-001–NOT-010 | 10 | [8. Notifications and repeat use](../../../docs/09-functional-requirements.md#8-notifications-and-repeat-use); [11. Provider trials and subscriptions](../../../docs/09-functional-requirements.md#11-provider-trials-and-subscriptions); [14. Reporting and operational oversight](../../../docs/09-functional-requirements.md#14-reporting-and-operational-oversight) |
| RET-001–RET-010 | 10 | [8. Notifications and repeat use](../../../docs/09-functional-requirements.md#8-notifications-and-repeat-use) |
| PRIV-001–PRIV-016 | 16 | [7. Customer support and privacy](../../../docs/09-functional-requirements.md#7-customer-support-and-privacy); [13. Administration and platform controls](../../../docs/09-functional-requirements.md#13-administration-and-platform-controls); [14. Reporting and operational oversight](../../../docs/09-functional-requirements.md#14-reporting-and-operational-oversight) |
| BUS-001–BUS-020 | 20 | [9. Provider business management](../../../docs/09-functional-requirements.md#9-provider-business-management); [10. Listing approval and verification](../../../docs/09-functional-requirements.md#10-listing-approval-and-verification); [11. Provider trials and subscriptions](../../../docs/09-functional-requirements.md#11-provider-trials-and-subscriptions) |
| CHK-001–CHK-015 | 15 | [10. Listing approval and verification](../../../docs/09-functional-requirements.md#10-listing-approval-and-verification); [13. Administration and platform controls](../../../docs/09-functional-requirements.md#13-administration-and-platform-controls) |
| SUB-001–SUB-021 | 21 | [11. Provider trials and subscriptions](../../../docs/09-functional-requirements.md#11-provider-trials-and-subscriptions); [12. Subscription payments and billing](../../../docs/09-functional-requirements.md#12-subscription-payments-and-billing); [15. Release scope and decisions](../../../docs/09-functional-requirements.md#15-release-scope-and-decisions) |
| PAY-001–PAY-021 | 21 | [12. Subscription payments and billing](../../../docs/09-functional-requirements.md#12-subscription-payments-and-billing); [15. Release scope and decisions](../../../docs/09-functional-requirements.md#15-release-scope-and-decisions) |
| ADM-001–ADM-019 | 19 | [7. Customer support and privacy](../../../docs/09-functional-requirements.md#7-customer-support-and-privacy); [10. Listing approval and verification](../../../docs/09-functional-requirements.md#10-listing-approval-and-verification); [13. Administration and platform controls](../../../docs/09-functional-requirements.md#13-administration-and-platform-controls) |
| REP-001–REP-015 | 15 | [14. Reporting and operational oversight](../../../docs/09-functional-requirements.md#14-reporting-and-operational-oversight) |
| SYS-001–SYS-034 | 34 | [14. Reporting and operational oversight](../../../docs/09-functional-requirements.md#14-reporting-and-operational-oversight); [16. Engineering and review references](../../../docs/09-functional-requirements.md#16-engineering-and-review-references) |
| LATER-001–LATER-007 | 7 | [15. Release scope and decisions](../../../docs/09-functional-requirements.md#15-release-scope-and-decisions) |

<details>
<summary>Original detailed clauses — superseded presentation, retained for engineering traceability</summary>

### PUB — original clauses

- **PUB-001:** Explain that customers browse free and providers fund CEREMO through subscriptions.
- **PUB-002:** Display active service categories and occasion types without requiring login.
- **PUB-003:** Let the customer choose a service area from supported localities.
- **PUB-004:** Show that Greater Accra is the launch area; do not imply nationwide provider coverage.
- **PUB-005:** Open an eligible provider's public profile from search or a direct link.
- **PUB-006:** Display only approved public fields; exclude private identity documents, staff notes, billing and customer records.
- **PUB-007:** Provide access to current terms, privacy information, contact-sharing explanations and platform support information.
- **PUB-008:** Display reviewed occasion advisories only for their applicable locality and effective period, with their source and review date.
- **PUB-009:** Explain an empty catalogue or unavailable category without creating sample providers that appear genuine.
- **PUB-010:** Permit manual locality selection. Exact address, device location permission and phone access are not required for initial discovery.
- **PUB-011:** Preserve public browsing when optional analytics or marketing permission is declined.

### ACC — original clauses

- **ACC-001:** Offer registration and sign-in through the configured Keycloak identity service.
- **ACC-002:** Support Keycloak's configured credential recovery and MFA flows without storing passwords or MFA secrets in CEREMO.
- **ACC-003:** After login, look up the local account using the trusted issuer and subject from validated authentication.
- **ACC-004:** Read the current local profile without automatically creating one.
- **ACC-005:** When a local profile is missing, offer explicit CEREMO profile registration and explain why it is needed.
- **ACC-006:** Collect the display name, the terms version being accepted and explicit acceptance. Never preselect acceptance on the customer's behalf.
- **ACC-007:** Validate the display name as nonblank and at most 120 characters, matching the current domain contract.
- **ACC-008:** Accept only the current configured terms version, at most 40 characters. Return a clear correction path when terms have changed.
- **ACC-009:** Generate the local account ID and terms-acceptance timestamp on the server. Do not accept client-assigned status, roles, identity or audit fields.
- **ACC-010:** Create new local accounts as `PENDING`, matching current behaviour. Explain outstanding activation requirements; do not silently promote the account to `ACTIVE`.
- **ACC-011:** Make repeated or simultaneous registration safe: one external identity maps to one local account, with no orphan record or overwritten profile.
- **ACC-012:** Handle an expired or invalid login by requesting authentication again while preserving only safe, authorised navigation context.
- **ACC-013:** Keep suspension and anonymisation effective after a new login; authentication must not restore restricted permissions.
- **ACC-014:** Do not merge accounts because email addresses or phone numbers match. Route disputed ownership through reviewed recovery.
- **ACC-015:** After successful login, return to the intended permitted action. Reconfirm an enquiry's content before sending it.
- **ACC-016:** Allow the same person to use customer features and separately authorised provider membership without creating an extra identity.

### PRO — original clauses

- **PRO-001:** View the customer's saved display name, application status and relevant contact-verification state.
- **PRO-002:** Edit permitted profile fields with validation and an optimistic-version check to avoid overwriting another recent change.
- **PRO-003:** Prevent profile edits from changing staff permissions, provider membership, identity links or subscription entitlement.
- **PRO-004:** Explain why contact verification is required before an affected protected action.
- **PRO-005:** Request a verification code for the proposed contact and defined verification purpose.
- **PRO-006:** Enforce code expiry, single use, attempt limits and resend controls; invalidate superseded codes.
- **PRO-007:** Confirm the contact only after successful verification. Sending a code or receiving a delivery receipt does not verify ownership.
- **PRO-008:** Reject a code issued for another account, number or purpose.
- **PRO-009:** Support correction of a mistyped number and a safe retry after failed delivery.
- **PRO-010:** Keep contact verification separate from Keycloak login and from provider identity/business assessment.
- **PRO-011:** Require the approved reauthentication and recovery process for sensitive identity/contact changes, and notify the previous contact when that policy requires it.
- **PRO-012:** Show only the customer's own private contact data. Publishing a provider's business contact requires a separate owner decision.

### PREF — original clauses

- **PREF-001:** Offer a short guide after registration and allow the customer to skip it entirely.
- **PREF-002:** Ask whether the customer is exploring or planning a particular occasion; allow an unknown or unanswered response.
- **PREF-003:** Let the customer choose one or more service categories of interest.
- **PREF-004:** Offer an occasion type when relevant, without inferring religion, ethnicity, family relationships or bereavement status.
- **PREF-005:** Ask for preferred locality, known date or date range, and service needs only where useful.
- **PREF-006:** Accept an unknown date or budget without blocking registration, discovery or a sufficiently informative enquiry.
- **PREF-007:** When saving a budget, identify the service, GHS currency and unit, such as per guest or whole service. Validate any known minimum and maximum.
- **PREF-008:** Keep occasion-specific budgets separate from persistent category interests; do not create a permanent wealth classification.
- **PREF-009:** Show which answers will be saved and how they affect recommendations.
- **PREF-010:** View, edit, replace or clear saved preferences and category interests.
- **PREF-011:** Apply a preference reset atomically without deleting unrelated enquiries, support or legally required records.
- **PREF-012:** Give current explicit search criteria precedence over saved defaults.
- **PREF-013:** Keep preference choices separate from marketing consent and contact-sharing permission.
- **PREF-014:** Prevent providers from receiving the customer's interests or budgets merely because they subscribe.

### EVT — original clauses

- **EVT-001:** Create a private occasion plan with an occasion type, useful title and available locality/date information.
- **EVT-002:** Plan different occasion types and festivals without assuming a wedding-specific workflow.
- **EVT-003:** Add separate service requirements, such as catering, photography, decoration, MC or faith-appropriate officiating services.
- **EVT-004:** Record a known guest count, service budget and category-specific requirements using the relevant catalogue schema.
- **EVT-005:** Validate known dates, guest counts and budget bounds. Preserve unknown values rather than inventing estimates.
- **EVT-006:** Keep different service budgets separate so that a catering budget does not filter photographers by mistake.
- **EVT-007:** View and edit a draft plan; activate or archive it through the permitted lifecycle.
- **EVT-008:** Remove an unused draft or unnecessary personal content under the retention policy. Do not cascade-delete submitted enquiry history.
- **EVT-009:** Search providers from a selected service brief using that brief's category and area.
- **EVT-010:** Reuse an owned brief when preparing an enquiry, with an explicit preview of the fields to be shared.
- **EVT-011:** Prevent another customer from reading, editing or attaching the plan to an enquiry.
- **EVT-012:** Preserve the original submitted enquiry snapshot when the customer later changes the plan.
- **EVT-013:** Permit another independent plan for a later occasion while retaining authorised access to earlier records.

### SRC — original clauses

- **SRC-001:** Search published services by service category and supported locality coverage.
- **SRC-002:** Apply current occasion/service needs and explicitly selected filters where the provider data supports a valid comparison.
- **SRC-003:** List only approved, published, accepting-enquiries providers with a current trial or paid entitlement for active discovery.
- **SRC-004:** Exclude suspended providers even when a paid period remains.
- **SRC-005:** Calculate eligibility from actual time boundaries and revocations, not a cached `ACTIVE` flag alone.
- **SRC-006:** Display the active filters and allow each to be removed or changed.
- **SRC-007:** Make compatible indicative prices filterable while retaining a clear treatment for unknown or quote-needed prices.
- **SRC-008:** Do not compare a per-guest price with a whole-event budget without a known quantity and valid conversion.
- **SRC-009:** Present approved ordering options and explain the main recommendation factors. Ranking weights remain a policy decision.
- **SRC-010:** Display published rating averages and review counts separately from relevance and sponsored placement.
- **SRC-011:** Include eligible providers with no reviews in ordinary search; their displayed average is unknown, not five stars.
- **SRC-012:** Return bounded, stable pages without repeated providers caused by normal pagination.
- **SRC-013:** Show a truthful empty result and offer explicit changes to area, category or budget.
- **SRC-014:** Do not silently expand the location, claim real-time availability or insert unrelated providers to fill a page.
- **SRC-015:** Distinguish service-area coverage from the provider's office location.
- **SRC-016:** Refresh eligibility before opening a contact action or accepting an enquiry, even when the search result was previously valid.

### VIEW — original clauses

- **VIEW-001:** Display the approved business name, description, services, coverage and public portfolio.
- **VIEW-002:** Describe each service's indicative price, currency, unit and conditions. Unknown price must not appear as free.
- **VIEW-003:** Display available published contact methods only while the provider is eligible for active contact.
- **VIEW-004:** Show relevant check types, results, review dates and expiry where disclosure is appropriate.
- **VIEW-005:** Explain what a verification check covers. Do not claim guaranteed quality, availability or a lawful officiating authority that has not been checked.
- **VIEW-006:** Keep subscription benefits and sponsorship labels separate from check results.
- **VIEW-007:** Display published reviews, rating count and provider responses, including a clear no-reviews state.
- **VIEW-008:** Explain whether a review is customer-reported or has had supporting evidence reviewed.
- **VIEW-009:** Let customers compare the available public information for several providers without manufacturing comparable prices or quality scores.
- **VIEW-010:** Offer save, contact, enquiry and report actions according to the caller's permission and provider's eligibility.
- **VIEW-011:** Show an inactive state on permitted historical profiles when a trial or paid period has expired; preserve eligible review history while disabling active contact actions.
- **VIEW-012:** Handle withdrawn or restricted profiles safely without exposing private suspension reasons, unpublished evidence or cached contact details.

### SAVE — original clauses

- **SAVE-001:** Save an eligible provider to the customer's private saved-provider list.
- **SAVE-002:** Treat repeated save requests for the same customer/provider as one saved entry.
- **SAVE-003:** View and remove saved providers without affecting the provider's public record.
- **SAVE-004:** Reopen the current public profile from a saved entry and show its present eligibility, not the status at the time of saving.
- **SAVE-005:** Identify inactive or unavailable saved providers and offer ordinary search for alternatives.
- **SAVE-006:** Prevent access to another customer's list and avoid publishing the customer's interests to the saved provider.
- **SAVE-007:** Preserve an intended save action through login, then recheck that the target is still valid.
- **SAVE-008:** Allow repeated independent selection of providers for different services. A saved entry is never a booking, reservation or payment commitment.

### CON — original clauses

- **CON-001:** Offer the provider-approved phone, WhatsApp or website destination for an eligible listing.
- **CON-002:** Let the person choose the contact channel without requiring a structured enquiry or in-app conversation first.
- **CON-003:** Resolve contact destinations from approved stored records. Do not accept an arbitrary client-supplied redirect URL.
- **CON-004:** Recheck that the contact belongs to the named provider and is currently published and eligible.
- **CON-005:** Explain that opening a channel transfers the person to an external service, where that service's terms and privacy practices apply.
- **CON-006:** Do not disclose the customer's private profile, phone or occasion brief to a provider merely because a contact link was opened.
- **CON-007:** If a prepared message is offered, let the customer inspect its contents before sharing through the external channel.
- **CON-008:** Record only permitted minimal contact-action analytics and deduplicate retries using the client event identifier.
- **CON-009:** Keep contact available when optional tracking is declined or analytics recording fails, provided the contact itself remains permitted.
- **CON-010:** Describe a recorded contact action as an opened channel, not a delivered message, conversation, meeting, hire or payment.
- **CON-011:** Permit customers to return to CEREMO and consider other providers after outside discussions.
- **CON-012:** Do not monitor or import private external conversations automatically.

### ENQ — original clauses

- **ENQ-001:** Start an enquiry from a provider service or an owned occasion-service brief.
- **ENQ-002:** Show the chosen provider and service category before submission.
- **ENQ-003:** Collect enough relevant context for a useful response, while allowing an unknown date, budget or guest count where valid.
- **ENQ-004:** Let the customer choose the permitted contact information and brief fields they want to share.
- **ENQ-005:** Preview the exact recipient and information before the customer confirms submission.
- **ENQ-006:** Require an active local account and the approved contact-verification conditions before the protected submission.
- **ENQ-007:** Verify ownership of an optional occasion/service reference; reject references to another person's plan.
- **ENQ-008:** Recheck provider approval, service publication, coverage, accepting-enquiries setting and entitlement at submission.
- **ENQ-009:** Store an immutable snapshot of the submitted brief, sharing choices, recipient service, customer and server timestamp.
- **ENQ-010:** Create the enquiry and its notification work atomically, so a notification outage cannot erase the request.
- **ENQ-011:** Use an idempotency key so a network retry does not send the same request twice; reject reuse of a key with different content.
- **ENQ-012:** Send the enquiry only to the selected provider. Do not broadcast it to subscribers without a separately approved product change.
- **ENQ-013:** Return an enquiry identifier and a saved/submitted confirmation. Do not claim that the provider has read or accepted it.
- **ENQ-014:** Rate-limit abusive submissions and return a usable retry time without blocking normal public browsing.
- **ENQ-015:** Do not persist a partially created enquiry when validation, ownership or transaction checks fail.

### TRACK — original clauses

- **TRACK-001:** List the customer's own enquiries and open each submitted snapshot.
- **TRACK-002:** List only enquiries addressed to providers that the current provider member may access.
- **TRACK-003:** Show `NEW`, `ACKNOWLEDGED`, `CLOSED` or `SPAM` with relevant timestamps and safe reasons.
- **TRACK-004:** Allow an authorised provider member to acknowledge an enquiry once without creating a service contract.
- **TRACK-005:** Notify the customer of acknowledgement through the approved operational channel.
- **TRACK-006:** Permit authorised participants to close an enquiry with a reason while preserving its original submitted content.
- **TRACK-007:** Allow provider spam reports and an authorised staff review rather than treating a provider's label as an unchallengeable fact.
- **TRACK-008:** Preserve access to permitted historical enquiries when a listing expires; prevent new restricted actions that need current entitlement.
- **TRACK-009:** Handle an unanswered enquiry honestly. Offer another search or provider contact; do not fabricate a response time or acceptance.
- **TRACK-010:** Require a separate customer confirmation to submit a new enquiry to another provider.
- **TRACK-011:** Keep separate enquiries for caterers, photographers and other services in the same occasion.
- **TRACK-012:** Reject unauthorised reads, guessed IDs and state changes without exposing the private recipient or customer data.

### EXT — original clauses

- **EXT-001:** Explain before external contact that service availability, scope, final prices and cancellation terms must be agreed directly.
- **EXT-002:** Permit the parties to exchange contacts, arrange meetings and continue outside the application.
- **EXT-003:** State that outside service payments are not CEREMO subscription payments or protected CEREMO checkout transactions.
- **EXT-004:** Avoid changing an enquiry into a confirmed booking because it was acknowledged or the customer opened WhatsApp.
- **EXT-005:** Keep the customer's service searches and saved providers usable if they decide not to hire a particular provider.
- **EXT-006:** Let an authenticated participant later report what happened without requiring access to their external conversation or payment account.
- **EXT-007:** Provide a platform report route for suspected misconduct, misleading listings or service complaints.
- **EXT-008:** Explain the scope of CEREMO's investigation without promising recovery of an external deposit, a replacement service or a service refund.

### OUT — original clauses

- **OUT-001:** Offer an optional outcome report for an accessible enquiry.
- **OUT-002:** Record `UNKNOWN`, `HIRED`, `NOT_HIRED` or `CANCELLED`, with reporter identity and server time.
- **OUT-003:** Derive the customer/provider source from the authenticated relationship rather than a client-supplied source field.
- **OUT-004:** Keep each submitted report as history; a correction adds a new source-labelled report rather than rewriting the other party's account.
- **OUT-005:** Preserve contradictory customer/provider reports and unresolved outcomes.
- **OUT-006:** Keep enquiry closure, reported hire, actual service experience and verified CEREMO subscription payment as distinct facts.
- **OUT-007:** Do not require an outcome report before ordinary browsing or another service search.
- **OUT-008:** Allow the owner to archive an occasion plan after the occasion or after cancellation, without inferring that all listed providers were hired.
- **OUT-009:** Do not change provider ratings automatically from reported outcomes or unanswered enquiries.

### REV — original clauses

- **REV-001:** Start a review from an eligible provider profile or the customer's own related enquiry.
- **REV-002:** Collect provider, service category, rating from one to five, review text and available service-date information under the approved policy.
- **REV-003:** Require an actual-experience declaration and the required identity/contact checks before publication.
- **REV-004:** Reject a review linked to someone else's enquiry or to an unrelated provider.
- **REV-005:** Apply authenticity, duplicate-experience, spam and conflict-of-interest checks without imposing one lifetime review per provider/customer.
- **REV-006:** Accept limited supporting evidence through private authorised upload where the review policy requires or permits it.
- **REV-007:** Never require OTPs, payment PINs, complete card information or an entire private chat history as review evidence.
- **REV-008:** Show whether the review is pending, published, hidden or removed, with an appropriate explanation to the author.
- **REV-009:** Display `CUSTOMER_REPORTED` or `EVIDENCE_REVIEWED` accurately; reviewed evidence is not a guarantee of service quality.
- **REV-010:** Calculate public average and count from eligible published reviews only. Display an unknown average when there are no reviews.
- **REV-011:** Allow an authorised provider member to publish a permitted response without editing or deleting the customer's review.
- **REV-012:** Allow reporting and appeal of a review or moderation decision.
- **REV-013:** Apply the same moderation rules to free-trial and paid providers; payment cannot remove legitimate criticism or buy stars.
- **REV-014:** Preserve moderation reasons and relevant history under the retention policy, including when a provider's subscription expires.
- **REV-015:** Define author correction/withdrawal and provider-reply editing rules before enabling those operations; route policy disputes to support in the meantime.

### CASE — original clauses

- **CASE-001:** Open a case for a misleading profile, review concern, account recovery, platform problem or reported outside service issue.
- **CASE-002:** Identify the relevant provider, enquiry or review only when the customer may access that relationship.
- **CASE-003:** Record the issue description and minimally necessary private evidence.
- **CASE-004:** Return a case reference and let the customer view their own case status and authorised correspondence.
- **CASE-005:** Record requests for additional information and let the customer provide a response through the approved case workflow.
- **CASE-006:** Distinguish internal staff notes from information that may be disclosed to either party.
- **CASE-007:** Notify the customer of relevant status changes and a reasoned resolution.
- **CASE-008:** Provide a route to request review or appeal under the agreed policy.
- **CASE-009:** Preserve necessary case evidence when a provider changes or loses its listing entitlement.
- **CASE-010:** Explain when an issue concerns an external service agreement and what CEREMO can investigate or change on its own platform.
- **CASE-011:** Do not promise a response deadline, legal remedy or compensation amount that has not been approved.
- **CASE-012:** Restrict sensitive evidence to authorised staff and record access where required.

### NOT — original clauses

- **NOT-001:** Produce operational notifications for verification, enquiry submission/acknowledgement, case updates and other approved account events.
- **NOT-002:** Maintain an authorised in-app notification record and dispatch supported external channels through the selected adapter.
- **NOT-003:** Keep the underlying business result committed if an external notification cannot be delivered.
- **NOT-004:** Retry temporary delivery failures with bounded attempts and deduplication; expose exhausted failures to authorised operations staff.
- **NOT-005:** Keep queued, sent and failed delivery distinct. A sent SMS is not proof that the person read it.
- **NOT-006:** Avoid sensitive evidence, full identity claims or private financial data in previews and notification text.
- **NOT-007:** Let the recipient manage optional communication choices separately from necessary security and transaction messages.
- **NOT-008:** Obtain separate marketing permission for each supported purpose/channel and record withdrawal.
- **NOT-009:** Do not enrol someone in marketing because they registered, chose an occasion or saved a provider.
- **NOT-010:** Avoid duplicate reminders and stop obsolete work when the referenced action is no longer needed.

### RET — original clauses

- **RET-001:** Let the customer sign in to the same account and access their permitted saved providers, plans and enquiries.
- **RET-002:** Offer previous preferences as editable defaults without applying an old occasion's date or budget to a new search silently.
- **RET-003:** Allow a new occasion plan with its own service requirements, dates and budget.
- **RET-004:** Let the customer find another category for the same occasion without completing earlier enquiries first.
- **RET-005:** Recheck the current eligibility and public information of a previously used or saved provider.
- **RET-006:** Explain when a familiar provider is inactive and allow ordinary search for alternatives.
- **RET-007:** Allow a new enquiry for a genuine new need while distinguishing a retry from a separate request.
- **RET-008:** Keep historical outcome reports and reviews distinct from a new service experience.
- **RET-009:** Permit normal browsing without forcing a new survey, an outcome report or a review.
- **RET-010:** Offer useful return paths through saved information and current results, without requiring unsolicited marketing messages.

### PRIV — original clauses

- **PRIV-001:** Explain the purpose of each optional data collection and which information will be public or shared with a selected provider.
- **PRIV-002:** Let customers inspect and correct permitted personal profile and preference information.
- **PRIV-003:** Let customers clear optional interests and preferences without closing the account.
- **PRIV-004:** Record marketing consent, its purpose/channel, notice version and withdrawal independently of terms acceptance.
- **PRIV-005:** Submit and track authenticated access, correction, objection or deletion requests through an authorised privacy workflow.
- **PRIV-006:** Verify request identity proportionately before disclosing data or applying irreversible changes.
- **PRIV-007:** Provide an authorised data-access response without exposing other customers' information, private staff notes or protected third-party records.
- **PRIV-008:** Explain when data cannot be erased immediately because an applicable obligation or documented hold requires retention; do not invent a blanket retention exemption.
- **PRIV-009:** Make required anonymisation/deletion propagate to application records, files, search indexes and derived identifiable data under the approved process.
- **PRIV-010:** Explain that CEREMO cannot recall information already voluntarily sent to an independent provider or external messaging service.
- **PRIV-011:** Support logout from the application and the separately required identity-provider logout/revocation steps.
- **PRIV-012:** Stop treating a browser logout alone as proof that every previously issued bearer token has been revoked; enforce the chosen revocation policy.
- **PRIV-013:** Permit an account-closure request without deleting evidence or financial records that require authorised handling.
- **PRIV-014:** Require provider ownership transfer or a reviewed provider-closure process before closing an account that is the sole provider owner.
- **PRIV-015:** Explain the result of closure and retain a minimal permitted record of the request and decision.
- **PRIV-016:** Restrict backup restores so that deleted/revoked data is not reactivated without the required deletion and revocation reconciliation.

### BUS — original clauses

- **BUS-001:** Create a provider account free after the person's local account meets the applicable requirements.
- **BUS-002:** Assign the first authorised member as owner without granting platform staff privileges.
- **BUS-003:** Create and edit a draft business name, description, business type and base locality.
- **BUS-004:** Manage individual services, service categories, descriptions and coverage areas.
- **BUS-005:** Add indicative price information with GHS currency, pricing unit, conditions and an explicit unknown/quote-needed option.
- **BUS-006:** Support service-specific information rather than requiring identical fields from a caterer, photographer and officiant.
- **BUS-007:** Upload portfolio material with rights confirmation, validation and scanning before public publication.
- **BUS-008:** Add and withdraw owner-approved business phone, WhatsApp and website contacts.
- **BUS-009:** Keep private verification evidence separate from public portfolio and contact information.
- **BUS-010:** Preview the public profile before submission and identify missing required information.
- **BUS-011:** Submit the profile for review and view its decision and required corrections.
- **BUS-012:** Publish only approved content; apply the agreed re-review policy when approved content changes materially.
- **BUS-013:** Let an authorised member stop accepting new enquiries without deleting the provider or its history.
- **BUS-014:** Invite, accept, assign and revoke provider membership through authenticated, bounded workflows that do not grant more authority than the actor may delegate.
- **BUS-015:** Restrict membership management, ownership changes and subscription purchases to the approved membership permissions.
- **BUS-016:** Prevent an owner from accidentally removing the last accountable owner; require verified transfer or reviewed closure.
- **BUS-017:** Remove a member's access to private provider records promptly after revocation, including on subsequent requests made with an otherwise valid login.
- **BUS-018:** Keep provider account management, authorised history, support and billing available after listing entitlement expires.
- **BUS-019:** Prevent duplicate provider accounts or ownership changes from automatically restarting a consumed trial; send disputed duplicates for review.
- **BUS-020:** Route provider closure through a reviewed process that addresses open cases, retained reviews and subscription billing.

### CHK — original clauses

- **CHK-001:** List submitted profiles awaiting assessment with their submission time and relevant completeness information.
- **CHK-002:** Assign or claim review work under staff permissions and prevent conflicting final decisions.
- **CHK-003:** Review the business details, service category, coverage, public contact choices and portfolio rights.
- **CHK-004:** Request specific corrections or limited evidence with a stated purpose.
- **CHK-005:** Record each check's type, scope, result, reviewer, date and applicable expiry.
- **CHK-006:** Approve or reject a submitted profile with a reason and notify the provider.
- **CHK-007:** Accept corrected resubmissions without erasing earlier decisions and their evidence history.
- **CHK-008:** Restrict evidence access, audit sensitive reads and apply the approved evidence-retention period.
- **CHK-009:** Publish an accurate check description or badge only when the recorded result supports it.
- **CHK-010:** Expire or withdraw a check when its validity ends or a reasoned reassessment changes the result.
- **CHK-011:** Keep profile approval, check validity, paid entitlement and review rating independently assessable.
- **CHK-012:** Suspend a provider or service for an authorised safety/policy reason, even during a trial or paid period.
- **CHK-013:** Explain the permitted correction or appeal route without publishing sensitive internal evidence.
- **CHK-014:** Require an authorised reasoned decision before reinstating a suspended listing, followed by current entitlement and check re-evaluation.
- **CHK-015:** Do not sell a passed check or describe a subscribed provider as verified without the necessary assessment.

### SUB — original clauses

- **SUB-001:** Maintain versioned plans with explicit benefits, currency, billing interval and approved price.
- **SUB-002:** Keep a plan unavailable for purchase until its required price, benefits and terms have been approved and configured.
- **SUB-003:** Explain trial eligibility, duration, benefits, end date and post-trial restrictions before publication.
- **SUB-004:** Start one trial at the first approved publication, not at account registration or while a profile awaits assessment.
- **SUB-005:** Grant and save that trial atomically so concurrent publication actions cannot create multiple trials.
- **SUB-006:** Do not promise a trial end when no approved duration is configured.
- **SUB-007:** Provide the initial trial without an automatic charge or required payment method under the proposed launch policy.
- **SUB-008:** Show the owner the actual trial end, paid-through time and permitted current features.
- **SUB-009:** Allow the owner to select a published plan and inspect its accepted price, benefits and renewal/cancellation terms.
- **SUB-010:** Create one fixed-price invoice for the intended purchase and reuse an unresolved invoice where appropriate.
- **SUB-011:** Grant the invoice's paid period once only after verified payment, under subscription/invoice locking.
- **SUB-012:** Preserve unused trial or paid time when a provider pays early; schedule the new period after existing entitlement.
- **SUB-013:** Start a renewal after expiry at the verified payment time, using the approved billing-time policy.
- **SUB-014:** Preserve the monthly anchor and handle shorter months consistently; do not silently lose paid days on repeated renewal.
- **SUB-015:** Notify the provider of approaching expiry through the approved communication policy without promising customer leads.
- **SUB-016:** End active discovery/contact/promotion entitlement at the actual expiry time unless a valid following period exists.
- **SUB-017:** Preserve profile management, billing, support and authorised history after expiry.
- **SUB-018:** Let the owner stop future renewal activity while respecting the remaining accepted paid period.
- **SUB-019:** Make suspension override a current trial, paid period or promotion.
- **SUB-020:** Keep historical plan/invoice terms stable when a new plan version or price is published.
- **SUB-021:** Prevent trial, plan or paid-through values from being arbitrarily edited by a provider or by an unaudited staff shortcut.

### PAY — original clauses

- **PAY-001:** Save the invoice, expected positive amount in integer pesewas, GHS currency and unique merchant reference before starting hosted checkout.
- **PAY-002:** Initialise a supported one-off hosted MoMo/card collection without storing card numbers, CVV or MoMo PINs.
- **PAY-003:** Return the existing payment state when checkout is interrupted or times out; reconcile uncertain attempts before another charge.
- **PAY-004:** Display pending, succeeded, failed, expired or review-required states without allowing the client to choose a financial status.
- **PAY-005:** Treat a browser return as navigation only; it cannot activate a subscription.
- **PAY-006:** Authenticate payment notifications against the raw request body using the provider's current account-specific signing contract before trusting an event.
- **PAY-007:** Save a durable authenticated event receipt before acknowledgement, then process it with retry and deduplication.
- **PAY-008:** Independently verify transaction success, merchant reference, expected invoice, exact amount and currency before applying entitlement.
- **PAY-009:** Match the payment to the authorised provider subscription and accepted invoice; reject cross-provider substitutions.
- **PAY-010:** Apply repeated, delayed or out-of-order notifications without granting duplicate paid periods.
- **PAY-011:** Retain unsuccessful/uncertain payment attempts and their reconciliation result instead of treating a timeout as a confirmed failure.
- **PAY-012:** Open a billing case for mismatched amounts/currencies, duplicate successful collections or unexplained provider states.
- **PAY-013:** Do not grant extra time simply because two collections succeeded for one invoice.
- **PAY-014:** Let the owner view authorised invoices, payment history and evidence of CEREMO subscription collection.
- **PAY-015:** Reconcile pending collections and notify the owner when the financial state is resolved.
- **PAY-016:** Accept a platform billing refund request under the accepted refund policy and record its case reference.
- **PAY-017:** Separate refund request and approval authority for the required finance control.
- **PAY-018:** Cap pending plus completed refunds at the verified collected amount, using locked transactions.
- **PAY-019:** Reconcile an uncertain refund submission before retrying it; show its actual state and reference.
- **PAY-020:** Change or revoke entitlement after refund only through the approved case/policy outcome, with an audit record.
- **PAY-021:** Keep financial documents and reconciliation history for the applicable approved period; account closure must not erase them indiscriminately.

### ADM — original clauses

- **ADM-001:** Create, edit, activate and retire stable occasion-type codes, including the supported festival and ceremony families.
- **ADM-002:** Maintain service-category hierarchies and versioned service-question schemas; reject cycles and preserve schemas used in historical briefs.
- **ADM-003:** Maintain region/district/locality relationships and explicit pilot coverage without fabricating provider supply.
- **ADM-004:** Retire used catalogue records without breaking existing provider, enquiry or review references.
- **ADM-005:** Draft, review, publish, expire and withdraw locality-specific advisories with a source, review date and effective period.
- **ADM-006:** Check authoritative cultural/municipal sources before an advisory is published; do not represent a notice as a permit or legal authorisation.
- **ADM-007:** Manage current terms and policy versions through authorised publication; retain the version accepted in historical records.
- **ADM-008:** Grant and revoke staff permissions under the agreed approval process, with no client-controlled role assignment.
- **ADM-009:** Require the configured MFA and recent-authentication conditions for sensitive staff actions.
- **ADM-010:** Assign support cases and move them through authorised open, in-review, waiting, resolved and closed states.
- **ADM-011:** Record support actions, authorised communication, evidence requests and resolutions with actor and time.
- **ADM-012:** Moderate profile/review reports with reasons, consistent policy and an appeal route independent of subscription tier.
- **ADM-013:** Hide or remove prohibited public content without granting arbitrary access to unrelated private records.
- **ADM-014:** Protect original evidence and decision history while permitting necessary correction or redaction under policy.
- **ADM-015:** Review enquiry spam labels and qualified/unsuitable assessments separately from reported hire outcomes.
- **ADM-016:** Handle account recovery and disputed provider ownership through reviewed, identity-checked cases.
- **ADM-017:** Process privacy requests with authorised identity checks, deadlines, permitted holds and response records.
- **ADM-018:** Restrict support, finance and administrator operations by responsibility; platform-wide administration must not be an implicit provider membership benefit.
- **ADM-019:** Require reasons for overrides and preserve them in audit history. An override cannot manufacture verified payment or alter customer stars.

### REP — original clauses

- **REP-001:** Show a provider its profile-view counts for the requested permitted period.
- **REP-002:** Show recorded contact actions by supported channel without describing them as conversations or sales.
- **REP-003:** Show submitted enquiries and their acknowledged/closed/spam states separately.
- **REP-004:** Show qualified, unsuitable and unclassified enquiries using the documented classification policy and source.
- **REP-005:** Show customer and provider outcome reports separately, preserving unknown and conflicting answers.
- **REP-006:** Show published review average/count and relevant moderation state without exposing private evidence.
- **REP-007:** Show subscription history, trial timing and renewal information to authorised billing members.
- **REP-008:** Prevent a provider from seeing another provider's private reporting or customers' saved preferences.
- **REP-009:** Report platform supply by category/locality, inactive listings, empty results and assessment backlog.
- **REP-010:** Report trial-to-paid conversion, renewal and CEREMO subscription collections/refunds using defined counting periods and deduplicated financial records.
- **REP-011:** Monitor enquiry usefulness and customer return for another category or occasion without claiming an external hire as verified revenue.
- **REP-012:** Report payment mismatches, uncertain attempts, failed notifications, open cases and overdue privacy work to permitted staff.
- **REP-013:** Define each metric's source, time window, exclusions and limitations; do not claim views represent unique people without the required measurement.
- **REP-014:** Use minimal permitted analytics and exclude identifiable details where aggregate reporting is sufficient.
- **REP-015:** Keep optional sponsored impressions separate from ordinary views if sponsored placement is later enabled.

### SYS — original clauses

- **SYS-001:** Validate public request fields before calling business operations, and enforce domain invariants again in the relevant model/application service.
- **SYS-002:** Accept only permitted writable fields; explicitly define whether unknown request properties are rejected or ignored for each contract.
- **SYS-003:** Return explicit safe response records rather than JPA entities, raw tokens, stack traces, SQL errors or private evidence paths.
- **SYS-004:** Validate authentication and perform local account, membership, ownership and permission checks for every protected operation, including non-HTTP entry points.
- **SYS-005:** Recheck time-based entitlement/check validity at the point of action and immediately enforce suspension/revocation policy.
- **SYS-006:** Return consistent, documented error keys, status and field errors, preserving authentication protocol headers on security failures.
- **SYS-007:** Bound pagination, request sizes, uploads, expensive searches and sensitive write rates; provide retry guidance for temporary throttling.
- **SYS-008:** Keep CORS, bearer API authentication and cookie/session CSRF policies consistent with their actual trust boundaries.
- **SYS-009:** Use optimistic concurrency for mutable profiles/preferences and appropriate locks/constraints for financial grants, trial allocation and unique identities.
- **SYS-010:** Make enquiry and financial retries idempotent; reject an idempotency key reused by the same scoped actor/action with different content.
- **SYS-011:** Commit complete business operations atomically. On failure, preserve the previous valid state and prevent orphan records.
- **SYS-012:** Express monetary API/storage values in GHS integer pesewas, store timestamps in UTC and interpret occasion context in `Africa/Accra`.
- **SYS-013:** Use the existing UUID generator for new records and never treat an identifier as proof of authority.
- **SYS-014:** Authorise each upload by owner, purpose, file type and size before issuing access.
- **SYS-015:** Quarantine and validate actual content, scan files and publish only approved public portfolio derivatives.
- **SYS-016:** Remove unnecessary location metadata from public images and keep private review/verification files inaccessible through public URLs.
- **SYS-017:** Treat temporary signed file URLs as credentials, with bounded validity and authorised issuance.
- **SYS-018:** Record notification/delivery work transactionally through a durable outbox when the workflow needs reliable later delivery.
- **SYS-019:** Lease background work, bound retries, deduplicate delivery and surface exhausted failures for authorised recovery.
- **SYS-020:** Run reconciliation, expiry, check-validity and cleanup jobs without making a delayed job the sole protection against expired access.
- **SYS-021:** Apply timeouts and controlled retries to identity, SMS, storage and payment integrations; do not hold a database lock while awaiting hosted checkout.
- **SYS-022:** Keep secrets, sensitive claims, OTPs and private evidence out of source, ordinary logs, URLs and client-visible configuration.
- **SYS-023:** Keep development/test adapters and synthetic data isolated from live users, production realms, storage and payments.
- **SYS-024:** Audit sensitive changes with actor, action, record, server time and permitted trace context; redact secrets and unnecessary personal data.
- **SYS-025:** Restrict audit access and prevent ordinary users from editing or deleting required audit history.
- **SYS-026:** Expose safe health/readiness checks and authorised operational metrics without disclosing internal credentials or customer data.
- **SYS-027:** Back up application, identity and private-file data under the approved recovery policy and verify restoration in an isolated environment.
- **SYS-028:** Reconcile expiry, revocation, privacy deletions and uncertain financial work after recovery before reopening affected operations.
- **SYS-029:** Reuse CEREMO's shared validation/errors/authentication/UUID components and existing wire configuration where their semantics fit.
- **SYS-030:** Preserve strict DDD with ports and adapters: controllers call inbound ports, the core remains framework-independent, and outer adapters handle persistence/integrations.
- **SYS-031:** Verify successful and denied actions, malformed data, cross-account access, retries, rollback, concurrency and integration failure where relevant.
- **SYS-032:** Enforce the existing 100% line and branch coverage rule per application class using combined regular/PostgreSQL execution data, without coverage exclusions added to satisfy the number.
- **SYS-033:** Run configured SonarQube analysis and verify the actual server quality-gate result before reporting a quality pass. Coverage alone does not prove security or correctness.
- **SYS-034:** Maintain a requirements-to-test mapping so that coverage and tests can be traced to observable behaviour.

### LATER — original clauses

- **LATER-001:** Offer labelled sponsored placement only after tier benefits, finite capacity, relevance and allocation rules are approved.
- **LATER-002:** For enabled sponsorship, enforce the same listing/safety/relevance rules, avoid duplicate appearances, preserve ordinary results and report sponsored impressions separately.
- **LATER-003:** Add shared shortlists only after invitation, editing, revocation and privacy rules are defined.
- **LATER-004:** Add additional Ghanaian languages after content, accessibility and customer research.
- **LATER-005:** Add native mobile applications after selecting their framework and delivery scope; the selected Next.js web stack does not choose a native framework.
- **LATER-006:** Add supported automatic card renewal only after consent, mandate, cancellation and payment-failure rules are approved and tested.
- **LATER-007:** Introduce more complex recommendation models or provider integrations only when reliable data and a measured need justify them.

</details>

<details>
<summary>Retained acceptance scenarios — verification requirements, not passing-test evidence</summary>

| Case | Scenario | Required observable result |
| --- | --- | --- |
| CYC-01 | Visitor searches for catering in a supported area without login | Relevant eligible public results or an honest empty state; no registration or customer payment required |
| CYC-02 | Visitor declines tracking and opens an eligible provider's published contact | Permitted contact opens; optional analytics refusal does not block it |
| CYC-03 | Person logs in but has no CEREMO profile | Read reports missing profile without creating one; registration requires explicit current-terms acceptance |
| CYC-04 | Two requests register the same trusted identity simultaneously | One account/link persists; no overwritten account or orphan row |
| CYC-05 | Customer skips all optional onboarding questions | Normal search works; no inferred budget, religion or marketing permission |
| CYC-06 | Customer budgets for catering per guest and separately for photography | Filters use the correct service/unit; unknown or incompatible prices are not falsely comparable |
| CYC-07 | Customer plans a festival, funeral or wedding with several services | The same general journey works; no mandatory wedding-specific fields or guaranteed supply |
| CYC-08 | Customer uses a saved provider whose entitlement has since expired | Current inactive state appears; new restricted contact/enquiry actions are refused safely |
| CYC-09 | Provider is suspended while a paid period remains | Active discovery/contact/promotion stops despite payment; authorised billing/support history remains |
| CYC-10 | Active, appropriately verified customer previews and sends one enquiry | Only selected data reaches the selected provider; immutable snapshot, enquiry ID and durable notification work are saved |
| CYC-11 | The same enquiry request is retried after a lost response | Same operation result is recovered without a duplicate enquiry or notification |
| CYC-12 | Another customer's event/enquiry/file ID is supplied | Access is denied without private disclosure; no cross-account record is attached or modified |
| CYC-13 | Provider acknowledges, but parties have not agreed a service | Enquiry is acknowledged; no booking, invoice for the service or hire claim is created |
| CYC-14 | Provider does not respond or declines outside the app | Customer can search/contact another provider; no fabricated availability or response |
| CYC-15 | Customer and provider negotiate and pay outside CEREMO | CEREMO charges no booking commission and records no verified service payment |
| CYC-16 | Customer reports HIRED and provider reports NOT_HIRED | Both source-labelled reports remain visible to authorised readers; neither overwrites the other |
| CYC-17 | Customer reviews a genuine experience without a CEREMO enquiry/payment | Approved authenticity/evidence workflow applies; lack of platform checkout alone is not a rejection |
| CYC-18 | Paid provider disputes a negative review | Same reasoned moderation/appeal rules apply; subscription cannot buy deletion or stars |
| CYC-19 | New provider has no reviews | Average is unknown and count is zero; eligible provider is not removed by a default five-star-only filter |
| CYC-20 | External notification delivery fails after enquiry commit | Enquiry remains saved; delivery is retried/reported without asking the customer to submit a second enquiry |
| CYC-21 | Customer returns for another occasion | Existing account/saved records work; old occasion dates/budgets do not silently control the new search |
| CYC-22 | Customer opens a complaint and uploads limited evidence | Case reference/status is available to authorised participants; private evidence is not published |
| CYC-23 | Customer clears preferences, withdraws marketing or requests deletion | Each choice has its own recorded effect; required retention/holds are explained and optional marketing stops as required |
| CYC-24 | First listing publication is retried concurrently | One trial is granted with the approved policy/time; no trial starts while awaiting assessment |
| CYC-25 | Provider pays early, and the same success notification arrives repeatedly | Accepted unused entitlement is preserved; invoice grants one additional period only |
| CYC-26 | Checkout times out or collection amount differs from the invoice | Existing reference is reconciled or flagged; no unverified activation or blind duplicate charge |
| CYC-27 | Refund request/approval is retried or exceeds collection | Permissions, refund cap and idempotency prevent excess or duplicate refunds |
| CYC-28 | Sole provider owner requests account closure | Transfer/closure obligations are resolved through the approved process; provider is not left ownerless |
| CYC-29 | Recovery restores a backup predating a deletion or revocation | Relevant deletion/revocation/financial reconciliation runs before affected access is reopened |
| CYC-30 | Required plan/trial/activation policy is absent | Dependent operation fails safely with a usable explanation; no duration, price or permission is invented |

</details>

<details>
<summary>Retained unresolved policy register from version 1.0</summary>

`null` means unapproved or unselected. It must not become an implicit production default. Only the functions that depend on a missing decision are blocked; ordinary permitted public browsing can remain available.

| Decision | Current position | Functions affected |
| --- | --- | --- |
| Pending-account activation | Required contact/other checks and transition policy need approval | ACC-010, ENQ-006, REV-003 |
| Trial policy | Duration, benefits and duplicate-provider assessment remain `null` | SUB-003 to SUB-007, BUS-019 |
| Paid plans | Prices, benefits, limits and live commercial approval remain `null` | SUB-001, SUB-002, SUB-009, PAY-001 |
| Billing terms | Cancellation, refund, tax/invoice treatment and entitlement revocation need approved terms/local review | SUB-018, PAY-014 to PAY-021 |
| Monthly billing time | Exact payment-time source, timezone/anchor convention and end-of-month examples need approval | SUB-012 to SUB-014 |
| Membership and ownership | Member permissions, invitation expiry, sensitive change and ownership-transfer approval need definition | BUS-014 to BUS-017, PRIV-014 |
| Publication changes | Material-edit re-review and temporary public-content behaviour need definition | BUS-012, CHK-007 |
| Verification | Check scope, accepted evidence, badge wording, validity and appeal rules remain unapproved | CHK-004 to CHK-015 |
| Search ordering | Sort choices, ranking weights, unknown-price treatment and new-provider exposure policy need validation | SRC-007 to SRC-013 |
| Enquiry operations | Qualification, participant closure, spam appeal, reopening and reminder rules need definition | TRACK-006 to TRACK-010, REP-004 |
| Review policy | Experience duplicate key, conflicts, evidence threshold, editing/withdrawal and appeal rules need approval | REV-003 to REV-015 |
| Notifications | Production adapters, supported channels, quiet hours, optional reminders and communication categories need approval | NOT-001 to NOT-010 |
| Privacy and records | Applicable grounds, retention periods, deletion/hold process and response deadlines need local review | PRIV-005 to PRIV-016, PAY-021 |
| Payments integration | Confirm PSP onboarding, live channels and actual modern/legacy webhook signing mode; test callbacks and reconciliation | PAY-002, PAY-006 to PAY-013 |
| Recovery and operations | Approve capacity/rate targets and recovery objectives after tests/drills; document staff procedures | SYS-007, SYS-019, SYS-026 to SYS-028 |
| Frontend/session delivery | Selected web stack is documented; screen/routes, session strategy and native framework remain open where specified | ACC-012, ACC-015, PRIV-011, LATER-005 |
| Sponsored placement | Capacity, tier benefits and allocation rules remain `null`; later scope only | LATER-001, LATER-002 |

No launch budget, target date, guaranteed provider leads or measured retention rate has been approved.

</details>

### Editorial and verification record

Version 2.0, 4 November 2026: full functional document and new local guidance reviewed with the installed humanizer. No company text was submitted externally. Grammarly is **pending** for version 2.0: no checker is available in this session, no excerpt was submitted and no score/result exists. This applies equally to the updated local documentation; a local editorial pass is not a Grammarly pass. Any future public/anonymised check must record exactly what was submitted, its version/date, actual result and limited coverage.

Final local verification on 4 November 2026: all file links and Markdown heading anchors in the six affected files resolve; sections 1–16 are sequential; all 14 functional tables have the expected three columns, containing 110 function rows. Document 09 is approximately 3380 whitespace-delimited words, down from 9303 (about 64%). The internal register retains all 373 distinct v1 clause IDs without duplicates, all 30 acceptance scenarios and the policy-decision register. The 26 responsibility groups each map to current sections/engineering owners. SHA-256 comparisons confirm documents 01–08 are unchanged, and the authorised documentation count remains nine. The removed introduction, permissions matrix and diagram remain absent. Reviewed the customer cycle, outside-contact/payment boundary, optional preferences, verification/rating separation and unresolved commercial policies for scope consistency.

These are structural and editorial checks, not proof that 110 functions are implemented or that the preserved acceptance scenarios pass. No application code, dependency, configuration, test, migration, coverage threshold or Sonar result changed. Grammarly verification remains pending.
