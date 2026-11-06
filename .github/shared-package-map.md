# CEREMO shared package reuse map

Access update, 5 November 2026: `SecurityConfiguration.filterChain(HttpSecurity, Accounts)` keeps only catalogue/limited-summary GETs public and delegates provider detail/subroutes to [ActiveAccountAuthorizationManager](../src/main/java/io/ceremo/ceremobackend/account/infrastructure/primary/security/ActiveAccountAuthorizationManager.java). It reuses `Accounts.requireActiveAccount()`; private tenant permission still belongs in the tenant use cases. This supersedes older public-profile/review allowlist descriptions below. Shared inventory remains 88 Java files/12 packages.

Reviewed from the current source on 20 October 2026: 86 Java files in 12 packages, including 12 `package-info.java` files. This is agent context, not a ninth product document. It records existing behaviour; it does not approve every inherited behaviour for every feature.

Update, 4 November 2026: the focused HTTP work adds `HttpErrorsHandler`, bringing shared to 87 Java files across the same 12 packages. The existing full inventory remains the starting point; source and current checks outrank older entries.

Update, 5 November 2026: **88 Java files in 12 packages**. `authentication/infrastructure/primary/KeycloakAccessTokenValidator.java` now requires interactive Keycloak access-token claims. The existing decoder also checks the logout module's persistent `SessionRevocations` after signature/claim validation, without a revocation cache. The browser chain requires the Logout port and Spring's OIDC success handler; it retains CSRF. See the [implementation record](research/ports-and-adapters/provider-account-security.md) before extending authentication. Reuse plain `TenantId` and `TenantSuspendedException` for the new tenant core; keep the inherited organisation-claim filter/aspects disabled. Tenant database membership is authoritative; existing tier/location fields grant no permissions or billing benefits.

Before sending code or guidance, read the relevant section below, then reopen the actual source and its tests. Source is authoritative. Refresh this map when a shared file, method, dependency or default changes. Do not copy this inventory into each answer.

Check the corresponding [wire package map](wire-package-map.md) and apply the [shared/wire reuse skill](skills/ceremo-shared-wire/SKILL.md) too. Shared supplies reusable behaviour; wire composes runtime infrastructure. Use each only where it fits the feature and DDD dependency boundaries.

All file paths in the tables are relative to the named package under [the shared source root](../src/main/java/io/ceremo/ceremobackend/shared). Tests are under [the matching test root](../src/test/java/io/ceremo/ceremobackend/shared). Read [AGENTS.md](../AGENTS.md#mandatory-reuse-check-before-code-or-guides) for the mandatory workflow.

## Choose the corresponding package

| Need | Existing package | Boundary |
| --- | --- | --- |
| New account/record ID | `generation` | Generate through `CeremoUUID.generate()` at the application/composition boundary. |
| Domain value validation or consistent failures | `error` | Domain assertions and application exceptions; HTTP advice stays in adapters. |
| Signed-in claims, roles, authentication failure | `authentication` | Claims/security-context helpers stay in security adapters; plain role/name types can be used where their semantics fit. |
| Email/name/phone values | `useridentity` | Read normalisation and validation limits before use. |
| JPA UUID, auditing and version | `domain` | Persistence entity superclass, not pure domain superclass. |
| List endpoint pagination | `pagination` | Domain page types in contracts, REST types in controllers, Spring conversion in persistence. |
| Immutable collection snapshots | `collection` | Pure helper; shallow copies, not deep immutability. |
| Matching constants between enum types | `enumeration` | Mapping requires matching names; explicit policy mappings still need explicit code. |
| Local domain-event publication | `event` | Existing publisher port and Spring adapter; no durable delivery guarantee. |
| Monetary calculations | `money` | Major-unit BigDecimal; explicitly convert at pesewa API/database boundaries. |
| Memoising stable calculations | `memoizer` | Per-instance, unbounded, no TTL; unsuitable for changing authorisation/billing state. |
| Legacy tenant context/filtering | `multitenancy` | Restricted inherited subsystem, disabled for current CEREMO membership design. |

## authentication — 17 files

| File | Behaviour and reuse |
| --- | --- |
| `application/AuthenticatedUser.java` | `username()`, `optionalUsername()`, `readPrincipal(...)`, `roles()` and `attributes()`. Uses Spring Security context; rejects unauthenticated/anonymous callers. `attributes()` supports OAuth2/JWT authentication. Reuse in security adapters; extract trusted issuer/subject for account lookup, never substitute `preferred_username`. |
| `application/AuthenticationException.java` | Package-private RuntimeException base for existing authentication failures. Do not create a parallel hierarchy. |
| `application/NotAuthenticatedUserException.java` | Existing missing-authentication failure; reuse in trusted-identity adapters. |
| `application/UnknownAuthenticationException.java` | Existing unsupported-authentication failure. |
| `domain/Username.java` | Nonblank string, maximum 100; `get()` and optional `of(...)`. Does not replace external identity. |
| `domain/Role.java` | ADMIN, PROVIDER, CUSTOMER, SUPPORT, FINANCE, ANONYMOUS, UNKNOWN. `key()` produces `ROLE_...`; `from(...)` expects that exact key and returns UNKNOWN for unmapped nonblank values. |
| `domain/Roles.java` | Immutable set snapshot using `CeremoCollections`; `EMPTY`, `hasRole()`, `hasRole(role)`, `stream()`, `get()`. Does not prove business membership or entitlement. |
| `infrastructure/primary/ApplicationSecurityProperties.java` | Existing `application.security` binding: CSP, origin fallback and audience list. Config class and nested OAuth2 properties stay outside the core. |
| `infrastructure/primary/AudienceValidator.java` | Accepts a JWT if at least one configured audience matches; rejects missing/mismatched audiences. Existing validator, not a new feature validator to duplicate. |
| `infrastructure/primary/AuthenticationExceptionAdvice.java` | Existing problem responses: unauthenticated -> 401; unsupported authentication -> 500. Uses shared `ApiProblemResponse`. |
| `infrastructure/primary/Claims.java` | Package-private authority extraction. Precedence is `groups`, then `roles`, then `urn:ceremo:roles`; keeps string `ROLE_` values, ignores malformed values. Does not automatically map nested Keycloak realm roles. |
| `infrastructure/primary/CustomAuthenticationSuccessHandler.java` | Browser-login success redirects to Swagger. Existing development-oriented behaviour, not customer onboarding. |
| `infrastructure/primary/CustomClaimConverter.java` | Optional userinfo enrichment with subject consistency check. Contains ClaimAppender, StandardClaimAppender, NameClaimAppender, GroupClaimAppender, RolesClaimAppender and SubAttributes. Not wired into current JWT decoding; do not enable it merely to obtain claims already in a validated token. |
| `infrastructure/primary/JwtGrantedAuthorityConverter.java` | Delegates validated JWT claims to existing `Claims` mapping. |
| `infrastructure/primary/OAuth2Configuration.java` | Existing authorised-client manager supports authorization code, refresh tokens with one-minute skew, and client credentials. Not a new login issuer. |
| `infrastructure/primary/SecurityConfiguration.java` | API/management bearer chain is stateless and rejects browser-session/token-cookie authentication; the browser chain retains CSRF and OIDC logout. JWT validation includes issuer/time, audience, interactive Keycloak claims and persistent session revocation. Public catalogue/summary GETs are limited; provider details delegate to the existing ACTIVE-account guard. Tenant membership and recent staff MFA remain use-case checks. CORS comes from `wire/security`, not a CSRF defence. The 6 November security regressions cover unsafe API writes and the documented bearer-only CSRF exception. |

Authentication helpers contain Spring dependencies despite some residing in `application`. They belong behind feature security adapters under our strict DDD rule. Tests include `AuthenticatedUserTest`, `ClaimsTest`, `AudienceValidatorTest`, `JwtValidationTest`, `SecurityConfigurationTest`, `AuthenticationExceptionAdviceTest`, converter, property and OAuth2 configuration tests.

Existing feature reuse: `account/infrastructure/secondary/security/SpringCurrentUser` implements the plain `CurrentUser` port using `AuthenticatedUser.attributes()`, exact configured issuer matching, `UserIdentity` validation and `NotAuthenticatedUserException`. Reuse this adapter for account operations rather than building a second security-context reader. It assumes the shared security chains already validated authentication; it is not a raw-token validator.

## collection — 2 files

| File | Behaviour and reuse |
| --- | --- |
| `domain/CeremoCollections.java` | `immutable(...)` overloads for Collection, Set, List and Map. Null becomes empty. Non-null input is copied and exposed unmodifiable; later changes to the original container do not leak through. Element objects remain shared; do not claim a deep copy or null-element validation. |

Test: `CeremoCollectionsTest`.

## domain — 2 files

| File | Behaviour and reuse |
| --- | --- |
| `BaseEntity.java` | JPA mapped superclass: assigned UUID, created/updated timestamps, created/updated actor strings and nullable Long optimistic version. Protected constructors/setId; getters; equality by exact class and ID. No automatic UUID generation. Two same-class instances with null IDs compare equal; assign IDs deliberately and consider collection use. Reuse only for persistence entities. |

Tests: `BaseEntityTest`; actual audit/version wiring is also exercised in `wire` tests. Timestamp auditing is enabled by `wire/database`; actor auditing still needs an AuditorAware adapter. Never extend this class from plain domain `User`.

## enumeration — 3 files

| File | Behaviour and reuse |
| --- | --- |
| `domain/Enums.java` | `map(from, targetClass)` maps by enum constant name. Null source returns null; target class is mandatory. Uses source declaring class and a concurrent cached mapping for the enum pair. All source constants must map while the mapping is built; not a partial policy translator. Nested EnumMappings and CacheKey are implementation details. |
| `domain/UnmappableEnumException.java` | Package-private failure for incompatible enum names; use the helper rather than recreating its cache. |

Test: `EnumsTest`.

## error — 26 files

| File | Behaviour and reuse |
| --- | --- |
| `domain/Assert.java` | Existing domain validation entry point. `notNull`, `notBlank`, `notEmpty` and overloaded `field(...)` for strings, numeric types, collections, arrays and Instant. Use rather than adding a validation utility. |
| `domain/AssertionException.java` | Base with field, assertion type and template parameters; emitted by assertions. |
| `domain/AssertionErrorType.java` | Ten standard assertion error keys; do not create a second list for equivalent checks. |
| `domain/CeremoException.java` | Existing application/domain failure base and builder. Carries ErrorKey, ErrorStatus, immutable parameters and optional cause. `builder`, `badRequest`, `internalServerError`, `technicalError`; technicalError explicitly stays 500. Give business failures an explicit status: the fallback depends on caller location and otherwise becomes 500. |
| `domain/ErrorKey.java` | Serializable key contract with `get()`. Feature error enums implement this. |
| `domain/ErrorStatus.java` | Existing status enum, including CONFLICT, FORBIDDEN and NOT_FOUND; no Spring/HTTP imports required in the core. |
| `domain/StandardErrorKey.java` | Generic bad-request/internal-server-error keys. Use a feature key for a distinct business failure and provide translations. |
| `domain/InvalidEmailException.java` | Existing email assertion failure with value parameter; normally emitted through `Assert.field(...).email()`. |
| `domain/MissingMandatoryValueException.java` | `forNullValue`, `forBlankValue`, `forEmptyValue`. Reuse through assertions. |
| `domain/NotAfterTimeException.java` | Existing strict/inclusive after-comparison failure; staged value/field/other builder. |
| `domain/NotBeforeTimeException.java` | Existing strict/inclusive before-comparison failure; staged value/field/other builder. |
| `domain/NullElementInCollectionException.java` | Existing collection/array null-element failure. |
| `domain/NumberValueTooHighException.java` | Maximum-bound failure with max/value parameters and builder. |
| `domain/NumberValueTooLowException.java` | Minimum-bound failure with min/value parameters and builder. |
| `domain/StringTooLongException.java` | Existing maximum-length failure and parameters; builder is mainly internal to error package. |
| `domain/StringTooShortException.java` | Existing minimum-length failure and parameters; builder is mainly internal to error package. |
| `domain/TooManyElementsException.java` | Collection/array size failure with maxSize/currentSize parameters. |
| `infrastructure/primary/ApiProblemResponse.java` | Existing JSON response with title/detail/key/instance/message/errors. Factory methods `withKey`, `withMessage`, `withErrors`; null properties omitted. HTTP status is on the response, not a record field. Do not invent a second error envelope. |
| `infrastructure/primary/ArgumentsReplacer.java` | Package-private replacement of spaced `{{ name }}` placeholders; unknown arguments remain placeholders. Reused by existing handlers. |
| `infrastructure/primary/AssertionErrorsConfiguration.java` | Existing assertion MessageSource, UTF-8, default English, no system-locale fallback. Resources: `messages/assertions-errors/assertion-errors-messages*.properties`. |
| `infrastructure/primary/AssertionErrorsHandler.java` | Existing assertion advice: primary-layer assertions -> 400; application/domain assertions normally -> 500. Do not rely on domain assertions alone for public request validation. Logs type/field without rejected values. |
| `infrastructure/primary/BeanValidationErrorsHandler.java` | Existing 400 response for body/constraint violations. Includes object-level errors; repeated keys keep first; missing messages become Invalid value. Reuse with transport Bean Validation. |
| `infrastructure/primary/CeremoErrorsConfiguration.java` | Existing application MessageSource, UTF-8, default English, no system-locale fallback. Resources: `messages/errors/ceremo-errors-messages*.properties`. |
| `infrastructure/primary/CeremoErrorsHandler.java` | Existing advice renders CeremoException status, translated title/detail and parameters. On 22 October, status mapping became an exhaustive switch over ErrorStatus (13 codes tested); null still returns 500. Add feature keys/messages, not parallel handlers. |
| `infrastructure/primary/HttpErrorsHandler.java` | Lowest-priority MVC fallback using ResponseEntityExceptionHandler and the existing ApiProblemResponse. Preserves framework status/headers and committed-response handling; hides parser/driver details, maps optimistic conflicts to 409, unexpected exceptions to safe 500 and sets no-store. Rethrows Spring AuthenticationException/AccessDeniedException to preserve security-filter handling. Does not replace earlier Bean Validation/domain advice or the bearer challenge. |

`Assert` nested types: StringAsserter, IntegerAsserter, LongAsserter, FloatAsserter, DoubleAsserter, BigDecimalAsserter, CollectionAsserter, ArrayAsserter and InstantAsserter. Important semantics:

- `maxLength(...)` allows null: chain `notBlank()` when mandatory. `email()` checks basic shape, not delivery or ownership.
- Numeric `positive()` means greater than or equal to zero. Use `min(1)` for strictly positive integer/long counts; floating/decimal types also offer `strictlyPositive()`.
- String checks use Java string length, not user-perceived character count. Collection/array null checks, empty checks and null-element checks are separate.
- Instant comparisons have strict and inclusive variants. `inPast()`/`inFuture()` use the system clock and include equality; use an explicit reference Instant for deterministic use-case rules.
- Several assertion exceptions retain rejected values in messages/parameters. Never pass credentials/tokens into these checks in a way that exposes them through responses or logging.

Tests: `AssertTest`, `CeremoExceptionTest`, `ErrorHandlingTest`, `BeanValidationErrorsHandlerTest`, `ArgumentsReplacerTest`. Existing feature reuse: `UserAlreadyExistsException` extends CeremoException with explicit CONFLICT, UserErrorKey and English/French messages. `HttpErrorsHandlerTest` covers malformed input, safe 500, optimistic conflicts, method headers and committed responses.

## event — 4 files

| File | Behaviour and reuse |
| --- | --- |
| `domain/DomainEvent.java` | Existing event contract: `eventId()`, `occurredOn()`, `eventType()`. Implement for genuine domain events rather than another base event interface. |
| `application/DomainEventPublisher.java` | Existing outbound publisher contract: `publish` and `publishAll`. Can be used by the plain application core. |
| `infrastructure/secondary/SpringDomainEventPublisher.java` | Existing Spring component delegates to ApplicationEventPublisher; null events/iterables are ignored. Local publication, no durable outbox, broker, retry or after-commit guarantee. |

Test: `SpringDomainEventPublisherTest`. Inspect listener/transaction configuration before deciding delivery semantics; an injected publisher alone does not provide durable billing notifications.

## generation — 3 files

| File | Behaviour and reuse |
| --- | --- |
| `domain/CeremoUUID.java` | `generate()` delegates to the existing shared TimeBasedEpochGenerator (UUID v7). Returns java.util.UUID. Reuse at creation boundaries; do not replace with UUID.randomUUID or add another UUID generator. Fixed UUID fixtures/parsing remain valid. |
| `domain/ExcludeFromGeneratedCodeCoverage.java` | Runtime annotation for types, methods and constructors, with optional reason. Metadata only: not proof JaCoCo is configured and not a way to hide handwritten business logic from coverage. |

Test: `CeremoUUIDTest` checks non-null, differing generated IDs and sequential ordering. Current test does not explicitly assert `version() == 7`; the generator selection establishes that implementation choice.

## memoizer — 2 files

| File | Behaviour and reuse |
| --- | --- |
| `domain/Memoizers.java` | `of(Supplier)` and `of(Function)` retain results in a ConcurrentHashMap, including null results/inputs using nested MemoizedInput/MemoizedResult records and MemoizedFunction. The supplier retains one cached result. No eviction, invalidation or TTL; exceptions are not cached as results. Use for bounded, stable calculations, not user claims, permissions, provider eligibility or balances. |

Test: `MemoizersTest`, including supplier retention and null results.

## money — 3 files

| File | Behaviour and reuse |
| --- | --- |
| `domain/Currency.java` | Existing currencies include GHS; `defaultCurrency()` returns GHS. `from(code)` is case-insensitive using Locale.ROOT, does not trim, rejects unknown codes. `symbol()` and `decimalPlaces()` support formatting/rounding. |
| `domain/Money.java` | Immutable BigDecimal amount plus Currency. Constructor rejects excessive scale; `of(...)` rounds HALF_UP. Factories for decimal/string/long and zero; add/subtract/multiply/negate/comparison/sign helpers, `get()`, `toString()`. Arithmetic/comparison requires matching currency. Allows negative amounts; business rules must constrain prices/refunds. |

`Money.of(100, GHS)` is GHS 100, not 100 pesewas. API/database fields remain integer pesewas by CEREMO's current contract. Explicit exact conversion, bounds and rounding policy are still needed at those boundaries; there is no existing shared minor-unit converter. BigDecimal record equality can be scale-sensitive; do not confuse equals with the numeric comparison helpers. Tests: `CurrencyTest`, `MoneyTest`.

## multitenancy — 13 files

This inherited subsystem is not the provider-membership architecture. Do not enable it or attach it to new CEREMO features just because it exists. Review isolation semantics and database tests before any future adoption.

| File | Behaviour and reuse restriction |
| --- | --- |
| `application/MultitenancyErrorKey.java` | Existing no-context and tenant-suspended keys; not general provider subscription statuses. |
| `application/NoTenantContextException.java` | Existing CeremoException with BAD_REQUEST through `missingTenant()`. |
| `application/SystemOperation.java` | Annotation requiring a reason for intentional cross-tenant operations. Does not itself authorise the caller. |
| `application/TenantSuspendedException.java` | Existing CeremoException with FORBIDDEN through `suspended(TenantId)`. |
| `domain/TenantContext.java` | ThreadLocal context: set/get/require current tenant or ID, hasCurrentTenant and clear. Must be restored/cleared across work boundaries. |
| `domain/TenantId.java` | Non-null UUID wrapper; UUID/string `of`, optional `ofNullable`, `get`, string representation. Not a UserId/ProviderId substitute. |
| `domain/TenantInfo.java` | ID/name/subscriptionTier/maxLocations record. Uses inherited `positive()` validation, which permits zero. Tier/location fields are not CEREMO listing-entitlement rules. |
| `domain/TenantScopedEntity.java` | JPA/Hibernate superclass extending BaseEntity; tenant column/filter and pre-persist context assignment. Persistence only; filter does not replace explicit ownership/authorisation. |
| `infrastructure/primary/TenantFilter.java` | Conditional servlet filter extracts exactly one Keycloak organisation, validates tenant UUID, sets context and clears in finally. Legacy tier defaults FREE and maxLocations 1. Requires `application.multitenancy.enabled=true`. |
| `infrastructure/secondary/SystemOperationAspect.java` | Conditional aspect disables the Hibernate filter and restores a previously enabled filter/context ID. Not an authorisation mechanism. |
| `infrastructure/secondary/TenantAwareTaskDecorator.java` | Existing Spring component captures caller tenant context and restores worker context in finally. Already used by `wire/async`; do not add another task decorator for this responsibility. |
| `infrastructure/secondary/TenantFilterAspect.java` | Conditional aspect enables tenant filter on public Spring @Service methods when context exists. Does not cover every possible persistence access path or plain application service. |

Tests cover context, ID/info, scoped entity, error keys/exceptions, annotation, filter, task decorator and both aspects. Mocked filter/aspect tests do not prove complete PostgreSQL isolation.

## pagination — 6 files

| File | Behaviour and reuse |
| --- | --- |
| `domain/CeremoPage.java` | Existing content snapshot and paging metadata; `singlePage`, `builder`, in-memory `of`, `map`, pageCount/previous/next helpers. Zero-based pages; empty results report one page. Nested CeremoPageBuilder does not independently validate every metadata combination; construct consistent metadata. |
| `domain/CeremoPageable.java` | Zero-based page >= 0; pageSize 1..100; checks multiplication overflow and calculates offset. Use in domain/application contracts instead of Spring Pageable. |
| `infrastructure/primary/RestCeremoPage.java` | `from(domainPage, mapper)` creates public content/metadata. Existing nested builder is internal. Map safe DTOs rather than exposing JPA entities or private fields. |
| `infrastructure/primary/RestCeremoPageable.java` | Mutable request bean: page defaults 0, pageSize defaults 10; @Min/@Max and `toPageable()`. Validate at the HTTP boundary. |
| `infrastructure/secondary/CeremoPages.java` | Converts domain pageable to Spring PageRequest, optionally Sort, and Spring Page to domain page with mapper. Persistence adapter only; explicit sorting needs a safe field policy. |

Tests: `CeremoPageTest`, `CeremoPageableTest`, `RestCeremoPageTest`, `CeremoPagesTest`; `CeremoPagesFixture` is test support. In-memory `of` is not a database pagination strategy for large provider catalogues.

## useridentity — 6 files

| File | Behaviour and reuse |
| --- | --- |
| `domain/Email.java` | Required nonblank input <=255 before trimming; trims and lowercases with Locale.ROOT; optional `of` and `get`. Does not call `Assert.email()` or verify delivery. CEREMO's proposed database email length is 254: reconcile at integration rather than assuming identical constraints. |
| `domain/Firstname.java` | Required <=150; trims and capitalises matched letter sequences using Locale.ROOT; `get` and comparison. Do not use for arbitrary business/display names or silently assume original spelling is preserved. |
| `domain/Lastname.java` | Required <=150; trims and uppercases with Locale.ROOT; optional `of`, `get` and comparison. |
| `domain/Name.java` | Combines mandatory Firstname/Lastname; string constructor, display `get` and comparison. It requires both names; not a substitute for CEREMO's single displayName. |
| `domain/PhoneNumber.java` | Keeps Character.isDigit characters, drops punctuation including plus sign, requires resulting nonblank value <=50. Optional `of` and `get`. Does not validate Ghana numbering, E.164 or ownership; does not ensure ASCII digits. Additional boundary validation/verification remains necessary. |

Tests: `EmailTest`, `FirstnameTest`, `LastnameTest`, `NameTest`, `PhoneNumberTest`; `UsersIdentitiesFixture` is test support. None of these types replaces the exact issuer/subject `UserIdentity` value object.

## Package metadata and file coverage

Each of the following has its own `package-info.java` annotated with `io.ceremo.ceremobackend.SharedKernel`: authentication, collection, domain, enumeration, error, event, generation, memoizer, money, multitenancy, pagination and useridentity. Those 12 metadata files plus the original 74 individual source files plus the new HTTP fallback account for all 87 shared Java files. Nested builders, interfaces and records are owned by their enclosing files; inspect them there instead of inventing duplicates.

## Use in subsequent guidance

Before each step, state briefly: existing class/method reused; correct DDD layer; any semantic gap; only the necessary new code. Check `wire` and feature implementations as well as this map. Existing helpers are not automatically wired features: no shared helper currently supplies full CEREMO account registration, issuer/subject-to-account persistence, staff grants, contact verification, provider membership, subscriptions or a durable outbox.

For executable verification when shared behaviour changes:

Latest 22 October coverage review: validation boundary/parameter tests, default exception status and copied parameters, request binding, authority/user-info guards, optional filter wiring and failure-safe tenant restoration have been added. The full run passed 457 regular and 33 PostgreSQL cases; combined line/branch/instruction counts are 100%. See [the full coverage record](research/quality/tests-and-analysis.md#full-coverage-completion-22-october-2026). Authentication mocks/configuration coverage do not establish live identity interoperability or end-to-end tenant isolation.

```powershell
.\gradlew.bat test --tests 'io.ceremo.ceremobackend.shared.*'
```

Include the affected feature and `wire` tests when integrations change. Documentation-only inventory updates require source/file coverage and link checks, not another unchanged test run.
