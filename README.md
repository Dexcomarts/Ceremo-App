# CEREMO Backend

CEREMO helps people find ceremony and event service providers in Ghana. Customers browse free and contact caterers, photographers, decorators, MCs and other providers directly. Providers register free, trial an approved listing and can pay a subscription for continued discovery. Conversations, meetings, service agreements and payments may happen outside CEREMO. The MVP earns no booking commission. Greater Accra is the first service area, covering all occasions and festivals.

The backend includes verified-email account registration, account access checks, provider-business tenants and invited teams, persistent session logout and 16 `wire` Java files. Public provider discovery, listing assessment and subscriptions remain planned. The [product requirements](docs/02-product-requirements.md) and [implementation tasks](docs/07-implementation-plan.md) describe the remaining work. The [frontend stack](docs/05-ui-ux-design-brief.md) is selected; the frontend has not been created.

## Technology

| Tool | Use |
| --- | --- |
| Java 25 | Application code |
| Spring Boot 4.1.1 | Backend framework |
| Gradle 9.8.1 | Build, using the checked-in wrapper |
| PostgreSQL | Application database; version 17 is proposed |
| Keycloak | Authentication in development and production |
| SonarQube | Code analysis |
| JaCoCo | Test coverage |
| Next.js (React) and TypeScript | Selected web frontend |
| Tailwind CSS and shadcn/ui | Styling and reusable UI components |
| TanStack Query | Frontend server-state fetching and caching |
| React Hook Form and Zod | Form state and validation |

Spring MVC, Security, OAuth2 client/resource server, Data JPA, Bean Validation, Springdoc, UUID generation, Ehcache/JCache, optional Logstash logging and the PostgreSQL driver are declared in the build. The account module includes the first migration, persistence adapter and transactional use-case wiring. JaCoCo 0.8.15 and SonarScanner for Gradle 7.5.0.8588 are configured. The latest 6 November analysis imported 584 passing tests and 100% line/branch coverage, with 100% hotspot review and rating A. Its gate returned OK for new issues and duplication; required release-policy verification remains open. One reliability bug and 71 code smells remain; see the [analysis record](.github/research/quality/tests-and-analysis.md#hotspot-remediation-6-november-2026). Shared packages include authentication, collections, base entities, enums, errors, events, UUID generation, memoization, money, multitenancy, pagination and user details.

## Run locally

Install JDK 25, start PostgreSQL with a dedicated local database/user, and start the configured development Keycloak realm. Check the issuer, OIDC client registration and API audience before starting the backend. Configure the database connection in PowerShell:

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5432/ceremo'
$env:SPRING_DATASOURCE_USERNAME = 'ceremo'
$env:SPRING_DATASOURCE_PASSWORD = '<your-local-database-password>'
$env:CEREMO_TERMS_VERSION = 'terms-v1'
.\gradlew.bat bootRun
```

Replace the password locally. `terms-v1` is a synthetic development example; deploy the actual approved terms version in production. Missing, blank or oversized terms configuration prevents startup. Keep credentials out of Git. Spring Boot does not load a `.env` file automatically.

The local services are defined in `src/docker` and included by the root `docker-compose.yml`. On a machine with Docker Compose, run `docker compose config --quiet`, then `docker compose up -d postgresql keycloak sonar`. Keycloak uses port 9080 and realm `ceremo`; SonarQube uses host port 9001. This stack is for development. See [local setup and existing-volume precautions](docs/08-development-and-quality.md#1-local-services).

Open the local [SonarQube dashboard](http://127.0.0.1:9001/dashboard?id=ceremo_backend) without signing in to view public project reports and source. It remains bound to localhost. Administration requires login, and Gradle analysis uses its token.

The token generator is the optional `sonar-token` service under the `tools` profile. Prepare its image/container with `docker compose --profile tools create --build sonar-token`; creation does not generate a token. The [SonarQube setup instructions](docs/08-development-and-quality.md#4-sonarqube) describe current-password authentication, the private token volume and the one-off generation job. A completed generator exits normally rather than remaining a running server.

These commands build and test the existing project:

```powershell
.\gradlew.bat compileJava
.\gradlew.bat test
.\gradlew.bat bootJar
```

The application context and wiring smoke tests use H2, synthetic OIDC registration and a mocked JWT decoder. The regular `test` task does not require live PostgreSQL or Keycloak. Docker is required for `.\gradlew.bat integrationTest`, which runs schema, adapter, transaction and concurrent-registration tests against disposable PostgreSQL. The security suite additionally uses real signed tokens and loopback discovery/JWKS through the production decoder. No suite establishes live Keycloak/SMTP/MFA interoperability. On Unix, use `./gradlew`.

Run `.\gradlew.bat check` for both suites, combined coverage reporting and the 100% line / 100% branch coverage rule for every application class. The combined HTML report is `build/reports/jacoco/combined/html/index.html`. For analysis, supply `SONAR_TOKEN` through your environment or secret manager and run `.\gradlew.bat sonar`; it depends on `check` and waits for the server's quality gate. `SONAR_HOST_URL` defaults to `http://localhost:9001`. Do not put tokens in command arguments or tracked files.

On 6 November 2026, fresh suites passed **534 regular tests and 50 PostgreSQL tests**, zero failures/errors/skips, with **100% line and branch coverage per application class**. The subsequent strict `check bootJar sonar` passed using those same test/report outputs. New security regressions reject session/cookie-only API writes and accept explicit bearer headers; browser CSRF remains enabled. The security suite also uses real signed JWTs and loopback discovery/JWKS; live Keycloak remains unverified. See [the current record](docs/08-development-and-quality.md#812-dependency-integrity-and-csrf-review). The local GitHub Actions template remains unverified remotely.

Builds require the reviewed [dependency checksums](gradle/verification-metadata.xml); settings reject weaker verification modes and missing metadata. All 748 baseline artifacts were checked against published SHA-256 values or fresh HTTPS downloads. Use [the maintenance procedure](docs/08-development-and-quality.md#812-dependency-integrity-and-csrf-review) and `scripts/verify-dependencies.ps1` when an approved dependency changes. Generated metadata alone is not a trusted review, and checksums do not establish publisher identity or absence of vulnerabilities.

The implemented account API requires bearer authentication:

| Operation | Result |
| --- | --- |
| GET `/api/v1/users/me` | Own account or 404, without creating or activating an account. |
| POST `/api/v1/users/me` | Register with displayName, termsVersion and true termsAccepted; 201 with Location. |
| POST `/api/v1/users/me/activate` | Activate PENDING after trusted verified-email authentication; ACTIVE retries do not write again. |
| GET `/api/v1/account` | Own saved profile, using the same safe response. |
| POST / GET `/api/v1/tenants` | Create a provider business / list own businesses. Active local account required. |
| GET `/api/v1/tenants/{tenantId}` | Private workspace summary for its owner or accepted member. |
| GET / POST `/api/v1/tenants/{tenantId}/members` | Owner lists the team / invites a registered account as MANAGER or FINANCE. |
| POST `/api/v1/tenants/{tenantId}/membership/accept` | Current invitee accepts membership. |
| DELETE `/api/v1/tenants/{tenantId}/members/{userId}` | Owner revokes an invitation or member; cannot remove the owner. |
| POST `/api/v1/admin/tenants/{tenantId}/suspend` or `/reactivate` | Active admin with recent trusted MFA; requires a reason. |
| POST `/api/v1/logout` | Revoke current session; further bearer tokens with that sid are rejected. |

Registration creates ACTIVE only when validated Keycloak authentication contains boolean `email_verified=true`; otherwise it creates PENDING. SUSPENDED/ANONYMISED cannot self-reactivate. No account operation grants provider membership, staff access or subscription entitlement. The local realm still needs a real SMTP/verification/refresh test; synthetic verified fixtures do not prove email ownership. See [the activation decision](docs/03-technical-requirements.md#117-account-activation-from-verified-keycloak-email).

Public discovery, enquiries and subscriptions remain unimplemented. API tokens must carry validated Keycloak user claims including payload `typ=Bearer`, `sub`, `sid`, `iat` and `exp`, with the configured issuer/audience. Private tenant access also checks current account status and database membership. Creating a workspace grants no trial, publication approval, verification badge or paid entitlement. Invitations require recipient acceptance; no public customer directory or invitation email delivery is provided yet.

The approved discovery boundary permits anonymous catalogue/search summaries only. Provider detail paths and subroutes require an authenticated ACTIVE local account; Keycloak login alone is insufficient. Full profiles, detailed reviews and contacts follow that gate when implemented. Private tenant records additionally require accepted membership and operation-specific permission. See the [access matrix and implementation limits](docs/03-technical-requirements.md#4-authentication-and-permissions).

API logout returns 204 after persistent revocation; the client must also clear credentials and complete standard Keycloak logout to end SSO. Browser POST `/logout` requires CSRF, revokes its session and redirects through Spring's registered-provider OIDC handler. No request Origin supplies a return URL. Tenant administration requires ROLE_ADMIN plus trusted `amr` containing `mfa` and Long epoch-second `auth_time` within five minutes. Configure the actual authentication flow before using staff endpoints; a user-editable attribute is insufficient. See [ADR 11.8](docs/03-technical-requirements.md#118-provider-business-tenants-account-access-and-logout).

The development realm import now enables email verification, brute-force protection, refresh rotation and event recording, and disables the unused admin service-account client. Existing realm volumes keep their current settings; they were not reset. Configure/test SMTP and MFA, and rotate any previously deployed unused admin credential through the responsible operator. Public deployment still requires request/resource limits and the release checks in document 08. Cross-origin API access requires explicit `application.cors.allowed-origins`; the legacy allowed-origins setting remains a fallback. Default origins are empty.

Use the configured Java 25 toolchain; Java 21 alone cannot compile this project, and automatic JDK download is not configured.

## Project structure

```text
src/main/java/io/ceremo/ceremobackend/
  CeremoBackendApplication.java
  shared/
    authentication/ collection/ domain/ enumeration/ error/ event/
    generation/ memoizer/ money/ multitenancy/ pagination/ useridentity/
  account/ tenant/ logout/
    domain/ application/ infrastructure/  (create layers only where used)
  wire/
    account/ async/ cache/ database/ logstash/ logout/ security/ springdoc/ tenant/
src/main/resources/
  application.properties
  config/application.yml
  config/application-dev.yml
  config/application-prod.yml
src/test/java/io/ceremo/ceremobackend/
  CeremoBackendApplicationTests.java
  shared/
  wire/
  account/
src/integrationTest/java/io/ceremo/ceremobackend/
  account/
  security/
docs/
```

The YAML files contain shared, development and production configuration. Override local credentials through environment variables; keep environment-specific credentials/issuers isolated.

The shared package contains 88 Java files covering validation, identity, errors, IDs, pagination and infrastructure helpers. Tenant business access uses explicit database membership; keep the inherited organisation-claim filters/aspects disabled. Their tier/location fields do not define permissions or CEREMO plans. See the [shared package notes](docs/03-technical-requirements.md#21-shared-packages).

`wire` enables async/scheduled execution, JPA auditing and repository scanning, Ehcache configuration, CORS and CEREMO OpenAPI metadata. Async execution uses `spring.task.execution.*`; Logstash stays off unless `application.logging.logstash.tcp.enabled=true`. API documentation uses authorization code with PKCE. Register the Swagger redirect URI for a public API-testing client and configure its audience; never put an OIDC client secret in Swagger UI settings. See [wiring settings](docs/03-technical-requirements.md#22-application-wiring).

New features follow CEREMO's DDD and ports-and-adapters rules: `domain`, `application`, and `infrastructure` inside each business package. For example, `providermanagement` contains `Provider`, `ProviderService` and `ProviderResource`. Consult Salesync only when explicitly requested for the current task. See the [technical requirements](docs/03-technical-requirements.md) for the full layout and API list.

## Documentation

The shared product baseline contains nine numbered documents. The separately authorised local founder guide/tasklist are excluded from Git and from the index below. Start with 02 and 01 for product/commercial review, 03 and 06 for architecture/data review, 04/05/09 for experience and functional scope, and 07/08 for delivery and assurance. Version 3.4 updates record registered profile/contact access, retained private-tenant permissions and fresh verification on 5 November 2026; market traction and launch approval remain unestablished.

| Document | Contents |
| --- | --- |
| [01. Ghana market and customer research](docs/01-ghana-research.md) | Ceremony culture, competitors, subscriptions, onboarding, retention and field research |
| [02. Product requirements](docs/02-product-requirements.md) | Customer problem, strategic choices, release scope, measures and decision gates |
| [03. Technical requirements](docs/03-technical-requirements.md) | Architecture decisions/tradeoffs, API, security, billing, quality scenarios and risks |
| [04. User journeys](docs/04-app-flow-and-user-journeys.md) | Customer/provider steps, transitions, failure recovery and client handoff |
| [05. Frontend and UI/UX brief](docs/05-ui-ux-design-brief.md) | Selected stack, experience priorities, components, acceptance and open design/session decisions |
| [06. Database design](docs/06-database-architecture.md) | Data ownership, relationship tables, schema constraints and migration/recovery acceptance |
| [07. Implementation tasks](docs/07-implementation-plan.md) | Milestone outcomes, current status, dependencies, policy blockers and release checks |
| [08. Development and quality](docs/08-development-and-quality.md) | Local setup, security scenarios, testing, SonarQube/JaCoCo and dated assurance evidence |
| [09. Functional requirements](docs/09-functional-requirements.md) | Complete business functions, module acceptance and customer/provider release cycles |

Research belongs in document 01 and architecture decisions in document 03. Contributors follow the writing and naming rules in [AGENTS.md](AGENTS.md#writing-and-naming). VS Code Copilot instructions are in [.github/copilot-instructions.md](.github/copilot-instructions.md).

Documentation verification: Grammarly remains pending for the revised set. Sources, preservation checks and limitations are in the [suite review record](.github/research/product/documentation-suite.md). Workflows and data relationships are expressed in text/tables; no diagram renderer is required.


