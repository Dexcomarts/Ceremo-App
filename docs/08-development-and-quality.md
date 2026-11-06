# CEREMO Development, Security and Release Quality

Version 3.8 · 6 November 2026 · Engineering assurance and operating requirements

## 1. Local services

CEREMO's quality controls must provide evidence that private records are isolated, listing/payment rules survive failure and concurrency, and the service can be operated and restored. Passing coverage alone does not establish any of these outcomes.

| Evidence area | Checked-in capability or dated result | Remaining release evidence |
| --- | --- | --- |
| Build and tests | Java 25/Gradle build, regular suite and disposable PostgreSQL integration suite are configured. | Fresh results for the release revision, including failures/skips and report provenance. |
| Accounts and persistence | Domain/application, trusted identity, JPA adapter, migration, transactional composition, self-account HTTP and verified-email activation exist. | Contact/ownership/recovery policies, live identity/email verification and production request limits. |
| Coverage | Fresh 6 November suites passed per-class 100% line/branch enforcement; Sonar imported their results. | Fresh combined reports for each later release revision and unchanged class scope. |
| SonarQube | Latest analysis imported 584 tests and 100% coverage; hotspot review is 100%, rating A. Gate OK evaluated new issues and duplication. | Existing reliability/maintainability findings, required server-policy configuration and deliberate failing-gate verification. |
| Dependency integrity | Reviewed SHA-256 baseline covers 748 artifacts; normal builds reject weaker modes, missing metadata and checksum mismatches. | Review each dependency change; checksum verification does not authenticate publishers or scan for vulnerabilities. |
| Identity | Shared route/token handling and principal/decoder tests exist. | Live issuer/JWKS exchange, rotation, assurance/revocation policy and production integration. |
| Operations | Local Compose services and development instructions exist. | Production ownership, least privilege, monitoring, incident handling and demonstrated restoration. |

The latest application verification is the 6 November hotspot checkpoint in section 8.12. Earlier dated results remain historical evidence for their revisions. Live Keycloak email verification and production operations remain unverified.

Use Keycloak for authentication, PostgreSQL for application data, SonarQube for code analysis and JaCoCo for coverage. Keycloak is also the selected production identity provider. Shared OAuth2 adapters and `wire` configuration are implemented. The wiring smoke test uses in-memory H2 and test identity configuration; it does not call a live realm or PostgreSQL. The current YAML files contain connection settings. The environment, live integration and quality checks below remain requirements to complete and verify.

| Service | Local address | Data |
| --- | --- | --- |
| CEREMO API | `127.0.0.1:8080` | Application PostgreSQL |
| Keycloak | `127.0.0.1:9080` | Local `ceremo` realm; development file database in `keycloak_data` |
| PostgreSQL | `127.0.0.1:5432` | Local `ceremo` database/user, PostgreSQL 18.0 in `postgresql_data` |
| SonarQube | `127.0.0.1:9001` | Local evaluation database and dedicated data/extensions/log volumes |

Service files are under `src/docker`. The root Compose file includes them and uses project name `ceremo`. For a fresh local setup, validate and start the services from the project root:

```powershell
docker compose config --quiet
docker compose up -d postgresql keycloak sonar
docker compose ps
```

The existing image tags are retained. PostgreSQL and Keycloak now have health checks and persistent volumes. PostgreSQL 18 mounts its named volume at `/var/lib/postgresql`, following the [official image layout](https://hub.docker.com/_/postgres). Before switching an existing stack, identify and back up/migrate its data: older containers may use anonymous volumes or a different Compose project. These file changes do not migrate existing data. Do not delete volumes as part of setup.

This is a development stack: PostgreSQL uses local trust authentication, Keycloak uses `start-dev`/a file database, and SonarQube uses its evaluation database. Production needs password-authenticated PostgreSQL, Keycloak production startup/HTTPS and dedicated database/users. Keycloak's management health port stays internal; its readiness check uses `/health/ready`. See [Keycloak health checks](https://www.keycloak.org/observability/health).

Override bootstrap credentials with `CEREMO_KEYCLOAK_ADMIN_USERNAME` and `CEREMO_KEYCLOAK_ADMIN_PASSWORD` before first startup. Compose reads a root `.env`; keep it untracked. A Spring process launched from the IDE/Gradle does not automatically read that file. Existing realm exports retain their imported local fixtures; replace fixture users/client credentials before any shared or production use.

## 2. Keycloak

The current local import is `src/docker/keycloak-realm-config/ceremo-realm.json`, with realm `ceremo` and issuer `http://localhost:9080/realms/ceremo`. The public `web_app` client uses authorization code with S256 PKCE, exact localhost/127.0.0.1 Swagger and backend callbacks, and the existing Postman callback. Implicit/password grants and its offline-access scope are disabled. Its audience mapper adds `ceremo-api` to access tokens. CEREMO roles are CUSTOMER, PROVIDER, SUPPORT, FINANCE and ADMIN, with `ROLE_` prefixes.

The backend's local `oidc` registration uses that public client with `client-authentication-method: none` and no client secret, which enables Spring's PKCE behaviour. Keep production client/session design separate. See [Spring public clients](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/authorization-grants.html).

Startup imports skip realms that already exist. Restarting Keycloak will not apply edits to the JSON in a persisted realm; update the existing local realm through its administration tools or perform a deliberate backed-up import. Do not delete realm data automatically. See [Keycloak import rules](https://www.keycloak.org/server/importExport).

The existing Swagger configuration uses authorization code, with PKCE enabled through `springdoc.swagger-ui.oauth.use-pkce-with-authorization-code-grant`. Register the exact `/swagger-ui/oauth2-redirect.html` URL for the local API origin and set the public API-testing client ID in Swagger's OAuth settings. Do not add a client secret there. The browser login's `oidc` registration can use a separate confidential client. Authorization/token URLs default to that provider's endpoints; override `springdoc.oauth2.authorization-url`/`token-url` only when needed.

An API tool can obtain a token before the selected Next.js frontend is built. Call CEREMO with `Authorization: Bearer ...`. Keycloak handles passwords, refresh, recovery and MFA; the backend validates issuer/signature/audience/time claims and links `(issuer, subject)` to a local user. The API-testing client is separate from the future web client, whose callback and session configuration still need definition. [Spring resource-server JWT guidance](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).

Seed synthetic customers, two provider businesses and staff roles. Application permissions remain in CEREMO: a Keycloak administration role must not automatically grant platform finance/admin rights. Configure a verifiable MFA assurance level for staff and financial changes.

Proposed realm policy: five-minute access tokens, 30-minute idle sessions, a 12-hour maximum session and refresh rotation with reuse disabled. Confirm the accepted assurance/session policy before production use. Use fake SMS for contact verification. Do not store refresh tokens, passwords or MFA secrets in the application database.

Keep issuer URLs consistent across Docker, API tools and the backend. A token issued as `localhost:8081` cannot be accepted as though it came from a different container hostname. Configure a reachable JWKS endpoint where needed; do not disable issuer checks to work around networking.

Test invalid/expired tokens, wrong realm/audience, missing MFA, foreign records, local suspension, revocation, signing-key rotation and one real issuer/JWKS flow. Mocked principals alone do not prove token validation works. Logout does not instantly invalidate every issued JWT; the local revocation check handles required immediate blocking.

Production Keycloak uses a separate issuer/realm, production startup, HTTPS, strict hostname, dedicated persistent database, restricted administration, managed secrets and tested backups. Local `start-dev` is for development. [Keycloak configuration](https://www.keycloak.org/server/configuration), [container setup](https://www.keycloak.org/server/containers).

## 3. Tests and JaCoCo

Agents must follow the persistent [security and quality rules](../.github/security-quality-rules.md) before coding guidance, implementation or review. They connect the checks below to feature authorisation, abuse cases, shared code reuse and honest reporting of verification results. Gradle now declares JaCoCo and the SonarQube scanner; their configuration alone is not a passing result.

The [research memory index](../.github/research/README.md) organises past evidence by domain, application, repositories, controllers, ports/adapters, database and quality. Agents read the relevant entry before work and save new investigations with sources, compatibility limits and actual verification status.

JUnit and Spring tests execute the tests. JaCoCo measures line/branch coverage. Use PostgreSQL Testcontainers for real migrations, entitlement/invoice constraints and concurrent trial/period grants, with stub servers for PSP/SMS failures.

The existing shared and wiring suite runs without external services:

```powershell
.\gradlew.bat test --tests 'io.ceremo.ceremobackend.shared.*' --tests 'io.ceremo.ceremobackend.wire.*'
```

It checks signed JWT validation, route permissions, CORS binding, cache creation, executor configuration/context propagation, JPA timestamp/version behaviour and generated OpenAPI metadata. H2 is a test-only dependency; this suite does not establish PostgreSQL migration behaviour, live Keycloak/JWKS rotation or external Logstash delivery. The original application context test now also uses H2 and the shared test OIDC configuration, with a mocked JWT decoder.

The separate PostgreSQL migration suite is now executable with Docker available:

```powershell
.\gradlew.bat integrationTest --tests '*UserSchemaIntegrationTest'
```

It starts disposable PostgreSQL 18.0 and supplies Flyway/JDBC with that container's connection directly. It does not load the local application database settings. On 20 October 2026, all 14 cases passed: migration history/validation/re-run, identity uniqueness, foreign keys, identifier case, deletion restrictions, required identity values, invalid status and inherited audit/version defaults. This is dated evidence of the schema tests. Adapter/registration atomicity and competing creation were subsequently exercised in the 21–22 October runs; see sections 8.6–8.7.

Gradle's `jacoco` plugin is configured with **0.8.15**. Version 0.8.14 introduced official Java 25 support. [JaCoCo release history](https://www.jacoco.org/jacoco/trunk/doc/changes.html).

Generate HTML for local review and XML for SonarQube. Wire test/report dependencies explicitly; `jacocoTestReport` does not automatically run tests and coverage verification is not automatically part of `check`. [Gradle JaCoCo plugin](https://docs.gradle.org/current/userguide/jacoco_plugin.html).

| Output | Configured path |
| --- | --- |
| Unit HTML | `build/reports/jacoco/test/html/index.html` |
| Unit XML | `build/reports/jacoco/test/jacocoTestReport.xml` |
| Combined unit/integration XML | `build/reports/jacoco/combined/combined.xml` |
| JUnit results | `build/test-results/test/`, `build/test-results/integrationTest/` |

`jacocoCombinedReport` and `jacocoCombinedCoverageVerification` use execution data from both suites and the same main classes/sources. The report depends on both test tasks and rejects missing or empty execution files. `check` includes coverage verification. Do not add percentages from separate reports or run the same tests twice.

The user requires **100% line and 100% branch coverage for every application class**, superseding the earlier 80%/70% proposal on 21 October 2026. Enforce `element = "CLASS"` with `LINE` and `BRANCH` counters, `value = "MISSEDCOUNT"` and `maximum = 0`. Both suites contribute execution data for the same compiled revision. Any missed line/branch or missing required input fails the coverage checkpoint. No new exclusions were added; do not bypass checks, mark handwritten code as generated, disable tests or use assertion-free tests to manufacture a pass. Cover meaningful behaviour and failure paths.

## 4. SonarQube

The local [SonarQube dashboard](http://127.0.0.1:9001/dashboard?id=ceremo_backend) is configured for viewing without login, approved on 6 November. `src/docker/sonar.yml` sets `SONAR_FORCEAUTHENTICATION=false` and retains the `127.0.0.1:9001` binding. Public project reports and source are visible to anonymous local visitors; private projects retain their visibility restrictions. This does not create an administrator browser session. Administration and analysis still require the relevant authenticated permissions; the current Anyone group has no global or project grants. The [local access record](../.github/research/quality/tests-and-analysis.md#dashboard-login-comparison-6-november-2026) records the comparison, restart and verification. [SonarQube user-account guidance](https://docs.sonarsource.com/sonarqube-community-build/instance-administration/security/user-accounts) explains the setting; its newer 26.10 anonymous-analysis guarantee must not be assumed for our 25.12 server.

The pinned `org.sonarqube` Gradle plugin is **7.5.0.8588**. Its `sonar` task depends on `check`, imports the combined XML and both JUnit result directories, and includes the integration source root/test binaries. It waits for the server quality gate. `SONAR_HOST_URL` defaults to `http://localhost:9001`; inject `SONAR_TOKEN` through the environment or a secret manager. Verify server/analyser support for Java 25 independently of the application JDK and scanner runtime. [SonarScanner for Gradle](https://docs.sonarsource.com/sonarqube-community-build/analyzing-source-code/scanners/sonarscanner-for-gradle).

The local `sonar-token` helper is opt-in through the `tools` profile. Starting only `sonar`, or starting the default stack without this profile, does not create the helper. To build it and make its container visible without starting generation, run:

```powershell
docker compose --profile tools create --build sonar-token
```

On 6 November 2026, the founder confirmed that this was a fresh instance using the initial administrator login. Initial setup changed that password to a randomly generated value, saved it in `/tokens/sonar-admin-password`, and verified authentication. The existing helper then generated `/tokens/sonar-token`. Both files are in the local `ceremo_sonar_tokens` Docker volume, owned by UID/GID 10001 with mode 0600; the directory has mode 0700. No credential value belongs in this document or command history. The successful one-off generator exited and was removed; the separately prepared `ceremo-sonar-token-1` container remains CREATED. See the [authenticated analysis record](../.github/research/quality/tests-and-analysis.md#authenticated-analysis-6-november-2026).

The helper currently creates an administrator-owned user token, not a project-scoped analysis token. It was used only against the loopback-bound local evaluation server. Do not promote this credential into CI or production; a dedicated, limited analysis credential and its lifecycle need separate configuration. Docker file permissions do not protect secrets from a person with Docker daemon access.

Sign in to SonarQube at `http://localhost:9001` and complete initial password setup if needed. Set `CEREMO_SONAR_PASSWORD` temporarily in the current PowerShell session, using a masked prompt rather than a literal password in command history, and run:

```powershell
docker compose --profile tools run --rm sonar-token
```

The helper uses the current password; it no longer changes the administrator password automatically. It waits for readiness with bounded retries, rejects HTTP/JSON failures and writes the generated token to `/tokens/sonar-token` in the `sonar_tokens` volume. Its value is not written to logs. An existing non-empty file is reused without creating a new token; check/reissue it manually if revoked or expired. Clear the temporary password environment variable in a finally block. A successful generator is a one-off job that exits; it is not another long-running server, and `run --rm` removes that job container after execution. Load the token privately into `SONAR_TOKEN` for analysis. The Docker helper does not add the Gradle scanner/coverage tasks.

Local analysis completed on 6 November with `.\gradlew.bat sonar --console=plain --stacktrace`. SonarQube 25.12.0.117093 and Java analyser 8.22 imported **582 tests**, **100% line coverage**, **100% branch coverage** and **0% duplication**. The unchanged application reused the 5 November test/coverage outputs; the input guard and actual scanner executed. The first attempt failed to extract the provisioned Java runtime; an unchanged diagnostic retry succeeded. Its underlying extraction failure remains unexplained.

The server returned quality-gate status **OK**, with an empty evaluated-conditions list and inherited `PREVIOUS_VERSION` new-code definition. This is the initial baseline, not evidence that CEREMO's proposed new-code conditions have been exercised. The default Sonar way configuration requires zero new issues, 80% new-code coverage, at most 3% new-code duplication and 100% new hotspot review. It has not been changed to CEREMO's proposed server policy; the stricter per-class Gradle rule remains enforced separately.

That initial analysis reported one potential null-dereference bug, 71 code smells and two TO_REVIEW hotspots. The later implementation and analysis supersede the hotspot status: dependency verification is now enforced, and the scoped CSRF exception is reviewed as Safe with supporting regressions. Sonar reports **100% hotspot review, rating A, zero pending hotspots, 584 tests and 100% line/branch coverage**. One reliability bug and 71 code smells remain. No rule was suppressed. JUnit class-to-file mapping and missing Git blame warnings remain; aggregate tests and coverage imported correctly. See the [remediation evidence](../.github/research/quality/tests-and-analysis.md#hotspot-remediation-6-november-2026).

The latest analysis completed at **14:08:12 UTC on 6 November**, with gate **OK**. It evaluated zero new issues and 0% new-code duplication against thresholds of zero and 3%; it did not evaluate a coverage or hotspot-review condition. Its `PREVIOUS_VERSION` baseline is the earlier same-day analysis. Required server-policy configuration and failing-gate verification remain outstanding; the local per-class coverage rule is enforced independently.

Use the checked-in project key `ceremo_backend`. Configure `sonar.coverage.jacoco.xmlReportPaths` to read the combined XML report, along with unit/integration JUnit results. SonarQube imports coverage; it does not generate the report or run tests. [Java coverage](https://docs.sonarsource.com/sonarqube-server/2026.1/analyzing-source-code/test-coverage/java-test-coverage).

Supply `SONAR_HOST_URL` and secret `SONAR_TOKEN` through the environment/CI. Compile, run tests and generate reports before analysis. Keep the scanner token out of command history and runtime images.

Proposed rules for new code:

- No unresolved blocker/critical security or reliability issues.
- Review all security hotspots.
- 100% new-code coverage and no more than 3% duplicated lines, subject to explicit server configuration and verification.

Agree the new-code definition and compatible conditions in the selected edition. Branch/PR decoration features depend on the edition. CI must fail when the required quality gate fails or cannot be determined; a successful report upload is not a passing gate. These proposed server conditions must supplement the mandatory local per-class coverage rule, rather than replacing it. [SonarQube gate integration](https://docs.sonarsource.com/sonarqube-community-build/analyzing-source-code/ci-integration/overview).

## 5. Quality commands

The suites/plugins/tasks are configured. With Docker running, use this sequence from the project root:

```powershell
.\gradlew.bat check bootJar
# Supply SONAR_TOKEN securely in this process before analysis.
.\gradlew.bat sonar
```

`check` depends on both suites, combined reporting and the 100% coverage rule. The 22 October report meets the target; future missed lines/branches must fail this checkpoint even when tests pass. Analyse the same compiled revision used for reports. Preserve JUnit/XML/HTML output after a failure, with no credentials or real personal data in artifacts.

The project provides `compileJava`, `test`, `integrationTest`, `bootJar`, `jacocoTestReport`, `jacocoCombinedReport`, `jacocoCombinedCoverageVerification` and `sonar`. Analysis requires the server and an injected token; test success alone is not a passing coverage or server quality gate.

## 6. Setup acceptance criteria

| Release area | Required acceptance evidence | Proposed review role |
| --- | --- | --- |
| Reproducibility | Fresh setup uses documented versions/commands and correct disposable test targets. No test falls back to shared/production data. | Engineering |
| Identity/access | Real issuer/JWKS works; wrong tokens, missing required assurance and foreign records fail. | Engineering/security |
| Integrity | Constraints, rollback, concurrent identity/trial/invoice grants and uncertain payment recovery are verified for implemented features. | Engineering/finance |
| Coverage | Failing tests, any uncovered application line/branch and missing/empty required report input block the build. | Engineering |
| Server analysis | Combined coverage imports correctly; a deliberate required-gate failure blocks promotion. Confirm analyser compatibility. | Engineering/security |
| Runtime configuration | No fixture issuer/users, start-dev, development trust credentials or exposed secrets in production. | Engineering/operations |
| Recovery | Restore application/identity data and keys; preserve privacy decisions and reconcile billing before resuming charges. | Operations/finance/privacy |
| Support readiness | Named owners, incident escalation, assessment/case capacity and dependency/job alerts are demonstrated. | Operations/product |

Record the revision, date, command/environment, result, artifacts and unresolved findings for each check. Roles are proposed; named people and numeric operating targets still require approval.

## 7. Account recovery and existing defaults

Source status reviewed 4 November 2026. The 20 October recovery baseline is superseded where implementation progressed. These are source/configuration observations, not a claim that the account workflow is deployed.

| Concern | Existing implementation and reuse decision |
| --- | --- |
| Required domain values | `User` and `UserIdentity` use `shared.error.domain.Assert`; keep their different display-name, terms-version and identity length limits. |
| Account state | The domain factory starts PENDING. Registration activates only after trusted boolean email verification; explicit later activation is idempotent for ACTIVE and denies SUSPENDED/ANONYMISED. GET never activates. Suspension/anonymisation operations remain unimplemented. |
| Identity | `UserIdentity` preserves issuer and subject. It does not duplicate shared `Username`, `Email` or `Name`: those values cannot replace the stable external identity pair. |
| Account lookup | `UserService` uses the domain `UserRepository` and application `CurrentUser` port. Looking up a missing account does not create it. `SpringCurrentUser` implements the security port. `JpaUserRepository` and `wire/account/AccountConfiguration` provide persistence and decorated application composition. |
| Authentication failures | `SpringCurrentUser` reuses `AuthenticatedUser.attributes()`, `UserIdentity` validation, `NotAuthenticatedUserException` and the existing `AuthenticationExceptionAdvice`. It requires string issuer/subject claims, checks the existing configured issuer, and rejects invalid identity values without exposing them. `username()` is not the identity key. |
| Duplicate account | `UserAlreadyExistsException` extends the existing `CeremoException`, with `UserErrorKey.USER_ALREADY_EXISTS` and explicit CONFLICT status. The shared handler renders HTTP 409 with existing problem-response formatting and English/French message bundles. No feature-specific controller advice is needed. |
| Dependency checks | `Objects.requireNonNull` protects mandatory service dependencies. It is a JDK construction check, not a replacement domain-validation framework. |
| Database defaults | Hibernate `ddl-auto` is `none`; open-in-view is disabled. Shared JPA auditing and transaction management are enabled; default repository transactions are disabled. Explicit whole-use-case transactions remain necessary. |
| Database foundation | Flyway dependencies and the initial user/identity migration are implemented and tested on disposable PostgreSQL 18.0. User/identity JPA mappings exist; the selected 10-case mapping/domain run passed on 21 October 2026, including H2 rollback checks. The repository adapter and outer registration transaction now exist; later PostgreSQL checks cover rollback and competing registration. Version 17 is a production proposal, not the current local image. |
| Shared entity base | Existing JPA mappings reuse `BaseEntity`; keep it outside the plain domain model. Actor auditing still requires an adapter. |
| ID generation | `CeremoUUID.generate()` already exists; `UserService.register(...)` already uses it for new account IDs; composition also supplies it for persistence identity IDs. Do not add another generator or generate IDs during lookup. |
| Local identity | Configured issuer is `http://localhost:9080/realms/ceremo`, API audience is `ceremo-api`, and `web_app` is a public client using authentication method `none`. Keep credentials and token validation inside the existing identity setup. |
| Optional infrastructure | Legacy multitenancy filters/aspects and Logstash TCP logging require explicit enablement; do not use tenant subscription fields as CEREMO provider entitlements. CORS starts with no allowed origins unless configured, and credentials are disabled by default. |
| Quality setup | JUnit, PostgreSQL integration, combined JaCoCo enforcement and Sonar analysis passed the latest checkpoint in section 8.12. Required release-gate policy and remote CI remain unverified. |

The shared error regression uses a test-only endpoint to verify duplicate-account responses in English, French and fallback English. The account HTTP boundary is implemented; the earlier recovery checkpoint described here predates it. Run the shared, wiring and account suites together after recovery changes. The wiring smoke test uses H2 and test identity configuration; it does not verify real PostgreSQL constraints, Keycloak, Docker startup or production configuration.

The error integration follows the existing shared handler and [Spring MVC exception handling](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html). Keep database adapter state handling consistent with [Spring Data JPA entity persistence](https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html), rather than copying domain objects directly into JPA.

The current-identity adapter is under `account/infrastructure/secondary/security`, outside the plain domain/application core. It translates authentication already established by the shared Spring Security chains; it does not decode raw tokens, perform another login, or replace issuer/audience/signature verification. Tests exercise JWT and OIDC principals, missing/anonymous/unauthenticated callers, foreign or malformed claims and the adapter-to-account-lookup path. These principal-based tests do not establish live Keycloak interoperability. See [Spring Security's validated JWT flow](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html). Existing UUID generation and error/validation helpers remain authoritative; the rebuild adds no parallel generators or error handlers.

## 8. Database and tooling evidence

Original evidence review: 20 October 2026, with later implementation checkpoints in sections 8.6–8.7. This register explains the design references; current implementation status is stated above. Each guide must attach a short relevant evidence excerpt, identify adaptations and end with a check of the actual CEREMO implementation. Public maintainer documentation, source and tests provide the technical evidence. No private Slack messages were accessed.

The user has excluded Salesync as a reference unless explicitly requested for the current task. The earlier Salesync entries have been replaced below with public maintainer code. Existing CEREMO code remains the first place to check for reusable behaviour.

### 8.1 Current compatibility

The original dependency review found Boot 4.1.1-managed Flyway 12.4.0 and Testcontainers 2.0.5. The build now declares the Flyway/PostgreSQL and Testcontainers dependencies; dependency management alone would not have installed them. Resolve exact transitive versions from the build when upgrading or reproducing a release. The existing PostgreSQL Docker image is 18.0; use that target for the first database tests. PostgreSQL 17 remains an earlier production proposal and must not be mistaken for the local image.

The [Boot 4.1 database guide](https://docs.spring.io/spring-boot/how-to/data-initialization.html) specifies `spring-boot-starter-flyway` plus `flyway-database-postgresql`. It recommends one schema-initialisation mechanism. Migrations will own the schema; use Hibernate validation in the PostgreSQL migration tests. Existing H2 mapping tests deliberately generate their own schema with Flyway disabled and cannot prove migration correctness.

### 8.2 Source register

| Evidence | What it demonstrates | CEREMO adaptation and limits |
| --- | --- | --- |
| [Boot 4.1.1 FlywayMigrationInitializer](https://github.com/spring-projects/spring-boot/blob/v4.1.1/module/spring-boot-flyway/src/main/java/org/springframework/boot/flyway/autoconfigure/FlywayMigrationInitializer.java) | Framework-owned source invokes Flyway during initialisation | Use the existing Boot integration and versioned SQL; no separate handwritten startup migration runner is needed. Apache-2.0 source. |
| [Spring Data JPA 4.1.1 SimpleJpaRepository](https://github.com/spring-projects/spring-data-jpa/blob/4.1.1/spring-data-jpa/src/main/java/org/springframework/data/jpa/repository/support/SimpleJpaRepository.java) | `save` chooses persist or merge based on entity state; `saveAndFlush` explicitly flushes | This explains persistence mechanics, not CEREMO's architecture. Keep a separate adapter implementing the existing domain repository and preserve its insert-only creation contract. Apache-2.0 source. |
| [Keycloak 26.4.2 FederatedIdentityEntity](https://github.com/keycloak/keycloak/blob/26.4.2/model/jpa/src/main/java/org/keycloak/models/jpa/entities/FederatedIdentityEntity.java) | A persistent relationship separates a local user, provider and external user identifier | Learn the separation only. Keycloak's broker provider/realm key is not CEREMO's exact `(issuer, subject)` key or proof of its uniqueness. Do not copy token storage or Keycloak's schema. Apache-2.0 source. |
| [Flyway versioned migrations](https://documentation.red-gate.com/fd/versioned-migrations-273973333.html) and [validation](https://documentation.red-gate.com/fd/validate-277578898.html) | Ordered versioned migrations and checksum validation | Keep applied shared migrations immutable. Correct deployed mistakes with a new migration. Do not use repair, baseline or clean as automatic remedies for unexplained failures. |
| [PostgreSQL 18 constraints](https://www.postgresql.org/docs/18/ddl-constraints.html) | Composite uniqueness, foreign keys and checks enforced by the database | Name the `(issuer, subject)` unique constraint, require both values, reference `app_user` and index `user_id`. Test on PostgreSQL. |
| [Testcontainers 2.0.5 PostgreSQL source test](https://github.com/testcontainers/testcontainers-java/blob/2.0.5/modules/postgresql/src/test/java/org/testcontainers/postgresql/PostgreSQLContainerTest.java) | A real PostgreSQL container started and queried by a test | MIT-licensed reference. Use the 2.x API and CEREMO's PostgreSQL version. Its historical image in `testSimple` is not a recommended deployment version. |
| [Testcontainers Spring Boot quickstart build](https://github.com/testcontainers/testcontainers-java-spring-boot-quickstart/blob/main/build.gradle) | A runnable example combining JPA, Flyway and PostgreSQL tests | The inspected build uses Boot 3.1.3 and Testcontainers 1.19.0. Learn the workflow; use current Boot 4/Testcontainers 2 dependencies and imports for CEREMO. |
| [Boot service connections](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html) | Test-owned connection details for container-backed applications | Connect tests to their disposable PostgreSQL container, including Flyway's target. Check for separately configured Flyway URLs rather than assuming a datasource override covers every connection. |
| [Spring transaction templates](https://docs.spring.io/spring-framework/reference/data-access/transaction/programmatic.html) | Programmatic transaction boundaries and rollback handling | Put the Spring transaction decorator outside the plain application core and cover the entire registration use case. |
| [SonarSource Gradle coverage example](https://github.com/SonarSource/sonar-scanning-examples/tree/master/sonar-scanner-gradle/gradle-multimodule-coverage) | XML reports and explicit analysis/report ordering | Reference its wiring only. CEREMO is one Gradle project with multiple test suites; no subprojects or floating plugin version are needed. Keep secrets in environment injection. |
| [Gradle JaCoCo](https://docs.gradle.org/current/userguide/jacoco_plugin.html), [JaCoCo releases](https://www.jacoco.org/jacoco/trunk/doc/changes.html), [Sonar Gradle scanner](https://docs.sonarsource.com/sonarqube-community-build/analyzing-source-code/scanners/sonarscanner-for-gradle) | Coverage generation, verification tasks and scanner prerequisites | The build now pins JaCoCo 0.8.15 and scanner 7.5.0.8588 with Gradle 9.8.1/Java 25. Fresh server/analyser compatibility and the server gate remain unverified. |

Repository links using a branch can change. Prefer a released tag or resolved commit in the actual implementation guide and retain applicable licence notices if copying substantive code. A reference to maintained public code is not evidence that its design is universally used or that CEREMO's adaptation passes tests.

GitHub displayed approximately 81.8k stars for [Spring Boot](https://github.com/spring-projects/spring-boot), 3.3k for [Spring Data JPA](https://github.com/spring-projects/spring-data-jpa), 8.8k for [Testcontainers Java](https://github.com/testcontainers/testcontainers-java), 10.2k for [Flyway](https://github.com/flyway/flyway), 37.4k for [Keycloak](https://github.com/keycloak/keycloak) and 4.6k for [JaCoCo](https://github.com/jacoco/jacoco) when reviewed on 20 October 2026. These are dated, rounded observations, not a quality guarantee. Relevant maintainer code and tests are the evidence for each decision.

For company engineering evidence, [Stripe's online migrations article](https://stripe.com/blog/online-migrations) explains phased migration, dual writes, backfill and switching reads/writes. Apply those ideas when evolving populated tables; this is not Spring Boot code and does not justify introducing dual writes for empty initial tables. [Stéphane Nicoll's public LinkedIn post](https://www.linkedin.com/posts/snicoll_using-spring-boot-with-testcontainers-activity-7270784651009437696-w3K3) discusses improved Boot detection of container-backed test databases; current Spring documentation remains the API reference. Medium search results included examples using older versions and `ddl-auto=update`; none was selected as implementation authority for this stage. No relevant authenticated Slack discussion was available as verified evidence.

### 8.3 Proposed implementation sequence

Historical plan from 20 October, retained for traceability. Migration, adapter, transaction and tooling work below subsequently progressed; sections 7 and 8.6–8.7 describe the current/recorded checkpoints. Do not repeat these installations or recreate the implemented classes.

1. Add the migration dependencies and a separate `integrationTest` suite. Its connection must come from a disposable PostgreSQL container; no test may silently fall back to the local shared database. A missing Docker runtime fails the required database check rather than skipping it.
2. Add a timestamped initial migration for `app_user` and `user_identity`, matching the implemented fields and inherited `BaseEntity` columns. Do not add unimplemented feature tables or seed real identities. Test migration, validation and a second migration run against the disposable database.
3. Add `UserIdentityJpaEntity` and `JpaUserRepository`, reusing `UserIdentity`, `UserMapper`, `UserRepository`, shared UUID generation and existing feature errors. Exact issuer/subject lookup must preserve account status; never merge by email or phone. Keep the identity table extensible to multiple explicitly linked identities without implementing an account-linking flow yet.
4. Compose `UserService` and an outer transaction decorator in infrastructure/wiring. Persist the account and first identity together. Force constraints to be checked before success is returned. Translate only the expected identity-uniqueness failure into `UserAlreadyExistsException`; unrelated integrity and connectivity failures must retain their actual meaning. Never recover by querying inside an aborted PostgreSQL transaction.
5. Configure JaCoCo execution/report data for both suites and then SonarQube analysis using the same compiled revision. Enforce the quality gate only after verifying that the reports are complete, imported and current. Follow sections 3 to 5 for proposed thresholds and secret handling.

### 8.4 Required database evidence

The first migration check must prove empty-database creation, checksum validation and no pending work on a second run. Schema tests must reject duplicate issuer/subject pairs and missing parents, allow distinct subjects and the same subject under a different issuer, and preserve exact identifier case. Test identifier collation explicitly. Test account status and required-value constraints too.

The adapter stage must prove that a failed identity insertion rolls back the newly inserted account. Use two independent transactions for the concurrent-registration test: exactly one commits, the loser receives the expected duplicate failure and no orphan account remains. Assert both the failure and final database contents. PostgreSQL documents the concurrency behaviour of unique conflicts under [transaction isolation](https://www.postgresql.org/docs/18/transaction-iso.html); a mocked repository cannot establish that behaviour.

The expanded [persistence investigation](../.github/research/repositories/user-persistence.md#persistence-and-request-boundary-research-for-the-next-stage) records pinned source excerpts, insert-only semantics, structured duplicate detection, transaction composition and PostgreSQL acceptance cases. The [HTTP boundary review](../.github/research/controllers/http-validation.md#request-and-response-boundary-review) distinguishes the future controller from the repository and records required DTO, authentication, status, malformed-input and rate-limit checks. Security-filter responses and MVC advice have different entry points; do not claim a uniform error body without testing both. These are researched requirements, not an implemented controller or a completed security audit.

Production preparation also needs separate runtime/migration privileges, target verification, backup/restore evidence and a forward correction plan. Do not point a new migration guide at an existing shared database until its schema and history have been inspected. These are delivery requirements, not operations performed by this research.

### 8.5 Short source excerpts for the first guides

These are evidence excerpts, not complete CEREMO implementations. Links are pinned to the inspected release where available. Adaptations must be tested in this repository before being treated as working code.

[Boot 4.1.1 FlywayMigrationInitializer.afterPropertiesSet](https://github.com/spring-projects/spring-boot/blob/v4.1.1/module/spring-boot-flyway/src/main/java/org/springframework/boot/flyway/autoconfigure/FlywayMigrationInitializer.java) contains:

```java
this.flyway.migrate();
```

This supports using Boot's initializer instead of duplicating it. It does not prove a CEREMO migration is valid.

[Spring Data JPA 4.1.1 SimpleJpaRepository.save](https://github.com/spring-projects/spring-data-jpa/blob/4.1.1/spring-data-jpa/src/main/java/org/springframework/data/jpa/repository/support/SimpleJpaRepository.java) contains:

```java
if (entityInformation.isNew(entity)) {
    entityManager.persist(entity);
    return entity;
} else {
    return entityManager.merge(entity);
}
```

This explains why newness and version state matter. It does not provide an account-linking policy or an outer transaction boundary.

[Keycloak 26.4.2 FederatedIdentityEntity](https://github.com/keycloak/keycloak/blob/26.4.2/model/jpa/src/main/java/org/keycloak/models/jpa/entities/FederatedIdentityEntity.java) contains:

```java
@Column(name = "FEDERATED_USER_ID")
protected String userId;
```

Its local-user relationship is a separate field. CEREMO's issuer/subject names, constraints and linking rules are project-specific adaptations.

[Testcontainers 2.0.5 PostgreSQLContainerTest.testSimple](https://github.com/testcontainers/testcontainers-java/blob/2.0.5/modules/postgresql/src/test/java/org/testcontainers/postgresql/PostgreSQLContainerTest.java) contains:

```java
postgres.start();
ResultSet resultSet = performQuery(postgres, "SELECT 1");
```

This demonstrates an actual PostgreSQL query; CEREMO must additionally run migrations and assert constraints, rollback and concurrency.

[JaCoCo 0.8.15 ICoverageNode](https://github.com/jacoco/jacoco/blob/v0.8.15/org.jacoco.core/src/org/jacoco/core/analysis/ICoverageNode.java) exposes:

```java
ICounter getBranchCounter();
ICounter getLineCounter();
```

The EPL-2.0 API separates branch and line counters. Their build thresholds and Sonar import still require configuration and verification; this excerpt does not configure either tool.

[SonarSource's Gradle coverage example](https://github.com/SonarSource/sonar-scanning-examples/blob/master/sonar-scanner-gradle/gradle-multimodule-coverage/README.md) enables report output with:

```groovy
reports.xml.required = true
```

This is Groovy DSL evidence for XML generation, not CEREMO's Kotlin DSL configuration. Our guide must wire unit/integration reports explicitly and use the correct report path. [SonarQube's CI documentation](https://docs.sonarsource.com/sonarqube-community-build/analyzing-source-code/ci-integration/overview) supports waiting for the quality-gate result with:

```properties
sonar.qualitygate.wait=true
```

The property does not configure coverage or make an uploaded analysis pass. No Sonar token belongs in these files or example commands.

### 8.6 Registration verification and the 100% coverage rule

On 21 October 2026, authorised implementation added transactional account composition, required current-terms binding and a shared disposable PostgreSQL test configuration. The complete registration uses one transaction and the plain domain/application core remains independent of Spring/JPA. No migration or HTTP endpoint was added. All **397 regular and 32 PostgreSQL cases** passed with zero failures/errors/skips, including the race where one of two simultaneous registrations succeeds and the loser leaves no orphan account. The regular smoke test now uses H2 and synthetic OIDC setup rather than shared development services.

JaCoCo 0.8.15 combines both suites. Historical 21 October baseline: **86.74% line coverage (1498 covered / 229 missed)** and **70.22% branch coverage (250 covered / 106 missed)**. These passed the initial 80%/70% check, which the user replaced with 100% for both counters per class. That incomplete baseline was superseded by [22 October's completion](#87-full-coverage-completion). Report HTML: `build/reports/jacoco/combined/html/index.html`; XML: `build/reports/jacoco/combined/combined.xml`.

At the user's explicit permission, Salesync's `build.gradle.kts`, lines 39-59, was inspected read-only for this rule. It uses `CLASS`, `MISSEDCOUNT` and zero maximum for `LINE`/`BRANCH`. CEREMO adapts that configuration to its existing combined report and `check` dependency; no Salesync application code, exclusions or task filters were copied. This permission does not authorise future unrelated Salesync research. [Gradle's verification guidance](https://docs.gradle.org/current/userguide/jacoco_plugin.html#sec:jacoco_report_violation_rules) supports counter limits; [JaCoCo counter definitions](https://www.jacoco.org/jacoco/trunk/doc/counters.html) distinguish executed lines from covered decisions.

Source evidence for transaction composition remains [Spring 7.0.9 TransactionTemplate.execute](https://github.com/spring-projects/spring-framework/blob/v7.0.9/spring-tx/src/main/java/org/springframework/transaction/support/TransactionTemplate.java), [PostgreSQL uniqueness checks](https://www.postgresql.org/docs/18/index-unique-checks.html), [Boot constructor binding](https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.typesafe-configuration-properties.constructor-binding) and [ArchUnit dependency rules](https://www.archunit.org/userguide/html/000_Index.html#_package_dependency_checks). Their mechanisms are adapted to CEREMO's existing contracts. The detailed verification outcome is in [quality memory](../.github/research/quality/tests-and-analysis.md).

SonarScanner 7.5.0.8588 is configured, includes both suites, depends on `check` and waits for the gate. The status endpoint was unavailable and SONAR_TOKEN was absent, so server analysis/gate and Java-25 analyser support remain unverified. Principal-based registration checks do not prove a live Keycloak/JWKS exchange. Production actor auditing, broader onboarding and the HTTP boundary remain pending.

### 8.7 Full coverage completion

On **22 October 2026**, the full combined run passed **457 regular tests and 33 PostgreSQL integration tests**, with zero failures/errors/skips. JaCoCo reports **1736/1736 lines, 367/367 branches and 7116/7116 instructions covered (100% each)**. The 100% per-class limits, source scope and automatic generated-code filtering are unchanged. No new exclusions, coverage annotations, disabled tests or dependencies were used to reach the result.

Tests now cover validation boundaries and error parameters, date ordering, optional collections/arrays, startup rendering and DNS failure, log encoder configuration/output/failure, OIDC/user-info authority handling, exact account equality, request binding, missing audience, optional filter registration and tenant-filter restoration after failure. A genuine PostgreSQL CHECK violation verifies unrelated integrity failures propagate and roll back. Its temporary constraint is part of the failed disposable transaction; no application migration changed.

One production improvement accompanies the tests: the error handler maps the closed `ErrorStatus` enum exhaustively to HTTP codes, preserving the null-to-500 safeguard. All 13 supported codes are tested. Future enum additions must update the mapping at compile time. This removes an unreachable string-conversion fallback rather than simulating an impossible enum value in a test.

Reproduce with `.\gradlew.bat check bootJar` using JDK 25 and Docker. The final command's outcome is recorded in [quality memory](../.github/research/quality/tests-and-analysis.md#full-coverage-completion-22-october-2026). HTML/XML paths remain as above. SonarQube server verification, live Keycloak/JWKS and real TCP delivery remain separate checks; 100% execution coverage is not proof of those integrations or of complete business correctness.

### 8.8 Account HTTP and email activation

On **4 November 2026**, `.\gradlew.bat check bootJar --console=plain` passed with **512 regular tests and 37 PostgreSQL integration tests**, zero failures, errors or skips. The combined report contains **1818/1818 lines and 381/381 branches covered (100% each)**, with zero missed lines/branches in every reported application class. Instructions are also 7469/7469. Required input checks and the existing per-class zero-missed enforcement passed. No coverage exclusions, annotations, thresholds or application dependencies were changed.

The completed feature exposes GET/POST `/api/v1/users/me` and POST `/api/v1/users/me/activate`. Registration stores ACTIVE only after trusted boolean Keycloak email verification; otherwise it stores PENDING. Explicit later activation uses refreshed authentication. GET does not activate; restricted accounts cannot self-reactivate. Request records reject extra fields, responses expose five declared account fields and successful responses use no-store. Existing shared/wire security, errors, UUIDs, auditing and transaction composition are reused.

HTTP tests run the actual security chain/application/transactions with H2 and a mocked JWT decoder. They establish transport and downstream caller-policy behaviour, not token signature validity. Disposable PostgreSQL checks establish commit/rollback, duplicate races, activation/retry and rejection of an activation that races a committed suspension. The standalone MVC-to-PostgreSQL test establishes authentication directly; it is not a second bearer-chain test. See [HTTP source evidence](../.github/research/controllers/http-validation.md#account-http-implementation-4-november-2026), [activation evidence](../.github/research/application/user-registration.md#verified-email-activation-4-november-2026) and the [full result record](../.github/research/quality/tests-and-analysis.md#account-feature-verification-4-november-2026).

The local [GitHub Actions template](../.github/workflows/backend.yml) runs the same build on pushes and pull requests, with Java 25, pinned official actions and read-only repository permissions. It assumes this project becomes the repository root; the current checkout has no project-local `.git`. The template has not run remotely. Configure repository hosting and required status checks before relying on it. Sonar analysis is a separate opt-in manual job requiring the approved `SONAR_HOST_URL` variable and `SONAR_TOKEN` secret; it reruns the checks and waits for the configured server gate. No secret value belongs in the workflow or command arguments. No remote workflow, upload or server gate pass is claimed.

The original development realm had verification and brute-force controls disabled. The updated import enables verification, brute-force protection, refresh rotation and login/admin events, and disables the unused admin service-account client. Existing volumes are unchanged. Configure and test SMTP, verification/refresh and staff MFA claims before public use. Source removal of the unused admin credential does not rotate deployed copies. Request/resource limits, contact/recovery policies, general actor auditing and future feature permissions remain release work.

### 8.9 Account, logout and provider-tenant security

5 November 2026: `check bootJar` passed with 525 unit and 48 PostgreSQL cases, zero failures/errors/skips, 2141/2141 lines and 443/443 branches covered, and no class missing either counter. Sonar analysis was not run because SONAR_TOKEN was unset. Live identity, including public Keycloak signup/password-reset (currently disabled in the import), SMTP/MFA and operational/request limits remain release prerequisites. Grammarly remains pending.

The [implementation record](../.github/research/ports-and-adapters/provider-account-security.md) records the approved tenant model and exact Spring/Keycloak/PostgreSQL references. Accounts remain independent of business membership. Tenant commands check local status and ownership; invitees must accept. Administrative suspension/reactivation requires an active ROLE_ADMIN account with trusted recent MFA. Business suspension remains separate from subscription, listing assessment and email verification.

The new security suite uses disposable PostgreSQL 18.0 and a loopback OIDC discovery/JWKS fixture. RSA-signed JWTs pass through the production decoder, with no mocked decoder. Cases cover malformed/foreign claims, tenant isolation, invitation consent, owner retention, session rejection after logout, browser CSRF/OIDC logout, mutation/audit rollback and concurrent membership revocation. This does not prove live Keycloak email delivery or MFA.

Two additive migrations preserve the initial account migration. Review import settings separately from live realm changes; do not delete volumes to apply them. Full-build results and per-class coverage are maintained in the [quality register](../.github/research/quality/tests-and-analysis.md). Passing Gradle checks do not establish a Sonar server result.

### 8.10 Account module consolidation

5 November 2026: `.\gradlew.bat clean check bootJar --console=plain` passed after consolidating usermanagement into account. Fresh results: 525 regular tests and 48 PostgreSQL tests, zero failures/errors/skips; 2139/2139 lines and 443/443 branches covered, with zero missed counters per application class. Coverage rules remain unchanged. The executable JAR contains the unified account implementation and no legacy usermanagement classes.

The consolidation preserves API routes, database schema, account restrictions, tenant permissions and logout behaviour. One AccountConfiguration composes lifecycle and access services; the architecture test prevents restoring the removed feature package. See [ADR 11.9](03-technical-requirements.md#119-consolidate-the-account-module) and the [fresh quality record](../.github/research/quality/tests-and-analysis.md#account-consolidation-5-november-2026). Sonar analysis was not run because SONAR_TOKEN was unset. Live identity and operational release checks remain outstanding; Grammarly verification is pending.

### 8.11 Registered profile access and tenant privacy

The approved 5 November access policy keeps catalogue/summary discovery public and protects full provider profiles, detailed reviews and contacts behind authenticated ACTIVE local accounts. The route adapter reuses `Accounts.requireActiveAccount()` and Spring's authenticated manager. Tenant services retain their stronger accepted-membership, owner and staff checks; registering never grants private tenant access. [ADR 11.10](03-technical-requirements.md#1110-registered-profile-and-contact-access) records the decision.

Fresh `.\gradlew.bat check bootJar --console=plain` passed: **532 regular tests and 50 PostgreSQL tests**, zero failures/errors/skips; **2155/2155 lines and 449/449 branches**, with zero missed counters per application class under the unchanged strict rule. The executable JAR contains the guard and excludes the test fixture. The [exact quality record](../.github/research/quality/tests-and-analysis.md#registered-profile-access-5-november-2026) records report timestamp and limits. Sonar analysis was not run because SONAR_TOKEN was unset; Grammarly remains pending.

Verify anonymous rejection before account storage, signed-token/no-account rejection, pending/restricted accounts, successful ACTIVE access and unexpected failures without a grant. The PostgreSQL security suite exercises real RSA-signed tokens through production decoding and reads live account restrictions. It also checks that independent customers and an unrelated administrator cannot read another tenant's private records. Its provider-detail handlers are test fixtures; production marketplace handlers and response/media-leak checks remain future feature work. Preserve meaningful per-class 100% line/branch coverage. Record fresh results in the [quality register](../.github/research/quality/tests-and-analysis.md).

### 8.12 Dependency integrity and CSRF review

The reviewed [dependency baseline](../gradle/verification-metadata.xml) contains SHA-256 checksums for **748 artifacts across 404 components**, including plugin and dependency metadata. Metadata verification stays enabled. The [settings guard](../settings.gradle.kts) rejects weaker verification modes and a missing baseline during ordinary builds. The wrapper distribution checksum is pinned, and the existing wrapper JAR matched Gradle's published checksum. These controls follow [Gradle dependency verification](https://docs.gradle.org/current/userguide/dependency_verification.html) and [wrapper verification](https://docs.gradle.org/current/userguide/gradle_wrapper.html).

For an approved dependency change, generate candidate metadata, independently review it, inspect the diff and then run the normal strict build. Generation alone trusts the resolved artifacts; it is not verification. With PowerShell 7.4 or later:

```powershell
.\gradlew.bat --write-verification-metadata sha256 check bootJar sonarResolver --console=plain
.\scripts\verify-dependencies.ps1 -UpdateOrigins
.\gradlew.bat check bootJar sonar --console=plain
```

Supply the analysis token through the environment as described in section 4. Use no generation flags in normal CI. The helper compares repository-published SHA-256 values or hashes fresh HTTPS downloads; it writes origins only after every artifact matches and never replaces expected hashes. Plugin marker POMs are checked against actual Plugin Portal downloads because a checksum redirect can describe a different Central variant. Investigate mismatches; do not add broad trusted-artifact exceptions. This establishes a repository checksum baseline, not publisher-signature verification or a vulnerability scan.

The existing API/management security chain accepts explicit bearer headers and rejects browser-session or token-cookie authentication. Browser routes retain CSRF protection. Two new unsafe-write regressions demonstrate those boundaries; the source comment records why the API exception applies. Reassess it if token resolution, browser credentials or proxy behaviour changes. [Spring's CSRF guidance](https://docs.spring.io/spring-security/reference/features/exploits/csrf.html) explains why statelessness alone is insufficient.

Fresh suites passed **534 regular and 50 PostgreSQL tests**, with no failures/errors/skips. Combined coverage is **2155/2155 lines and 449/449 branches**, zero missed per application class. All 748 artifact checks matched: 543 published checksums and 205 fresh-download hashes. Isolated negative probes rejected a tampered artifact checksum, missing metadata and disabled verification. The subsequent normal `check bootJar sonar` passed, reusing the same fresh test/report outputs and executing analysis. Sonar removed the dependency hotspot; the CSRF hotspot was recorded as **REVIEWED / SAFE** with evidence. The [research record](../.github/research/quality/tests-and-analysis.md#hotspot-remediation-6-november-2026) records commands, report provenance and remaining limits.

## 9. Security scenarios and operational ownership

| Scenario | Required control and evidence |
| --- | --- |
| Caller guesses another provider/account ID | Scope reads/writes by account status and ownership/membership. Test list, child-record and evidence access, not only top-level routes. |
| Customer/client supplies privileged fields | Explicit transport mapping rejects arbitrary status, ownership, check or entitlement assignment. Domain invariants remain independent of transport validation. |
| Provider pays while suspended or expired | Payment cannot override suspension; eligibility uses authoritative provider/period data. Test stale client/cache behaviour. |
| Malicious/repeated processor event | Validate the signature, persist receipt, verify expected collection and grant once. Test restart and transaction rollback. |
| Token remains valid after a sensitive restriction | Apply the approved local account/revocation policy in protected use cases; demonstrate actual live identity behaviour. |
| Private upload or secret reaches public output | Restrict evidence, validate media and redact logs/errors/artifacts. Test public serializers and denied access. |
| Worker or external dependency fails | Preserve committed work, bound retries, deduplicate and expose backlog/failure with a responsible operator. |
| Restore reintroduces old data | Reapply required deletion/consent/revocation and financial reconciliation before live processing resumes. |

Use the persistent [security rules](../.github/security-quality-rules.md) and [OWASP ASVS](https://asvs.owasp.org/) for control selection, not as certification claims. Existing scanner configuration is not proof that dependency/container/secret scans run; explicitly identify installed checks and gaps during release review.

Each production dependency needs a named owner, alert condition, runbook, escalation and recovery evidence. Adopt a proportional readiness checklist informed by [AWS operational readiness reviews](https://docs.aws.amazon.com/wellarchitected/latest/operational-excellence-pillar/ops_ready_to_support_const_orr.html); this does not select AWS infrastructure or imply an external audit.

## 10. Verification records and outstanding work

| Record | Meaning |
| --- | --- |
| Hotspot remediation, 6 November | Latest checkpoint: 534 regular + 50 PostgreSQL tests, strict per-class coverage and artifact verification passed; section 8.12 records controls and limits. |
| Registered profile access, 5 November | Boundary checkpoint: 532 regular + 50 PostgreSQL tests and strict per-class coverage passed; section 8.11 records implementation and remaining provider-feature limits. |
| Account consolidation, 5 November | Fresh clean build, 525 regular + 48 PostgreSQL tests and strict per-class coverage passed; section 8.10 is the current source-layout checkpoint. |
| Account/logout/tenant implementation, 5 November | 525 unit + 48 PostgreSQL checks and strict per-class coverage passed; section 8.9 records scope and limits. |
| Account implementation, 4 November | Fresh full build passed after HTTP, activation and security/concurrency tests; section 8.8 records the command, scope and limitations. |
| 21–22 October test/coverage records | Evidence for the named earlier compiled revisions only; retain their commands/results and limitations in sections 8.6–8.7. |
| Sonar server analysis, 6 November | Latest gate OK evaluated new issues and duplication. Tests/coverage imported; hotspot review 100%, A. One bug, 71 code smells and scanner warnings remain. Required release-gate policy remains unverified. |
| Live identity and production operations | Still require issuer/JWKS, recovery, secrets, assurance and operational checks. |
| Future release | Must produce fresh same-revision tests, reports, server analysis and required operational acceptance evidence. |

Do not describe the product as launch-ready while required commercial, privacy, identity, billing or operational decisions remain open. The [PRD](02-product-requirements.md) owns product gates; [implementation tasks](07-implementation-plan.md) own delivery dependencies.

Editorial verification: version 3.8, 6 November 2026; Grammarly pending, no text submitted. Local writing/structure checks do not substitute for executable assurance.
