# CEREMO User Journeys

Version 3.4 · 5 November 2026 · Experience requirements draft

## 1. Free customer discovery

The customer can search and compare limited provider summaries before creating an account. Opening a full profile, detailed reviews or contact requires a free ACTIVE account. Private tenant records additionally require accepted membership and the operation's permission.

| Step | Customer action | Required response and decision |
| --- | --- | --- |
| 1. Start | Browse a service category or occasion family. | Offer all occasion families in the catalogue; make Greater Accra coverage explicit. |
| 2. Describe the need | Select a service and locality; optionally supply a date or unit-based budget. | Match provider service coverage. Unknown dates/budgets remain valid; do not claim availability. |
| 3. Compare | Inspect eligible provider summaries and refine criteria. | Show approved name, category, service area, thumbnail, indicative price/unit and check/rating summaries; omit contacts and private records. No invented new-provider score. |
| 4. Inspect | Select a full provider profile or detailed reviews. | If unsigned, explain free registration and return after verified-email activation. Deny missing, PENDING, SUSPENDED or ANONYMISED local accounts. |
| 5. Choose contact | Open an approved phone, WhatsApp, email or website destination. | Require the ACTIVE account and current provider eligibility. No structured enquiry or optional analytics/marketing consent is required. |
| 6. Save or enquire | Sign in to save providers, prepare a plan or submit an enquiry. | Establish the local account and apply its status/contact policy; return to the intended authorised action. Never auto-submit after login. |
| 7. Continue directly | Discuss, meet, negotiate, contract and pay the provider. | Explain that the parties manage their service arrangement outside CEREMO. No calendar hold, accepted quote or service-payment confirmation is created. |
| 8. Follow up | Optionally report an outcome, review an actual experience, raise a concern or find another service. | Keep reports source-labelled. A saved profile or contact action is not a booking. |

After login, GET the self profile without provisioning it. If absent, the customer explicitly accepts the current CEREMO terms and registers. A trusted verified-email claim permits ACTIVE; otherwise show PENDING and direct the person to Keycloak's verification process. After verification, obtain a refreshed token and explicitly activate. Display restricted status without offering self-reactivation. Account activation does not grant provider privileges.

After registration, offer an optional guide covering purpose, interests, categories, locality, date and budget with its service/unit. Provide Skip, Not sure, Back, Edit and Reset. The guide need not create an occasion plan. Current search choices override saved defaults; marketing consent remains a separate decision.

If the customer finds no suitable provider, retain the chosen criteria and offer explicit changes. Never silently widen the area, substitute a different category or populate results with unrelated subscribers.

**Acceptance:** an unsigned visitor can search and compare limited summaries but cannot open full profiles, detailed reviews or contact destinations. A customer can skip every optional guide question. Re-entering after login preserves permitted context and requires confirmation before sharing a brief. Registration never exposes another tenant's team, evidence or billing.

## 2. Provider registration and trial

| Stage | Provider action | Platform response and retained information |
| --- | --- | --- |
| Account | Sign in through Keycloak, activate the account after verified email and create a provider business free. | Create its tenant workspace and retain the accountable owner. Workspace creation starts no listing trial and grants no staff privileges. |
| Team | Owner invites a registered account; the intended recipient accepts. | An invitation grants no access before acceptance. Check current account status and business membership on every protected operation. Owners may revoke access; tenant suspension blocks invitations and acceptance. |
| Preparation | Add services, coverage, prices/units, approved business contacts and portfolio material. | Validate permitted fields, contact approval and media rights. Contacts are available only through registered access; private account contacts are never copied automatically. |
| Assessment | Submit the listing and required evidence. | Record review assignment, corrections and reasoned decisions. Payment cannot determine a check result. |
| First publication | Publish the approved listing. | Atomically grant its single trial using the approved policy. Missing duration/benefit configuration prevents an invented offer or end date. |
| Trial use | Respond to relevant enquiries and inspect performance. | Show trial end, benefits and the paid option. Distinguish views, contact actions, qualified enquiries and reported outcomes. |
| Purchase decision | Subscribe or allow the trial to end. | Explain the actual offer and expiry consequences. Trial length, prices and limits remain unapproved. |

The proposed trial requires no payment method or automatic charge. Resubmission, business ownership changes and duplicate publication requests must not restart it.

Suspension overrides trial, payment and promotion. Entitlement expiry pauses active discovery/contact access while preserving authorised management, billing, support and history. Permitted historical profiles retain an inactive label and review accountability; expiry must not appear to be a failed check.

**Acceptance:** concurrent publication requests grant one trial. An unapproved or suspended listing cannot become discoverable through payment. The owner can still reach retained account and billing records after expiry.

## 3. Provider subscription and payment

### 3.1 Purchase and recovery

| Step | Responsible party | Required result |
| --- | --- | --- |
| Choose offer | Authorised provider owner | See published price, benefits, period, renewal/cancellation terms and restrictions before purchase. |
| Create invoice | CEREMO | Save accepted terms and expected amount/currency. Reuse the applicable unresolved purchase invoice. |
| Start checkout | CEREMO and payment processor | Persist the payment/reference before requesting hosted authorisation. Keep external network work outside the database transaction. |
| Authorise | Provider | Complete a supported hosted MoMo/card payment. Closing the browser or returning to CEREMO does not prove success. |
| Receive notification | CEREMO | Authenticate the raw notification, durably record it and acknowledge receipt. |
| Verify collection | CEREMO | Verify the reference, successful status, amount, currency and intended invoice/provider. |
| Grant period | CEREMO | Lock the required invoice/subscription records and grant the invoice period once. |
| Confirm status | Provider | Read authoritative payment and entitlement status. Show pending or review-required results honestly. |
| Resolve uncertainty | CEREMO/finance | Reconcile the existing reference before another uncertain charge. Investigate duplicate success or mismatches through a billing case. |

The proposed launch method is a new one-off payment for each manual monthly renewal. Ghana MoMo automatic debit is not part of this design. Payment integration and merchant access remain release work.

### 3.2 Entitlement states

| Current condition | Trigger | Access result |
| --- | --- | --- |
| No initial publication | Approved first publication and configured trial | Trial access begins once. |
| Current trial | Verified early purchase | Preserve unused trial time; schedule paid access afterwards. |
| Current paid period | Verified renewal | Extend after the existing paid-through boundary without overlapping paid periods. |
| Expired access | Verified invoice payment | Start the new period at verified payment time. |
| Trial/paid end reached | No further current entitlement | Pause restricted discovery/contact/promotion features. |
| Any access period | Provider suspension | Restrict publication regardless of payment; preserve authorised billing/support handling. |
| Current paid period | Cancellation | Preserve accepted remaining access unless a reviewed refund/revocation applies. |

Subscription display states are `INACTIVE`, `TRIALING`, `ACTIVE` and `EXPIRED`. Derive access from actual timestamps, revocations and provider status. Trial and a future paid period can coexist; a displayed subscription status alone is insufficient.

An invoice grants one period. Additional successful collections open billing review rather than purchasing time automatically. Refunds concern CEREMO subscription charges, not outside ceremony-service payments.

**Acceptance:** repeated, late and reordered payment events cannot duplicate access. Interrupted checkout returns to the original payment. Early renewal, expiry, month-end clamping and cancellation preserve the agreed period.

## 4. Enquiries and outcomes

A signed-in customer selects one provider service, prepares a brief and previews the exact information/contact details to share. The brief can originate from an owned occasion plan, but a plan is optional. Confirm current provider eligibility and the applicable account/contact checks at submission.

`POST /api/v1/enquiries` saves the submitted snapshot and confirmation reference. Later changes to preferences or plans do not alter that snapshot. Notification failure does not remove the enquiry; delivery recovery is handled separately.

| State or record | Permitted meaning and next action |
| --- | --- |
| NEW | A submitted request awaiting action. The provider may acknowledge; an authorised participant may close with a reason. |
| ACKNOWLEDGED | The provider acknowledged the request. No service contract or availability commitment has been accepted. |
| CLOSED | An authorised participant ended the enquiry workflow. Closure does not establish a hire or payment. |
| SPAM | A recorded abuse decision subject to the approved review process. It does not replace a relevance assessment. |
| Outcome report | A participant reports UNKNOWN, HIRED, NOT_HIRED or CANCELLED, with reporter and time. Keep later corrections and conflicting reports. |

External conversations are not monitored. Even matching customer/provider hire reports are not CEREMO-verified bookings. Choosing another provider creates a separate request only after customer approval; the original brief is never broadcast.

**Acceptance:** retries return the original submission; another account cannot read or attach the customer's private plan. Provider acknowledgement, enquiry closure and reported hiring remain separate facts.

## 5. Reviews and reports

| Stage | Required behaviour |
| --- | --- |
| Describe experience | An authenticated customer identifies the provider, service category and approximate service date, then declares an actual experience. A CEREMO enquiry or payment is not required. |
| Assess eligibility | Apply the approved identity/contact, authenticity, conflict and duplicate-experience rules. Legitimate repeat experiences need a fair policy. |
| Supply evidence | Accept limited, authorised private evidence. Never request PINs, OTPs or complete private chat histories. |
| Publish | Show rating, published-review count and CUSTOMER_REPORTED or EVIDENCE_REVIEWED status. Evidence review is not a payment or quality guarantee. |
| Respond/report | Allow the provider to reply or raise a concern without editing the customer's review or vetoing legitimate criticism. |
| Moderate/appeal | Apply equal rules across subscription levels; retain reasons, history and appeal outcomes. |
| Report other misconduct | Accept profile and outside-service concerns. Staff may investigate, correct, moderate or suspend under the approved policy. |

CEREMO must not promise external deposit recovery, a guaranteed refund or a replacement provider. Subscription expiry does not erase legitimate criticism.

**Acceptance:** a genuine outside experience can enter the review process; a paying provider cannot suppress an unfavourable eligible review. Private evidence remains absent from public and ordinary provider responses.

## 6. Errors and recovery

The codes below are planned contract requirements, not evidence of implemented endpoints. Error payloads use the existing shared `ApiProblemResponse` and its `key` field.

| Condition | Required result | Recovery |
| --- | --- | --- |
| Skipped guide/unknown answer | Normal browsing remains available. | Edit optional information later. |
| No match | Honest empty result with active criteria. | Customer chooses which criterion to change. |
| Unknown/incomparable price | Quote-needed or non-comparable indication. | Confirm scope and unit with the provider. |
| Expired/suspended listing | Restrict active contact/enquiry/promotion. | Explain the status and allow a new relevant search. |
| Missing required contact verification | 403; planned key PHONE_UNVERIFIED. | Complete the approved verification; browsing continues. |
| Invalid/expired token | 401; planned key INVALID_TOKEN. | Resume through the selected session flow without repeating an unconfirmed submission. |
| Excess requests | 429 with Retry-After. | Retry after the stated delay. |
| Missing commercial configuration | 422; planned key BILLING_NOT_CONFIGURED. | Staff publish the approved configuration. |
| Stale editable record | 409; planned key STALE_VERSION. | Read current data and review the edit before resubmission. |
| Checkout timeout | Return/read the existing pending payment. | Reconcile that reference. |
| Billing mismatch/extra collection | REVIEW_REQUIRED and a billing case. | Finance reviews without duplicate entitlement. |
| Expired paid access | 403; planned key PLAN_REQUIRED for restricted features. | Renew or use retained management/history. |
| Rejected evidence/upload | Safe reason without private-file exposure. | Correct, resubmit or appeal. |
| Inaccessible private record | 404 under the private-record disclosure policy. | Return to an authorised list. |

Keep recoverable form values and record references. Distinguish a saved action from a failed notification and an unknown payment from a failed payment.

## 7. Frontend connections

The [Next.js stack](05-ui-ux-design-brief.md) is selected. Screen names, routes, navigation and native mobile technology remain `null`; no client route is selected by naming a backend connection.

| User action | Planned backend connection | Design completion criterion |
| --- | --- | --- |
| Browse/search | GET /api/v1/providers and catalogues | Visible criteria, bounded results, loading and no-match handling. |
| Sign in | Keycloak OIDC | Safe return to the authorised intended action. |
| Manage guide | GET/PUT/DELETE /api/v1/users/me/preferences | Skip, unknown, edit/reset and separate marketing controls. |
| Save a provider | /api/v1/users/me/saved-providers | Idempotent save/remove and current listing status. |
| Plan services | /api/v1/events and /api/v1/events/{eventId}/services | Private editable briefs with date/unit context. |
| Inspect profile/reviews | /api/v1/providers/{providerId} and its reviews | Require ACTIVE registration, preserve the selected provider through login, and show readable information and distinct inactive states. |
| Contact or enquire | Provider contact-actions; /api/v1/enquiries | Require ACTIVE registration and provider eligibility; return an approved destination or customer-confirmed shared snapshot. |
| Review/report | Provider review and review-report endpoints | Actual-experience declaration, privacy and appeal route. |
| Manage business | Provider services, contacts, members and verification | Ownership-scoped edits and assessment decisions. |
| Renew/check payment | Subscription invoices, invoice payments and payment reads | Terms before purchase; persistent pending/recovery state. |
| Seek support | /api/v1/support-cases | Case reference, authorised progress and escalation. |

Validate loading, offline, empty, malformed-input, permission and recovery states before client implementation. The complete function catalogue is in [document 09](09-functional-requirements.md); API contracts are in [document 03](03-technical-requirements.md).

Editorial verification: version 3.4, 5 November 2026; Grammarly pending.
