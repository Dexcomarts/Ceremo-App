# CEREMO Functional Requirements

## 1. Service discovery

| Function | Required behaviour | Essential business rule |
| --- | --- | --- |
| Browse services | Explore ceremony and event services without signing in. | Customer registration and discovery are free. |
| Browse occasions | Find services for weddings, funerals, naming ceremonies, festivals, faith, community and corporate occasions. | Greater Accra is the initial service area. |
| Select a location | Choose the locality where the service is required. | Match service coverage, not just the provider's office address. |
| Search providers | Find providers by category, locality and current service needs. | Active results require an approved listing and valid trial or paid entitlement. |
| Filter and order results | Refine results by relevant service criteria, comparable prices and approved ordering options. | Unknown prices remain identifiable; five-star-only filtering is not the default. |
| Browse result pages | Continue through a consistent, bounded set of matching providers. | Pagination must not repeatedly show the same provider. |
| Refine an unsuccessful search | Change criteria when suitable providers are unavailable. | Broader areas or different categories require an explicit customer choice. |
| Read local advisories | View relevant ceremony or locality notices with sources and effective dates. | Publish only reviewed, current notices. |

**Acceptance:** An unsigned visitor can find a category/area, inspect eligible results and deliberately revise an empty search. Unknown price/availability and no-review states remain truthful.

## 2. Customer accounts and access

| Function | Required behaviour | Essential business rule |
| --- | --- | --- |
| Register and sign in | Authenticate through Keycloak and create a CEREMO profile with explicit terms acceptance. | Reading a missing profile must not create an account. |
| Accept current terms | Record the accepted terms version and server timestamp. | Changed terms require a fresh decision; acceptance is never preselected. |
| Activate an account | Activate after verified-email confirmation from Keycloak, at registration or by an explicit later request with refreshed authentication. | Missing/unverified email leaves PENDING. Reads do not activate; suspended/anonymised accounts cannot self-reactivate. No provider or staff rights are granted. |
| Manage a profile | View and update permitted personal details and contact information. | Customers control their own profile; identity and permissions are server-controlled. |
| Verify a contact | Send, resend and confirm a purpose-specific verification code. | Codes expire, are single-use and have attempt/resend limits. |
| Recover access | Use Keycloak recovery and reviewed CEREMO recovery for disputed account/contact ownership. | Matching email addresses or phone numbers do not authorise account merging. |
| Resume an interrupted action | Return to an authorised action after login or session expiry. | Sending an enquiry still requires customer confirmation. |
| Enforce account restrictions | Apply account status and permission checks to protected actions. | A new login does not remove suspension or grant provider/staff authority. |

**Acceptance:** Profile reads do not provision accounts. Protected actions enforce the approved account/contact policy and deny foreign records; PENDING is not silently treated as ACTIVE.

## 3. Preferences and occasion planning

| Function | Required behaviour | Essential business rule |
| --- | --- | --- |
| Complete the optional guide | Choose interests, service categories, locality and relevant occasion details. | Every optional question can be skipped. |
| Set a service budget | Record a known budget range, GHS currency and pricing unit. | A per-guest budget and whole-service budget are different values. |
| Manage preferences | View, update or clear saved interests and discovery defaults. | Current search choices override saved defaults; marketing consent is separate. |
| Create an occasion plan | Maintain an optional private plan with available dates, locality and occasion type. | Unknown dates and budgets are valid where the workflow permits them. |
| Add service requirements | Prepare separate briefs for catering, photography, decoration and other required categories. | Each service keeps its own budget and requirements. |
| Maintain a plan | Edit, activate or archive an owned plan; remove unused drafts under retention rules. | Submitted enquiry history is preserved. |
| Reuse a service brief | Prepare an enquiry from an owned plan and preview the information to be shared. | Later plan changes do not alter an already submitted brief. |

**Acceptance:** A customer can skip/reset the guide, keep unknown values and maintain separate service budgets without changing an already submitted enquiry.

## 4. Provider profiles and shortlists

| Function | Required behaviour | Essential business rule |
| --- | --- | --- |
| Inspect a provider | An authenticated ACTIVE customer views the full approved business profile, services, coverage and portfolio. | Anonymous users receive limited summaries only. Private tenant evidence, staff notes, billing and team records remain restricted. |
| Understand pricing | Show indicative prices, currency, units and applicable conditions. | An unknown price is shown as quote-needed, not free. |
| Assess verification | Display the checks performed, their scope, result and relevant validity dates. | Subscription payment cannot buy a passed check or quality guarantee. |
| Assess customer feedback | Display published reviews, responses, average rating and review count. | Providers without reviews have no invented rating. |
| Compare providers | Compare limited summaries freely; compare full profiles after ACTIVE registration. | Do not manufacture comparable prices or availability or expose private tenant data. |
| Save a provider | Add or remove a provider from a private saved list. | Repeated saves create one entry; saving is not a booking. |
| Revisit a saved profile | An ACTIVE customer views current approved information and listing status. | Expired listings lose active contact access; permitted historical reviews remain behind registered access. |

**Acceptance:** Published comparisons exclude private data, show current status and count saved candidates once. Subscription or evidence does not manufacture stars.

## 5. Contact and enquiry management

| Function | Required behaviour | Essential business rule |
| --- | --- | --- |
| Contact a provider directly | An authenticated ACTIVE customer opens an approved phone, WhatsApp or website contact. | Require a registered active account and current provider eligibility; an in-app enquiry, survey or marketing consent is not compulsory. |
| Control contact sharing | Let customers inspect any prepared external message and select what they disclose. | Opening a contact link does not share a private profile automatically. |
| Prepare an enquiry | Select one provider service and describe the relevant need. | Share only the brief and contacts approved by the customer. |
| Submit an enquiry | Save the recipient, submitted brief and confirmation reference, then notify the provider. | Require the applicable active-account/contact checks and current provider eligibility. |
| Recover a submission | Return the original result when the customer retries after an interrupted response. | A retry must not create another enquiry. |
| Track enquiries | Show the customer and authorised provider members their own submitted enquiries and history. | An enquiry's original submitted content is retained. |
| Acknowledge or close | Record provider acknowledgement or authorised closure with a reason. | Acknowledgement is not acceptance of a service contract. |
| Review spam or suitability | Record spam reports and authorised relevance assessments with review routes. | Qualification and spam decisions are separate from hire outcomes. |
| Choose another provider | Start a separate search or enquiry if the customer wants an alternative. | Do not broadcast the original brief or promise a provider response. |
| Continue discussions outside CEREMO | Allow calls, meetings, negotiations, service agreements and payments directly between the parties. | No booking commission, service checkout or calendar reservation is provided. |

**Acceptance:** One confirmed request reaches one eligible service with the approved snapshot. Retries return the original result; failed notification and acknowledgement do not imply a service contract.

## 6. Outcomes, reviews and trust

| Function | Required behaviour | Essential business rule |
| --- | --- | --- |
| Report an outcome | Record whether an enquiry led to a reported hire, no hire, cancellation or an unknown result. | Customer and provider reports retain their source, timestamp and disagreements. |
| Correct an outcome report | Add a participant's revised account while preserving earlier reports. | One party cannot overwrite the other's report. |
| Review a service experience | Submit a rating and account of a genuine experience with a provider. | A CEREMO enquiry or payment is not required proof of the experience. |
| Assess review eligibility | Apply identity, duplicate-experience, conflict and authenticity checks. | Rules must support legitimate repeat experiences. |
| Submit supporting evidence | Provide limited private evidence through the authorised review process. | Never require PINs, OTPs or complete private chat histories. |
| Publish ratings | Calculate average and count from eligible published reviews. | Evidence-reviewed and customer-reported experiences remain distinguishable. |
| Reply to a review | Let an authorised provider member publish a response. | The provider cannot edit the customer's review. |
| Report, correct or appeal | Handle disputed content and author corrections under the approved moderation policy. | Paid and trial providers receive the same fair review treatment. |

**Acceptance:** A genuine outside experience can be reviewed without platform checkout. Conflicting outcome reports remain separate, evidence stays private and a provider cannot veto valid criticism.

## 7. Customer support and privacy

| Function | Required behaviour | Essential business rule |
| --- | --- | --- |
| Report a problem | Open a case about a profile, review, account, platform fault or outside service issue. | CEREMO investigates platform conduct without promising an external-service refund. |
| Track a support case | View the case reference, progress, information requests and resolution. | Participants see only authorised correspondence and evidence. |
| Supply case evidence | Submit relevant documents and respond to authorised information requests. | Internal notes and private evidence are not customer-facing profile content. |
| Request review of a decision | Seek a reasoned review or appeal through the approved process. | Response commitments and remedies require an approved policy. |
| Manage personal information | Correct permitted details, clear optional preferences and control marketing permission. | Preference changes do not silently delete necessary transaction/case records. |
| Exercise privacy rights | Submit and track access, correction, objection or deletion requests. | Identity, permitted retention and any applicable holds must be assessed. |
| Close an account | Request account closure and receive the authorised outcome. | Sole provider owners must resolve ownership/closure obligations first. |
| Log out | End application and identity-provider access through the selected session/revocation process. | Bearer-token validity must follow the actual revocation policy. |

**Acceptance:** Case participants receive authorised progress and reasoned outcomes. Privacy/account closure respects necessary records, holds and provider ownership obligations.

## 8. Notifications and repeat use

| Function | Required behaviour | Essential business rule |
| --- | --- | --- |
| Receive account notifications | Deliver verification, recovery and sensitive-account notices through approved channels. | Keep private evidence and secrets out of messages. |
| Receive service updates | Record and notify relevant enquiry, review and case events. | A failed message does not erase a saved business action. |
| Manage communication choices | Control optional messages and withdraw purpose/channel-specific marketing consent. | Registration or occasion selection does not enrol the customer in marketing. |
| Return to saved information | Reopen permitted saved providers, plans, enquiries and past experiences. | Recheck current listing eligibility and account permissions. |
| Plan another occasion | Start fresh service requirements using editable preferences. | Old dates and budgets must not silently control the new search. |
| Find an additional service | Continue sourcing other categories for the same occasion. | Completing a prior enquiry, survey or review is not a prerequisite. |

**Acceptance:** Committed actions survive message failure. Return visits use editable context; optional communications and new service searches do not depend on completing a prior survey/review.

## 9. Provider business management

| Function | Required behaviour | Essential business rule |
| --- | --- | --- |
| Create a provider account | Prepare a business profile free and establish its accountable owner. | Ownership grants no platform staff privileges. |
| Maintain business details | Manage permitted business descriptions, location and customer-facing profile information. | Publish only approved content; distinguish anonymous summaries from registered profile access. |
| Manage services | Add and maintain service categories, descriptions, coverage and indicative prices. | Service details and pricing units must fit the category. |
| Manage portfolio material | Upload, replace or withdraw business media with rights confirmation. | Public media must pass the required checks; private evidence is separate. |
| Manage business contacts | Select and maintain contact methods approved for registered customer access. | Release only approved stored destinations after ACTIVE-account and provider-eligibility checks. |
| Manage a team | Owner invites members, recipients accept their assigned responsibilities, and the owner may revoke access. | Invitations grant no access before acceptance. Members are limited to their provider business; staff rights and subscription eligibility remain separate. |
| Transfer or close ownership | Complete an authenticated transfer or reviewed provider-closure process. | Do not leave a provider without an accountable owner. |
| Manage enquiry intake | Pause new enquiries and handle authorised existing requests. | Expiry does not erase permitted account, billing or support history. |

**Acceptance:** An authorised member can manage only the assigned business. Published contact/media approval and an accountable owner survive team changes.

## 10. Listing approval and verification

| Function | Required behaviour | Essential business rule |
| --- | --- | --- |
| Submit for assessment | Send a complete provider profile and required evidence for review. | Assessment precedes first publication. |
| Review a submission | Assign authorised staff to check the business, services, contacts and portfolio. | Evidence access is restricted and sensitive reads are auditable. |
| Request corrections | Identify missing or unacceptable information and accept a corrected resubmission. | Retain previous decisions and relevant history. |
| Record a verification check | Save its type, scope, result, reviewer and validity information. | A badge must reflect an actual supported check result. |
| Approve or reject publication | Record a reasoned decision and notify the provider. | Approval and subscription entitlement are separate requirements. |
| Reassess changed content | Review material profile changes and expired or withdrawn checks. | Paid status cannot preserve an invalid verification claim. |
| Suspend or reinstate | Apply a reasoned restriction or authorised reinstatement. | Suspension overrides payment; reinstatement requires fresh eligibility checks. |

**Acceptance:** Checks/publication have reasoned staff decisions and dates. Suspension wins over payment, and reinstatement rechecks eligibility rather than restoring every claim automatically.

## 11. Provider trials and subscriptions

| Function | Required behaviour | Essential business rule |
| --- | --- | --- |
| Maintain subscription plans | Define versioned prices, benefits, periods and commercial terms. | Publish paid plans only after the required commercial approval. |
| Start a listing trial | Grant one trial at the provider's first approved publication. | Registration, resubmission or ownership changes must not restart the trial. |
| Display entitlement | Show trial end, paid-through date and the features currently permitted. | Use actual periods and restrictions, not payment status alone. |
| Select a plan | Let the owner review benefits, accepted terms and the payable invoice. | Price and invoice terms are fixed for that purchase. |
| Renew access | Extend access once after the invoice payment is verified. | Early renewal preserves unused trial or paid time. |
| Calculate the next period | Apply the approved monthly anchor and shorter-month rules. | Renewal after expiry follows the agreed verified-payment-time policy. |
| Handle expiry | Pause active discovery, contacts and eligible promotion when entitlement ends. | Retain authorised management, invoices, reviews and support records. |
| Cancel future renewal | Stop future renewal activity under the accepted terms. | Cancellation does not silently erase the remaining paid period. |
| Notify subscription changes | Communicate trial end, renewal and expiry through the approved channels. | Trial duration, prices, benefits and notice rules require configuration. |

**Acceptance:** First approved publication grants one trial under concurrency. Actual period boundaries govern access, early renewal preserves time and expiry retains authorised history.

## 12. Subscription payments and billing

| Function | Required behaviour | Essential business rule |
| --- | --- | --- |
| Issue an invoice | Create a provider subscription invoice with the accepted price and plan version. | Reuse the applicable unresolved purchase invoice. |
| Start payment | Open supported hosted MoMo/card checkout against a saved invoice and reference. | Collect CEREMO subscription fees only; never customer ceremony-service payments. |
| Show payment progress | Present pending, successful, failed or review-required collection states. | A browser return alone does not establish payment success. |
| Verify collection | Authenticate notifications and verify the transaction against the expected invoice. | Reference, successful status, amount, currency and provider must match. |
| Activate paid access | Apply the purchased subscription period after verified collection. | Each invoice grants one period despite notification retries. |
| Reconcile uncertainty | Resolve interrupted checkout, late responses and pending payment attempts. | Check the existing reference before initiating another uncertain charge. |
| Investigate billing exceptions | Review mismatches and duplicate successful collections through a billing case. | Excess collection does not automatically purchase more time. |
| Provide billing history | Let authorised members view invoices and collection/refund records. | Preserve accepted terms and required financial history. |
| Review and process refunds | Apply authorised request, approval, submission and reconciliation workflows. | Refunds cannot exceed verified collection; entitlement changes require a reasoned policy decision. |

**Acceptance:** Only CEREMO subscription invoices collect funds. Browser returns, retries, duplicate success and uncertain processor outcomes cannot create duplicate grants or uncapped refunds.

## 13. Administration and platform controls

| Function | Required behaviour | Essential business rule |
| --- | --- | --- |
| Maintain catalogues | Manage occasion types, service categories and service-area hierarchies. | Retiring a used record must preserve historical references. |
| Maintain service questions | Version category-specific requirements used in provider information and customer briefs. | Existing submitted briefs retain their original meaning. |
| Publish advisories | Review, publish, expire or withdraw locality-specific notices. | Require an authoritative source and effective dates. |
| Publish policies | Maintain current terms and approved operational/commercial policy versions. | Preserve the versions previously accepted or applied. |
| Manage staff access | Grant and revoke support, finance and administrative permissions. | Apply required approval, MFA and recent-authentication controls. |
| Operate support and moderation | Assign cases, investigate reports, record decisions and process appeals. | Separate private staff notes from participant-visible information. |
| Handle privacy and retention | Process authorised requests, evidence cleanup and documented holds. | Applicable grounds and retention periods require local review. |
| Audit sensitive actions | Record staff decisions, overrides and sensitive data access. | Ordinary account holders cannot alter required audit history. |

**Acceptance:** Only authorised staff can change catalogues/policies, assess cases or access sensitive evidence. Historical accepted versions and audited decisions remain retrievable.

## 14. Reporting and operational oversight

| Function | Required behaviour | Essential business rule |
| --- | --- | --- |
| Report provider visibility | Show the provider its measured views and contact actions by permitted period/channel. | Clicks are not conversations, paid jobs or unique customers. |
| Report enquiry performance | Show enquiry states, qualification and source-labelled outcomes. | Unknown and conflicting reports remain identifiable. |
| Report customer feedback | Show eligible published ratings and relevant moderation status. | Private evidence is excluded from provider analytics. |
| Report subscription performance | Track trials, paid conversion, renewal, collections and refunds. | Use defined periods and deduplicated financial records. |
| Monitor marketplace supply | Identify category/locality coverage, inactive listings and empty searches. | Do not imply supply or measured demand that the records do not establish. |
| Monitor platform operations | Surface assessment/case backlogs, failed delivery, uncertain payments and privacy work. | Staff reporting is permission-controlled; provider data remains scoped. |
| Recover service operations | Resume failed background work and reconcile data after outages or restores. | Required expiry, revocation, financial and privacy controls must remain effective. |

**Acceptance:** Reports distinguish views, contacts, qualified enquiries, verified subscription collections and source-labelled service outcomes. Restored/retried work preserves access, financial and privacy controls.

## 15. Release scope and decisions

| Area | Required position before release |
| --- | --- |
| Customer access | Free catalogue/summary discovery; full profiles, detailed reviews and contacts require free ACTIVE registration. Private tenant access requires accepted membership and the operation's permission. |
| Provider revenue | Subscriptions only. Trial length, prices, benefits, cancellation/refund terms and invoice/tax treatment require approval. |
| Listing and trust | Verification scope, re-review, review evidence, duplicate-experience and appeal policies require approval. Payment cannot buy stars or passed checks. |
| Search and communications | Approve ranking, price comparability, reminder and production-channel policies; keep optional marketing separate. |
| Account and data controls | Apply verified-email activation; resolve team/ownership permissions, recovery, privacy, retention and closure policies. |
| Payment integration | Confirm merchant onboarding, supported channels, signing mode, billing dates and reconciliation/refund procedures. |
| Later features | Labelled sponsorship, shared shortlists, additional languages, native apps, advanced recommendations/integrations and automatic card renewal need separate scope approval. |
| Excluded operations | No booking commission, service checkout, escrow, wallets, provider payouts, quote acceptance, calendar holds or guaranteed outside-service refunds. Ticketing, donations, lending, permits and marriage registration remain outside scope. |

## 16. Engineering and review references

| Responsibility | Reference |
| --- | --- |
| Product priorities and business model | [Product requirements](02-product-requirements.md) |
| Detailed API, security, payment and operational rules | [Technical requirements](03-technical-requirements.md) |
| Customer/provider workflows and failure paths | [User journeys](04-app-flow-and-user-journeys.md) |
| Data ownership, states and integrity constraints | [Database architecture](06-database-architecture.md) |
| Build sequence and acceptance work | [Implementation plan](07-implementation-plan.md) |
| Keycloak, SonarQube, JaCoCo and quality verification | [Development and quality](08-development-and-quality.md) |
| Source research, original detailed clauses, coverage mapping and editorial status | [Functional specification research record](../.github/research/application/customer-lifecycle.md#professional-specification-method-and-rewrite-4-november-2026) |

## 17. End-to-end release acceptance

| Cycle | Required demonstrated result |
| --- | --- |
| Visitor to contact | Free relevant summary discovery, explicit registration/activation before full profiles and contact, and return to the selected provider without compulsory tracking or marketing. |
| Account to enquiry | Explicit terms and approved activation/contact checks; optional plan; previewed sharing; one retry-safe enquiry to an eligible provider. |
| Enquiry to next occasion | Acknowledgement/closure without invented hiring; optional source-labelled outcomes/review/support; independent next-category/occasion search. |
| Provider to publication | Free preparation, scoped membership, assessment, one approved-publication trial and truthful check/review claims. |
| Invoice to renewed access | Accepted terms, verified collection, one non-overlapping grant, recoverable uncertainty and retained billing/support after expiry. |
| Report to resolution | Restricted evidence, reasoned moderation/billing/privacy decisions and applicable appeal/retention handling. |

All fourteen modules are required capabilities for the complete MVP; implementation remains staged under [document 07](07-implementation-plan.md). Sponsored placement and other later features retain the separate approval requirement. The catalogue is a requirements baseline, not a claim that the functions are implemented.

Use the [PRD's measures and validation gates](02-product-requirements.md#7-outcomes-and-measurement) for product assessment. Numeric thresholds, policy details and named accountable owners remain open where recorded. No function may infer them from a library default.

Editorial verification: functional catalogue version 3.4, 5 November 2026; Grammarly pending. The original 110 function rows are retained; local verification is recorded in the [suite research entry](../.github/research/product/documentation-suite.md).


