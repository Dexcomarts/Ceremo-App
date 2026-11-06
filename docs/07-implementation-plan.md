# CEREMO Backend Implementation Plan

Version 3.4 · 5 November 2026 · Milestones and release acceptance

## 1. Milestones

The plan delivers a usable, supportable provider-discovery product, then tests actual provider purchase and renewal. Work is sequenced by dependency; a completed foundation is not a launched marketplace.

| Milestone | Tasks | Exit evidence | Current position |
| --- | --- | --- | --- |
| MVP foundation | BE-T01, UM-T01, UM-T02, OP-T01 | Trusted account boundary, tested schema, usable catalogues and restricted case/audit operations. | Account/status/staff checks, persistent logout, provider-business ownership/teams and transactional audit exist. Catalogues, cases and remaining account/privacy/contact work remain open. |
| Provider discovery | PR-T01, PR-T02, SB-T01, DS-T01 | Approved eligible supply, one trial per first publication and useful service/area matching. | Planned; offer/trust/ranking policies required. |
| Customer contact and trust | EN-T01, RV-T01 | ACTIVE-account profile/contact access, customer-approved enquiries and fair genuine-experience review handling. | Route gate implemented; provider feature and evidence/appeal policy remain planned. |
| Provider billing | PY-T01, SB-T02 | Verified manual renewal, single period grant and recoverable billing exceptions. | Planned; merchant access and commercial terms required. |
| Polish and operations | OP-T02; DS-T02 optional | Working notices, support/privacy operations and honest provider insights. | Planned; sponsorship can remain disabled. |
| Deployment and pilot | BE-T02 | Engineering/operations review passed; pilot measures collected under approved rules. | Not ready; no launch date or budget approved. |

The recorded 22 October run passed 457 regular and 33 PostgreSQL tests with combined 100% line/branch/instruction coverage. Preserve the per-class line/branch rule. This dated result does not establish today's build, a Sonar server gate, live Keycloak/JWKS or production readiness. [Verification record](08-development-and-quality.md#87-full-coverage-completion).

Frontend delivery is separate; its [stack is selected](05-ui-ux-design-brief.md). No calendar estimate or effort figure is supplied without a team estimate.

The MVP uses provider subscriptions only. Do not implement booking commission, quote acceptance, customer service checkout, provider payouts or calendar holds. [ADR 0006](03-technical-requirements.md#116-provider-subscriptions-and-discovery).

## 2. Foundation

### BE-T01: Development services and quality checks

**Outcome:** Establish a reproducible build and checks that can block an unsafe release.

**Layer:** build/configuration. **Dependencies:** none.

**Files:** service files under `src/docker`, the root `docker-compose.yml`, Gradle configuration, test suites, JaCoCo/SonarQube configuration and environment documentation. The current local Keycloak/SonarQube services use their development databases. Dedicated production databases/users remain separate work.

**Acceptance:** JDK 25 builds the starter; persistent local services have health checks; PostgreSQL Testcontainers integration tests run separately; combined JaCoCo XML reaches SonarQube; required quality failures stop CI. The suites and coverage/scanner tasks already exist; verify report freshness, server analysis and CI enforcement before treating this task as complete. See [development setup](08-development-and-quality.md).

### UM-T01: User and catalogue migrations

**Outcome:** Persist the customer identity and reference catalogues needed by all subsequent features.

**Packages:** `account`, `administration`. **Dependencies:** BE-T01.

**Files:** Flyway migrations, `User`, identity/role repositories and JPA mappings; occasion/category/locality/advisory models and catalogue resources. Follow [schema](06-database-architecture.md) table definitions and existing root package.

**Acceptance:** migrations work on empty and supported prior-release PostgreSQL databases. Existing issuer/subject uniqueness remains intact; future contact uniqueness follows the approved account policy. Catalogue hierarchy cycles are rejected; synthetic seed data covers the occasion families and Greater Accra pilot areas. No credentials or scraped provider profiles are seeded.

### UM-T02: Keycloak and application permissions

**Outcome:** Give account actions a trusted HTTP boundary without confusing login with business permission.

**Packages:** `account`, `logout`, `tenant`. **Dependencies:** UM-T01.

**Implemented:** self-account HTTP and verified-email activation; AccountService active/staff checks; persistent LogoutService session revocations; TenantService business ownership, team invitation/acceptance/revocation, administrative suspension and transactional audit. Reuse the existing security chains, trusted identity reader, errors, UUIDs, paging and transaction wiring. Additive migrations preserve the initial account schema. See [ADR 11.8](03-technical-requirements.md#118-provider-business-tenants-account-access-and-logout).

**Remaining:** live SMTP/MFA/refresh/key-rotation tests, request/resource limits, contact/recovery and account restriction administration, ownership-transfer policy, frontend invitation/session integration and required release gates. New provider profiles must reference their unique tenant; database membership is the only business-team authority. A workspace's ACTIVE state does not approve publication or start a trial.

**Acceptance:** public browsing works without a token; wrong issuer/audience/type/time claims fail; local suspension and provider/staff roles are checked. Contact verification does not become a second login issuer. Test a real issuer/JWKS flow, rotation, foreign records, logout and required MFA as well as mocked principals.

### OP-T01: Cases and audit foundation

**Outcome:** Make platform concerns traceable and actionable without promising outside-service compensation.

**Package:** `administration`. **Dependencies:** UM-T02.

**Files:** `SupportCase`, `SupportCaseService`, `SupportCaseResource`, audit repositories and staff permission tests. Case types cover profiles, reviews, subscriptions, recovery and reported outside service issues.

**Acceptance:** customers/providers see only authorised cases; sensitive evidence access and staff actions are audited; financial approval can be separated from the requester. No outside-service refund or replacement guarantee is implied.

## 3. Provider supply and entitlements

### PR-T01: Provider accounts and services

**Outcome:** Let a business prepare an accurate, accountable listing free of charge.

**Package:** `providermanagement`. **Dependencies:** UM-T02.

**Files:** `Provider`, `ProviderService`, `ProviderResource`, `ProviderJpaEntity`, repositories/mappers, provider member/service/area/contact models, `RestCreateProviderRequest` and response records.

**Acceptance:** registration/profile preparation are free; membership protects every child record; owners explicitly approve published business contacts and portfolio rights. Prices have a unit and can be unknown. No payout/subaccount setup is required.

### PR-T02: Profile assessment and safe media

**Outcome:** Make public evidence and assessment claims defensible before discovery.

**Packages:** `providermanagement`, `administration`. **Dependencies:** PR-T01, OP-T01.

**Files:** `ProviderCheck`, assessment service/resources, `FileStorage` port, S3 adapter, file completion/scanning pipeline and public profile mapper.

**Acceptance:** paid status cannot approve a profile or check. Passed checks have reviewer/date/scope and expiry where relevant. Private evidence is never public. Unsafe files cannot be published. Approval and first active publication are separate until trial entitlement exists in SB-T01.

### SB-T01: Plans, trial and publication access

**Outcome:** Connect approved publication to one clearly defined trial and current listing access.

**Package:** `subscriptionmanagement`. **Dependencies:** PR-T02.

**Files:** plan/subscription/invoice/period models and migrations, `SubscriptionService`, repositories/JPA mappings, entitlement checks and first-publication operation.

**Acceptance:** one provider has one subscription record and at most one initial trial. Concurrent approval/publication cannot grant two trials. Trial starts on first approved publication, using configured duration; missing rules fail clearly. Actual period dates and suspension determine access. Expiry preserves account/billing/support/review history while pausing active discovery/contact. Prices and tier limits remain unconfigured until approved.

## 4. Discovery, contact and reviews

### DS-T01: Search, preferences and occasion briefs

**Outcome:** Help customers choose relevant candidates without compulsory profiling.

**Packages:** `discovery`, `eventmanagement`. **Dependencies:** PR-T02, SB-T01.

**Files:** `DiscoveryService`, `PreferenceService`, `CustomerPreference`, `SavedProvider`, `EventService`, `RequestedService`, repositories/resources and API records. Add explicit ordinary-result eligibility and versioned ranking policy.

**Acceptance:** category/coverage/entitlement filters run on the server; no default five-star-only gate; new providers have null rating. Customer can browse without the guide, skip unknowns, edit/reset preferences and use explicit filters over saved defaults. Unit-based budgets are not silently compared with incompatible prices. Private needs stay private; empty results are honest. Test expiry during cached browsing.

### EN-T01: Business contact and structured enquiries

**Outcome:** Make contact useful while preserving consent, provenance and retry safety.

**Package:** `enquirymanagement`. **Dependencies:** DS-T01, OP-T01.

**Files:** `Enquiry`, `EnquiryOutcome`, `EnquiryService`, `EnquiryResource`, contact-action recording, snapshot mappings and rate-limit/idempotency checks.

**Acceptance:** full profiles, detailed reviews and approved contact targets require a live ACTIVE account; anonymous summaries never carry those details. Optional tracking/marketing consent is separate. Provider eligibility is rechecked. Enquiries go to one eligible service and carry only approved brief/contact fields. NEW/ACKNOWLEDGED/CLOSED/SPAM does not confirm hiring. Outcome reports retain actor/source and conflicting accounts. Retries do not duplicate enquiries or inflate contact counts. Private tenant operations also require accepted membership and the operation's role. No in-app chat/quote/payment is required.

### RV-T01: Reviews and moderation

**Outcome:** Support credible experience feedback independently of payment.

**Package:** `reviewmanagement`. **Dependencies:** EN-T01, PR-T02, OP-T01.

**Files:** `Review`, `ReviewService`, `ReviewResource`, evidence/report mappings, provider reply operation, moderation/appeal resources and aggregate rating queries.

**Acceptance:** a genuine external service experience can be reviewed without CEREMO checkout. CUSTOMER_REPORTED and EVIDENCE_REVIEWED are distinct. Document/test duplicate-experience, conflict and moderation policy before publishing reviews. Provider cannot veto valid criticism; payment cannot buy stars or favourable treatment. Show published average/count accurately and retain review accountability after expiry. Evidence is private.

## 5. Provider billing

### PY-T01: Invoice checkout and reconciliation

**Outcome:** Collect CEREMO subscription fees with recoverable, auditable financial results.

**Package:** `paymentmanagement`. **Dependencies:** SB-T01, OP-T01.

**Files:** `Payment`, `PaymentService`, `PaystackClient`, `PaymentResource`, signed-webhook handler, durable receipt/reconciliation jobs, `RefundService` and billing API records.

**Acceptance:** only provider subscription invoices accept payments. Verify signature, reference, success, amount/currency independently of browser returns. Lock invoice/subscription, reconcile timeouts, deduplicate callbacks and preserve late successes. Extra collections open cases rather than grant extra time. Independent refunds are capped and uncertain submissions checked before retries. Test restart/outage recovery. No commission, split or service-payment allocation code.

### SB-T02: Manual renewal and provider insights

**Outcome:** Demonstrate what providers purchased and whether renewal delivers value.

**Package:** `subscriptionmanagement`, with aggregate insights in `discovery`. **Dependencies:** PY-T01, EN-T01, RV-T01.

**Files:** subscription/invoice resources, `createInvoice(...)`, `startInvoicePayment(...)`, paid-period grant, cancellation, expiry/reminder operations and `ProviderInsightsService`.

**Acceptance:** one verified invoice grants one non-overlapping paid period. Early renewal preserves unused trial/paid time; expired renewals, short months and leap years work. Cancellation preserves accepted access. Insights distinguish views, contacts, qualified enquiries and source-labelled outcomes; trials are not paying subscribers. Manual MoMo/card renewal does not claim auto-debit.

## 6. Polish and release

### OP-T02: Notifications, privacy and operational reporting

**Outcome:** Operate notifications, privacy work and support without losing business records.

**Packages:** `notification`, `administration`. **Dependencies:** EN-T01, RV-T01, SB-T02, OP-T01.

**Files:** `NotificationService`, `SmsClient`, `HubtelSmsClient`, consent/privacy resources, outbox/retention workers, moderation and billing dashboards/API reports.

**Acceptance:** business changes and outbox insertion are atomic; retries deduplicate; failed SMS does not undo a business record. Marketing consent is separate from preferences/terms. Exports are authorised/private, sensitive evidence is minimised and restores respect deletion/consent changes. Surface failed jobs, abuse, empty results and billing/moderation backlog.

### DS-T02: Sponsored placement, optional

**Outcome:** Test additional paid visibility only after ordinary relevance works.

**Package:** `discovery`. **Dependencies:** DS-T01, SB-T02, RV-T01.

**Files:** `ProviderPromotion`, eligibility/allocation policy, owner settings resource and search response fields.

**Acceptance:** only relevant approved providers with current tier benefits enter the labelled Sponsored section. Define finite inventory, stable rotation/pagination, deduplication and limits first. No purchased rating/check outcome or guaranteed first position. Separate boosts/pay-per-lead checkout is not added. Ordinary search works while promotion is disabled.

### BE-T02: Production setup and supervised pilot

**Outcome:** Release within proven operational capacity and collect evidence before expansion.

**Layer:** deployment/operations. **Dependencies:** BE-T01, UM-T01, UM-T02, OP-T01, PR-T01, PR-T02, SB-T01, DS-T01, EN-T01, RV-T01, PY-T01, SB-T02, OP-T02. DS-T02 is optional and can remain disabled.

**Files:** production container/configuration, deployment/restore runbooks, secret binding, alerts and release checks. Configure persistent secured Keycloak separately from the API. Test HTTPS, strict hostnames, admin restrictions, backups, migrations, capacity, failures and key rotation.

**Acceptance:** development fixtures cannot authenticate to production; required tests/coverage/quality gates pass; application/Keycloak restore and PSP reconciliation are demonstrated. Live billing stays disabled without approved prices, terms and PSP/privacy arrangements. Run the [PRD pilot](02-product-requirements.md): 30 qualified enquiries plus trial/paid-renewal observation, not 30 deposit-confirmed bookings. Report uncertainty, costs and failures before expanding.

## 7. File and naming rules

Follow [writing conventions](../AGENTS.md#writing-and-naming). Java classes use PascalCase, fields/JSON camelCase and SQL singular snake_case. Keep REST records separate from domain and JPA models. Introduce interfaces between features where needed; do not share repositories arbitrarily.

Use ordered unique Flyway names such as `V20261019000001__create_user_tables.sql`; never edit applied migrations. Unit/API tests use `*Test`; use `*IntegrationTest` under `src/integrationTest`. Prioritise ownership, entitlement races, review independence, payment failures and date boundaries over getter tests.

Record implemented behaviour and actual checks as tasks finish. Proposed endpoints, Docker files and Gradle tasks must not be documented as already available. No source code is renamed by this plan.

## 8. Delivery controls and review gates

| Control | Required evidence before completion |
| --- | --- |
| Ready to implement | Current CEREMO/shared/wire reuse check; relevant source evidence; resolved policy/input contract for the step. The user writes code unless the current request authorises edits. |
| Behaviour complete | Success, denied access, invalid input, meaningful failure and retry behaviour demonstrated for the capability. |
| Persistence complete | Migration/constraint, complete transaction rollback and relevant race tests on disposable target PostgreSQL. |
| Quality complete | Required suites and per-class 100% line/branch gate pass on one compiled revision; required Sonar gate is observed, not inferred from upload. |
| Ready for exposure | Current terms/policies, support/finance owner, secrets, dependency monitoring and recovery procedure reviewed for the feature. |
| Ready to expand | Product usefulness, paid renewal and operating costs assessed against pre-agreed pilot criteria. |

The account HTTP and verified-email activation increment is implemented and tested; see [the current verification record](08-development-and-quality.md#88-account-http-and-email-activation). The next discovery milestone needs provider onboarding, assessment/publication, trial eligibility and customer search/profile/contact. Settle its permission and commercial rules before construction. Broader account/phone/recovery and catalogue work remain explicit tasks.

### Policy and integration dependencies

| Decision | Blocks | Proposed decision role |
| --- | --- | --- |
| Remaining contact/downstream permission requirements | UM-T02 and protected downstream submissions | Product/security |
| Verification, re-review and evidence rules | PR-T02 and public trust claims | Trust/operations |
| Trial, benefits, price and cancellation/refunds | SB-T01, PY-T01 and SB-T02 live offers | Founder/product/finance |
| Ranking and price comparability | DS-T01 | Product/engineering |
| Genuine-experience duplication/moderation | RV-T01 | Trust/operations |
| Processing, retention and production integrations | OP-T02 and BE-T02 | Privacy/legal/engineering |
| Pilot thresholds, workload and operational targets | BE-T02 | Product/engineering/operations |

Named accountable people remain unassigned. Missing commercial configuration must keep live trial/paid offers unavailable; missing sponsorship policy does not block ordinary discovery.

Each completed task must link its actual code, checks, revision and remaining limitations in the relevant research entry. Use [PRD gates](02-product-requirements.md#8-validation-and-release-decisions) for product decisions and [operational readiness](https://docs.aws.amazon.com/wellarchitected/latest/operational-excellence-pillar/ops_ready_to_support_const_orr.html) as a review precedent. No new deployment, production approval or test result is created by this plan.

Editorial verification: version 3.4, 5 November 2026; Grammarly pending.
