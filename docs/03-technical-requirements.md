# CEREMO Technical Requirements

Version 3.4 · 5 November 2026 · Architecture and delivery contract

## 1. Stack and application boundaries

CEREMO uses one backend organised by business feature. This keeps provider publication, subscription access and financial changes within explicit transaction boundaries while avoiding the operational cost of several marketplace services before demand is known.

| Architectural priority | Decision | Tradeoff and release evidence |
| --- | --- | --- |
| Business integrity | PostgreSQL constraints plus complete use-case transactions. | Requires database-specific integration tests; an application pre-check cannot prevent a uniqueness race. |
| Clear ownership | Strict DDD with ports/adapters and separate domain, REST and JPA models. | Adds mapping/composition work; makes framework and integration changes less likely to alter business rules. |
| Independent identity | Keycloak owns credentials; CEREMO owns local account status and record permissions. | A second production service needs operational ownership; login does not prove business access. |
| Limited integration failure | Persist payment references and durable work before retryable external activity. | Workers and reconciliation need monitoring; a synchronous success page cannot establish payment. |
| Controlled initial cost | One feature-based application, database-backed jobs and explainable discovery rules. | Capacity is bounded by measured deployment resources; service extraction requires evidence. |

### System context and trust boundaries

| Boundary | CEREMO responsibility | External responsibility |
| --- | --- | --- |
| Customer/provider client | Validate transport data, authorise the use case and return only permitted fields. | Render status truthfully; obtain confirmation before disclosure or purchase. |
| Keycloak | Validate trusted tokens and map issuer/subject to the local account. | Credentials, login, MFA, recovery and issuer keys. |
| Payment processor | Create invoices/references, authenticate receipts, verify collection and reconcile grants/refunds. | Hosted payment authorisation and processor-side transaction/refund status. |
| File/SMS services | Restrict access, validate media, minimise message content and recover delivery failures. | Storage/delivery mechanisms under confirmed contracts and access. |
| Outside service arrangement | Support discovery, reporting and moderation. | Customer/provider negotiations, availability commitments, contracts and service payments. |

The API does not confirm an outside service contract or guarantee a refund. Each integration needs a timeout, bounded retry policy, observable failure state and an assigned operator before live use.

### Implementation status

The current backend includes account HTTP/activation, active-account and staff checks, provider tenants/teams, persistent logout, Flyway and PostgreSQL security tests. Public discovery/publication, billing and live payment/SMS/storage integrations remain planned.

The 22 October test/coverage record is historical evidence for that compiled revision. It is not a new pass for today's source or proof of a Sonar server gate, live issuer/JWKS exchange or production readiness.

| Component | Choice |
| --- | --- |
| Backend | Java 25, Spring Boot 4.1.1, MVC, Security and Data JPA |
| Build | Gradle 9.8.1 with Kotlin build scripts |
| Database | PostgreSQL with Flyway implemented; local/tests use 18.0, production 17 remains proposed |
| Identity | Keycloak in development and production |
| API | REST under `/api/v1`, documented through OpenAPI |
| Provider billing | Paystack hosted checkout, subject to merchant approval |
| SMS | Hubtel, subject to current access and sender approval; fake adapter in development |
| Files | Private S3 evidence and separately approved public portfolio copies |
| Quality | JUnit, Spring Security tests, PostgreSQL Testcontainers, JaCoCo and SonarQube |
| Backend hosting | Render Docker services and PostgreSQL in Frankfurt, proposed |
| Web frontend | Next.js/React, TypeScript, Tailwind CSS, shadcn/ui, TanStack Query, React Hook Form and Zod |
| Native mobile framework | `null` |

The starter dependencies, 12 shared packages and 16 wire Java files are present. Account, logout and provider-tenant modules have HTTP/persistence adapters and outer transaction composition. The quality register distinguishes current checks from historical runs and unverified live services. The per-class 100% line/branch rule remains mandatory; the Sonar server gate is unverified. Use one backend with feature boundaries and separate identity/database credentials. No Redis, Kafka or recommendation model is required for this increment.

[ADR 0006](03-technical-requirements.md#116-provider-subscriptions-and-discovery) replaces the transaction model: the API does not collect customer service payments, calculate commission, confirm bookings or reserve provider calendars. Subscription checkout pays CEREMO only.

## 2. Packages and naming

| Package | Main classes |
| --- | --- |
| `account` | `User`, `UserService`, `AccountService`, `UserResource`, `AccountResource`, `CurrentUser` |
| `tenant` | `Tenant`, `TenantMember`, `TenantService`, `TenantResource`, `AdminTenantResource` |
| `logout` | `UserSession`, `LogoutService`, `LogoutResource` |
| `providermanagement` | `Provider`, `ProviderService`, `ProviderResource`, `ProviderCheck` |
| `discovery` | `DiscoveryService`, `CustomerPreference`, `PreferenceService`, `SavedProvider` |
| `eventmanagement` | `Event`, `RequestedService`, `EventService`, `EventResource` |
| `enquirymanagement` | `Enquiry`, `EnquiryService`, `EnquiryResource`, `EnquiryOutcome` |
| `reviewmanagement` | `Review`, `ReviewService`, `ReviewResource` |
| `subscriptionmanagement` | `Subscription`, `SubscriptionPlan`, `SubscriptionService` |
| `paymentmanagement` | `Payment`, `PaymentService`, `RefundService` |
| `notification` | `NotificationService`, `SmsClient` |
| `administration` | Profile/check review, support, billing review and privacy services |
| `shared` | Money, API errors and narrowly shared types |

Keep root package `io.ceremo.ceremobackend`. Within a feature use `domain/model`, `domain/repository`, `application`, `application/port`, `infrastructure/primary/rest`, DTO request/response directories and `infrastructure/secondary/persistence`. Examples are `ProviderService`, `ProviderResource`, `RestCreateProviderRequest` and `ProviderJpaEntity`. Do not expose JPA entities as API models.

Methods describe the operation directly: `findProviders(criteria)`, `updatePreferences(...)`, `acknowledge(enquiryId)` and `createInvoice(providerId, planId)`. Java/JSON uses camelCase; SQL uses singular snake_case. `RequestedService` maps to `event_service`; `EventService` remains the application service.

### DDD and ports and adapters

The user confirmed this architecture on 20 October 2026. Build and explain features in the order Domain -> Repository contracts -> Application -> Controllers. Keep dependencies directed towards the core: the domain does not depend on application or infrastructure, and application services depend on domain types and port contracts. DDD also requires explicit aggregates, business language and invariant enforcement; directories alone do not establish these boundaries.

```text
account/
  domain/model/                 User, UserStatus, UserIdentity
  domain/repository/            UserRepository (outbound port)
  application/port/in/          UserAccounts, Accounts, UserDirectory
  application/port/out/         CurrentUser, StaffAccess
  application/                 UserService, AccountService
  infrastructure/primary/rest/  UserResource, AccountResource, DTOs
  infrastructure/secondary/
    persistence/               UserJpaEntity, UserIdentityJpaEntity,
                               JpaUserRepository, JpaUserDirectory
    security/                  SpringCurrentUser, SpringStaffAccess
  infrastructure/primary/transaction/ TransactionalUserAccounts
wire/account/                  AccountConfiguration, UserRegistrationProperties
```

Names for new ports/adapters are proposed until their actual methods are implemented. Existing `*Resource` names denote controllers and remain consistent with current documentation. An HTTP controller calls the inbound use-case contract. The application service calls domain behaviour and outbound contracts; the persistence adapter implements the domain repository contract. Runtime calls can reach the database while compile-time dependencies still point inward.

Keep domain and application code free of Spring, JPA, Hibernate, HTTP and Keycloak-specific imports. Use constructor injection and outer `wire` bean configuration. A transaction decorator or equivalent infrastructure boundary must cover the whole application use case, not separate individual repository calls. Pass authenticated identity through an application contract rather than reading Spring's security context from the domain. Enforce account status and resource ownership for every protected use case, including non-HTTP entry points.

Use plain Java repository contracts returning domain objects and Java types. Spring Data interfaces and JPA mappings live in persistence infrastructure. Maintain explicit mapping; do not expose JPA entities as domain or REST models. In particular, the existing JPA-annotated `shared.domain.BaseEntity` belongs to persistence usage and must not become the superclass of a pure domain model. Database constraints reinforce aggregate rules; integration tests must check locking, uniqueness and transaction rollback where those rules require them.

This mapping follows [Cockburn's ports and adapters explanation](https://alistair.cockburn.us/hexagonal-architecture). A concrete Spring Boot learning reference is [DDD Sample](https://github.com/citerus/dddsample-core): its [CargoRepository](https://github.com/citerus/dddsample-core/blob/master/src/main/java/se/citerus/dddsample/domain/model/cargo/CargoRepository.java) is a plain domain contract and [BookingServiceImpl](https://github.com/citerus/dddsample-core/blob/master/src/main/java/se/citerus/dddsample/application/impl/BookingServiceImpl.java) receives dependencies through its constructor. The reference uses Spring transactions in application code; CEREMO deliberately places that concern outside its core. Do not copy its business model or dependencies wholesale. Existing shared/wire code has not been refactored by this decision.

### 2.1 Shared packages

The 87 shared Java files live under `io.ceremo.ceremobackend.shared`; package annotations use CEREMO's `SharedKernel`. Reuse their current semantics and dependencies through the appropriate adapter/core boundary. The [shared inventory](../.github/shared-package-map.md) retains provenance and detailed limits; it is not permission to consult another project.

| Package | Main types |
| --- | --- |
| `authentication` | `AuthenticatedUser`, `Username`, `Role`, `Roles`, OAuth2/security configuration and claim converters |
| `collection` | `CeremoCollections` |
| `domain` | `BaseEntity` with UUID, audit fields and optimistic version |
| `enumeration` | `Enums`, `UnmappableEnumException` |
| `error` | Assertions, `CeremoException`, `ApiProblemResponse` and exception advice |
| `event` | `DomainEvent`, `DomainEventPublisher`, `SpringDomainEventPublisher` |
| `generation` | `CeremoUUID`, `ExcludeFromGeneratedCodeCoverage` |
| `memoizer` | `Memoizers` |
| `money` | `Money`, `Currency`; default currency GHS |
| `multitenancy` | Tenant context/IDs/entities, request filter, task decorator, aspects and exceptions |
| `pagination` | `CeremoPage`, `CeremoPageable`, REST models and Spring Data adapters |
| `useridentity` | `Email`, `Firstname`, `Lastname`, `Name`, `PhoneNumber` |

The copied pagination types use zero-based pages, default size 10 and maximum 100. They do not implement the planned cursor API below. `Money` holds a decimal amount in major currency units; `Money.of(100, GHS)` means GHS 100. Convert explicitly to/from integer pesewas at future API/persistence boundaries. `PhoneNumber` strips formatting to digits; it does not verify a Ghanaian number, add a country code or prove ownership.

`BaseEntity` declares auditing annotations; `wire/database` enables timestamp auditing. An `AuditorAware` adapter still needs to supply the current application actor for `createdBy`/`updatedBy`. Spring event publication is local to the process and does not replace the planned durable outbox. The recorded 22 October combined JaCoCo run met the mandatory 100% rule. Do not add coverage annotations to hide handwritten code; new changes must retain the rule.

The `tenant` module represents provider business workspaces, with database-authoritative ownership and membership. Its protected operations use explicit application checks and scoped repository reads. The inherited organisation-claim filter/aspects remain disabled; leave `application.multitenancy.enabled` unset. `TenantInfo.subscriptionTier` and `maxLocations` remain compatibility fields with no billing authority. The task decorator propagates thread context but does not grant membership. See ADR 11.8.

### 2.2 Application wiring

Keep configuration under `io.ceremo.ceremobackend.wire`, inside the application's component-scan root. After account consolidation, 16 Java files cover nine configuration areas.

| Package | Configuration and settings |
| --- | --- |
| `async` | Async/scheduled execution; Spring's executor builder applies `spring.task.execution.*` and attaches `TenantAwareTaskDecorator` |
| `cache` | Ehcache through JCache; `application.cache.ehcache.max-entries` defaults to 100 and `time-to-live-seconds` to 3600 |
| `database` | Transaction management, JPA auditing and repository scanning under `io.ceremo.ceremobackend`; service operations own transactions |
| `account` | One AccountConfiguration composes Clock, UserRepository/UserDirectory, transactional UserAccounts, Accounts and StaffAccess; validated `application.registration.terms-version` supplied by `CEREMO_TERMS_VERSION` |
| `tenant` | TenantRepository and the complete transactional tenant/team/audit use-case boundary |
| `logout` | Trusted current session, persistent JDBC revocations and LogoutService |
| `logstash` | Optional TCP JSON logging; `application.logging.logstash.tcp` binds `enabled`, `host`, `port`, `ring-buffer-size` and `shutdown-grace-period` |
| `security` | One CORS filter used by Spring Security; `application.cors` accepts origins, methods and headers; servlet auto-registration is disabled to avoid duplicate execution |
| `springdoc` | CEREMO API title/description, `all` API group and authorization-code OAuth scheme using the `oidc` provider endpoints; optional `application.documentation-url` for a published documentation link |

Cache helpers create a missing cache or clear an existing one. No marketplace cache names or caching of customer/private data are enabled by this copy. JCache is a local cache facility, not the durable outbox or billing ledger. The Hibernate JCache adapter supports the existing second-level-cache setting; choosing entity/query caches remains separate work.

`TransactionalUserAccounts` uses a read-only REQUIRED transaction for lookup and writable REQUIRED transactions for registration and activation. A joined call leaves physical commit to its existing owner. `UserService` and the domain stay plain Java. Missing/blank terms configuration or values longer than 40 characters fail startup; `terms-v1` is a synthetic development fixture, not a production legal decision. See [verification](08-development-and-quality.md#88-account-http-and-email-activation).

Logstash is disabled by default. When enabled, the lifecycle attaches one appender and detaches/stops it on shutdown. Do not put access tokens, contact details or uploaded evidence in logs. The copy adds the logging adapter, not an external logging destination or production log-access policy.

Swagger uses authorization code; `springdoc.swagger-ui.oauth.use-pkce-with-authorization-code-grant=true` is set in the application YAML. By default the OAuth scheme uses the registered `oidc` authorization/token endpoints; `springdoc.oauth2.authorization-url` and `token-url` can override them. Use a public API-testing client, exact Swagger redirect URI and correct API audience. Keep its client ID separate from any confidential backend login client, and never configure a client secret in Swagger UI. The available scopes are `openid`, `profile` and `email`; there is no Salesync-specific scope.

## 3. Planned API

Public reads exclude private evidence, customer preferences, member contacts and billing records. A published business contact is separate from a provider member's private account phone.

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/v1/occasion-types`, `/service-categories`, `/localities`, `/advisories` | Published catalogues; each path uses `/api/v1` |
| GET / POST | `/api/v1/providers` | Public matching search with limited safe summaries / create own provider |
| GET / PATCH | `/api/v1/providers/{providerId}` | ACTIVE-account full profile / authorised member edits |
| GET | `/api/v1/providers/{providerId}/settings` | Member-only settings and listing entitlement |
| GET / POST / PATCH / DELETE | Provider subcollections `services`, `service-areas`, `contacts`, `members` | Manage owned records; updates/deletes use a child ID |
| POST | `/api/v1/providers/{providerId}/verification` | Submit for assessment, not purchase a passed check |
| POST | `/api/v1/providers/{providerId}/contact-actions` | Require an ACTIVE account and provider eligibility; return an approved contact and record an action when permitted |
| GET | `/api/v1/providers/{providerId}/insights` | Authorised aggregate provider performance |
| GET / PUT | `/api/v1/providers/{providerId}/promotion` | Optional tier-funded promotion settings; no separate boost checkout |
| GET / POST | `/api/v1/users/me` | Read/create application profile after Keycloak login |
| POST | `/api/v1/users/me/activate` | Activate the caller's PENDING account after trusted Keycloak email verification; safe retry for ACTIVE |
| POST | `/api/v1/users/me/phone-verifications` | Send contact verification |
| POST | `/api/v1/users/me/phone-verifications/{verificationId}/confirm` | Confirm single-use code |
| POST | `/api/v1/users/me/logout` | Revoke application session; complete IdP logout separately |
| POST | `/api/v1/users/me/recovery-requests` | Reviewed account/contact recovery |
| GET / PUT / DELETE | `/api/v1/users/me/preferences` | Read/replace/reset optional preferences and interests |
| GET / POST | `/api/v1/users/me/saved-providers` | Read/add saved provider |
| DELETE | `/api/v1/users/me/saved-providers/{providerId}` | Remove saved provider |
| GET / POST | `/api/v1/events` | Optional private occasion plans |
| GET / PATCH | `/api/v1/events/{eventId}` | Read/edit owned plan |
| GET / POST | `/api/v1/events/{eventId}/services` | Optional service needs and unit-based budgets |
| GET / POST | `/api/v1/enquiries` | Participant enquiries / send to one selected provider service |
| GET | `/api/v1/enquiries/{enquiryId}` | Participant-only submitted snapshot |
| POST | `/api/v1/enquiries/{enquiryId}/acknowledge`, `/close`, `/outcomes` | Provider acknowledgement, participant closure, source-labelled follow-up reports |
| GET / POST | `/api/v1/providers/{providerId}/reviews` | ACTIVE-account published experiences / submit genuine experience |
| POST | `/api/v1/reviews/{reviewId}/reply`, `/reports` | Provider response / report a problem |
| GET | `/api/v1/subscription-plans` | Published provider plans |
| GET | `/api/v1/providers/{providerId}/subscription` | Current trial, periods and allowed benefits |
| POST | `/api/v1/providers/{providerId}/subscription/cancel` | Stop future renewal activity under accepted terms |
| GET / POST | `/api/v1/providers/{providerId}/subscription/invoices` | Owner billing history / create invoice |
| POST | `/api/v1/subscription-invoices/{invoiceId}/payments` | Start provider invoice hosted checkout |
| GET | `/api/v1/payments/{paymentId}` | Authorised provider billing payment status |
| GET / POST | `/api/v1/support-cases` | Own platform complaints or billing cases |
| GET / POST | `/api/v1/users/me/privacy-requests` | Read/submit personal-data requests |
| PUT | `/api/v1/users/me/marketing-consent` | Separate optional marketing decision |
| POST | `/api/v1/files`, `/api/v1/files/{fileId}/complete` | Authorised upload and safe completion |
| Staff APIs | `/api/v1/admin/providers`, `/reviews`, `/support-cases`, `/payments`, `/refunds`, `/plans`, `/advisories`, `/privacy-requests` | Role-restricted actions; each collection uses `/api/v1/admin` |
| POST | `/webhooks/paystack` | Signed provider-billing payment notifications |

Nested action paths share the stated parent. There are no booking, quote-acceptance, deposit or calendar-reservation APIs in the MVP. No endpoint allows arbitrary payment, verification or subscription status editing.

Use UUID identifiers, UTC ISO-8601 timestamps and GHS integer pesewas. Collections use stable cursor pagination, default 20 and maximum 100. Reuse the implemented `ApiProblemResponse` for `application/problem+json`: `title`, `detail`, `key`, `instance`, optional `message` and optional field-to-message `errors`; null fields are omitted. The HTTP response carries the status. No `code`, `traceId` or JSON `status` field is currently added by this shared module. Planned endpoints must use this contract consistently.

`CeremoException` supplies an error key, status and message parameters. Assertion errors from primary adapters return 400; assertion failures originating in application/domain code return 500. Bean Validation returns 400 with an `errors` map: field errors use their field name, and object-level errors use the binding object's name. Repeated keys keep the first message; missing or blank validation messages fall back to `Invalid value`. The new lowest-priority MVC fallback preserves framework status and headers, maps optimistic conflicts to 409 and unexpected failures to a safe 500. It preserves committed responses and rethrows Spring Security denials for the existing filter chain. Bearer-filter failures retain their standard challenge; they do not promise the MVC problem body. English is the message fallback, with French bundles retained. Adding these bundles is not a frontend language selection. Handlers avoid rejected values and exception parameters in logs. Message templates belong in `messages/assertions-errors` and `messages/errors/ceremo-errors-messages`.

Planned endpoint statuses include 400 malformed data, 401 authentication, 403 permission/entitlement, 404 missing/inaccessible private data, 409 stale version/conflict, 422 business-rule failure, 429 rate limiting and 503 dependency failure. The copied `ErrorStatus` enum currently exposes its reference statuses; add any additional status and message key together when implementing the endpoint that needs it.

Use optimistic version checks for mutable profiles/preferences. Use `Idempotency-Key` for enquiry submission, invoice creation and financial writes; store actor, method/path, request hash and result for 24 hours. A reused key with different data returns 409. Database uniqueness protects longer-lived financial facts. Repeated contact actions must not inflate analytics through retries.

## 4. Authentication and permissions

The shared adapters provide a stateless bearer-token chain for `/api/**` and `/management/**`, plus a separate OAuth2 browser-login chain with CSRF protection. Public GET access covers catalogues and limited provider summaries. Provider detail paths and their subroutes require both authentication and an ACTIVE persisted local account, through `ActiveAccountAuthorizationManager` and the existing `Accounts.requireActiveAccount()` port. Staff routing requires `ROLE_ADMIN`; use-case checks still enforce ownership, accepted membership, recent MFA where required and provider eligibility. Registration never grants general tenant access.

| Caller or scope | Permitted access | Restriction |
| --- | --- | --- |
| Anonymous visitor | Catalogues and limited provider summaries | No full profiles, detailed reviews, contact destinations or private tenant records. |
| Authenticated identity without a local account | Registration and applicable own-account operations | A signed JWT or role cannot bypass the profile/contact gate. |
| PENDING, SUSPENDED or ANONYMISED account | Applicable own-account/status and recovery/logout operations | No full provider profiles or contacts; restricted accounts cannot self-reactivate. |
| ACTIVE registered account | Eligible customer-facing profiles, detailed reviews and approved contacts | No private tenant team, billing, evidence or internal notes. |
| Accepted provider member | Own tenant workspace and specifically authorised provider operations | Owner-only team control remains owner-only; FINANCE/MANAGER are not staff grants. |
| Authorised staff | Explicitly scoped administrative/case/finance operations | ROLE_ADMIN alone does not authorise the general member-only tenant read; current tenant administration also requires active account and recent trusted MFA. |

Public summaries use an allowlisted response: approved business name, service/category, locality, thumbnail, indicative price/unit and check/rating summaries. Omit direct contact targets, full portfolios/profile content, private tenant identifiers/owner/team and billing/evidence fields. Full profile/contact responses are private and no-store; apply the same rules to frontend HTML, JSON-LD and caches. The planned feature use cases must repeat account and record checks at execution time; the route guard does not replace them.

Configure `spring.security.oauth2.client.provider.oidc.issuer-uri`, the `oidc` client registration and `application.security.oauth2.audience`. The decoder checks JWT signatures, issuer, audience and standard time validity. It reads signed claims locally. `CustomClaimConverter` is copied for optional userinfo enrichment but is not wired into decoding; it does not retain requests in a global cache and rejects mismatched userinfo subjects. Configure CEREMO-specific `ROLE_CUSTOMER`, `ROLE_PROVIDER`, `ROLE_SUPPORT`, `ROLE_FINANCE` and `ROLE_ADMIN` values in a `roles` or `groups` array, or the optional `urn:ceremo:roles` claim. Do not map Keycloak realm-management privileges into CEREMO staff roles. Provider membership remains authoritative for business access.

`application.cors.allowed-origins` defaults to an empty list. The earlier `application.security.allowed-origins` setting remains a fallback; explicit `application.cors` origins take precedence. List exact web origins when a frontend is connected. The separate browser OAuth2 client can hold authorised-client tokens in server-side storage; its production session/token storage is still an integration decision. The isolated tests cover public/private routes, role restrictions, session separation, CSRF, CORS and signed token rejection. Live Keycloak discovery/JWKS rotation remains to be tested.

Keycloak owns credentials, MFA, login recovery and refresh tokens. CEREMO is a Spring OAuth2 resource server validating signature/JWKS, configured issuer, audience `ceremo-api`, required expiry/issued-at/session, user-token type and not-before when present with bounded clock skew. Reject ID tokens and foreign realms. Bind verified `(issuer, subject)` to an application user; never merge by matching email/phone. [Spring JWT resource server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).

Check application status, provider membership, staff roles and current entitlement independently of JWT claims on protected actions. Public discovery needs no authentication. Persistent preferences, enquiries and reviews require an active account and verified contact as applicable. MFA and recent authentication are required for staff and sensitive account/billing changes.

Proposed realm defaults: five-minute access tokens, 30-minute idle and 12-hour maximum sessions, refresh rotation with reuse disabled, no offline access and no password grant. JWTs may survive logout until expiry; use local subject/session revocations where immediate rejection is required. Notify the previous contact after reviewed contact recovery and audit sensitive changes. Test key rotation, suspension, recovery and logout.

Application user tables store no password or MFA seed. API bearer validation does not store refresh tokens; the optional browser OAuth2 client needs protected authorised-client storage as described above. Planned contact verification uses eight random digits, five-minute expiry, single use, a keyed HMAC with a server-only pepper, five attempts and resend invalidation. Fake development SMS must not create a production bypass.

The Next.js session library/strategy remain `null`. A server-held token approach with an opaque HttpOnly browser session is proposed, requiring protected session storage, PKCE, exact redirects, refresh coordination, CSRF/origin controls and logout handling. Never put access/refresh tokens in localStorage, URLs or logs. [Next.js authentication](https://nextjs.org/docs/app/guides/authentication), [OWASP sessions](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html), [Keycloak configuration](https://www.keycloak.org/server/configuration).

## 5. Preferences, discovery and promotion

The guide is optional, skippable, editable and resettable. Separate current event needs from customer-selected persistent category interests. Save unknowns as null. A budget needs category, currency and unit; do not compare per-guest starting prices to a whole-event budget without a valid conversion and known quantity. Avoid collecting exact private addresses for discovery.

Build an eligible result set from approved providers, published matching services, locality coverage, accepting-enquiries state and an actual current trial/paid period. Suspension overrides payment. Calculate entitlement from timestamps, not just an ACTIVE flag; cache invalidation alone cannot enforce expiry. Recheck on contact/enquiry access. Inactive historical profiles may retain safe review history, without active contacts.

Current explicit search criteria take precedence over saved defaults. Default search is not five-star-only. Show raw published-review average/count or no reviews; keep any credibility-weighted ranking distinct from the displayed score. A new-provider rotation must use genuine eligible inventory. Ranking rules and their version must be reviewable; weights/thresholds remain unapproved.

If enabled, sponsored candidates meet the same relevance/safety/entitlement rules plus the purchased tier's promotion benefit. Return labelled sponsored results separately from ordinary matching results, avoid duplicate provider appearances, and use stable cursor/rotation rules. No guaranteed first position, paid stars or hidden review changes. Missing matches produce a truthful empty state.

Do not distribute stored interests or budgets to subscribers. The customer approves the brief/contact fields shared with one selected provider. Personalisation controls and marketing consent are separate. [Matching and privacy research](01-ghana-research.md#7-subscription-model-comparisons).

## 6. Enquiries, contacts and reviews

An enquiry holds an immutable submitted brief snapshot, chosen provider service and optional event-service reference. It has NEW/ACKNOWLEDGED/CLOSED/SPAM states. No chat, on-platform contract or payment is required. Source-labelled outcome reports are separate; customer and provider reports can disagree and neither is a PSP-verified booking.

Business contact actions use only approved stored targets; clients cannot supply arbitrary redirect URLs. Require an authenticated ACTIVE local account and recheck provider eligibility before contact release. Optional tracking or marketing consent is separate. Store minimal permitted analytics and distinguish clicks, requests, qualified enquiries and reported agreements. Opening a contact must not silently send the customer's brief or private profile to the provider. Outside communication remains permitted; do not monitor those conversations.

Reviews concern genuine service experiences, not completed CEREMO bookings. Require author identity, provider/category, an actual-experience declaration and an evidence/moderation process. Display CUSTOMER_REPORTED or EVIDENCE_REVIEWED; neither implies CEREMO processed payment or inspected the service. Prevent duplicate/spam/conflicted submissions through documented checks. Providers can reply/report but cannot veto legitimate criticism. Preserve moderation reasons and appeal history independently of paid tier.

Private selected evidence is access-controlled and minimised. Never require entire WhatsApp histories, PINs or OTPs. Support can investigate profiles and external-service complaints, but cannot promise an outside deposit refund or replacement.

## 7. Provider trials, subscriptions and billing

Grant one trial atomically at first approved publication. Store its actual start/end and the approved policy version. No configured duration means no invented trial promise. Recommend no automatic trial charge. Provider changes and duplicate accounts must not restart the trial.

Issue versioned, fixed-price monthly invoices with accepted benefits and cancellation/refund terms. Manual hosted MoMo/card renewal is the initial design. Paystack automatic subscriptions do not provide Ghana MoMo auto-debit. [Subscriptions](https://paystack.com/docs/payments/subscriptions/), [payment channels](https://paystack.com/docs/payments/payment-channels/).

Save the expected payment and reference before contacting Paystack, outside the database transaction. Verify raw-body HMAC SHA512 webhooks, persist a durable receipt, acknowledge, then independently verify the transaction reference, success, exact amount and currency. A browser callback cannot activate a plan. [Webhooks](https://paystack.com/docs/payments/webhooks/), [verification](https://paystack.com/docs/payments/verify-payments/).

Lock the subscription/invoice when granting a period. Grant each paid invoice once; do not overlap paid periods. An early payment starts after unused trial/paid entitlement; an expired renewal starts at verified payment time. Preserve the saved monthly anchor with end-of-month clamping. Reconcile uncertain references before another charge. Duplicate successes or mismatched payments open billing review without additional entitlement.

Expiry pauses active listing/contact/promotion, preserving management, invoices, support and history. Cancellation preserves accepted paid time. Subscription refund/revocation needs a reasoned case and independent approval; cap pending plus completed refunds at the verified collected amount and reconcile uncertain PSP submissions. No provider service-payment splits, payouts, settlement ledger or commission policy is needed. [Refund API](https://paystack.com/docs/payments/refunds/).

## 8. Abuse controls, uploads and jobs

Proposed limits: public search 120/IP/minute; authenticated writes 60/account/minute; contact actions 30/IP/minute; enquiries five/account/hour and a separate provider-target abuse limit; contact-code sends three/number/15 minutes and ten/IP/hour with a 60-second resend gap; five verification attempts; uploads 20/provider/hour. Tune through load and abuse testing. Use atomic database counters, keyed identifiers, separate webhook capacity and `Retry-After` responses. Bound ordinary JSON/webhook bodies at 256 KiB.

S3 uploads use unique keys, five-minute signed URLs, quarantine and scanning. Portfolio JPEG/PNG/WebP and approved private evidence PDF/images have a proposed 10 MiB cap. Verify content, strip image location metadata, create safe public derivatives and retain rights confirmation. Never publish verification/review evidence. Signed URLs are reusable bearer credentials until expiry. [S3 signed URLs](https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-presigned-url.html).

Commit notification work with the business change through an outbox. Lease claims, use `FOR UPDATE SKIP LOCKED`, retry/backoff/deduplication and a failed-job queue. Encrypt any temporary OTP delivery value and remove it after send/expiry. Separate operational messages from consented marketing.

| Job | Proposed schedule |
| --- | --- |
| Pending subscription payment checks | Every minute, backing off as attempts age |
| Notification processing | Every few seconds |
| Trial/paid expiry and check expiry | Hourly; request-time checks still enforce actual expiry |
| Renewal notices | Daily, respecting preferences and accepted terms |
| Provider-billing refund reconciliation | Daily and on relevant PSP events |
| Retention and analytics cleanup | Daily, respecting legal holds |

## 9. Environments and hosting

Use isolated local, test, staging and production databases, realms, files and secrets. Development/tests use fake SMS/payment adapters; staging uses synthetic data and PSP test keys. Production rejects development issuers and missing commercial configuration.

| Configuration | Purpose |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | Environment |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Application JDBC credentials |
| `spring.security.oauth2.client.provider.oidc.issuer-uri`, `application.security.oauth2.audience` | Implemented Keycloak issuer and allowed API audiences; configure the `oidc` client registration separately |
| `application.cors.allowed-origins`, `CEREMO_API_BASE_URL` | Implemented exact client origins; proposed API base URL setting |
| `CEREMO_OTP_PEPPER`, `CEREMO_DATA_ENCRYPTION_KEY` | Separate versioned secrets |
| `CEREMO_PAYSTACK_SECRET_KEY`, `CEREMO_PAYSTACK_CALLBACK_URL` | Server-only provider billing |
| `CEREMO_SUBSCRIPTION_CATALOG_VERSION`, `CEREMO_TRIAL_POLICY_VERSION` | Approved prices, benefits and trial duration |
| `CEREMO_PAYMENTS_ENABLED`, `CEREMO_SUBSCRIPTIONS_ENABLED`, `CEREMO_PROMOTIONS_ENABLED`, `CEREMO_SMS_ENABLED` | Separate live switches, initially false |
| `CEREMO_HUBTEL_CLIENT_ID`, `CEREMO_HUBTEL_CLIENT_SECRET`, `CEREMO_HUBTEL_SENDER_ID` | SMS integration |
| `CEREMO_S3_REGION`, `CEREMO_S3_PRIVATE_BUCKET`, `CEREMO_S3_PUBLIC_MEDIA_BUCKET` | File storage |
| `CEREMO_RETENTION_POLICY_VERSION` | Approved cleanup rules |
| `SONAR_HOST_URL`, `SONAR_TOKEN` | Analysis only, excluded from runtime |

These proposed custom settings need validated binding; standard datasource properties already apply. The implemented `application.registration.terms-version` binds through `CEREMO_TERMS_VERSION` and fails startup when absent/invalid. Flyway owns schema changes. Current runtime Hibernate `ddl-auto` is `none`; schema validation is required in migration-backed checks and must be explicitly configured where used. Runtime and migration users have distinct permissions. Hosting remains proposed: API and separate production Keycloak services with isolated persistent PostgreSQL databases. Confirm cost, backups, cross-border processing and private connection TLS for the selected deployment. [Render Docker](https://render.com/docs/docker), [regions](https://render.com/docs/regions), [PostgreSQL](https://render.com/docs/postgresql-creating-connecting).

Use HTTPS, HSTS, restricted admin access, explicit trusted proxies and least-privilege file access. Keep secrets and private data out of frontend props, caches and public environment variables. The [frontend brief](05-ui-ux-design-brief.md) retains the selected stack and open browser-session choices.

## 10. Verification and operations

The following are planning proposals, not approved service commitments or measured results. Product/engineering must approve the workload, measurement point, error definition, observation window and capacity budget before launch.

| Quality scenario | Proposed target | Acceptance evidence |
| --- | --- | --- |
| Service availability | 99.5% monthly | Define eligible requests, downtime and dependency treatment; demonstrate alerting against that definition. |
| Public reads | p95 below 500 ms at 50 concurrent users | Representative data/query mix, cache conditions and measured API boundary. |
| Business writes | Below one second excluding external services | Define percentile and write mix; verify complete transaction outcomes under contention. |
| Payment receipt | Durable acknowledgement within two seconds | Test duplicates, invalid signatures and restart after receipt/before processing. |
| Pending collections | Recent pending payments checked within 15 minutes under normal processor availability | Observe worker lag, retry limits and operator escalation. |
| Recovery | RPO 24 hours; RTO four hours | Restore application/identity data, keys and reconciled billing in an isolated drill. |

Security/integrity acceptance is independent of latency: cross-provider access must fail, repeated verified events must grant once, and expiry/suspension must be enforced at use time. Numeric business/pilot thresholds remain open in the [PRD](02-product-requirements.md).

Test ownership/JWTs, relevance and entitlement boundaries, optional/reset preferences, sponsored labels, cold-start reviews, review permissions, one-time trial concurrency, end-of-month renewal, duplicate/late payments and refund caps on real PostgreSQL. Use [JaCoCo and SonarQube](08-development-and-quality.md) with the tests; a coverage percentage does not establish correctness.

Log trace/record IDs with redaction. Never log tokens, codes, raw credentials, private budgets or full contact data. Monitor empty results, enquiries, abuse, job failures, entitlement expiry, billing mismatches and moderation backlog. Distinguish reported service outcomes from verified subscription collections.

Test encrypted application/Keycloak backup restoration and key rotation. Reconcile PSP invoices/payments before resuming billing after restore. Build one image, promote through staging, use compatible migrations and document rollback or forward repair. No marketplace feature is currently implemented merely because it appears in these requirements.

## 11. Architecture decisions

Decision records are kept here within the approved nine-document set. The current subscription-only model is in section 11.6. Records 11.2 and 11.3 preserve the superseded transaction design and are not implementation instructions.

### 11.1 Java backend and PostgreSQL

Status: Java/Spring/feature-based backend selected and partially implemented; production PostgreSQL version/hosting proposed. Decision date: 19 October 2026; status reviewed 4 November 2026.

#### Context

The project starts with Java 25, Spring Boot 4.1.1 and Gradle. Provider publication, subscription entitlement and platform billing need consistent database changes. The team has not set a budget or launch date.

#### Decision

Keep one marketplace application with business packages such as `providermanagement`, `discovery` and `subscriptionmanagement`. Use PostgreSQL 17 and Flyway. Keep pending notifications and scheduled work in the database initially. Run Keycloak separately with its own database/user. The current product scope is defined by [ADR 0006](03-technical-requirements.md#116-provider-subscriptions-and-discovery).

#### Reasons and consequences

This keeps entitlement/billing changes in one database and avoids operating several marketplace services before the need exists. Features still need clear application interfaces and separate ownership of their repositories. Revisit a split when measured load or team responsibilities justify it.

Render is a proposed backend hosting option, subject to cost and data-processing review. The separate frontend stack is recorded in [ADR 0005](03-technical-requirements.md#115-frontend-stack).

References: [technical requirements](03-technical-requirements.md), [implementation tasks](07-implementation-plan.md).

### 11.2 Earlier quotes and calendar reservations, superseded

Status: Superseded for the MVP by [ADR 0006](03-technical-requirements.md#116-provider-subscriptions-and-discovery). Date: 19 October 2026. The text below records the earlier transaction design; it is not current build scope.

#### Context

Providers quote for different hours, locations and deliverables. A business may have several crews, while a solo provider has one schedule. Two customers can try to accept the same time.

#### Decision

A booking accepts one quote for one requested service and event session. The quote names a provider calendar. Acceptance saves the terms and a 30-minute hold atomically; verified deposit payment and a valid reservation confirm it. PostgreSQL rejects overlapping active calendar entries, including manual blocks.

#### Reasons and consequences

Named calendars protect the capacity actually sold without blocking the entire company. Availability shown in search remains indicative until acceptance. Late payment after expiry requires safe rebooking of the time or a refund case. Several provider bookings remain separate agreements.

References: [journeys](04-app-flow-and-user-journeys.md), [database design](06-database-architecture.md).

### 11.3 Earlier commission and payments, superseded

Status: Superseded by [ADR 0006](03-technical-requirements.md#116-provider-subscriptions-and-discovery). Date: 19 October 2026. The text below records the earlier revenue/payment design; only provider subscription billing remains in the MVP.

#### Context

CEREMO will earn booking commission and provider subscription revenue. Mobile Money authorisation is asynchronous. The reviewed Paystack subscription service does not support Ghana MoMo auto-debit.

#### Decision

Use hosted checkout, signed webhooks and server payment verification. Calculate commission proportionally on booking instalments using the accepted rate. Issue separate subscription invoices and grant a monthly period once after a verified one-off renewal payment.

Keep payment collection, provider settlement, service completion and refund progress separate. Use ordinary PSP-approved settlement; do not promise escrow or a platform wallet.

#### Requirements before launch

Agree prices, fee/tax treatment, cancellation terms and refund funding. Obtain PSP approval for the actual provider/subscription money flow. Live charges must fail when required commercial rules are missing. Automatic card renewal can follow later with consent and supported authorisation.

References: [research and payment sources](01-ghana-research.md), [technical requirements](03-technical-requirements.md).

### 11.4 Keycloak and quality tools

Status: Tools selected and local/build setup implemented; production identity and Sonar server gate unverified. Decision date: 19 October 2026; status reviewed 4 November 2026.

#### Context

Keycloak is the selected identity provider in development and production. The team will use SonarQube for code analysis and JaCoCo for coverage. OAuth2 adapters exist under `shared/authentication`; SonarQube/JaCoCo Gradle tasks are configured. A passing server analysis/gate and production identity integration remain unverified.

#### Decision

Keycloak owns login, credentials, MFA, recovery and refresh tokens. CEREMO validates JWTs and keeps its own user/provider permissions. Separate development and production issuers, data and configuration.

Run JUnit/Spring tests, generate JaCoCo XML/HTML, verify combined coverage and submit analysis through SonarScanner for Gradle. A failed or missing required gate blocks promotion.

#### Reasons and consequences

The backend avoids duplicating credential/recovery code. Keycloak adds a production service and database that need backups, key rotation and operational ownership. SonarQube/JaCoCo stay outside the application runtime. Coverage supplements discovery, review, billing and security tests; it does not replace them.

Reference: [development tools](08-development-and-quality.md).

### 11.5 Frontend stack

Status: Accepted for stack selection. Date: 19 October 2026.

#### Context

CEREMO needs a web interface for public provider discovery and private customer, provider and staff work. The backend uses Java and Spring Boot, with Keycloak for authentication. The founder has supplied the frontend stack; earlier documents left it unselected.

#### Decision

Use Next.js with React and TypeScript, Tailwind CSS, shadcn/ui, TanStack Query, and React Hook Form with Zod. The application will consume the Spring Boot REST API. Keep marketplace rules and database access in the Java backend.

The selection replaces the earlier `null` frontend stack. It does not select library versions, hosting, the routing model, an authentication client library or visual design. Those decisions remain open in the [frontend brief](05-ui-ux-design-brief.md).

#### Reasons and consequences

Next.js supports server-rendered and static public content alongside interactive account work. Rendering and SEO still require page-specific decisions. TanStack Query manages interactive server state, while React Hook Form and Zod manage forms and validation. shadcn/ui provides component source the project can adapt; the team must maintain those adaptations.

Use separate policies for public and private caching. Spring Boot must validate requests and permissions regardless of frontend checks. Keycloak remains the identity provider; the browser session design needs its own integration work.

Frontend implementation remains outside the current backend task plan. Pin compatible versions and define the project location, commands and tests when the frontend is created. This decision adds no packages or runtime configuration to the backend.

References: [frontend brief](05-ui-ux-design-brief.md), [technical requirements](03-technical-requirements.md), [Next.js rendering](https://nextjs.org/docs/app/getting-started/server-and-client-components), [TanStack Query SSR](https://tanstack.com/query/latest/docs/framework/react/guides/advanced-ssr), [shadcn/ui](https://ui.shadcn.com/docs).

### 11.6 Provider subscriptions and discovery

Status: Business model accepted; detailed policies proposed. Date: 19 October 2026.

#### Context

The earlier plan earned commission from service payments and sold provider subscriptions. The founder has approved replacing it with provider subscriptions only. Customers and providers can arrange conversations, meetings, contracts and service payments directly.

#### Decision

Customers browse and find providers free. Providers register free, receive a trial for an approved listing and choose a paid subscription afterwards. Trial length, prices and tier limits remain `null`. Verification assessment and labelled sponsored placement may be defined as subscription benefits; separately charged checks, boosts and leads are not approved.

Remove booking commission, customer service checkout, PSP provider splits, quote acceptance, calendar holds and service-payment refunds from the MVP. Retain hosted payment and verification for CEREMO's provider invoices. Use manual monthly renewal initially; do not assume Ghana MoMo auto-debit.

Use optional customer preferences and current occasion needs to match eligible providers. Keep subscription entitlement, actual verification results, sponsored visibility and customer ratings independent. No default five-star requirement or purchased stars. Outside service outcomes require source-labelled reports; reviews need their own evidence and moderation policy.

#### Consequences

Provider renewal becomes the main commercial validation. CEREMO must demonstrate relevant demand and accurately report contact/enquiry activity. It does not confirm availability, service contracts or external payments. It still handles profile/review complaints and its own billing issues.

This decision supersedes [ADR 0002](03-technical-requirements.md#112-earlier-quotes-and-calendar-reservations-superseded) for the MVP and replaces the revenue/payment scope of [ADR 0003](03-technical-requirements.md#113-earlier-commission-and-payments-superseded). Their original text remains as decision history. The Java/PostgreSQL, Keycloak and frontend selections continue.

References: [research](01-ghana-research.md#7-subscription-model-comparisons), [PRD](02-product-requirements.md), [technical requirements](03-technical-requirements.md).

### 11.7 Account activation from verified Keycloak email

Status: approved by the founder and implemented; live issuer/email delivery verification remains outstanding. Date: 4 November 2026.

The existing domain factory creates PENDING. Registration activates the new account only when the trusted identity adapter reads the boolean `email_verified=true` from validated Keycloak authentication. Missing, false and incorrectly typed values do not activate it. A later `POST /api/v1/users/me/activate` uses a refreshed token and transitions only PENDING to ACTIVE; repeating it for ACTIVE performs no further write. SUSPENDED and ANONYMISED remain restricted.

GET is read-only and may return the authenticated person's own saved profile/status, including a restricted status. It does not enable protected marketplace operations. Email verification grants no provider membership, staff permissions, publication approval or subscription access. Future protected use cases still require their specific checks.

Activation and registration use the existing outer write transaction. The status update retains inherited optimistic versioning; a conflicting update returns 409 rather than silently overwriting it. No schema change or replacement identity key is required.

The request accepts only `displayName`, `termsVersion` and affirmative `termsAccepted`. Unknown registration fields are rejected. The self response contains only `id`, `displayName`, `status`, `termsVersion` and `termsAcceptedAt`, with no-store caching. GET is 200/404; registration is 201 with a self-profile Location, duplicate 409 and invalid/stale terms 400. Activation is 200, unverified/restricted 403 or missing account 404. Bearer failures preserve the security filter's 401 challenge; MVC errors use the existing problem envelope.

References: [OpenID Connect standard claims](https://openid.net/specs/openid-connect-core-1_0.html#StandardClaims), [Keycloak email verification](https://www.keycloak.org/docs/latest/server_admin/index.html), and [verification record](08-development-and-quality.md#88-account-http-and-email-activation). The specification defines the claim, not CEREMO's activation policy. The token is a point-in-time statement; this change does not implement ongoing email-change handling or account revocation.

### 11.8 Provider-business tenants, account access and logout

Status: provider-business tenant model approved by the founder; implementation recorded 5 November 2026. The [implementation evidence](../.github/research/ports-and-adapters/provider-account-security.md) records the expressly authorised Salesync references, primary sources, adaptations and limits.

Each provider business has one tenant with an accountable owner. Customers keep independent accounts and may belong to several businesses. Creating an ACTIVE workspace grants no approved listing, trial, subscription, verification badge or stars. The future provider-profile record will reference a unique tenant; profile/publication remains unimplemented.

`usermanagement` retains registration and activation. `account` adds current-account and active-account/staff checks. `tenant` owns workspaces, team invitations, acceptance, revocation, suspension and audit. `logout` owns current-session revocation. All use the plain core, repository ports and outer adapters/transactions. No Keycloak admin SDK, organisation provisioning or new runtime dependency is introduced.

| Implemented route | Result and authority |
| --- | --- |
| GET `/api/v1/account` | Own saved profile/status, using the existing safe response. |
| POST / GET `/api/v1/tenants` | Create a free workspace / list own workspaces; active account and bounded pagination. |
| GET `/api/v1/tenants/{tenantId}` | Private workspace for its owner or accepted member; foreign/missing ID returns 404. |
| GET / POST `/api/v1/tenants/{tenantId}/members` | Owner lists the team / invites a registered user ID as MANAGER or FINANCE. |
| POST `/api/v1/tenants/{tenantId}/membership/accept` | Current active invitee accepts; an invitation alone grants no access. |
| DELETE `/api/v1/tenants/{tenantId}/members/{userId}` | Owner removes a team member/invitation; cannot remove the owner. |
| POST `/api/v1/admin/tenants/{tenantId}/suspend` or `/reactivate` | Active ROLE_ADMIN account, recent trusted MFA and a reason. |
| POST `/api/v1/logout` | Commit session revocation, return 204; further tokens from that session return 401. |
| POST `/logout` | CSRF-protected browser logout, persistent revocation and Spring's registered-provider OIDC redirect. |

Point operations lock the tenant root before a fresh membership read. Mutation and audit share one transaction under PostgreSQL READ COMMITTED. Suspended owners may inspect and revoke team access; invitations/acceptance are blocked. Future listing/enquiry/billing operations still require the specific member role, tenant status and applicable entitlement. Workspace reads grant no general edit permission.

Bearer authentication now requires the configured signature/issuer/audience and Keycloak user-token claims: payload `typ=Bearer`, bounded nonblank `sub`/`sid`, `iat`, `exp`, consistent lifetime and permitted time validity. `nbf` is validated when present. ID tokens and sessionless service tokens are rejected. Revocation is read from the shared database after validation; storage failure denies authentication. No stale revocation cache is used.

Tenant administration requires `amr` containing `mfa` and a trusted Long epoch-second `auth_time` within five minutes, with no future time. The actual Keycloak authentication flow must establish this assurance; an editable user attribute is insufficient. Ordinary admin tokens remain denied until the live flow is configured and tested.

API logout revokes this CEREMO session, including refreshed tokens with the same sid; the client must clear local credentials and complete standard IdP logout to end SSO. Browser logout uses the registered endpoint and encoded ID-token hint, without an Origin-derived return location. No post-logout return URI is selected. Already in-flight requests, other sessions and IdP-initiated logout are separate cases; back-channel processing remains unimplemented. Cleanup must retain revocations for the relevant refresh/session lifetime.

The unused application admin credential has been removed. New development realm imports enable verification, brute-force controls, refresh rotation and events, and disable the unused admin service-account client. Existing realm volumes are unchanged. Real SMTP, assurance, credential rotation, request limits and release checks remain operational work. [Spring OIDC logout](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/logout.html), [OWASP tenant isolation](https://cheatsheetseries.owasp.org/cheatsheets/Multi_Tenant_Security_Cheat_Sheet.html), [PostgreSQL isolation](https://www.postgresql.org/docs/18/transaction-iso.html).

### 11.9 Consolidate the account module

Status: approved by the founder and implemented, 5 November 2026. This supersedes the separate `usermanagement`/`account` package arrangement in ADR 11.8; its business and security policies continue to apply.

One `account` module owns the local user model, identity mapping, registration, activation, account status and protected account/staff checks. `UserService` handles registration/activation; `AccountService` handles access checks inside the same feature. Repository contracts, DTOs, JPA/security adapters and tests moved with their responsibilities. The single `wire/account/AccountConfiguration` composes the lifecycle and access-check beans with the existing UTC Clock, UUID generator and complete transaction decorator.

The Java package move adds no business rule or database migration. Existing `/api/v1/users/me`, `/api/v1/account`, activation, tenant and logout contracts continue. SQL tables, identity links, terms and stored account statuses are preserved. Tenant and logout import the account feature's existing contracts and identity value instead of the removed package. Architecture tests reject the legacy production package and retain the plain-core and HTTP-adapter dependency rules.

This choice follows [Spring's module guidance](https://docs.spring.io/spring-modulith/reference/fundamentals.html): group cohesive functionality behind explicit interfaces. Spring Modulith is a design reference here, not an installed dependency or a claim that CEREMO passes its module verifier. Existing [TransactionTemplate](https://github.com/spring-projects/spring-framework/blob/v7.0.9/spring-tx/src/main/java/org/springframework/transaction/support/TransactionTemplate.java) composition is retained. See the [research and verification record](../.github/research/ports-and-adapters/provider-account-security.md#account-module-consolidation-5-november-2026).

### 11.10 Registered profile and contact access

Approved 5 November 2026. This supersedes earlier unconditional public-profile/contact access. Visitors retain free catalogue search and limited provider summaries; full provider profiles, detailed reviews and contact destinations require free ACTIVE local registration. Private tenant records require accepted membership and operation-specific owner/role permission. Customers remain independent of provider tenants. Do not create an all-powerful REGISTERED role or derive membership from token organisation claims.

Reuse `Accounts.requireActiveAccount()` through a small outer HTTP authorisation adapter. Missing or restricted local accounts are denied at the profile gate; other account/storage failures propagate without granting access. JWT validation, revocation, CORS, browser CSRF and existing tenant transactions remain unchanged. The gate does not introduce a payment, mandatory survey, marketing enrolment, chat platform or phone OTP. No schema migration is required for the route boundary. Provider publication/contact-entitlement checks and safe summary/detail DTOs remain required when those features are built.

Evidence reviewed 5 November: [Jiji Ghana privacy policy](https://jiji.com.gh/privacy.html) establishes a local registered-contact precedent, not a retention result. Spring Security [request authorisation](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html) supports custom access managers; pinned [7.1.1 RequestMatcherDelegatingAuthorizationManager](https://github.com/spring-projects/spring-security/blob/7.1.1/web/src/main/java/org/springframework/security/web/access/intercept/RequestMatcherDelegatingAuthorizationManager.java) delegates the first matching request rule to its manager. [OWASP API3](https://api-security.owasp.org/editions/2023/en/0xa3-broken-object-property-level-authorization/) supports property-level response restriction. The current manual is 7.1.2; source matches installed 7.1.1, Apache-2.0. The CEREMO adapter is original code calling existing APIs, not copied framework source. Detailed evidence, expected denials and actual verification belong in the [research record](../.github/research/product/customer-contact-retention.md).

Trade-off: the gate adds registration/verification friction and may lose useful connections. Review contact completion, abandonment, repeat discovery and provider renewal alongside registrations. No retention improvement is claimed. Existing provider profile/review/contact controllers are not yet implemented; test fixtures establish route protection rather than marketplace completion.

## 12. Architecture risks and approval points

| Risk or unresolved choice | Delivery consequence | Required response |
| --- | --- | --- |
| Local PostgreSQL 18.0 versus proposed production 17 | Passing local migrations do not prove production-version compatibility. | Select the production version and test its migrations, constraints and restoration before release. |
| Listing eligibility and stale clients/caches | Expired/suspended providers could remain actionable. | Enforce authoritative request-time checks and test changes between search, contact and enquiry. |
| Payment uncertainty and worker failure | Duplicate collection, access errors or delayed refunds. | Test receipt durability, reconciliation, idempotency and manual escalation before live charges. |
| Unselected browser session strategy | Private caching, CSRF, refresh and logout behaviour remain incomplete. | Review the client/security integration before authenticated frontend work. |
| Unapproved offer/trust/privacy policy | Technical defaults could become unintended commercial or privacy promises. | Bind approved versions; keep affected live features unavailable until requirements are resolved. |
| Unassigned operational ownership | Alerts/backups do not ensure response or restoration. | Assign named owners and demonstrate release/incident/recovery procedures. |

These are implementation conditions, not changes to the approved subscription model. [arc42](https://arc42.org/overview/) informs the separation of context, decisions, quality scenarios and risks; CEREMO's existing document/ADR locations are retained. [Operational readiness guidance](https://docs.aws.amazon.com/wellarchitected/latest/operational-excellence-pillar/ops_ready_to_support_const_orr.html) supports evidence-based release review without selecting AWS hosting.

Editorial verification: version 3.4, 5 November 2026; Grammarly pending.
