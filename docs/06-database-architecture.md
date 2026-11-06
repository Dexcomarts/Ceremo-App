# CEREMO Database Design

Version 3.4 · 5 November 2026 · Data architecture and integrity requirements

## 1. Conventions

PostgreSQL 17 remains the proposed production baseline; the current local service and migration tests use PostgreSQL 18.0. Flyway now has an initial migration for the implemented `app_user` fields and `user_identity`. The fuller table definitions below remain the target schema, including features not implemented yet. [ADR 0006](03-technical-requirements.md#116-provider-subscriptions-and-discovery) replaces the previous booking/commission schema for the MVP. The [database research memory](../.github/research/database/user-schema.md) records current evidence and checks.

Domain tables have UUID `id` primary keys, `created_at`/`updated_at` TIMESTAMPTZ and optimistic `version` BIGINT default 0. Append-only tables need creation time; composite/technical keys are stated below. A `?` means nullable. Foreign keys default to RESTRICT; use archive/anonymisation when records must remain.

Use singular snake_case SQL and camelCase Java/JSON. Money is BIGINT pesewas with GHS currency. Unknown budgets/prices are null, not zero. Timestamps are UTC; date-only occasion fields retain their meaning in the recorded timezone, default `Africa/Accra`. Status strings have database checks. JSONB requires a versioned bounded schema and cannot replace domain foreign keys.

### Implementation boundary

The initial [account migration](../src/main/resources/db/migration/V202610200001__create_user_accounts.sql) is preserved. Additive migrations create [session revocations](../src/main/resources/db/migration/V202611040001__create_session_revocations.sql) and [provider tenants/team audit](../src/main/resources/db/migration/V202611040002__create_provider_tenants.sql). Other marketplace and billing tables below remain target requirements. Back up an existing environment and follow the normal reviewed migration process before deployment.

| Implemented table | Key fields and constraints | Authority |
| --- | --- | --- |
| `tenant` | UUID PK; name <=160; immutable owner_id FK app_user; ACTIVE/SUSPENDED; audit/version columns | Provider business workspace; its accountable owner cannot be removed through team operations. |
| `tenant_member` | UUID PK; tenant_id/user_id FKs; unique pair; MANAGER/FINANCE; INVITED/ACTIVE; audit/version columns | Accepted team membership. Ownership remains on the root. |
| `tenant_audit` | UUID PK; tenant_id/actor_id FKs; constrained action; reason <=500; occurred_at | Event history committed atomically with workspace/team changes. No public read endpoint. |
| `revoked_session` | UUID PK; exact issuer/session_id unique pair; subject; revoked_at; bounded strings | Reject further tokens from that session across refresh. Stores no raw tokens. |

Tenant root locks precede fresh membership queries and cover each point operation under READ COMMITTED. Mutation and audit roll back together. Collection reads are scoped and paginated. The future provider profile must reference a unique tenant. The earlier proposed `provider_member` is superseded by `tenant_member` for business access; do not create a second membership authority. Publication, verification and subscription state remain separate.

Revocations have no automatic expiry in this increment. Cleanup needs a safe bound on refresh/session lifetime; access-token expiry alone is insufficient. Inherited actor columns remain nullable; tenant event history does not implement a general JPA actor auditor. See [ADR 11.8](03-technical-requirements.md#118-provider-business-tenants-account-access-and-logout).

The current runtime has Hibernate `ddl-auto: none`; migration-backed schema validation is a required check, not an already enabled production setting. Local PostgreSQL 18.0 and proposed production 17 remain an unresolved compatibility decision. Exercise the selected production version before release.

### Data ownership and consistency

| Responsibility | Authoritative records | Consistency boundary |
| --- | --- | --- |
| Account identity | app_user and user_identity | Create both in one use-case transaction; identity uniqueness survives competing requests. |
| Provider publication | provider, service/contact/check data and subscription eligibility | Publish only after approval and valid entitlement; suspension overrides all commercial benefits. |
| Discovery/planning | Explicit interests, preferences and owned event/service records | Reset optional preferences atomically without rewriting submitted enquiries or required history. |
| Enquiries/reviews | Submitted snapshots, participant reports and moderation history | Preserve source, ownership and historical meaning; derive published ratings from eligible reviews. |
| Subscription billing | Accepted invoice, verified collection and granted period | Lock the relevant records; deduplicate receipt, payment and grant independently. |
| Operations | Cases, outbox, consent/privacy and audit records | Commit durable work with the business change; restrict evidence and preserve authorised decision history. |

Provider insight counters, search indexes/caches, displayed subscription state and rating aggregates are derived views. They must be rebuildable or reconciled against source records and cannot grant access independently.

## 2. Main relationships

| Parent | Child/reference | Cardinality and ownership rule |
| --- | --- | --- |
| app_user | user_identity.user_id | One user to zero/many identities; each identity belongs to one user. The issuer/subject pair is unique. |
| app_user / tenant | tenant_member | Accepted team membership; the accountable owner is retained on tenant.owner_id. |
| provider | provider_service, provider_contact, provider_check | One provider to zero/many services, contacts and checks; each child belongs to one provider. |
| service_category | provider_service.category_id | One category to zero/many provider services. |
| provider_service / locality | provider_service_area | Many-to-many coverage; office location alone does not establish the covered area. |
| app_user | customer_preference.user_id | Zero/one preference record per user. |
| app_user / service_category | customer_interest | Many-to-many explicit category interests. |
| app_user / provider | saved_provider | Many-to-many saved candidates; no hire relationship is inferred. |
| app_user | event.customer_id | One customer to zero/many private occasion plans. |
| event | event_service.event_id | One event to zero/many service needs. |
| app_user / provider_service | enquiry | One sender and one selected service per enquiry. Optional event_service must belong to the sender. |
| enquiry | enquiry_outcome.enquiry_id | One enquiry to zero/many append-only, source-labelled reports. |
| app_user / provider | review | One author and one provider per review; a platform enquiry/payment is not required. |
| provider | provider_subscription.provider_id | Zero/one subscription record per provider. |
| provider_subscription | subscription_invoice.subscription_id | One subscription to zero/many invoices. |
| subscription_plan | subscription_invoice.plan_id | Each invoice references one versioned plan and snapshots accepted terms. |
| subscription_invoice | subscription_period.invoice_id | Zero/one granted paid period per invoice. |
| subscription_invoice | payment.subscription_invoice_id | Zero/many payment attempts; extra successful collections do not grant extra periods. |
| payment | refund.payment_id | Zero/many refunds, bounded by verified collected funds including pending refunds. |
| payment | webhook_event.payment_id | Zero/many receipts; a receipt may remain unmatched until reconciliation. |
| provider | provider_promotion.provider_id | Zero/many later promotions, subject to independent eligibility and policy. |

All payments belong to provider subscription invoices. No booking, quote, calendar hold, commission, payout or customer service-payment allocation table is part of this MVP.

## 3. Users and catalogues

| Table | Columns | Constraints |
| --- | --- | --- |
| `app_user` | `display_name` VARCHAR(120), `email` VARCHAR(254)?, `phone` VARCHAR(16)?, `phone_verified_at` TIMESTAMPTZ?, `status` VARCHAR(20), `terms_version` VARCHAR(40), `terms_accepted_at` TIMESTAMPTZ, `tokens_valid_after` TIMESTAMPTZ?, `anonymised_at` TIMESTAMPTZ? | Unique non-null normalised E.164 phone; PENDING/ACTIVE/SUSPENDED/ANONYMISED; contact needed for protected submissions, not public browsing |
| `user_identity` | `user_id` UUID FK app_user, `issuer` VARCHAR(512), `subject` VARCHAR(255), `last_seen_at` TIMESTAMPTZ | Unique `(issuer,subject)`; verified allowed issuer; no email/phone account merging |
| `user_role` | `user_id` UUID FK app_user, `role` VARCHAR(32), `granted_by` UUID? FK app_user, `granted_at` TIMESTAMPTZ | Composite PK `(user_id,role)`; CUSTOMER/SUPPORT/FINANCE/ADMIN; audited staff grants |
| `phone_verification` | `user_id` UUID FK app_user, `phone` VARCHAR(16), `purpose` VARCHAR(32), `code_hmac` BYTEA, `pepper_version` VARCHAR(40), `expires_at` TIMESTAMPTZ, `attempt_count` SMALLINT, `consumed_at` TIMESTAMPTZ? | Single use, five attempts, resend invalidation; contact verification is separate from Keycloak authentication |
| `session_revocation` | `user_id` UUID FK app_user, `issuer` VARCHAR(512), `session_id_hash` BYTEA, `expires_at` TIMESTAMPTZ, `reason` VARCHAR(200) | Unique `(issuer,session_id_hash)` |
| `rate_limit_bucket` | `bucket_key` BYTEA, `window_start` TIMESTAMPTZ, `request_count` INTEGER, `expires_at` TIMESTAMPTZ | Composite PK `(bucket_key,window_start)`; keyed identifiers and atomic non-negative increments |
| `occasion_type` | `code` VARCHAR(60), `name` VARCHAR(120), `description` VARCHAR(1000)?, `active` BOOLEAN | Unique stable code; all requested occasion families |
| `service_category` | `parent_id` UUID? FK service_category, `code` VARCHAR(60), `name` VARCHAR(120), `requirements_schema` JSONB, `schema_version` INTEGER, `active` BOOLEAN | Unique code; no hierarchy cycles; retain used schemas |
| `locality` | `parent_id` UUID? FK locality, `code` VARCHAR(80), `name` VARCHAR(120), `type` VARCHAR(20), `pilot_enabled` BOOLEAN | Unique code; REGION/DISTRICT/LOCALITY; valid hierarchy |
| `event_advisory` | `title` VARCHAR(200), `body` VARCHAR(4000), `effective_from` TIMESTAMPTZ, `effective_to` TIMESTAMPTZ, `source_url` VARCHAR(2048), `verified_at` TIMESTAMPTZ, `reviewed_by` UUID FK app_user, `status` VARCHAR(20) | End after start; DRAFT/PUBLISHED/WITHDRAWN; publication needs source/review date |
| `advisory_locality` | `advisory_id` UUID FK event_advisory, `locality_id` UUID FK locality | Composite PK `(advisory_id,locality_id)` |

Activation uses the existing account status and inherited optimistic version. The domain factory starts PENDING; the application may activate after trusted Keycloak email verification. It persists no email-verification timestamp or identity override in this increment. External identity remains the exact issuer/subject pair. [Activation decision](03-technical-requirements.md#117-account-activation-from-verified-keycloak-email).

Keycloak owns credentials, MFA secrets, refresh tokens and its authentication sessions in a separate database. CEREMO owns application status and permissions. Public guest browsing creates no application account. Notices apply only to their affected localities.

## 4. Providers, contacts and checks

| Table | Columns | Constraints |
| --- | --- | --- |
| `provider` | `tenant_id` UUID FK tenant, `display_name` VARCHAR(160), `slug` VARCHAR(180), `description` VARCHAR(4000), `business_type` VARCHAR(20), `locality_id` UUID FK locality, `status` VARCHAR(24), `accepting_enquiries` BOOLEAN, `first_published_at` TIMESTAMPTZ? | Unique tenant_id and slug; DRAFT/UNDER_REVIEW/APPROVED/REJECTED/SUSPENDED; first publication immutable; no PSP payout fields |
| `tenant_member` | Implemented business-access model described in section 1 | Replaces the earlier proposed provider_member table; retain ownership on the tenant root. |
| `provider_service` | `provider_id` UUID FK provider, `category_id` UUID FK service_category, `title` VARCHAR(160), `description` VARCHAR(4000), `price_from_minor` BIGINT?, `price_unit` VARCHAR(40)?, `currency` CHAR(3), `capabilities` JSONB, `published` BOOLEAN | Price/unit together or both null; non-negative indicative price; capabilities are self-described unless checked |
| `provider_service_area` | `service_id` UUID FK provider_service, `locality_id` UUID FK locality, `travel_notes` VARCHAR(1000)? | Composite PK `(service_id,locality_id)` |
| `provider_contact` | `provider_id` UUID FK provider, `channel` VARCHAR(20), `value_ciphertext` BYTEA, `publication_approved_at` TIMESTAMPTZ, `approved_by` UUID FK app_user, `active` BOOLEAN | PHONE/WHATSAPP/WEBSITE/EMAIL; normalised validated target; owner-approved business contact, not automatically a member's private phone |
| `provider_check` | `provider_id` UUID FK provider, `check_type` VARCHAR(40), `status` VARCHAR(20), `reviewer_id` UUID? FK app_user, `checked_at` TIMESTAMPTZ?, `expires_at` TIMESTAMPTZ?, `evidence_file_id` UUID? FK stored_file, `public_summary` VARCHAR(300)?, `private_notes` VARCHAR(2000)? | PENDING/PASSED/FAILED/EXPIRED; passed needs reviewer/date; subscription payment cannot set outcome |
| `stored_file` | `owner_user_id` UUID FK app_user, `provider_id` UUID? FK provider, `purpose` VARCHAR(30), `object_key` VARCHAR(500), `mime_type` VARCHAR(100), `size_bytes` BIGINT, `sha256` BYTEA?, `status` VARCHAR(24), `retention_until` TIMESTAMPTZ?, `rights_confirmed_at` TIMESTAMPTZ?, `metadata` JSONB | Unique key; PORTFOLIO/VERIFICATION/REVIEW_EVIDENCE/CASE_EVIDENCE/EXPORT; approved derivative required for public media |

Publishing requires provider approval and a current listing entitlement. Suspension wins over any paid period. Expired listings retain an inactive historical profile/reviews behind registered access but expose no active contact/enquiry action. Derive check validity from its result and expiry independently of subscription status. Private evidence is never included in customer-facing profiles.

## 5. Preferences and optional occasion plans

| Table | Columns | Constraints |
| --- | --- | --- |
| `customer_preference` | `user_id` UUID FK app_user, `purpose` VARCHAR(24)?, `locality_id` UUID? FK locality, `personalisation_enabled` BOOLEAN, `notice_version` VARCHAR(40) | Unique user; purpose EXPLORE/PLAN/UNDECIDED; optional fields; separate from marketing |
| `customer_interest` | `user_id` UUID FK app_user, `category_id` UUID FK service_category | Composite PK `(user_id,category_id)`; explicitly selected persistent interests only |
| `saved_provider` | `user_id` UUID FK app_user, `provider_id` UUID FK provider | Composite PK `(user_id,provider_id)`; not a hired-provider record |
| `event` | `customer_id` UUID FK app_user, `occasion_type_id` UUID? FK occasion_type, `title` VARCHAR(160)?, `starts_on` DATE?, `ends_on` DATE?, `timezone` VARCHAR(64), `locality_id` UUID? FK locality, `status` VARCHAR(20) | DRAFT/ACTIVE/ARCHIVED; end not before start when both supplied; one accountable customer initially |
| `event_service` | `event_id` UUID FK event, `category_id` UUID FK service_category, `guest_count` INTEGER?, `budget_min_minor` BIGINT?, `budget_max_minor` BIGINT?, `budget_unit` VARCHAR(40)?, `currency` CHAR(3), `requirements` JSONB, `schema_version` INTEGER | Positive known guest count; non-negative ordered known bounds; a budget needs a unit; unknown is null |

The guide need not create an event. Reset preferences and category interests atomically, without deleting unrelated necessary billing or enquiry records. Store occasion budgets here, not as permanent wealth attributes. Do not infer religion, ethnicity or bereavement traits. Exact private venue data is not required for discovery.

## 6. Enquiries and contact actions

| Table | Columns | Constraints |
| --- | --- | --- |
| `enquiry` | `customer_id` UUID FK app_user, `provider_service_id` UUID FK provider_service, `event_service_id` UUID? FK event_service, `status` VARCHAR(20), `brief_snapshot` JSONB, `shared_contact_snapshot` BYTEA?, `submitted_at` TIMESTAMPTZ, `acknowledged_at` TIMESTAMPTZ?, `closed_at` TIMESTAMPTZ?, `qualification_status` VARCHAR(20), `qualification_reason` VARCHAR(200)?, `classified_by` UUID? FK app_user, `classified_at` TIMESTAMPTZ? | NEW/ACKNOWLEDGED/CLOSED/SPAM; separate UNCLASSIFIED/QUALIFIED/UNSUITABLE assessment; immutable submitted brief; authorised optional event ownership and selected sharing |
| `enquiry_outcome` | `enquiry_id` UUID FK enquiry, `reported_by` UUID FK app_user, `source` VARCHAR(20), `outcome` VARCHAR(20), `reported_at` TIMESTAMPTZ, `notes` VARCHAR(1000)? | CUSTOMER/PROVIDER source derived from actor; UNKNOWN/HIRED/NOT_HIRED/CANCELLED; append-only reports with contradictory accounts retained |
| `contact_action` | `provider_id` UUID FK provider, `contact_id` UUID FK provider_contact, `user_id` UUID? FK app_user, `client_event_id` UUID, `channel` VARCHAR(20), `occurred_at` TIMESTAMPTZ, `category_id` UUID? FK service_category, `locality_id` UUID? FK locality | Unique `(provider_id,contact_id,client_event_id)`; minimal permitted analytics; contact must belong to provider; no external chat/payment contents |
| `provider_insight_daily` | `provider_id` UUID FK provider, `metric_date` DATE, `profile_views` BIGINT, `sponsored_impressions` BIGINT | Composite PK `(provider_id,metric_date)`; non-negative aggregate counters; views are measured requests, not unique people or jobs; enquiries/contact counts come from their own records |

Acknowledgement is not acceptance of a service contract. A click is not proof of conversation; HIRED is a source-labelled report, not a verified payment. Staff classification and aggregate insights must preserve unknown and conflicting outcomes. Failed/declined optional analytics permission must not prevent an ACTIVE account from authorised business contact. Contact-action recording must follow the approved purpose and retention policy; account registration is not analytics or marketing consent.

Validate cross-table ownership in application transactions and tests: an optional event service belongs to the enquiry customer, contact belongs to the named provider, and an optional enquiry on a review belongs to that author/provider relationship. Do not let a client attach somebody else's evidence or enquiry by ID. Qualification is an auditable relevance assessment, separate from a claim that the provider was hired.

## 7. Reviews and moderation

| Table | Columns | Constraints |
| --- | --- | --- |
| `review` | `provider_id` UUID FK provider, `author_user_id` UUID FK app_user, `category_id` UUID FK service_category, `enquiry_id` UUID? FK enquiry, `service_date` DATE?, `rating` SMALLINT, `body` VARCHAR(2000), `status` VARCHAR(20), `evidence_status` VARCHAR(24), `provider_reply` VARCHAR(2000)?, `moderation_reason` VARCHAR(500)? | Rating 1–5; PENDING/PUBLISHED/HIDDEN/REMOVED; CUSTOMER_REPORTED/EVIDENCE_REVIEWED; genuine-experience and duplicate/conflict checks; no completed booking FK |
| `review_evidence` | `review_id` UUID FK review, `file_id` UUID FK stored_file, `reviewer_id` UUID? FK app_user, `reviewed_at` TIMESTAMPTZ?, `decision` VARCHAR(20)? | Composite PK `(review_id,file_id)`; private authorised access; evidence review does not prove service quality |
| `review_report` | `review_id` UUID FK review, `reporter_id` UUID FK app_user, `reason` VARCHAR(60), `details` VARCHAR(2000)?, `status` VARCHAR(20), `resolved_by` UUID? FK app_user, `resolution` VARCHAR(1000)? | OPEN/IN_REVIEW/RESOLVED; reasoned audit and appeal; no paid moderation preference |

One genuine service experience should not create repeated ratings. Before release, define the experience duplicate key and appeal process for repeated work; do not impose one lifetime review per provider/customer. Providers may respond/report but cannot approve or delete criticism. Rating average/count uses published eligible reviews only; zero reviews produces null average, never five stars. An internal ranking adjustment must not change displayed customer scores.

## 8. Provider subscriptions and platform payments

| Table | Columns | Constraints |
| --- | --- | --- |
| `subscription_plan` | `code` VARCHAR(60), `plan_version` INTEGER, `name` VARCHAR(120), `price_minor` BIGINT?, `currency` CHAR(3), `interval` VARCHAR(20), `benefits` JSONB, `terms` JSONB, `published` BOOLEAN | Unique `(code,plan_version)`; CALENDAR_MONTH initially; published paid plan needs approved positive price/defined entitlements |
| `provider_subscription` | `provider_id` UUID FK provider, `current_plan_id` UUID? FK subscription_plan, `status` VARCHAR(20), `trial_started_at` TIMESTAMPTZ?, `trial_ends_at` TIMESTAMPTZ?, `trial_policy_version` VARCHAR(40)?, `paid_through` TIMESTAMPTZ?, `anchor_day` SMALLINT?, `cancel_at_period_end` BOOLEAN, `billing_mode` VARCHAR(20) | Unique provider; INACTIVE/TRIALING/ACTIVE/EXPIRED; trial timestamps paired/ordered and set once; MANUAL billing initially; anchor 1–31 |
| `subscription_invoice` | `subscription_id` UUID FK provider_subscription, `plan_id` UUID FK subscription_plan, `invoice_number` VARCHAR(80), `amount_minor` BIGINT, `currency` CHAR(3), `status` VARCHAR(20), `due_at` TIMESTAMPTZ, `plan_details` JSONB, `paid_at` TIMESTAMPTZ? | Unique number; one unresolved purchase invoice per subscription; OPEN/PAID/VOID/EXPIRED; fixed accepted price/benefits/term |
| `subscription_period` | `subscription_id` UUID FK provider_subscription, `invoice_id` UUID FK subscription_invoice, `starts_at` TIMESTAMPTZ, `ends_at` TIMESTAMPTZ, `benefits` JSONB, `revoked_at` TIMESTAMPTZ?, `revocation_case_id` UUID? FK support_case | Unique invoice; end after start; non-revoked paid periods do not overlap |
| `payment` | `subscription_invoice_id` UUID FK subscription_invoice, `merchant_reference` VARCHAR(120), `psp_transaction_id` VARCHAR(160)?, `expected_amount_minor` BIGINT, `currency` CHAR(3), `status` VARCHAR(24), `checkout_url_ciphertext` BYTEA?, `verified_at` TIMESTAMPTZ?, `psp_paid_at` TIMESTAMPTZ?, `verification_summary` JSONB | Unique reference/non-null PSP transaction ID; amount > 0; CREATED/PENDING/SUCCEEDED/FAILED/EXPIRED/REVIEW_REQUIRED; no booking parent |
| `webhook_event` | `source` VARCHAR(30), `dedupe_key` VARCHAR(256), `payment_id` UUID? FK payment, `event_type` VARCHAR(100), `payload_ciphertext` BYTEA, `payload_sha256` BYTEA, `status` VARCHAR(24), `attempt_count` INTEGER, `received_at` TIMESTAMPTZ, `processed_at` TIMESTAMPTZ? | Unique `(source,dedupe_key)`; signature first; durable receipt before acknowledgement |
| `refund` | `payment_id` UUID FK payment, `case_id` UUID FK support_case, `amount_minor` BIGINT, `status` VARCHAR(24), `merchant_reference` VARCHAR(120), `psp_reference` VARCHAR(160)?, `requested_by` UUID FK app_user, `approved_by` UUID? FK app_user, `completed_at` TIMESTAMPTZ? | Positive amount; unique references; independent approval; completed plus pending refunds capped at successful collection |

Grant the trial under a provider lock on first approved publication. Lock subscription/invoice for payment attempts and paid grants. One invoice grants once even if callbacks repeat or several collections succeed. An excess collection creates a billing case, not extra paid time. Reconcile a timeout/late success against the original reference before another charge.

Start early paid time after unused trial/paid entitlement, otherwise at verified payment time. Preserve the monthly anchor and clamp shorter months. Check actual current timestamps and revocations on reads/writes; cached status alone is insufficient. Trial and future paid time may coexist as records without consuming each other.

Use a PostgreSQL exclusion constraint on non-revoked paid period ranges, with `btree_gist` for UUID equality, plus invoice uniqueness. Cross-table provider/invoice ownership and refund sums need locked application transactions and integration tests; ordinary CHECK constraints do not enforce them. [PostgreSQL ranges](https://www.postgresql.org/docs/17/rangetypes.html).

No auto-debit credentials, raw card data or MoMo PINs are stored. Service payments outside CEREMO never enter these tables.

## 9. Sponsored placement and operations

| Table | Columns | Constraints |
| --- | --- | --- |
| `provider_promotion` | `provider_id` UUID FK provider, `category_id` UUID FK service_category, `locality_id` UUID FK locality, `starts_at` TIMESTAMPTZ, `ends_at` TIMESTAMPTZ, `status` VARCHAR(20), `policy_version` VARCHAR(40) | DRAFT/ACTIVE/PAUSED/EXPIRED; ordered times; current paid-tier entitlement plus relevance required; no separate boost payment |
| `support_case` | `user_id` UUID FK app_user, `provider_id` UUID? FK provider, `payment_id` UUID? FK payment, `type` VARCHAR(32), `status` VARCHAR(24), `assigned_to` UUID? FK app_user, `summary` VARCHAR(2000), `resolution` JSONB?, `legal_hold` BOOLEAN | PROFILE/REVIEW/SUBSCRIPTION/RECOVERY/REPORTED_SERVICE_ISSUE; OPEN/IN_REVIEW/WAITING/RESOLVED/CLOSED; limited evidence access |
| `notification` | `recipient_user_id` UUID FK app_user, `template_code` VARCHAR(80), `record_type` VARCHAR(40), `record_id` UUID, `channel` VARCHAR(20), `status` VARCHAR(24), `dedupe_key` VARCHAR(160), `attempt_count` INTEGER, `sent_at` TIMESTAMPTZ? | Unique dedupe; SMS/IN_APP_RECORD; PENDING/SENT/FAILED/SUPPRESSED |
| `outbox_event` | `event_type` VARCHAR(80), `record_type` VARCHAR(40), `record_id` UUID, `payload` JSONB, `status` VARCHAR(24), `available_at` TIMESTAMPTZ, `lease_until` TIMESTAMPTZ?, `attempt_count` INTEGER | Atomic business-change insertion; leased retry/deduplication; no general plaintext OTP storage |
| `audit_log` | `actor_id` UUID? FK app_user, `action` VARCHAR(80), `record_type` VARCHAR(40), `record_id` UUID?, `trace_id` VARCHAR(100), `details` JSONB | Append-only; redacted; staff reads of sensitive evidence audited |
| `privacy_request` | `user_id` UUID FK app_user, `type` VARCHAR(20), `status` VARCHAR(24), `due_at` TIMESTAMPTZ?, `resolved_at` TIMESTAMPTZ?, `resolution` JSONB? | ACCESS/CORRECTION/DELETION; identity-checked, authorised handling |
| `user_consent` | `user_id` UUID FK app_user, `purpose` VARCHAR(60), `policy_version` VARCHAR(40), `granted` BOOLEAN, `recorded_at` TIMESTAMPTZ, `source` VARCHAR(40) | Append-only; separate marketing purposes/channels from personalisation and terms |
| `idempotency_record` | `actor_key` VARCHAR(255), `method_path` VARCHAR(500), `idempotency_key` VARCHAR(160), `request_hash` BYTEA, `response_ciphertext` BYTEA?, `status` VARCHAR(20), `expires_at` TIMESTAMPTZ | Unique `(actor_key,method_path,idempotency_key)`; mismatched body rejected; concurrency-safe pending result |

Sponsored placement does not change review/check data. Allocation requires a defined versioned rule and finite inventory; no universal first-position promise. Generic record IDs in operational tables are intentional metadata, not replacements for domain foreign keys.

## 10. Access, indexes and retention

The planned `contact_action.user_id` may remain nullable for approved minimisation or erasure, not to authorise guest contact. Check the ACTIVE account before releasing a target. Analytics records are not the authorisation source. Safe summary/detail DTOs and media delivery rules must be tested when provider discovery is implemented; this access-policy change adds no migration to existing account/tenant tables.

Anonymous users read safe catalogues and limited provider summaries. Full provider profiles, detailed reviews and contacts require authenticated ACTIVE local accounts. These customer-facing projections exclude private tenant ownership/team, billing, evidence and internal notes. Customers access only their own preferences, plans, enquiries and reports. Provider managers manage their authorised listing/enquiries; owners buy subscriptions and manage members; finance members see authorised billing. SUPPORT handles assigned cases/reports, FINANCE reconciles/refunds, and ADMIN controls permitted catalogues/policies. Private tenant reads require accepted membership; staff access must be explicitly scoped. Registration, a provider role or a tenant UUID alone grants none of those permissions. Staff changes and sensitive reads are audited.

Index joined foreign keys, provider status/locality, services/categories/coverage, current trial/paid ranges, participant enquiries, review publication, pending invoice/payment jobs and outbox availability. Use real PostgreSQL tests for uniqueness, locking, exclusion and migration behaviour. Seed synthetic profiles and stable catalogue codes, not scraped provider identities.

Proposed operational defaults: remove expired OTP delivery values immediately and verification rows within 24 hours; expire idempotency responses after 24 hours; minimise contact analytics and delete identifiable raw actions within 90 days; remove assessment evidence within 30 days after review unless a documented need or hold applies; review optional preferences and stale occasion plans for deletion rather than retaining indefinitely. Financial and legal retention periods require local review. Keep aggregated analytics only when they cannot identify people.

Create tables in dependency order and add circular references afterwards, particularly payments/support cases. Test migrations from an empty and prior-release database. Anonymisation must preserve necessary transaction facts without restoring deleted personal data or marketing consent after a backup restore. Use versioned encryption keys and tested rotation/restoration.

## 11. Migration and release acceptance

| Check | Required evidence |
| --- | --- |
| Target database | Confirm the production major version, extensions, collation, driver and migration compatibility. |
| Schema lifecycle | Apply from empty and prior-release databases; validate checksums and re-run with no pending work. Applied migrations remain immutable. |
| Integrity | Reject duplicate identities, missing parents, invalid states/amounts and overlapping non-revoked periods. Verify identifier case and null/unknown semantics. |
| Atomicity | Roll back account/identity and billing grants together on failure. Test two independent transactions for duplicate registration, first trial and invoice grant. |
| Authorisation | Test cross-account/provider references, evidence attachment, staff access and private-record lists. UUID possession is not permission. |
| Query behaviour | Verify foreign-key/job indexes and bounded queries against representative synthetic data. Approve workload/latency targets before claiming capacity. |
| Recovery | Demonstrate encrypted backup restore, key availability, deletion/consent preservation and payment reconciliation before live writes resume. |
| Operational policy | Approve retention/holds, staff access, refund authority and experience-duplication rules before affected features go live. |

The schema tables define implementation contracts. Unimplemented limits, OTP settings and retention periods are proposed values requiring policy review. No migration, production data change or new database test was run for this documentation revision.

PostgreSQL's [constraint guidance](https://www.postgresql.org/docs/18/ddl-constraints.html) supports database-enforced row/referential rules; cross-record financial caps and ownership still require correctly locked transactions and tests. Use the [implementation plan](07-implementation-plan.md) for dependency order and [quality requirements](08-development-and-quality.md) for recorded verification.

Editorial verification: version 3.4, 5 November 2026; Grammarly pending.
