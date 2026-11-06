# User repository and identity persistence

- Entry ID: repositories/user-persistence
- Researched: 20 October 2026
- Status: mappings, adapter and production transaction composition implemented; account HTTP/activation and PostgreSQL concurrency verified on 4 November 2026. See the current status-update checkpoint below.
- Question: how to persist a user and initial identity atomically without overwriting an existing identity link

## Evidence

[Spring Data JPA 4.1.1 SimpleJpaRepository.save](https://github.com/spring-projects/spring-data-jpa/blob/4.1.1/spring-data-jpa/src/main/java/org/springframework/data/jpa/repository/support/SimpleJpaRepository.java) chooses persist or merge by entity state. [Entity state detection](https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html) checks a nullable version property before an assigned ID. Framework source is Apache-2.0. See [document 08 excerpts](../../../docs/08-development-and-quality.md#85-short-source-excerpts-for-the-first-guides).

[Keycloak 26.4.2 FederatedIdentityEntity](https://github.com/keycloak/keycloak/blob/26.4.2/model/jpa/src/main/java/org/keycloak/models/jpa/entities/FederatedIdentityEntity.java) separates local-user linkage and external-provider identifiers. Its key and token-storage fields are not CEREMO's schema and must not be copied as an identity policy.

## CEREMO decisions and reuse

[UserRepository](../../../src/main/java/io/ceremo/ceremobackend/account/domain/repository/UserRepository.java) stays a plain domain outbound port. `create` is insert-only; do not implement an upsert that reassigns another account's identity. `findByIdentity` must use the exact issuer/subject pair and preserve account status.

Reuse `UserMapper`, `UserJpaEntity`, shared `BaseEntity` and existing feature errors. A new JPA entity's inherited version remains null until persistence. Future updates must preserve managed audit/version state rather than rebuilding a detached entity as new.

## Verification and next work

22 October completion: nine adapter cases now pass, adding a genuine PostgreSQL CHECK failure to prove a non-23505 integrity error is propagated and both inserts roll back. The temporary constraint is rolled back inside its failed test transaction; no shared/development database or production migration is altered. All 457 regular and 33 PostgreSQL cases pass with 100% combined line/branch/instruction coverage; see [the completion record](../quality/tests-and-analysis.md#full-coverage-completion-22-october-2026). This supersedes the prior eight-case adapter count below.

21 October verification: the authorised implementation moved the adapter test to the integration source set and ran all eight adapter cases successfully, together with 14 schema and ten registration-use-case cases. Complete production transaction composition and the two-worker uniqueness race were implemented and verified; see [the application record](../application/user-registration.md#applied-checkpoint-and-verification). Later checkpoints supersede these counts and the earlier pending work below.

The original mapping-only stage required an adapter and outer use-case transaction; both now exist. Continue testing duplicate constraint translation, rollback and independent concurrent transactions against PostgreSQL. Translate only the intended identity uniqueness failure; unrelated database errors retain their meaning. Never query an aborted transaction to recover.

Related: [database](../database/user-schema.md), [application](../application/user-registration.md).

## Next guided checkpoint: identity JPA mapping

Reviewed: 21 October 2026. Historical status at guide delivery: researched and proposed, not implemented or executed. At that point source had `UserJpaEntity` and `UserMapper`, but no `UserIdentityJpaEntity` or `JpaUserRepository`. The user subsequently implemented the identity mapping and tests; see the correction below for the observed result. The agent delivered guidance without editing application files.

Reference code: [Keycloak 26.4.2 FederatedIdentityEntity](https://github.com/keycloak/keycloak/blob/26.4.2/model/jpa/src/main/java/org/keycloak/models/jpa/entities/FederatedIdentityEntity.java), inspected including its Apache-2.0 header, uses:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "USER_ID")
private UserEntity user;
```

This supports a separate identity-link entity with an explicit local-user association. CEREMO's proposed entity is original project-specific code using that mapping pattern, not a copied Keycloak implementation. Do not copy Keycloak's composite key, provider alias, realm fields, token storage, setters or its external-user identifier policy. No Keycloak SDK dependency is needed. Our standalone UUID primary key and `(issuer, subject)` unique constraint already exist in the migration.

[Jakarta Persistence 3.2](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2.html), sections 2.1, 2.2 and 11.1.31, establishes the protected/public no-argument constructor, field access and many-to-one mapping semantics. Set `optional=false`, `nullable=false` and request `LAZY`; lazy fetching remains a provider hint, not a promise of a particular query count. Omit cascading to the account. Mark the association/issuer/subject non-updatable through ordinary entity mapping; this is not database authorisation and cannot stop native SQL or a future linking operation by itself. Flyway continues to own the PostgreSQL foreign key, case-sensitive collation, index and uniqueness rules.

[OpenID Connect Core 1.0, section 5.7](https://openid.net/specs/openid-connect-core-1_0.html#ClaimStability) explains the stable issuer/subject pair rather than email/name. This is identifier guidance, not proof that an arbitrary bearer token has been validated; CEREMO's shared resource-server chain and `SpringCurrentUser` remain the trusted authentication boundary.

Reuse inspected immediately before guidance: `BaseEntity` for ID/audit/version; `Assert.notNull` for constructor requirements; `UserIdentity` for existing issuer/subject validation and exact values; `CeremoUUID.generate()` at the caller/test-fixture boundary; `UserMapper` and `UserJpaEntity` for the local account. The proposed identity constructor receives its UUID, local `UserJpaEntity`, existing `UserIdentity` and server-supplied `Instant`. It must not generate an ID or call `Instant.now()` internally. There is no demonstrated need for another identity value object, validation utility, bidirectional collection or mapper in this checkpoint.

Test evidence: [Boot 4.1.1 DataJpaTest](https://github.com/spring-projects/spring-boot/blob/v4.1.1/module/spring-boot-data-jpa-test/src/main/java/org/springframework/boot/data/jpa/test/autoconfigure/DataJpaTest.java) is Apache-2.0 and declares `@Transactional`; its Javadoc describes embedded replacement and rollback. [Petclinic ClinicServiceTests](https://github.com/spring-projects/spring-petclinic/blob/main/src/test/java/org/springframework/samples/petclinic/service/ClinicServiceTests.java), inspected including tests and Apache-2.0 header, uses the same Boot 4 package for `@DataJpaTest` and exercises persistence relationships. That link tracks `main` on the review date, not a pinned release. Reuse the current CEREMO `UserJpaEntityTest` H2/auditing setup for a proposed `UserIdentityJpaEntityTest` under `src/test/java`, preserving the existing test-layout decision. Do not copy Petclinic's `Replace.NONE`, which would change our test datasource policy.

Proposed acceptance checks: persist/flush/clear/reload a link and verify the local account reference, exact mixed-case issuer/subject, last-seen timestamp, inherited audit timestamps and version; delete the identity entity and verify the account remains, guarding against accidental cascade removal. H2 tests demonstrate mapping and cascade behaviour only. Existing disposable PostgreSQL schema tests cover actual migration constraints; PostgreSQL JPA validation, atomic account-plus-identity insertion, duplicate translation and races remain for the adapter/composition stage. Production registration needs a complete outer transaction; a successful mapping slice does not establish that boundary. No REST identity-link/delete operation is introduced by this guide.

Proposed user check:

```powershell
.\gradlew.bat test --tests '*UserIdentityJpaEntityTest' --tests '*UserJpaEntityTest' --tests '*UserIdentityTest'
```

This command has not been run for the proposed classes, which do not exist yet. SonarQube/JaCoCo configuration remains pending. Failed direct API/test-file lookups were replaced with the successfully retrieved specification, tagged Boot source and actual Petclinic service test; no claims depend on inaccessible pages.

### Correction after the user's first mapping test run

Observed on 21 October 2026: the user implemented both identity files. The saved JUnit XML reports 10 selected cases, 9 passing and 1 failing. `reloadsIdentityWithItsAccountAndAuditFields` fails at line 43: expected null, actual 0. The no-cascade-removal test passes; the reload assertions after line 43 have not been reached in this run. This supersedes the earlier statement that the files do not exist, not the pending adapter/transaction work.

Root cause: the agent's guide checked `entity.getVersion()` for null after calling `persistIdentity()`. That helper already invokes `entityManager.persist(identity)`, which initialises the inherited `@Version Long`. The existing `UserJpaEntityTest` correctly checks null before persist. Do not change shared `BaseEntity`, initialise its version manually or weaken the persisted version assertion.

Actual runtime evidence: the test log names Hibernate ORM 7.4.5.Final. In the matching Apache-2.0 maintainer source, [AbstractSaveEventListener.substituteValuesIfNecessary](https://github.com/hibernate/hibernate-orm/blob/7.4.5/hibernate-core/src/main/java/org/hibernate/event/internal/AbstractSaveEventListener.java) invokes `seedVersion` during persist preparation; [Versioning.seedVersion](https://github.com/hibernate/hibernate-orm/blob/7.4.5/hibernate-core/src/main/java/org/hibernate/engine/internal/Versioning.java) generates an initial version for null state; [LongJavaType.seed](https://github.com/hibernate/hibernate-orm/blob/7.4.5/hibernate-core/src/main/java/org/hibernate/type/descriptor/java/LongJavaType.java) returns `ZERO`. Short evidence excerpt: `return ZERO;`.

Corrective guide: move the null-version assertion from `reloadsIdentityWithItsAccountAndAuditFields` to immediately before `entityManager.persist(identity)` in the fixture helper. Keep `assertEquals(0L, saved.getVersion())` after flush/clear/reload. This preserves both the new-instance and persisted-state checks. No application/test files were changed and no corrected run is claimed. Re-run the same three selected classes after the user makes this change.

### Applied correction and H2 transaction-isolation finding

On 21 October 2026, the user explicitly authorised applying the changes. The version assertion was moved before persist. The first corrected run reached and passed the reload assertions, but exposed a second failure: the next test's insert violated identity uniqueness because the previous test's row remained. This was a test transaction configuration defect, not evidence to weaken the constraint or vary fixture identities to hide leakage.

The application config pairs Hikari `auto-commit=false` with Hibernate `provider_disables_autocommit=true`. The JPA slices replace Hikari with an embedded datasource, so the inherited Hibernate promise is wrong for those connections. [Hibernate 7.4.5 JdbcSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT](https://github.com/hibernate/hibernate-orm/blob/7.4.5/hibernate-core/src/main/java/org/hibernate/cfg/JdbcSettings.java) warns that enabling this setting for connections whose auto-commit remains enabled can execute statements outside the transaction. The maintainer source is Apache-2.0. Applied `spring.jpa.properties.hibernate.connection.provider_disables_autocommit=false` only to `UserJpaEntityTest` and `UserIdentityJpaEntityTest`, allowing Hibernate to manage connection auto-commit. Runtime datasource settings, entity mappings and uniqueness remain unchanged.

Added `@AfterTransaction` checks to the identity test to query both tables and assert zero remaining rows after rollback. [Spring TestContext transaction guidance](https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/tx.html) documents this callback for verification outside the test-managed transaction. No cleanup SQL or randomised identity pair masks rollback failures. Reuse remains shared BaseEntity, UUID generator, existing identity validation and mapper.

Verified with JDK 25 on 21 October 2026:

```powershell
.\gradlew.bat test --tests '*UserIdentityJpaEntityTest' --tests '*UserJpaEntityTest' --tests '*UserIdentityTest' --console=plain
```

Result: BUILD SUCCESSFUL; 10 cases (2 identity mapping, 4 account mapping, 4 domain identity), zero failures/errors/skips. Both the version lifecycle and post-rollback isolation checks passed. This supersedes the pending-correction status above. This run did not exercise PostgreSQL, production transaction composition, JaCoCo or SonarQube.

## Persistence and request-boundary research for the next stage

Researched: 21 October 2026. Status: researched; adapter/composition remain unimplemented. Scope: the existing `UserRepository` contract, registration transaction and eventual HTTP entry point. The user asked to apply all project research/reuse/security rules; this authorises research/context updates, not application implementation.

### Versions and source evidence

The inspected Gradle file uses Java 25, Boot 4.1.1 and Gradle 9.8.1. Its cached Boot BOM declares Spring Framework 7.0.9, Spring Security 7.1.1 and Hibernate 7.4.5.Final. The database tests use PostgreSQL 18.0; proposed production PostgreSQL 17 is not the tested baseline. Use existing managed dependencies; no new library is justified by this research.

| Maintainer source | Inspected evidence | CEREMO use and limits |
| --- | --- | --- |
| [Spring Data JPA 4.1.1 SimpleJpaRepository.save/saveAndFlush](https://github.com/spring-projects/spring-data-jpa/blob/4.1.1/spring-data-jpa/src/main/java/org/springframework/data/jpa/repository/support/SimpleJpaRepository.java) | `entityManager.persist(entity);` and explicit flush in the save-and-flush path | Existing persistence API mechanics; not CEREMO registration rules. `save` can merge non-new entities, so do not use it as an unexplained insert-only guarantee. |
| [Spring Framework 7.0.9 TransactionTemplate.execute](https://github.com/spring-projects/spring-framework/blob/v7.0.9/spring-tx/src/main/java/org/springframework/transaction/support/TransactionTemplate.java) | `rollbackOnException(status, ex);` on callback failure; `this.transactionManager.commit(status);` before returning the callback result | Supports outer imperative transaction composition. This couples its caller to Spring, so use it outside the plain application service. A joined transaction physically commits only at its owning outer boundary. |
| [Hibernate 7.4.5 ConstraintViolationException.getConstraintName](https://github.com/hibernate/hibernate-orm/blob/7.4.5/hibernate-core/src/main/java/org/hibernate/exception/ConstraintViolationException.java) | `return constraintName;`; the API documents that names can be unavailable | Inspect structured constraint metadata and SQLSTATE in the actual exception chain. An unknown constraint must not be guessed from a user-facing error string. |
| [Spring Data JPA UserRepositoryTests](https://github.com/spring-projects/spring-data-jpa/blob/main/spring-data-jpa/src/test/java/org/springframework/data/jpa/repository/UserRepositoryTests.java) | `testCreation`, `testRead`, `testReadByIdReturnsNullForNotFoundEntities` and `savingUserThrowsAnException` | Maintainer tests exercise observable storage and integrity behaviour. This reviewed `main` version is not the pinned 4.1.1 test suite; it does not prove CEREMO-specific PostgreSQL races. |
| [PostgreSQL 18 transaction isolation](https://www.postgresql.org/docs/18/transaction-iso.html) and [error codes](https://www.postgresql.org/docs/18/errcodes-appendix.html) | Concurrent uniqueness conflicts and distinct SQLSTATE conditions | Database constraints remain necessary after a pre-insert lookup; use real independent transactions to test our adaptation. |

Spring/Hibernate sources above have inspected Apache-2.0 headers. They are evidence for the APIs/patterns; no complete external implementation was copied. Do not attribute CEREMO schema, class names or business policy to these projects. The attempted pinned 4.1.1 repository test URL and transaction-support test URL were not retrievable; the accessible maintainer tests are labelled `main` explicitly.

### Application workflow and trust boundaries

HTTP requests eventually pass through shared security and Spring MVC to a feature controller, then `UserAccounts`, a complete transaction boundary, the existing plain `UserService`, and `UserRepository`. The adapter handles persistence; it is not the HTTP request/response entry point. Domain/application source dependencies still point inward even though the runtime call passes outward through a port. See the [HTTP boundary findings](../controllers/http-validation.md#request-and-response-boundary-review).

A caller who has authenticated with Keycloak may prepare a free CEREMO account. Public provider browsing does not require this registration. The current application service obtains the trusted identity, verifies current terms acceptance, checks for an existing link, generates the account UUID and creates a PENDING account. Registration must not confer provider membership, verification, staff roles or paid entitlement.

Relevant misuse: two requests for one trusted identity attempt to create two accounts, or an attacker tries to bind a supplied identity to another account. Control: derive identity through the existing trusted adapter, preserve its exact issuer/subject and enforce the existing unique pair inside an atomic transaction. Check both exception outcomes and final stored rows; a UUID, role or successful login alone is not record ownership.

### Planned adapter contract

Keep one plain domain port, `UserRepository`, with its existing `findByIdentity` and `create` signatures. Implement `JpaUserRepository` under `usermanagement/infrastructure/secondary/persistence`. A direct EntityManager adapter is a suitable small implementation for these two operations; Spring Data interfaces are an alternative only if they remove actual duplication. Do not add two extra CRUD interfaces merely because a sample uses them. This is a CEREMO recommendation, not a mandate from Spring.

- `findByIdentity`: require the existing value object; bind issuer/subject as query parameters; load only its linked account; map within the transaction using `UserMapper`; preserve saved account status. Return empty only for no link. Do not turn an outage/integrity error into empty, concatenate query inputs, lower-case identifiers, match by email/phone, or mutate last-seen state during a read-only lookup. Do not silently choose one row if integrity is broken.
- `create`: validate required arguments with shared assertions, map the account with the existing mapper, persist account then its initial identity and flush constraints before reporting persistence success. Use insert semantics, never identity reassignment, `merge` as an upsert policy, `ON CONFLICT DO UPDATE` or swallowed conflicts. Inherit version/audit fields rather than setting them manually. Account ID generation stays in `UserService`; compose technical identity-row ID generation through the existing UUID helper, with no new generator or request-supplied identifier. Last-seen time must come from the server operation, not a client-supplied timestamp.
- Require a real transaction for writes and fail before writing when that precondition is absent. An infrastructure `MANDATORY` transaction guard is a candidate implementation; verify Spring proxy/bean behaviour before relying on it. It supplements, rather than replaces, the complete outer use-case transaction.
- Translate only the intended `(SQLSTATE 23505, uq_user_identity_issuer_subject)` violation into the existing `UserAlreadyExistsException`. Handle provider wrapping/batch exceptions with structured metadata proven by PostgreSQL tests. Other uniqueness failures, foreign keys, invalid values, connection problems and transaction failures must not all become "account already exists". Propagate failure so the transaction rolls back. Never query a transaction already aborted by a constraint failure or expose driver/SQL messages to the HTTP client.

### Planned outer composition

Reuse `wire/database/infrastructure/secondary/DatabaseConfiguration`: repository scanning, transaction management and JPA auditing already exist, with default repository transactions disabled. Keep Spring out of `UserService` and `UserRepository`. Compose one inbound `UserAccounts` boundary that wraps the whole registration call; wire its repository and `CurrentUser` dependencies plus a server Clock and explicit current-terms version. Resolve bean names/visibility deliberately so callers cannot accidentally inject an unwrapped service or duplicate adapter.

No success response may be constructed on the assumption that a callback return means a write has committed. The transaction-owning outer call must complete first. Constraint failures at flush or commit must retain their intended meaning. Keep the transaction short; do not call Keycloak, send messages, schedule background work or make remote calls while holding the account write transaction. Authentication is already performed through shared infrastructure; no second login/password store is required.

Actor auditing is still missing and must not be described as implemented. Existing nullable actor columns and timestamp auditing remain as observed. No cache, new executor, broker, CORS filter, security chain or new migration is needed just to implement this repository contract. Do not use async tenant decoration as proof of transaction/security-context propagation.

### Verification required before calling persistence complete

| Case | Evidence required |
| --- | --- |
| Migrations plus mapping | Disposable PostgreSQL with Flyway and Hibernate schema validation, not Hibernate-generated production DDL. Prove the new JPA mapping matches the actual migration. |
| Exact lookup | Missing identity returns empty without creating rows; correct pair returns the saved account/status; foreign issuer, different subject and case differences do not return someone else's account. |
| Successful registration | Call through the composed inbound boundary, allow commit, then use a separate transaction/connection to verify one account and one correct identity link. |
| Duplicate identity | The known unique constraint becomes the existing feature exception; original identity/account remain unchanged and the losing account is absent. |
| Rollback | Force a genuine failure after the account insert, then inspect both tables outside the failed transaction. A mock `verify(rollback)` is insufficient. |
| Concurrent registration | Two threads use independent transactions and persistence contexts for the same trusted pair; coordinate before conflicting writes with bounded waits, not sleeps or a barrier after one insert is blocked. Exactly one commits, one receives the expected duplicate outcome, and no orphan account remains. |
| Other integrity/connection failure | Not misclassified as duplicate, missing account or successful creation; no partially committed registration. |
| Write without transaction | If a MANDATORY guard is chosen, direct adapter use outside a transaction fails before an insert. |
| Architecture/reuse | No Spring/JPA/Hibernate imports in core; no Lombok; no duplicate IDs/errors/security/configuration; existing shared/wire/feature regressions still pass. |

Use the existing `src/integrationTest` task and Testcontainers dependency. Supply its datasource directly from a disposable container, with no fallback to configured development/production credentials. A parent test rollback does not undo worker-thread commits; inspect final state and clean only the disposable database. Keep test credentials runtime-generated and fixtures synthetic. H2 remains a fast mapping check, not evidence for PostgreSQL commit/concurrency behaviour.

SonarQube and JaCoCo remain required quality-workflow work but their Gradle plugins are still absent. Do not give scanner/coverage commands as executable or claim a quality gate until configured. No new application code or tests were created/run in this research-only review. Prior 14-case schema, 10-case mapping and 388-case foundation results retain their original dates/scopes; they do not establish adapter completion.

### Supporting-publication search limits

Public Medium searches found a [Trendyol seller-eligibility migration article](https://medium.com/trendyol-tech/seller-eligibility-system-our-journey-from-nosql-to-rdbms-f56dafaa1f02), but the opened page exposed no usable article body. A [Vlad Mihalcea LinkedIn transaction-routing post](https://www.linkedin.com/posts/vladmihalcea_read-write-and-read-only-transaction-routing-activity-7448346752338046976-mFcD) appeared in search, but its body was not retrievable. Neither supports implementation claims here. No attributable public Slack conversation was available and no private Slack was accessed. Technical conclusions rely on the primary maintainer sources above, not snippets or presumed company practices. No Salesync source was consulted.

Documentation verification for this review: local links in the seven updated research/product files resolve and the product set remains eight documents. Corrected stale implementation-status text in documents 03, 07 and 08 to acknowledge the completed migration, registration core and identity mapping. Actual source checks still find no repository adapter or UserResource. Application source, tests, dependencies and runtime settings were not changed or rerun.

## First adapter coding guide

Prepared on 21 October 2026. Status: guide proposed; application/test files are not created by the agent. The user asked to start the coding guides, retaining the agreement that the user writes application code.

Subsequent status on 21 October: the user implemented `JpaUserRepository` and its test and reports a passing run. The actual test currently lives under `src/test/java`; move it to the existing integration source set in the next checkpoint. No saved adapter-suite XML was found in this planning review, and no new test run was performed. The [production composition plan](../application/user-registration.md#next-checkpoint-plan-production-composition-and-competing-registrations) is the next work. Historical proposed-code statements below describe the guide delivery, not current absence of the adapter.

Proposed checkpoint: `JpaUserRepository.java` in the existing persistence package plus `JpaUserRepositoryIntegrationTest.java` in the matching `src/integrationTest/java` package. Use direct EntityManager operations for the existing two-method domain port, with explicit constructor dependencies on the shared transactional EntityManager proxy and a `Supplier<UUID>` supplied as `CeremoUUID::generate` from outer composition. This keeps technical identity-row generation configurable without a new UUID utility. The initial last-seen timestamp uses the registration's server-generated `termsAcceptedAt`; future identity linking/login timestamps need their own operation semantics.

The adapter is public/non-final for external composition and Spring transaction proxies, with `@Transactional(propagation=MANDATORY)` on its infrastructure class. It has no component/repository stereotype in this first guide: production registration is deliberately deferred to one explicit wire factory with the complete inbound use-case boundary. Do not instantiate the adapter in controllers or plain services; a bare `new` object bypasses Spring's transaction guard. The integration test factory creates the managed bean and verifies the guard. No missing Clock/Supplier bean is introduced into existing production component scanning.

`findByIdentity` uses parameterized JPQL selecting the associated account, catches only JPA NoResultException as absence and maps inside the active transaction. `create` uses existing mappings, persists account then identity, flushes, and translates only PostgreSQL SQLSTATE 23505 plus the exact named identity uniqueness constraint. Other failures propagate. Inherited version/auditing and domain/repository interfaces remain unchanged.

Additional verified APIs: [Spring 7.0.9 Propagation.MANDATORY](https://github.com/spring-projects/spring-framework/blob/v7.0.9/spring-tx/src/main/java/org/springframework/transaction/annotation/Propagation.java), [SharedEntityManagerCreator](https://github.com/spring-projects/spring-framework/blob/v7.0.9/spring-orm/src/main/java/org/springframework/orm/jpa/SharedEntityManagerCreator.java), [Hibernate 7.4.5 JDBCException.getSQLException](https://github.com/hibernate/hibernate-orm/blob/7.4.5/hibernate-core/src/main/java/org/hibernate/JDBCException.java), [Boot 4.1.1 FlywayAutoConfiguration](https://github.com/spring-projects/spring-boot/blob/v4.1.1/module/spring-boot-flyway/src/main/java/org/springframework/boot/flyway/autoconfigure/FlywayAutoConfiguration.java) and the [Jakarta Persistence 3.2 exception semantics](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2.html). Spring/Hibernate source is Apache-2.0; the code proposed in the guide is original CEREMO adaptation, not copied complete maintainer classes.

Test design: use a Spring-managed PostgreSQL 18.0 container bean (init start, destroy stop) and dependent Hikari datasource built only from the container's runtime URL/credentials. Disable embedded datasource replacement, enable Boot Flyway and Hibernate validate, disable second-level cache, and pair Hikari auto-commit false with Hibernate provider-disables-auto-commit true. Spring manages dependent-resource shutdown through a dirty test context. This uses existing dependencies and avoids global properties, manual Spring context closure or a service-connection module.

The test class disables the enclosing test-managed transaction and uses explicit TransactionTemplates for committed writes/readbacks and rollback checks. Cleanup deletes identity/account rows only in the disposable database. Proposed cases: four status-preserving exact-identity round trips (including missing/foreign/case-mismatched lookup), duplicate rejection without orphan account, unrelated primary-key conflict not misclassified, explicit outer rollback after both inserts, and write rejection outside a transaction. Expected new case count is eight, not an observed pass. Race tests and the complete UserService transaction/wire composition remain the next checkpoint; no controller is exposed in this guide.

Proposed command after the user writes both files:

```powershell
.\gradlew.bat integrationTest --tests '*JpaUserRepositoryIntegrationTest' --tests '*UserSchemaIntegrationTest'
```

Historical expectation at guide delivery: eight adapter cases plus 14 schema cases. That proposal had not been run at that time. Later implementation, execution and coverage checkpoints above and below supersede it. Real Keycloak interoperability and production actor auditing remain pending.

## Status update checkpoint, 4 November 2026

Question: persist an authorised PENDING-to-ACTIVE transition without replacing the identity or overwriting a concurrent restriction. Existing [JpaUserRepository](../../../src/main/java/io/ceremo/ceremobackend/account/infrastructure/secondary/persistence/JpaUserRepository.java) now implements the plain repository's updateStatus operation. It loads the managed UserJpaEntity by ID, rejects a missing row, changes only status and flushes. Identity, account ID and terms fields are preserved. No migration, upsert or replacement entity was introduced.

Reuse: shared BaseEntity's optimistic version/audit fields and CeremoException/feature keys; the existing REQUIRED application transaction and MANDATORY repository boundary. The use-case read and update must share that transaction so version checking detects a concurrent change. Detached snapshots used in another transaction do not receive an implicit version guarantee from this signature. See [official persistence evidence and business policy](../application/user-registration.md#verified-email-activation-4-november-2026).

Verification: H2 tests cover status-only mutation/versioning and missing/null inputs. The actual PostgreSQL race commits a versioned suspension after the activation read, then confirms the activation fails with an optimistic exception, rolls back and leaves SUSPENDED. The registration uniqueness/rollback suites remain intact. The [4 November quality record](../quality/tests-and-analysis.md#account-feature-verification-4-november-2026) records all 37 PostgreSQL and 512 regular tests passing with 100% per-class line/branch coverage. A production suspension endpoint, live Keycloak and production actor auditor remain unimplemented/unverified.

## Provider account security, 5 November 2026

JpaUserDirectory implements the internal UserDirectory existence port inside User Management. It participates in the caller transaction and exposes no public directory. The tenant adapter locks its root before a fresh database membership query; account/tenant adapters share no persistence entity access across features. See [provider account security](../ports-and-adapters/provider-account-security.md).


Consolidation, 5 November 2026: current source, ports, adapters and tests are under `account`; composition is `wire/account/AccountConfiguration`. Existing domain behaviour, transactions, HTTP routes and database schema continue. Older package names in dated examples/commands are historical. See [the approved consolidation](../ports-and-adapters/provider-account-security.md#account-module-consolidation-5-november-2026).
