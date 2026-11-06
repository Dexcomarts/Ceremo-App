# CEREMO wire package reuse map

Profile access update, 5 November 2026: the existing Accounts bean is also injected into shared security's API chain for the ACTIVE-account provider detail gate. No duplicate chain or wire bean was added; the 16-file inventory remains. Existing tenant composition/membership authorisation is unchanged. See [ADR 11.10](../docs/03-technical-requirements.md#1110-registered-profile-and-contact-access).

Current consolidation, 5 November 2026: **16 Java files in nine configuration areas**, eight dedicated wire test classes and the OIDC fixture. One [AccountConfiguration](../src/main/java/io/ceremo/ceremobackend/wire/account/AccountConfiguration.java) now supplies UTC Clock, managed UserRepository/UserDirectory, transactional UserAccounts, StaffAccess and Accounts. [UserRegistrationProperties](../src/main/java/io/ceremo/ceremobackend/wire/account/UserRegistrationProperties.java) lives beside it. `wire/usermanagement` and UserManagementConfiguration are removed. Use [ADR 11.9](../docs/03-technical-requirements.md#119-consolidate-the-account-module); older counts below are historical.

Reviewed from current source and tests on 22 October 2026: **14 Java files across seven configuration areas** (13 types and one package metadata file), plus **eight test classes and a shared OIDC test fixture**. This is agent memory, outside the eight product documents. Source and actual test results outrank this map; a documented bean is not proof of a production deployment or a completed security review.

Before consolidation, 5 November 2026: **17 Java files across ten areas**. The account/lifecycle composition below is now combined as described above. The inherited global tenant filter remains off; explicit provider-business membership is used instead.

| Added/extended configuration | Actual composition |
| --- | --- |
| `account/AccountConfiguration` | Composes registration, activation, current identity/repositories, UTC Clock, the existing UserAccounts transaction decorator and AccountService/StaffAccess. Staff checks require ROLE_ADMIN, trusted recent MFA/auth_time and a live ACTIVE local account. |
| `logout/LogoutConfiguration` | CurrentUser maps to CurrentSession; JDBC revocations and Clock compose LogoutService. JdbcSessionRevocations owns transaction commit for persistent revocation; no raw token storage. Shared security requires this port for browser logout and reads the store for JWT validation. |
| `tenant/TenantConfiguration` | Managed EntityManager creates JpaTenantRepository. TransactionalTenantAccounts wraps plain TenantService with Accounts and the internal UserDirectory port. Root locks, fresh membership reads and audit share the transaction. |
| `account/UserRegistrationProperties` | Existing strict terms binding is retained beside the unified configuration; it adds no public customer directory or alternate identity policy. |

All new configuration is covered by the full-stack PostgreSQL security suite, alongside the eight dedicated wire tests. The OIDC fixture now includes a synthetic registered logout endpoint. Real Keycloak/SMTP/MFA, server-side client-token storage and live Sonar gates remain separate checks. See [provider security evidence](research/ports-and-adapters/provider-account-security.md).

Read this with the [shared package map](shared-package-map.md), [security rules](security-quality-rules.md) and [reuse skill](skills/ceremo-shared-wire/SKILL.md). Read only the relevant area, then reopen its implementation and tests before giving code. Refresh this inventory when files, beans, settings or behaviour change.

## Choose by responsibility

| Need | Existing wiring | How to use it |
| --- | --- | --- |
| Background execution | `async` | Inject the configured executor into an outer adapter; keep pool configuration central. |
| Repositories, transactions and audit timestamps | `database` | Use the existing infrastructure configuration; explicitly compose the complete use-case transaction. |
| Account lifecycle/access composition and current terms | `account` | Inject UserAccounts for lifecycle or Accounts for account/staff checks; one configuration supplies both. Supply the approved `CEREMO_TERMS_VERSION`. |
| Application cache configuration | `cache` | Inspect actual named caches and invalidation needs before enabling feature caching. |
| Browser cross-origin policy | `security` | Configure the existing CORS bean and filter; authentication remains in shared security. |
| API specification and Swagger OAuth | `springdoc` | Add feature HTTP operations to the existing API document; preserve the central OAuth configuration. |
| Optional TCP log export | `logstash` | Configure the existing lifecycle rather than adding appenders in feature classes. |
| Plain validation, IDs, errors or authenticated claims | `shared`, not a new wire helper | Select the corresponding shared component and keep its framework dependencies at the correct boundary. |

`wire` is the Spring composition/configuration layer. Features consume configured services through constructor injection; they do not instantiate configuration classes. Domain/application code must not import `wire`, Spring, JPA or Hibernate. A feature with no infrastructure need does not need a wire dependency.

## Complete source inventory

Account composition additions: [AccountConfiguration](../src/main/java/io/ceremo/ceremobackend/wire/account/AccountConfiguration.java) registers the UTC `Clock`, managed `UserRepository` using `SharedEntityManagerCreator` and `CeremoUUID::generate`, and one `UserAccounts` wrapping a privately constructed plain `UserService`. [UserRegistrationProperties](../src/main/java/io/ceremo/ceremobackend/wire/account/UserRegistrationProperties.java) binds strict `application.registration.terms-version` through constructor validation with shared `Assert`; null, blank and values longer than 40 characters fail startup. No production terms version is silently selected. These two files join the original twelve in the inventory below.

Paths below are relative to the [wire source root](../src/main/java/io/ceremo/ceremobackend/wire).

| File | Existing behaviour and reuse boundary |
| --- | --- |
| [async/infrastructure/secondary/AsyncConfiguration.java](../src/main/java/io/ceremo/ceremobackend/wire/async/infrastructure/secondary/AsyncConfiguration.java) | Enables async execution and scheduling. Constructor receives the shared `TenantAwareTaskDecorator`. `taskExecutor(ThreadPoolTaskExecutorBuilder)` publishes a `ThreadPoolTaskExecutor` from Boot's configured builder with that decorator. |
| [database/infrastructure/secondary/DatabaseConfiguration.java](../src/main/java/io/ceremo/ceremobackend/wire/database/infrastructure/secondary/DatabaseConfiguration.java) | Enables transaction management, JPA auditing and Spring Data repository scanning under `io.ceremo.ceremobackend`, with `enableDefaultTransactions=false`. No explicit transaction-manager bean or business use-case boundary is declared here. |
| [cache/infrastructure/secondary/EhcacheProperties.java](../src/main/java/io/ceremo/ceremobackend/wire/cache/infrastructure/secondary/EhcacheProperties.java) | Binds `application.cache.ehcache`; `maxEntries` defaults to 100 and `timeToLiveSeconds` to 3600. Explicit getters/setters support binding. No validation annotations enforce bounds. |
| [cache/infrastructure/secondary/CacheConfiguration.java](../src/main/java/io/ceremo/ceremobackend/wire/cache/infrastructure/secondary/CacheConfiguration.java) | Enables caching and builds Object/Object JCache configuration using heap entry limits and TTL. `cacheManagerCustomizer()` is currently empty. The instance `createCache(manager, name)` delegates to the static overload; an existing cache is cleared, while a missing cache is created. These helper methods have package visibility. |
| [security/infrastructure/primary/CorsProperties.java](../src/main/java/io/ceremo/ceremobackend/wire/security/infrastructure/primary/CorsProperties.java) | Publishes `corsConfiguration(Environment)`. Starts with legacy allowed origins, defaults for methods/headers and credentials false; strict `application.cors` property binding then applies overrides. |
| [security/infrastructure/primary/CorsFilterConfiguration.java](../src/main/java/io/ceremo/ceremobackend/wire/security/infrastructure/primary/CorsFilterConfiguration.java) | Publishes `corsFilter()` and a disabled servlet `corsFilterRegistration(...)`. Registers API, management, API-docs and Swagger paths. The filter is consumed through Spring Security's CORS configuration rather than a second servlet registration. |
| [security/package-info.java](../src/main/java/io/ceremo/ceremobackend/wire/security/package-info.java) | Marks the security package with the existing `BusinessContext` annotation. It is metadata, not a bean, permission check or security filter. |
| [springdoc/infrastructure/primary/SpringdocConfiguration.java](../src/main/java/io/ceremo/ceremobackend/wire/springdoc/infrastructure/primary/SpringdocConfiguration.java) | Publishes `ceremoOpenAPI()` and `ceremoAllOpenAPI()`. Sets CEREMO metadata/version, optional external documentation and group `all` matching `/api/**`. |
| [springdoc/infrastructure/primary/SpringdocOAuth2Configuration.java](../src/main/java/io/ceremo/ceremobackend/wire/springdoc/infrastructure/primary/SpringdocOAuth2Configuration.java) | Publishes `oauthOpenApi(ClientRegistrationRepository)`. Requires registration `oidc`; fails if missing. Uses configured endpoint overrides or that registration's endpoints. Adds `security_auth` authorization-code flow/scopes while preserving existing components. |
| [logstash/infrastructure/secondary/LogstashTcpProperties.java](../src/main/java/io/ceremo/ceremobackend/wire/logstash/infrastructure/secondary/LogstashTcpProperties.java) | Binds `application.logging.logstash.tcp`: enabled, host, port, optional ring buffer size and shutdown grace period. Enabled defaults false; host is null and port zero without binding. Setters do not validate values. |
| [logstash/infrastructure/secondary/LogstashTcpConfiguration.java](../src/main/java/io/ceremo/ceremobackend/wire/logstash/infrastructure/secondary/LogstashTcpConfiguration.java) | Active only when the enable property is true. Publishes `logstashTcpLifeCycle()` with application name and optional configured server port as metadata. Does not invent a missing port. |
| [logstash/infrastructure/secondary/LogstashTcpLifeCycle.java](../src/main/java/io/ceremo/ceremobackend/wire/logstash/infrastructure/secondary/LogstashTcpLifeCycle.java) | Implements `SmartLifecycle`. Copies metadata, uses Jackson 3 (`tools.jackson`) and builds a named Logstash TCP appender/encoder with shortened root-cause-first stack traces. Synchronized `start()` avoids duplicate start while running; `stop()` detaches/stops its own appender. Supports optional buffer/shutdown settings; serialization failures become `IllegalStateException`. |

## Async: consume the configured executor

The current YAML sets maximum pool size 16, queue capacity 100, keep-alive 10 seconds, thread prefix `ceremo-task-`, and scheduler pool size 2. `AsyncConfiguration` does not hardcode core size or create its own scheduler; distinguish Boot/property defaults from explicit settings. Recheck settings before capacity recommendations.

Reuse the `taskExecutor` bean in feature infrastructure adapters. Do not create an unmanaged pool or use `CompletableFuture`'s default pool when work needs these configured limits/context handling. Keep Spring async annotations outside the pure application/domain core. Spring proxy-based async calls need the appropriate bean boundary; self-invocation does not establish that boundary.

The shared [TenantAwareTaskDecorator](../src/main/java/io/ceremo/ceremobackend/shared/multitenancy/infrastructure/secondary/TenantAwareTaskDecorator.java) captures/restores tenant context and clears worker state in `finally`. It is wired even when optional tenancy filters are disabled. It does not propagate SecurityContext, MDC or a database transaction. Do not enable the inherited tenant subsystem to implement CEREMO provider membership. Async execution is not durable delivery; introduce a queue/outbox only for an actual delivery requirement and with evidence.

## Database: keep complete use cases transactional

Reuse repository scanning and auditing already enabled here. Do not add another broad `@EnableJpaRepositories` or duplicate auditing configuration in production feature packages. `enableDefaultTransactions=false` means inherited repository defaults cannot be relied upon; it does not prohibit explicit infrastructure transaction boundaries.

The current application config pairs Hikari auto-commit false with Hibernate `provider_disables_autocommit=true`. When a JPA test replaces Hikari with an embedded datasource, that promise must be reassessed. The two User Management mapping slices now override the Hibernate property to false; post-transaction checks confirm H2 rollback. See [the recorded finding](research/repositories/user-persistence.md#applied-correction-and-h2-transaction-isolation-finding). Do not copy that override blindly into production pool settings.

Pair this wiring with shared [BaseEntity](../src/main/java/io/ceremo/ceremobackend/shared/domain/BaseEntity.java) only in persistence entities. Timestamp auditing and nullable actor columns already exist; a production `AuditorAware` adapter is not implemented. Do not invent one as already available.

User Management production composition now exists. `TransactionalUserAccounts` wraps complete operations with REQUIRED transaction templates: read-only lookup and writable registration/activation. The existing `JpaUserRepository` MANDATORY guard participates in that boundary; a joined call leaves physical commit to its owning transaction. The existing component-scanned `SpringCurrentUser` remains the identity adapter; on 4 November it gained strict boolean email-verification reading. No duplicate bean/security chain or new wire file was introduced. PostgreSQL checks cover commit, rollback, competing registration and activation. See the [latest implementation record](research/application/user-registration.md#verified-email-activation-4-november-2026). Respect package visibility: configuration in another package cannot directly construct package-private persistence entities/helpers.

## Cache: a configured manager is not a populated feature cache

The current customizer does not declare a `providers` cache or any other application cache. The cache unit test creates a synthetic cache explicitly. Do not recommend `@Cacheable("providers")` as already executable without inspecting/adding the relevant named-cache setup and tests.

Despite the helper's "create or replace" comment, the existing-cache branch only clears entries; it does not replace configuration, TTL or capacity. Do not call `createCache` per request: it can erase useful entries. The helper is package-private, so another feature cannot directly call it without a deliberate supported configuration change.

Keep cache annotations/wrappers in infrastructure under our pure-core rule. Choose keys, ownership scope, expiry and invalidation from the actual feature. Never treat stale subscription, ownership or account-status cache values as sufficient authorisation. Existing Hibernate second-level cache configuration is a separate concern; it does not create application cache names.

## CORS: use the existing policy and shared security chains

Default origins are empty; arbitrary cross-origin browser requests are denied. Default methods are GET, POST, PUT, PATCH, DELETE and OPTIONS. Default headers are Authorization, Content-Type, Accept, Accept-Language, Idempotency-Key and X-Request-Id. Credentials default false. `application.cors.allowed-origins` takes precedence over legacy `application.security.allowed-origins`; unknown `application.cors` properties are rejected by binding.

Registered paths are `/api/**`, `/management/**`, `/v2/api-docs`, `/v3/api-docs`, `/v3/api-docs/**`, `/swagger-resources` and `/swagger-ui/**`. Reopen the source before adding a new route family. Keep the disabled servlet registration so the same filter is not installed twice.

[Shared SecurityConfiguration](../src/main/java/io/ceremo/ceremobackend/shared/authentication/infrastructure/primary/SecurityConfiguration.java) owns the API bearer chain, browser login/session chain, JWT decoder and request access rules. Both chains use Spring CORS integration. Reuse these rather than creating feature security chains or sprinkling controller `@CrossOrigin` annotations. CORS is a browser access policy; it does not replace authentication, record ownership or account-status checks. Preserve browser CSRF protection and explicit origin/credential decisions.

## Springdoc: extend the existing API document

`application.version` defaults to `undefined`; `application.documentation-url` is omitted from the document when blank. The all group covers `/api/**`. The OAuth customizer uses registration `oidc` and optional `springdoc.oauth2.authorization-url` / `token-url` overrides; scopes are openid, profile and email, under scheme `security_auth`.

Swagger UI PKCE is a separate YAML setting (`springdoc.swagger-ui.oauth.use-pkce-with-authorization-code-grant=true`), not a property of the entity or controller. The development client is public `web_app`; do not add a client secret or reintroduce the implicit flow. OpenAPI security metadata documents access; it does not enforce it. A global security requirement also needs deliberate per-operation documentation for public catalogue routes. Do not expose JPA entities as API models or create a second OpenAPI bean for each feature.

## Logstash: preserve opt-in lifecycle and data hygiene

Current YAML disables TCP export and supplies development host/port, ring buffer 8192 and a one-minute shutdown grace period. Keep the existing enable switch; no feature should start its own root appender. The local lifecycle unit test mocks the appender and does not establish TCP delivery, TLS, redaction or collector availability. The implementation does not configure a redaction policy or transport TLS here. Do not send tokens, credentials, raw JWT claims or customer records in added log messages/custom fields. A started appender alone is not evidence that logs arrived.

## Tests and actual verification scope

Latest 22 October additions: [LogstashEncodingTest](../src/test/java/io/ceremo/ceremobackend/wire/logstash/infrastructure/secondary/LogstashEncodingTest.java) binds optional buffer/shutdown settings, inspects a real appender without starting TCP, encodes a synthetic log event, verifies defensive metadata copying and serialization failure. [SpringdocConfigurationTest](../src/test/java/io/ceremo/ceremobackend/wire/springdoc/infrastructure/primary/SpringdocConfigurationTest.java) verifies an explicit documentation URL/version and missing OAuth registration rejection. Together with existing tests, the full application run passes 457 regular and 33 PostgreSQL cases with 100% combined line/branch/instruction coverage. This supersedes earlier inspection-only and 21 October counts below; live TCP, TLS and Sonar server verification remain separate. See [the quality record](research/quality/tests-and-analysis.md#full-coverage-completion-22-october-2026).

[AccountConfigurationTest](../src/test/java/io/ceremo/ceremobackend/wire/account/AccountConfigurationTest.java) adds five cases for one transactional inbound bean, UTC clock, terms binding and missing/blank/oversized configuration. [TestOidcConfiguration](../src/test/java/io/ceremo/ceremobackend/wire/TestOidcConfiguration.java) is a shared synthetic registration fixture used by the wire and application smoke tests; both mock JWT decoding and use H2. The full 21 October run passed 397 regular tests and 32 disposable PostgreSQL tests. This supersedes earlier inspection-only statements for the original ten wire cases below; live Keycloak, production deployment and a SonarQube gate remain unverified.

| Test file under the wire test root | What its existing cases check |
| --- | --- |
| [WireConfigurationTest.java](../src/test/java/io/ceremo/ceremobackend/wire/WireConfigurationTest.java) | Four cases: starts with tenant filter/Logstash disabled; configured executor and tenant cleanup; timestamp auditing/version increment; generated API authorization-code document. Uses H2, Flyway disabled, mocked JWT decoder and synthetic OIDC registration. |
| [cache/infrastructure/secondary/CacheConfigurationTest.java](../src/test/java/io/ceremo/ceremobackend/wire/cache/infrastructure/secondary/CacheConfigurationTest.java) | Creates an Ehcache-backed JCache and clears its existing entries without replacing the cache instance. |
| [security/infrastructure/primary/CorsConfigurationTest.java](../src/test/java/io/ceremo/ceremobackend/wire/security/infrastructure/primary/CorsConfigurationTest.java) | Two cases: denied origins by default/disabled servlet registration, and explicit wire-origin precedence over legacy origins. |
| [springdoc/infrastructure/primary/SpringdocOAuth2ConfigurationTest.java](../src/test/java/io/ceremo/ceremobackend/wire/springdoc/infrastructure/primary/SpringdocOAuth2ConfigurationTest.java) | Derives OAuth endpoints/scopes, supports absent components and preserves existing security schemes. |
| [logstash/infrastructure/secondary/LogstashTcpLifeCycleTest.java](../src/test/java/io/ceremo/ceremobackend/wire/logstash/infrastructure/secondary/LogstashTcpLifeCycleTest.java) | Two cases: starts once/detaches its own mocked appender; application metadata omits an unconfigured port. |

Relevant command when runtime wiring changes:

```powershell
.\gradlew.bat test --tests 'io.ceremo.ceremobackend.wire.*'
```

Include affected shared/feature tests and real PostgreSQL checks for the behaviour changed. These ten wire test cases were inspected in this review; no new application test run is claimed for this documentation-only change. The historical selected foundation run is recorded in [quality memory](research/quality/tests-and-analysis.md). Live Keycloak, production PostgreSQL, TCP delivery, durable async work and a SonarQube quality gate remain separate evidence requirements. Existing wire test fixtures use `UUID.randomUUID()`; do not copy that into new account/record generation, which uses `CeremoUUID.generate()`.

## Source evidence for future guides

Checked on 21 October 2026. These sources explain framework behaviour; CEREMO's actual configuration above determines what is active.

- [Spring Java configuration](https://docs.spring.io/spring-framework/reference/core/beans/java/basic-concepts.html): container-managed composition using `@Bean` and `@Configuration`. CEREMO keeps that composition outside the pure core.
- [Boot 4.1.1 ThreadPoolTaskExecutorBuilder](https://github.com/spring-projects/spring-boot/blob/v4.1.1/core/spring-boot/src/main/java/org/springframework/boot/task/ThreadPoolTaskExecutorBuilder.java): inspected Apache-2.0 source for the injected builder and task decorator API. Preserve property-driven limits instead of duplicating pool construction.
- [Spring Data JPA 4.1.1 EnableJpaRepositories](https://github.com/spring-projects/spring-data-jpa/blob/4.1.1/spring-data-jpa/src/main/java/org/springframework/data/jpa/repository/config/EnableJpaRepositories.java) and [transactionality](https://docs.spring.io/spring-data/jpa/reference/jpa/transactions.html): inspected maintainer source (Apache-2.0) and outer-transaction guidance. The source default is not our `enableDefaultTransactions=false` setting. Do not copy Spring annotations into our plain application core.
- [Spring task execution](https://docs.spring.io/spring-framework/reference/integration/scheduling.html) and [Spring Security CORS](https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html): support managed executors and security-integrated preflight handling; do not establish durable delivery or business authorisation.

No external application code was copied and no Salesync files were accessed. For new version-sensitive guidance reopen the exact maintainer API/source and the affected CEREMO tests; the live manuals can advance beyond our installed dependencies.
