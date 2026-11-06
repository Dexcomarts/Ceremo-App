# Tests, coverage and static analysis

- Entry ID: quality/tests-and-analysis
- Researched: 20 October 2026; latest review 6 November 2026
- Status: verified hotspot remediation on 6 November; 534 regular + 50 PostgreSQL tests, strict per-class coverage and reviewed dependency baseline pass. Sonar hotspot review 100% / A; gate OK evaluated new issues/duplication. Existing findings, release-policy verification and Grammarly remain outstanding.
- Question: how to retain meaningful evidence from tests and integrate JaCoCo/SonarQube honestly

## Evidence

[Gradle Java testing](https://docs.gradle.org/current/userguide/java_testing.html) supports separate source sets/test tasks. [Gradle JaCoCo](https://docs.gradle.org/current/userguide/jacoco_plugin.html) requires explicit report/test and coverage-verification wiring. [JaCoCo release history](https://www.jacoco.org/jacoco/trunk/doc/changes.html) supports selecting a released Java-25-compatible version. [Sonar Gradle scanner](https://docs.sonarsource.com/sonarqube-community-build/analyzing-source-code/scanners/sonarscanner-for-gradle) and [quality gates](https://docs.sonarsource.com/sonarqube-community-build/analyzing-source-code/ci-integration/overview) distinguish analysis upload from a passing gate.

## CEREMO decisions and observed state

Existing unit, shared security and H2 wiring tests are executable. A separate `integrationTest` source set/task runs real PostgreSQL checks and is a dependency of `check`. The application smoke test is now isolated with H2 and synthetic identity configuration; its full-suite result is recorded in the latest checkpoint.

JaCoCo 0.8.15 and scanner 7.5.0.8588 are configured, and the user requires 100% line/branch coverage per class. The [6 November remediation](#hotspot-remediation-6-november-2026) meets that target with fresh suites, a reviewed dependency baseline and actual Sonar analysis. It supersedes the initial analysis's pending-hotspot status. Earlier dated checkpoints retain their original scope.

## Full coverage completion, 22 October 2026

Question: cover the remaining application behaviour without changing thresholds, excluding classes or disabling checks. The user explicitly authorised completion of the full JaCoCo coverage. Scope remains the existing Java 25 / Boot 4.1.1 / Spring Security 7.1.1 / JaCoCo 0.8.15 application and combined regular/PostgreSQL suites. No Salesync files were consulted for this implementation; the previous permission was limited to its coverage rule.

Research/reuse: read AGENTS, both reuse maps, security rules, actual missed source lines and existing tests. Applied the shared/wire reuse, Spring Boot testing and security skills. Original CEREMO tests use existing assertions, exceptions, UUID helper, paging DTOs, tenant context, trusted identity adapters and production database wiring. No parallel utilities, dependencies or test framework were added.

Evidence reviewed:

| Primary source | What applies and what does not |
| --- | --- |
| [JaCoCo counters](https://www.jacoco.org/jacoco/trunk/doc/counters.html) | Read actual line/branch/instruction counts and inspect partial branches; percentages are not proof of secure or complete business behaviour. |
| [Spring Boot testing](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html) | Existing context runners/slices and isolated configuration tests; no live identity/database fallback. |
| [Spring Security authority mapping](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/advanced.html#oauth2login-advanced-map-authorities-grantedauthoritiesmapper) | Its OIDC authority pattern distinguishes ID-token/user-info claims. Test exact mapped authorities, ignored unrelated authorities and deduplication. Reference manual is 7.1.1. |
| [Logstash encoder 9.0 TCP appender](https://github.com/logfellow/logstash-logback-encoder/blob/logstash-logback-encoder-9.0/src/main/java/net/logstash/logback/appender/AbstractLogstashTcpSocketAppender.java) and [async lifecycle](https://github.com/logfellow/logstash-logback-encoder/blob/logstash-logback-encoder-9.0/src/main/java/net/logstash/logback/appender/AsyncDisruptorAppender.java) | Apache-2.0 source confirms destination/encoder/ring-buffer/shutdown APIs. Build the actual configured appender and encode a synthetic event without starting TCP. Short excerpt: `return Collections.unmodifiableList(destinations);`. No complete source class was copied. |
| [Java 25 switch expressions](https://docs.oracle.com/javase/specs/jls/se25/html/jls-15.html#jls-15.28) | Exhaustive ErrorStatus mapping replaces an unreachable string-conversion fallback; adding an enum value now requires updating the mapper. A null status still returns 500. |

The attempted pinned Spring OIDC authority test and Mockito static-mock Javadoc URLs were unavailable. No implementation claim depends on their contents. The successfully retrieved Spring manual, matching Logstash source, existing tests and actual compile/test results are the evidence. No new Medium/LinkedIn/Slack or company-practice claim is made.

Added/extended behavioural checks cover numeric inclusive/strict boundaries and precision, optional values, email shape, array/collection limits and null elements, validation response parameters, date ordering, startup URL/profile/config-server rendering and hostname failure, JSON log encoding/options/serialization failure, all 13 error status mappings plus missing status, exception parameter copying, exact account equality, request paging binding, missing audience, matching user-info names/roles/groups and rejected subjects, OIDC authorities, optional filter registration and tenant-filter restoration after failure. A new PostgreSQL CHECK failure proves unrelated integrity errors propagate and roll back instead of becoming duplicate-identity conflicts; its DDL is inside the failed disposable transaction and is rolled back.

Only production change: `CeremoErrorsHandler.mapStatus` now uses an exhaustive enum switch with the same public status codes and null fallback. No generator annotation, exclusion, threshold, dependency, security policy or migration was changed. Test sources use explicit constructors/imports and synthetic fixtures; no Lombok or assertion-free test was added.

Executed `.\gradlew.bat test integrationTest jacocoCombinedReport --console=plain`: **457 regular + 33 PostgreSQL cases**, zero failures/errors/skips. Combined XML: **1736/1736 lines, 367/367 branches, 7116/7116 instructions**, all 100%, with no source file reporting a missed line/branch/instruction. The PostgreSQL total is 14 schema + nine adapter + ten registration cases. An initial authority-assertion generic capture compilation error was corrected by comparing mapped authority names; the subsequent full run passed. Final `check bootJar` runs against the completed source/formatting and records its outcome below.

Final verification on 22 October 2026: `.\gradlew.bat check bootJar --console=plain` completed **BUILD SUCCESSFUL** with JDK 25. The final confirmation reused up-to-date test, report, per-class verification and JAR outputs; `verifyCoverageInputs` executed successfully. The preceding full run supplies the 490 passing test cases and coverage counters above. All 13 reviewed documentation/context files had valid relative file links; `docs/` still contains exactly eight files, shared contains 86 main Java files and wire contains eight test classes.

Limits unchanged: no live Keycloak/JWKS flow, real TCP log delivery, production actor auditor or Sonar server gate is established by these tests. The existing automatic generated-code filters remain unchanged; no handwritten code was newly marked or excluded. These results supersede the earlier incomplete coverage checkpoint, not the recorded historical commands and failures.

Generate reports against the same compiled revision and fail required checks on missing/stale inputs. Keep secrets in injected environment variables; no token values in commands or memory. Do not exclude handwritten business/security code to improve coverage. Use negative, rollback and concurrency tests rather than getter/reflection tests to increase a percentage.

## Verification and next work

Observed build compatibility: Gradle 9.8.1 warns on delegated `tasks.registering(Test::class)`. Use `val integrationTest = tasks.register<Test>("integrationTest") { ... }`, matching the [current additional-test-task example](https://docs.gradle.org/current/userguide/java_testing.html). This correction has been applied. Enumerate report XML files and read their suite names; this Gradle installation can shorten long report filenames rather than using the full Java class name as a filename.

Verified on 20 October 2026 with JDK 25:

```powershell
.\gradlew.bat integrationTest --tests '*UserSchemaIntegrationTest' test --tests 'io.ceremo.ceremobackend.usermanagement.*' --tests 'io.ceremo.ceremobackend.shared.*' --tests 'io.ceremo.ceremobackend.wire.*' bootJar --console=plain
```

Result: 14 PostgreSQL integration cases and 388 selected foundation regression cases passed, zero failures/errors/skips. The latter includes 40 User Management cases. The executable JAR contains the migration, and Testcontainers stopped its disposable containers. This was not an unfiltered `check` run. Existing Ehcache Unsafe warnings remain unrelated to this change. Combined coverage, XML report import and a SonarQube quality-gate outcome are unverified until configured and observed. Detailed acceptance conditions: [document 08](../../../docs/08-development-and-quality.md#3-tests-and-jacoco).

Related: [database](../database/user-schema.md), [security rules](../../security-quality-rules.md).

## Latest complete test and coverage run

21 October 2026, authorised implementation: `.\gradlew.bat test integrationTest jacocoCombinedReport bootJar --console=plain` passed with JDK 25 and Docker. Fresh JUnit XML totals: 397 regular tests and 32 PostgreSQL tests, zero failures/errors/skips. Includes five new wiring cases, one architecture case and ten registration integration cases. The original context smoke test is now hermetic H2 with a shared synthetic OIDC fixture and mocked decoder, so the unfiltered regular suite no longer requires the configured local database/issuer.

Combined XML/HTML generated from both nonempty execution files: 1498 covered / 229 missed lines (86.74%); 250 covered / 106 missed branches (70.22%). No handwritten class/package exclusions were added. Both test source roots/binary/result directories and the combined XML are explicitly configured for Sonar. `sonar` depends on `check` and waits for the quality gate, reading token authentication through SONAR_TOKEN rather than command arguments. Server status was unavailable and no token was configured, so analysis/Java-25 server compatibility and the gate remain unverified. Detailed source/version/reuse evidence is in [the application record](../application/user-registration.md#applied-checkpoint-and-verification).

## Mandatory 100% coverage rule

User instruction on 21 October 2026 supersedes the initial 80% line / 70% branch proposal and its successful `check bootJar` run. Require zero missed lines and branches for **every application class**. Read-only Salesync research was explicitly authorised for this task: its `build.gradle.kts` lines 39-59 use `element = "CLASS"`, `counter = "LINE"` / `"BRANCH"`, `value = "MISSEDCOUNT"`, and `maximum = "0.00".toBigDecimal()`. CEREMO adapts those limits with `BigDecimal.ZERO` on its combined verification task. No Salesync test filters, exclusions, application code or credentials were copied; the authorisation is not continuing permission for other tasks.

Official evidence: [Gradle coverage verification](https://docs.gradle.org/current/userguide/jacoco_plugin.html#sec:jacoco_report_violation_rules) supports per-element/counter limits; [JaCoCo counters](https://www.jacoco.org/jacoco/trunk/doc/counters.html) defines line/branch coverage and notes that exceptions are not branch-counter decisions. `verifyCoverageInputs` is an ordinary task before the report, so missing/empty execution files fail even if a JaCoCo report task would otherwise skip itself for missing data. `check` and `sonar` depend on verification. No new coverage exclusions, annotation changes, lowered thresholds or assertion-free tests were added.

The current baseline still has **229 missed lines and 106 missed branches**. The largest gaps are startup traces (53 lines plus nine builder lines), Logstash lifecycle (21 lines), numeric/array/string assertion edge cases and security configuration/claim conversion. The aggregate percentages must not be described as satisfying the 100% rule. Closing these gaps requires meaningful behavioural tests; this rule update does not claim they were written or that coverage reached 100%.

Executed `.\gradlew.bat jacocoCombinedCoverageVerification --console=plain` after applying the 100% CLASS limits: failed as required, reporting individual classes with missed line/branch counts above zero. Both test suites were UP-TO-DATE from the passing full run; this is a coverage failure, not a failing application test. The earlier `check bootJar` success used the superseded 80%/70% policy and must not be cited as a current quality pass. The ordinary input guard was then added before report generation to avoid skip-on-missing-data behaviour; final configuration validation is recorded with the completed task output.

Final rerun of the same command with the input guard installed: `test` and `integrationTest` UP-TO-DATE, `verifyCoverageInputs` passed, combined report generated, and `jacocoCombinedCoverageVerification` failed on the existing missed counts (exit 1). This verifies the final task graph and strict rule. The all-inputs-missing failure path was reviewed in the ordinary guard but not fault-injected. All 14 reviewed documentation/context files had valid local links, the wire inventory is 14 main files, the old adapter test copy is absent, and `docs/` remains exactly eight files. No further application code changed after the passing full run.

## Why the PostgreSQL test has a separate source root

Reviewed: 21 October 2026. Question: is `src/integrationTest/java` supported by maintainer guidance and existing Spring Boot repositories?

Evidence:

- [Gradle's integration-test configuration](https://docs.gradle.org/current/userguide/java_testing.html#sec:configuring_java_integration_tests) describes a separate source set, its dependencies/classpaths, a `Test` task and a `check` dependency. Its example names the source set `intTest`; CEREMO uses `integrationTest`. Short source excerpt: `tasks.check { dependsOn(integrationTest) }`. This is build configuration precedent, not a requirement imposed by Spring or DDD. The live manual was accessible; the attempted version-specific 9.8.1 URL was not retrievable through the browsing tool. CEREMO's actual wrapper is 9.8.1 and its configured task was verified on 20 October.
- The [Ministry of Justice Spring Boot microservice template README](https://github.com/ministryofjustice/laa-spring-boot-microservice-template#3-update-service-subproject) explicitly lists `src/integrationTest/java`, `src/main/java` and `src/test/java` in its service subproject, and documents `./gradlew integrationTest`. This establishes a public Spring Boot template precedent. It does not establish production deployment, exact CEREMO dependency compatibility or endorsement of our schema. The service tree/raw build could not be retrieved during this check; the README is the evidence for this claim.
- Spring's [Petclinic PostgresIntegrationTests.java](https://github.com/spring-projects/spring-petclinic/blob/main/src/test/java/org/springframework/samples/petclinic/PostgresIntegrationTests.java) lives under `src/test/java` and uses PostgreSQL Testcontainers with Spring Boot tests. Its Apache-2.0 header and source were inspected. This demonstrates that a separate integration source root is optional. No application code or dependency versions were copied. Both repository links track `main`, as observed on the review date, rather than a pinned release.

CEREMO decision: retain the separate source root for `UserSchemaIntegrationTest` because it starts real PostgreSQL through Docker and applies Flyway migrations. The existing package `usermanagement.infrastructure.secondary.persistence` still expresses its architectural responsibility; the source root expresses how the test is built and executed. `test` does not include this source set; `integrationTest` runs it explicitly; `check` depends on both regular and integration verification. Existing tests under `src/test` include H2/Spring wiring checks, so that directory must not be described as containing only pure unit tests or as entirely free of external-service requirements.

Trade-off: the extra source set needs maintained IDE/build configuration and explicit inclusion in future JaCoCo coverage and SonarQube test/report inputs. Moving the file into `src/test` would also be valid, but separate execution would then need tags, naming filters or another deliberate configuration. The folder alone does not protect a database: this test's direct container datasource, disposable lifecycle and transaction rollback provide the isolation.

Verification for this review: read the current Gradle source-set/task configuration and test setup; no code changed and no tests rerun. The earlier 14-case pass remains dated 20 October. This is focused evidence about test organisation, not a new security audit or a claim of universal senior-engineer approval.

## Account feature verification, 4 November 2026

- Entry: quality/tests-and-analysis; implemented and verified 4 November 2026.
- Question: retain the strict quality rule while completing account HTTP and trusted-email activation.
- Scope: Java 25, Boot 4.1.1, Gradle 9.8.1, PostgreSQL 18, JaCoCo 0.8.15. The built bootJar contains Spring MVC 7.0.9, Spring Security core 7.1.1 and Jackson annotations 2.21; live documentation may be newer.

### Fresh execution evidence

Final command: `.\gradlew.bat check bootJar --console=plain`, exit 0, BUILD SUCCESSFUL in 3m 35s. Both regular and integration tasks executed against the same compiled application; verifyCoverageInputs, jacocoCombinedReport, jacocoCombinedCoverageVerification, check and bootJar passed. Later edits changed only documentation/context.

| Report | Actual result |
| --- | --- |
| build/test-results/test/TEST-*.xml | 512 tests across 81 suites; failures 0, errors 0, skipped 0 |
| build/test-results/integrationTest/TEST-*.xml | 37 tests across three suites; failures 0, errors 0, skipped 0 |
| Combined JaCoCo LINE | Covered 1818, missed 0 |
| Combined JaCoCo BRANCH | Covered 381, missed 0 |
| Combined JaCoCo INSTRUCTION | Covered 7469, missed 0 |
| Per-class inspection | No LINE/BRANCH missed counter above zero in any of the 150 XML class nodes; aggregate executable-class counter 131 covered, 0 missed |
| Combined report timestamp | 2026-11-04T16:19:37.5577276Z |

Report: `build/reports/jacoco/combined/combined.xml`; HTML: `build/reports/jacoco/combined/html/index.html`. XML was read with external DTD resolution disabled. Original coverage scope and per-CLASS zero-missed rules remain unchanged; no exclusions, generated annotations, lowered thresholds or assertion-free percentage tests were added. Exception failures are exercised even where JaCoCo does not count them as branches.

Tests cover strict boolean verification, missing/foreign authentication, exact caller identity, registration consent/size/unknown-field rejection, five-field response mapping, not-found/duplicate/forbidden responses, safe framework failures and committed responses. New PostgreSQL tests prove verified registration commit, activation commit/idempotent retry and that a concurrent versioned suspension defeats a stale activation. The suspension is injected directly in a disposable transaction to test concurrency; a production suspension endpoint is not implemented. Existing duplicate-registration and rollback checks remain passing.

The complete HTTP suite uses actual security/core/transaction/H2 composition with a mocked decoder. The PostgreSQL MVC test uses a directly established principal. Neither proves live JWT issuance/signature verification, SMTP or ownership of the fixture email. Existing cryptographic decoder tests are distinct from a live Keycloak integration.

During development, one test worker lost its connection during overlapping Gradle work; subsequent Gradle runs were sequential. H2 cleanup initially failed because direct JDBC deletes used a pool with auto-commit disabled; cleanup and direct status mutations were corrected to use a transaction. Final full results above supersede those intermediate failures. Existing Ehcache/Unsafe and cache-shutdown warnings remain; no claim is made that all runtime warnings were resolved.

### CI template and reviewed sources

[GitHub's Gradle CI guidance](https://docs.github.com/en/actions/tutorials/build-and-test-code/java-with-gradle), reviewed 4 November, demonstrates checkout, JDK setup and a wrapper build. CEREMO's original [workflow](../../workflows/backend.yml) reuses the existing complete check task, including PostgreSQL and coverage, rather than introducing a parallel quality definition.

Official actions are pinned to reviewed release commits: [checkout v7.0.0](https://github.com/actions/checkout/commit/9c091bb21b7c1c1d1991bb908d89e4e9dddfe3e0), MIT; [setup-java v6.0.0](https://github.com/actions/setup-java/commit/dd06d9cba3e5552c54d9f8ea23572deb30010f7c), MIT. Release references, source/readme and licences were inspected; setup-java supports the selected Java 25. The workflow requests contents:read, disables persisted checkout credentials and uses no artifact upload, remote cache or additional third-party action. These pins require normal future security/update review.

Automatic verification is configured for push/PR in the template. Sonar is opt-in workflow_dispatch with runSonar=true, an approved SONAR_HOST_URL repository variable and SONAR_TOKEN secret; missing configuration fails explicitly. Its fresh checkout invokes the existing sonar task, whose check dependency and gate wait were inspected. Token values are never put in arguments or output. The template assumes this project is the hosted repository root; this checkout has no project-local .git. No Git initialisation, push, remote job, repository secret/variable or branch-protection change occurred. Local wrapper LF endings and existing .gitattributes are suitable for the bash invocation. actionlint is unavailable; the workflow has source/static review only, not a remote execution claim.

### Remaining evidence and writing check

Sonar server analysis/gate and Java analyser capability remain unverified. Live Keycloak/JWKS/SMTP, production actor auditing, approved request-size/rate controls and the permissions for future marketplace use cases remain release work. The CI template cannot enforce hosted-repository policy until it is placed/configured there. Coverage and passing tests cannot establish customer value or complete security.

Documentation/context version 3.1, 4 November 2026: changed account/API/quality prose reviewed locally with the installed humanizer guidance; no confidential text submitted externally. Grammarly check remains pending because no authorised checker was available in this session; no score, full language-check claim or authorship certification. Final local PowerShell validation passed for 22 documentation/context files, 285 relative links, 93 anchors and consistent table columns. A first Python invocation found only the unavailable Windows Store alias; no Python installation occurred. The approved nine numbered documents remain intact, and no product document was added.

## SonarQube and Grammarly verification, 6 November 2026

Request: complete the outstanding SonarQube and Grammarly verification. Read the current build, agent rules, quality evidence, shared/wire maps and reuse skill. Applied the installed Spring Boot verification and humanizer guidance; checked the plugin-management workflow for Grammarly access. No application code, dependency, migration or quality threshold changed.

### SonarQube readiness and scan prerequisite

- Initially no Docker containers were running and localhost:9001 refused connections. Started only the configured SonarQube service with `docker compose up -d sonar`, preserving its named volumes. The configured container was recreated and started; no application database/Keycloak service, realm or account was changed. One initial shell could not resolve Docker; retrying with the resolved Docker executable succeeded.
- Confirmed `http://127.0.0.1:9001/api/system/status` returns UP, version **25.12.0.117093**. Container startup alone was not counted as readiness; earlier timed-out polls remained unverified until this response.
- Ran `.\gradlew.bat sonar --dry-run --console=plain`: BUILD SUCCESSFUL, reported 1m 50s. The graph contains regular/PostgreSQL tests, input checks, combined report, per-class coverage verification, check, sonarResolver and sonar. All were SKIPPED by dry-run. This proves task composition only: no new tests/coverage analysis, code upload or server quality-gate result.
- SONAR_TOKEN was absent in process, user and machine environment; the optional helper password was also absent. No token volume existed. No password guessing, token generation, credential reset or recovery of old command-line secrets was attempted. The founder chose to configure SONAR_TOKEN locally; that intention is not evidence that the variable is set. A real scan must wait for configuration, read the secret without printing it and target the approved local server.
- Gradle scanner **7.5.0.8588** uses project key `ceremo_backend`, both test roots/results and combined XML; `sonar.qualitygate.wait=true`. A separate legacy `sonar-project.properties` has key `ceremo`, a regular-only coverage path and issue suppressions. It was not edited or assumed to be effective Gradle configuration; verify actual analysis scope/settings when a real scan is available. Do not switch to that CLI configuration to obtain a pass.

Official sources reviewed 6 November: [SonarScanner for Gradle](https://docs.sonarsource.com/sonarqube-community-build/analyzing-source-code/scanners/sonarscanner-for-gradle) documents the pinned scanner and environment authentication; [analysis authentication parameters](https://docs.sonarsource.com/sonarqube-server/analyzing-source-code/analysis-parameters/parameters-not-settable-in-ui) recommends SONAR_TOKEN rather than writing credentials in files/arguments. The latter live manual is Server 2026.5, newer than the observed local Community build; no broader compatibility or successful analyser result is inferred from it.

### Grammarly availability and writing-check record

- Material submitted: **none**. Target documentation remains version 3.4, dated 5 November; no document or excerpt has been checked by Grammarly in this session. Result: **not run/pending**; no score exists.
- No Grammarly tool was exposed. The plugin directory search for Grammarly returned no matching plugin. No installation/connection occurred. No authorised authenticated browser or Writing Score API access was available, and no company draft was uploaded.
- Reviewed the official [Editor user guide](https://support.grammarly.com/hc/en-us/articles/360003474732-Grammarly-Editor-user-guide) and [Writing Score API](https://developer.grammarly.com/writing-score-api.html). The editor checks uploaded/pasted text; the API requires an OAuth bearer token with scores-api read/write scopes plus a document upload. The API exists, but its existence does not establish account entitlement or agent access. An older help URL was inaccessible; the current guide was used instead.
- Any future check must use an approved public/anonymised text selection, review anonymisation before submission, and record exact material/version/date, actual result and limits. A checked excerpt does not cover all nine documents. Local humanizer review, a grammar score or a detector result must not be presented as full document assurance or proof of authorship.

Current limits: Sonar analysis/server gate awaits the local token; Grammarly awaits an authorised checker and approved material. The 5 November tests/coverage below remain historical results, not newly rerun checks on 6 November. No private founder guide was uploaded or copied into shared material. Further results must be appended after actual execution.

### Salesync token-generator review, 6 November 2026

Follow-up request to run analysis, 6 November: local Sonar still returns UP, but process/user/machine SONAR_TOKEN and helper password remain unset. The configured project token volume/helper image are absent; no token entry was found in the checked project settings or user Gradle properties, and no scanner report-task.txt exists. The token's location was requested without asking the founder to disclose its value. No scan, fresh test run or quality-gate result is claimed. This is credential availability evidence, not proof that the founder has never generated a token elsewhere.

Current-task authorisation: the founder explicitly requested Salesync's token generator as a reference. Read only `src/main/docker/sonar/sonar_generate_token.sh`, its Dockerfile and sonar.yml under the user-owned Salesync checkout. No Salesync script was executed or modified and no Salesync credential/token was reused. Credential-bearing source lines were omitted from tool output.

Observed reference: waits for UP, changes the administrator password using fixed credentials, then calls `/api/user_tokens/generate`. It lacks the current CEREMO helper's strict shell, HTTP-failure handling and bounded readiness check. Its Compose configuration disables authentication for the evaluation server. Its Dockerfile contains development account setup; those credentials and sudo membership are not needed by CEREMO. This is the founder's private reference, not independent engineering/security evidence or a public licence to redistribute.

CEREMO already has the adapted [generator](../../../src/docker/sonar/sonar_generate_token.sh), [Dockerfile](../../../src/docker/sonar/Dockerfile) and [opt-in Compose service](../../../src/docker/sonar.yml). Reuse these; no duplicate implementation or application/shared/wire change is needed. They require the current username/password, wait for UP with finite retries, use HTTP/JSON failure checks, set umask 077 and write the token into `/tokens/sonar-token` in the project token volume. The non-root image owns that directory. A nonempty existing file stops new generation, but this does not validate revocation/expiry. The helper supplies a token name only; do not describe its result as a project-scoped/expiring analysis token without changing and verifying that policy.

Read the actual resolved Compose configuration without printing secret values: helper exists and its username is configured, but CEREMO_SONAR_PASSWORD is empty. Process/user SONAR_TOKEN and helper password are also absent. Confirmed local Sonar UP, version 25.12.0.117093. The helper image was not present. Generation has not run: authentication remains the missing input; the script cannot obtain a token without a valid server credential.

Concrete next step: in the user's own PowerShell session, collect the current SonarQube password with Read-Host -AsSecureString, pass it temporarily through CEREMO_SONAR_PASSWORD, run `docker compose --profile tools run --rm sonar-token`, and remove the password environment variable in finally. Do not request the password in chat, store it in source or reset it automatically. After successful generation, load the token privately from the intended volume into SONAR_TOKEN and execute the existing Gradle sonar task. Current-password entry need not become a persistent user/machine environment variable.

Official [SonarQube token guidance](https://docs.sonarsource.com/sonarqube-server/user-guide/managing-tokens), reviewed 6 November, distinguishes user versus project-analysis tokens and the permissions needed to generate/use them. The live 2026.5 manual is newer than the local 25.12 server; verify actual API responses, scope and gate rather than assuming compatibility. The [Gradle scanner guide](https://docs.sonarsource.com/sonarqube-community-build/analyzing-source-code/scanners/sonarscanner-for-gradle) supports environment token authentication. No new token, scan, test or quality-gate result is claimed from this read-only comparison. Grammarly remains pending; no text was uploaded. The reference permission is specific to this token-generator task.

### Generator container created, 6 November 2026

The founder identified the missing runtime generator container and explicitly supplied Salesync's `src/main/docker` directory for comparison. Reopened both projects' sonar.yml, Dockerfile and generation script; read CEREMO's root Compose include. CEREMO already defines sonar-token under the optional tools profile, while the referenced Salesync service is unprofiled. Earlier startup targeted only the Sonar server. Service definition and created container are separate facts; no new duplicate configuration was needed.

Primary evidence: Docker's [Compose profiles](https://docs.docker.com/compose/how-tos/profiles/) explains opt-in services and explicit targeting; [compose create](https://docs.docker.com/reference/cli/docker/compose/create/) creates containers without starting the service. Reviewed 6 November. The [SonarQube default-user guide](https://docs.sonarsource.com/sonarqube-community-build/instance-administration/user-management/introduction) documents first-install administrator credentials; [Sonar's setup tutorial](https://www.sonarsource.com/resources/library/static-code-analysis-using-sonarqube/) directs initial password change. These references do not establish this existing local instance's password or authorise silently resetting it. No bootstrap login or password reset was attempted.

Applied the installed Docker-patterns skill and CEREMO reuse workflow. Read current source/configuration after research. Ran `docker compose --profile tools config --quiet`, then `docker compose --profile tools build sonar-token` successfully on the local desktop-linux context. Built ceremo-sonar-token:latest from the checked-in non-root Dockerfile, with the resolved Ubuntu 24.04 base. Installed only its existing declared curl/jq/CA packages in the helper image; no host dependency or application dependency was added. No remote script was executed or source uploaded to an external service.

Ran `docker compose --profile tools create --no-build --no-recreate sonar-token`: created **ceremo-sonar-token-1** and **ceremo_sonar_tokens**, leaving the existing Sonar server running and PostgreSQL/Keycloak stopped. The helper is **CREATED**, not running or successfully completed. Ran the image's `/bin/bash -n /usr/local/bin/sonar_generate_token` with network disabled: exit 0. Verified LF line endings and image runtime user ceremo. A read-only mount inspection reports token-directory mode **0700**, owner **10001:10001**, and **no token file**; no secret value was read or printed. The configured password remains absent. Sonar status is UP, version 25.12.0.117093.

The founder was asked whether initial login remains unchanged or a custom password exists, without requesting the password itself. Generation must authenticate before it can succeed. Do not start a predictable failing job merely to make a container appear active, copy Salesync's fixed credentials, disable authentication or change existing credentials automatically. Actual generation, token validation and Sonar analysis/gate remain pending. No application tests were rerun because source/build configuration did not change.

Updated existing document 08 to version 3.5 and README with the distinction between service definition, container creation and token generation. The nine numbered documents remain the baseline. Earlier same-day statements that the image/volume did not exist are historical and superseded by this preparation. Local prose reviewed; Grammarly pending, with no text upload or score.

### Authenticated analysis, 6 November 2026

Status: **verified local execution; findings and release-policy work remain**. The founder confirmed a fresh SonarQube instance with its initial login unchanged. This authorised completion of initial setup within the existing request to generate a token and analyse CEREMO. This checkpoint supersedes earlier same-day observations of absent credentials and unrun analysis; their dated preparation records remain above.

#### Credential setup and reuse

Used the existing `src/docker/sonar.yml`, non-root helper image and `sonar_generate_token.sh`; no application class, test, dependency, migration, scanner property, security chain or coverage threshold changed. Shared/wire code was inspected for context; no new Java helper or Spring configuration was needed. Salesync was not consulted again or executed.

Verified the documented initial login against the loopback server, then used its own authenticated Web API metadata for `/api/users/change_password` parameters. Generated a random password with the platform cryptographic random generator, saved it before the password-change request and verified the changed credential. The protected files are `/tokens/sonar-admin-password` and `/tokens/sonar-token` in `ceremo_sonar_tokens`: mode **0600**, UID/GID **10001:10001**, directory **0700**. Ran the existing generator as a one-off `compose --profile tools run --rm --no-deps` job with its password read from the mounted volume. It exited successfully and was removed; the separately prepared service container remains CREATED. Token authentication returned valid. No credential value was printed or saved to source, documentation or analysis artifacts. The host token environment was scoped to the analysis process and cleared afterwards.

The generated credential is an administrator-owned **user token**, not a project-scoped analysis token; the helper supplies no token type, project or expiry. This local evaluation setup does not establish least-privilege CI credential configuration or a production secret store. Docker daemon operators can access the volume. A future operational setup must issue a suitably limited credential, define rotation/expiry and protect its storage; do not reuse this local administrator token remotely.

#### Actual commands and results

First command: `.\gradlew.bat sonar --console=plain`. It reached the scanner but failed with `IllegalStateException: Failed to extract archive`, after 2m 53s. No report/gate resulted from that attempt. Diagnostic retry: `.\gradlew.bat sonar --console=plain --stacktrace`, **BUILD SUCCESSFUL**, exit 0, **8m 43s**, two tasks executed and nine up-to-date. The retry extracted the auto-provisioned OpenJDK 21.0.9 runtime successfully without a configuration change. The initial extraction failure's underlying cause was not established; do not attribute it to antivirus or filesystem policy without evidence. The documented installed-runtime alternative was researched but not used.

Both test tasks and the combined report/coverage verification were UP-TO-DATE for unchanged application source. The coverage-input guard and actual scanner executed. This is a fresh analysis of existing same-revision test results, **not a fresh test execution**. Read JUnit XML and the combined XML with external DTD resolution disabled; report timestamp remains `2026-11-05T18:49:38.0847817Z`.

| Evidence | Observed result |
| --- | --- |
| Local server / Java analyser | SonarQube 25.12.0.117093 / Java 8.22, build 41895 |
| Scanner / application source level | Gradle plugin 7.5.0.8588 / Java 25 |
| Regular / PostgreSQL results | 532 / 50 tests; zero failures, errors or skips |
| Combined JaCoCo | 2155 covered lines, 449 covered branches, 9244 covered instructions; zero missed counters and zero classes with missed lines/branches |
| Imported server coverage | 100.0% overall, line and branch; 2155 lines and 449 conditions to cover; zero uncovered |
| Imported server tests | 582; failures/errors/skips zero; success density 100.0% |
| Overall duplication | 0.0% |
| Main analysis measures | 164 files, 5607 non-comment lines; Java syntax outputs cover all 162 main + 87 regular-test + 5 integration-test files |
| Compute task | `c51d7a31-1d5f-4ea5-b9b2-1ac97d13d4a6`, SUCCESS; submitted 11:17:08 UTC, completed 11:18:42 UTC |
| Analysis ID | `4ebd200d-6c3e-431d-a901-35fad182683c` |
| Dashboard | [CEREMO Backend, local server](http://127.0.0.1:9001/dashboard?id=ceremo_backend) |

The effective scanner context uses project key `ceremo_backend`, both test roots/binary/result paths and the combined JaCoCo XML. Its main sources are Java, `.github`, `build.gradle.kts` and `settings.gradle.kts`; this does not establish scanning of Docker/runtime resource configuration. The legacy root `sonar-project.properties` was not changed or used as a substitute; its earlier key, regular-only report and issue-ignore settings are absent from the inspected effective context. No new exclusions, suppressions, generated annotations, disabled tests or lowered thresholds were introduced.

#### Gate interpretation and findings

Authenticated `/api/qualitygates/project_status?analysisId=...` returned **status OK, conditions [], ignoredConditions false**. The inherited new-code definition is `PREVIOUS_VERSION`. This is the project's first completed analysis; the empty conditions list establishes no evaluated new-code acceptance result. It must not be described as an issue-free codebase or proof that CEREMO's proposed release policy is enforced.

The default built-in **Sonar way** gate actually configures: zero new issues, new coverage at least **80%**, new duplication at most **3%**, and new security-hotspot review **100%**. No server policy was altered during this task. CEREMO's separate **100% per-class lines/branches** rule remains in Gradle; the proposed server 100% new-code requirement still needs an explicit gate and a meaningful baseline. A deliberate failing-gate CI check remains outstanding.

The server reports **72 unresolved issues: one BUG and 71 CODE_SMELL**, with **zero reported VULNERABILITY**. Severity facets: four CRITICAL, 43 MAJOR and 25 MINOR; no BLOCKER. All four CRITICAL entries are code smells, not four confirmed critical security vulnerabilities. Two additional security hotspots remain TO_REVIEW, zero reviewed. No issue or hotspot was dismissed or reclassified.

| Finding | Location / rule | Review needed |
| --- | --- | --- |
| Potential null dereference, MAJOR bug | `account/infrastructure/primary/security/ActiveAccountAuthorizationManager.java:30`, `java:S2259` | The general authorisation interface permits a nullable result; inspect the concrete authenticated manager's contract before deciding the appropriate defensive handling or evidenced disposition. No runtime exploit or access bypass was demonstrated. |
| API CSRF hotspot, HIGH review probability | `shared/authentication/infrastructure/primary/SecurityConfiguration.java:76`, `java:S4502` | The inspected API chain is stateless and bearer-authenticated; the separate browser chain retains CSRF. Validate the complete credential/route assumptions and tests before a contextual disposition; no automatic SAFE marking. |
| Dependency-integrity hotspot, LOW review probability | Project-level `kotlin:S6474` | `gradle/verification-metadata.xml` is absent. Plan reviewed checksums/signatures from trusted artifacts; generating metadata alone must not be treated as establishing artifact trust. |
| CRITICAL code smell | `tenant/infrastructure/secondary/persistence/JpaTenantRepository.java:64`, `java:S1192` | Repeated parameter-name literal; preserve query semantics in any change. |
| Three CRITICAL code smells | `BeanValidationErrorsHandlerTest.java:73`, `ErrorHandlingTest.java:229`, `HttpErrorsHandlerTest.java:97`, `java:S1186` | Empty test-fixture handlers need their intended behaviour assessed; do not add meaningless behaviour or suppress findings merely to clear the count. |

Observed scanner warnings: JUnit class-to-file lookup warnings occurred even though the server imported the correct 582-test aggregate. Class-level mapping remains to be assessed; do not claim the warnings were resolved. The compute task records missing blame for **256 files**. This checkout has no project-local Git repository; no repository initialisation, broad parent-repository operation, push or history change was performed. Missing SCM evidence limits blame/assignment and new-code interpretation. Remote CI, live Keycloak, production controls and dependency/container vulnerability scans remain separate checks.

#### Sources, applicability and writing verification

Reviewed 6 November 2026; no public implementation was copied and no remote script or new skill was installed:

- [SonarQube Community user-management introduction](https://docs.sonarsource.com/sonarqube-community-build/instance-administration/user-management/introduction): documents initial administrator access; applicable only after the founder confirmed a fresh instance. Local server Web API metadata supplied the actual password-change fields.
- [SonarScanner for Gradle](https://docs.sonarsource.com/sonarqube-community-build/analyzing-source-code/scanners/sonarscanner-for-gradle): scanner version, token injection and build/test ordering; checked against the effective local task/context and server result.
- [Managing scanner JRE provisioning](https://docs.sonarsource.com/sonarqube-community-build/analyzing-source-code/scanners/scanner-environment/managing-jre-auto-provisioning): automatic provisioning and the supported installed-runtime alternative; researched after extraction failed, but no provisioning setting was changed.
- [SonarQube SCM integration](https://docs.sonarsource.com/sonarqube-community-build/analyzing-source-code/scm-integration): explains blame and new-code limitations without SCM data. A guessed older new-code documentation URL was inaccessible and was not used as evidence; local API results establish the actual gate/baseline facts above.
- [Spring Security CSRF guidance](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html): guides review of automatically supplied browser credentials; statelessness alone is insufficient justification. Current manual is 7.1.2, so assess against the resolved project dependency before any code change.
- [Gradle dependency verification](https://docs.gradle.org/current/userguide/dependency_verification.html): metadata location, checksums/signatures and the trust limitation of bootstrapping. Supports the review plan, not a claim that dependency verification currently runs.

Writing record: document 08 **v3.6**, README and these local context changes reviewed with installed humanizer guidance on 6 November. Grammarly remains **not run/pending**, material submitted **none**, no score or authorship claim. No private founder guide or company text was sent to an external checker. Only the authorised local SonarQube server received the analysis.

Local documentation validation checked four changed files and 86 relative file links: zero missing targets, consistent table columns and the new analysis heading present. The nine numbered public documents remain intact. A separate in-memory comparison found the generated token in none of seven checked files: the four changed documents/context files, scanner task/context artifacts and the current Gradle daemon log. This is a targeted credential-hygiene check, not a repository-wide secret scan. Final container inspection confirms Sonar running and the prepared helper CREATED; no lingering one-off generator container remains.

### Dashboard login comparison, 6 November 2026

Question: why CEREMO asks for browser login when the founder expects Salesync's immediate dashboard access. Salesync was explicitly authorised as a read-only reference for this request. Applied the existing Docker and shared/wire review workflow; no Java component is involved.

Observed configuration: CEREMO `src/docker/sonar.yml:6` sets `SONAR_FORCEAUTHENTICATION` to true; the equivalent Salesync file sets it to false. Both bind port 9001 to 127.0.0.1 and use SonarQube 25.12.0.117093. Salesync's helper changes a password and generates an API token; it does not call the browser-login endpoint. No credential-bearing reference lines were displayed, copied or executed. The dashboard difference is anonymous browsing, not automatic administrator login. A scanner token authenticates analysis requests; it does not establish a session in the founder's browser.

Local read-only checks: anonymous `api/components/show?component=ceremo_backend` returns **401**. Authenticated project metadata reports visibility **public**. Global and project `api/permissions/groups` results show **Anyone with no explicit permissions**, including no scan or administration grants. These observations identify the current boundary; no runtime setting, permission, browser cookie or container was changed for this investigation.

Sources reviewed 6 November: [SonarQube Community user accounts](https://docs.sonarsource.com/sonarqube-community-build/instance-administration/security/user-accounts) documents forced authentication and anonymous public-project browsing; [project permissions](https://docs.sonarsource.com/sonarqube-community-build/project-administration/setting-project-permissions) distinguishes public source/report visibility from analysis and administration. The live user-account manual now describes removal of Anyone and anonymous analysis in **26.10**; CEREMO is **25.12**, so that newer guarantee must not be attributed to our server. Current local permissions were inspected instead.

Original proposal, before approval: match Salesync's local viewing behaviour by changing only the forced-authentication setting to false, retain the loopback binding and existing authenticated administration/analysis permissions, recreate only the Sonar service without deleting volumes, then verify anonymous report reads and denied protected access. Update the existing operating documentation with the approved behaviour. Alternative: retain mandatory login and use a persistent browser session. The distinction between anonymous viewing and automatic login was explained before requesting approval under the guided-development rules.

Writing/context check: local note reviewed; Grammarly unavailable/pending, no material submitted. No product document or installed skill was added. The preceding paragraphs record the original investigation; the approved implementation follows.

#### Applied local dashboard access

The founder replied **"Yes apply"** to the explicit local anonymous-viewing proposal. Changed `src/docker/sonar.yml` to `SONAR_FORCEAUTHENTICATION=false`, with a comment describing its scope. Kept the same image, loopback binding, credentials, named volumes and existing permissions. No application Java, test, dependency, shared/wire configuration or coverage rule changed. Reused the existing service and token volume; no duplicate infrastructure was introduced.

Ran `docker compose config --quiet`, exit 0, then `docker compose up -d --no-deps sonar`, exit 0. Only the Sonar service was recreated. Readiness polling initially observed startup; no pass was claimed until `/api/system/status` returned **UP**, version **25.12.0.117093**. The running container confirms `SONAR_FORCEAUTHENTICATION=false` and only `127.0.0.1:9001` mapped to port 9000.

Fresh HTTP requests without credentials or a saved browser session produced:

| Check | Result |
| --- | --- |
| Dashboard HTML at `/dashboard?id=ceremo_backend` | 200 |
| Public project metadata | 200 |
| Public coverage metrics | 200; overall, line and branch coverage remain 100.0% |
| Administrator permission management | 401 |
| User-token administration | 401 |
| Analysis cache for `ceremo_backend` | 403; local API metadata explicitly requires project scan permission |
| `/api/users/current` | `isLoggedIn=false`, no login identity |
| Existing token with `/api/users/current` | `isLoggedIn=true`; authenticated permission inspection succeeds |
| Global and project Anyone permissions | Empty in both scopes; no anonymous scan or administration grants |

Verification correction: `/api/authentication/validate` returned `valid=true` without credentials after forced authentication was disabled. It was an unsuitable assertion of a logged-in browser identity; `/api/users/current` supplied the explicit session-state evidence above. A separate initial PowerShell collection assertion counted a null projection from an empty permission list; normalising actual non-empty permission values confirmed both lists were empty. These were verification-script corrections, not findings of an administrator session or unintended grants. No permission was changed to make a check pass.

Preservation checks confirm the latest analysis remains `4ebd200d-6c3e-431d-a901-35fad182683c` and the same `ceremo_sonar_data`, `ceremo_sonar_extensions` and `ceremo_sonar_logs` volumes are mounted. The stored token still authenticates; no password or token was regenerated or printed. PostgreSQL and Keycloak retain their original container IDs and remain exited. The prepared token helper retains its original ID and CREATED state. No volume removal, database reset, private-project visibility change or unrelated service restart occurred.

This verifies local HTTP access and the inspected permissions; it is not an interactive browser-rendering test or a new code analysis. No fake analysis report was uploaded to test denial. The protected cache check and absence of anonymous Execute Analysis grants establish the inspected analysis-access boundary on this 25.12 instance. Earlier analysis findings and baseline-gate limitations remain unresolved. Java tests and Sonar analysis were not rerun for this Compose-only change.

Updated README, document 08 **v3.7** and the research index with the approved local behaviour. Reviewed [Docker Compose up](https://docs.docker.com/reference/cli/docker/compose/up/) on 6 November: configuration changes recreate the selected service while preserving mounted volumes; `--no-deps` avoids starting linked services. This applies to the local restart, not a production rollout. SonarQube references and version limits are recorded above. Grammarly remains unavailable/pending, material submitted none; no new document or skill was added.

### Hotspot remediation, 6 November 2026

- Status: implemented and verified locally; replaces the initial review's pending remediation below.
- Question: address the two reviewed hotspots without weakening authentication, browser CSRF or the mandatory coverage rule, and preserve the lessons for future work.
- Authorisation: user explicitly requested implementation after the read-only review. Scope covers dependency integrity, wrapper verification, CSRF rationale/regressions and evidence-based local hotspot disposition. No Salesync research, dependency upgrade, server-policy change or unrelated issue fix.
- Environment: Java 25, Spring Boot 4.1.1, Spring Security 7.1.1, Gradle 9.8.1, JaCoCo 0.8.15, scanner 7.5.0.8588, local SonarQube 25.12.0.117093.

#### Evidence and adaptation

Sources reviewed 6 November 2026. Current Gradle manuals identify 9.8.2 and Spring manuals 7.1.2; CEREMO remains one patch earlier. Relevant APIs were checked against the installed Gradle binary and exercised by actual builds. No external implementation was copied wholesale, skill installed or dependency introduced. The review script is original CEREMO code using .NET APIs; framework documentation is evidence, not an implied licence to copy unrelated code.

| Source | Finding and CEREMO adaptation | Limit |
| --- | --- | --- |
| [Gradle dependency verification](https://docs.gradle.org/current/userguide/dependency_verification.html) | Generate candidate SHA-256 metadata, independently review it, then verify in a normal strict build; cover artifacts, metadata and plugins. | Generated hashes initially trust resolved bytes. Repository HTTPS/checksums do not authenticate the publisher. |
| [Gradle wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html), [9.8.1 distribution digest](https://services.gradle.org/distributions/gradle-9.8.1-bin.zip.sha256) and [wrapper JAR digest](https://services.gradle.org/distributions/gradle-9.8.1-wrapper.jar.sha256) | Pin the published distribution checksum and compare the existing wrapper JAR with its published checksum. | An already extracted cached distribution was not freshly downloaded for verification. |
| [StartParameter API](https://docs.gradle.org/current/javadoc/org/gradle/StartParameter.html) | `getDependencyVerificationMode()` and `getWriteDependencyVerifications()` support settings guards while allowing explicit baseline maintenance. | Checked against installed 9.8.1 with `javap` and build execution; version-specific web Javadoc was inaccessible. |
| [Plugin resolution](https://docs.gradle.org/current/userguide/plugins_intermediate.html) and [Portal mirroring](https://plugins.gradle.org/docs/mirroring) | Inspect marker coordinates and actual resolved bytes; use fresh Portal POM hashes for marker checks. | A marker's checksum URL may redirect to a different Central publication. Do not waive mismatches. |
| [Spring bearer-token resolution](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/bearer-tokens.html), [CSRF](https://docs.spring.io/spring-security/reference/features/exploits/csrf.html) and [MockMvc CSRF tests](https://docs.spring.io/spring-security/reference/servlet/test/mockmvc/csrf.html) | Keep browser CSRF; demonstrate that unsafe API writes reject session/cookie credentials and accept an explicit bearer header. | These local tests do not establish live Keycloak behaviour or justify a future cookie-authenticated API exception. |
| [Sonar hotspot lifecycle](https://docs.sonarsource.com/sonarqube-community-build/user-guide/security-hotspots) | Record a contextual Safe disposition only with inspected controls and verification. | Current manuals discuss phase-out; our actual 25.12 API still exposes the reviewed hotspot lifecycle. |

#### Local implementation and reuse

Reused [SecurityConfiguration](../../../src/main/java/io/ceremo/ceremobackend/shared/authentication/infrastructure/primary/SecurityConfiguration.java) and its [existing test fixture](../../../src/test/java/io/ceremo/ceremobackend/shared/authentication/infrastructure/primary/SecurityConfigurationTest.java). Added the scoped CSRF rationale and two unsafe-write tests; no runtime authentication rule changed. Existing browser-CSRF/logout tests remain. Provider write handlers in this suite are fixtures, not implemented marketplace features. Wire configuration needs no additional bean; the dependency baseline is a build concern. Domain/application boundaries and coverage scope are unchanged.

Added [verification metadata](../../../gradle/verification-metadata.xml), [strict settings guards](../../../settings.gradle.kts) and the original [checksum-review helper](../../../scripts/verify-dependencies.ps1). The [wrapper properties](../../../gradle/wrapper/gradle-wrapper.properties) pin distribution SHA-256 `dce76f55f8e251a3a1f130eb120f30b3d271de2b76c9b0729d316b5a1b6dc01f`. Existing wrapper JAR SHA-256 matched `3b8a25775a69158b5ad2b1d17a80a88dc7b40a352a73eb27d06c612a7ce68e98`.

The baseline contains **404 components / 748 artifacts**, metadata verification enabled, no trusted-artifact exceptions and no signature-verification claim. Every artifact matched: **543 published SHA-256 files**, **205 fresh HTTPS download hashes**. The helper permits approved repository URLs, validates coordinates, prohibits XML DTDs, uses bounded parallel requests/timeouts and writes origins only after all comparisons succeed. It rejects invalid checksums, report/metadata path collisions and trusted-artifact exceptions. It never changes expected digest values. The public artifact evidence report is local `build/reports/dependency-verification/checksums.json`; reviewed metadata SHA-256 is `a845325afed40d5089d5854a6e33b3c0f2bdaaeba1dc4040be2a26f79148c1e5`.

Repository discrepancy investigated: Boot marker `org.springframework.boot:org.springframework.boot.gradle.plugin:4.1.1` has Portal POM SHA-256 `d78007dde3f83063d97031d076b266641409a2985fcc289ec88a7171ed8c85d2`; Central's POM is `dd53191d267c826a9eefa9006cac364a29d1eee206532b4bcdfb35a3bdd559e8`. Both name implementation dependency `org.springframework.boot:spring-boot-gradle-plugin:4.1.1`. The Portal checksum URL redirected to Central despite different actual marker bytes. Initial review therefore rejected the mismatch; the final helper hashes the fresh Portal marker actually used by Gradle. No checksum was replaced or mismatch accepted to obtain a pass.

#### Executed verification

| Check | Actual result |
| --- | --- |
| `.\gradlew.bat --write-verification-metadata sha256 check bootJar sonarResolver --console=plain` | BUILD SUCCESSFUL, 9m 10s. Fresh 534 regular + 50 disposable PostgreSQL tests; failures/errors/skips zero. Bootstrap generation is not the independent integrity check. |
| `.\scripts\verify-dependencies.ps1 -UpdateOrigins` | All 748 artifacts matched; origins recorded without changing expected hashes. |
| Combined JaCoCo | 2155 covered lines, 449 covered branches, 9244 covered instructions; zero missed line/branch counters per application class. XML timestamp 13:52:48 UTC on 6 November. No exclusions or threshold changes. |
| `.\gradlew.bat --dependency-verification off help --console=plain` | Expected exit 1: strict-mode settings guard rejected the command. |
| Isolated tampered-checksum build | Expected exit 1: changed only a copied commons-lang3 JAR checksum in `build/security-verification-probe`; dependency verification rejected it. Real metadata/cache untouched. |
| Isolated missing-metadata build | Expected exit 1: copied settings in `build/security-verification-missing` rejected an ordinary build with no baseline. |
| Helper fixtures | Valid digest accepted; zero digest rejected without metadata changes; report collision and trusted-artifact exception rejected. |
| `.\gradlew.bat check bootJar sonar --console=plain` | Normal strict build SUCCESSFUL, 5m 22s; 13 tasks, two executed and 11 up-to-date. Same-revision fresh test/report/coverage outputs reused; input guard and actual scanner ran. No generation flags. |

Compute task `6fee0ed7-b86b-419f-aa09-19b1f6575715` completed SUCCESS; analysis `3047eb3c-4f8f-43cf-af9f-745b17937845` completed **14:08:12 UTC**. Server measures: **584 tests, zero failures/errors/skips; 100% line/branch coverage; 0% duplication; zero reported vulnerabilities; one bug; 71 code smells**. Scanner still warns about JUnit class-to-file mapping and missing Git blame. Aggregate tests/coverage imported correctly.

HIGH CSRF hotspot `cfbdb73e-bbab-49f7-806b-9e617e3c794b`, now line 78, was set through the authenticated local API to **REVIEWED / SAFE**, with controls, tests and reassessment triggers in its comment; GET confirmed the result. LOW dependency hotspot `b62465ee-dc21-48cf-b753-444432bfc4a8` no longer exists after the new analysis (GET returned 404); no manual Fixed status was manufactured. Server review measures are **100% reviewed, rating 1.0 / A, zero pending hotspots**. No rule suppression or browser-CSRF weakening occurred.

Latest gate is **OK** with two evaluated conditions: `new_violations` actual 0 / threshold 0, and `new_duplicated_lines_density` actual 0.0 / threshold 3. Its `PREVIOUS_VERSION` baseline date is 11:13:03 UTC on 6 November. Coverage/hotspot-review conditions were not evaluated; the server's required release policy and deliberate failing-gate test remain unverified. The local per-class 100% rule remains independently enforced.

#### Lessons and remaining work

Saved dependency-bootstrap, marker-redirect and CSRF-review lessons in the [security rules](../../security-quality-rules.md); refreshed the [shared map](../../shared-package-map.md). Operating instructions and the latest checkpoint are in [document 08 v3.8](../../../docs/08-development-and-quality.md#812-dependency-integrity-and-csrf-review). The index and root README identify the current evidence rather than the initial pending state.

One reliability issue and 71 code smells are outside this approved hotspot scope. Publisher-signature authentication, vulnerability/container scanning, production identity/operations, remote CI and required release-gate policy are not established by these checks. Existing approved anonymous local Sonar viewing and authenticated administration/analysis remain unchanged. Credentials stayed in the protected local volume/process environment, never literal command arguments. No private founder documents or customer records were transferred.

Editorial record: document 08 v3.8, README and affected research/rule entries reviewed locally on 6 November; Grammarly unavailable/pending, material submitted none. No score or authorship certification. Documentation-only changes after analysis require content/link checks, not another application build. Final local validation checked six Markdown files and 126 relative links: zero missing targets and consistent table columns; nine numbered product documents remain. The PowerShell helper parses without syntax errors. Re-reading the actual JUnit, combined JaCoCo and artifact-review reports confirmed the totals above; no test output was overwritten by a filtered rerun.

### Security hotspot priority review, 6 November 2026

Question: explain the dashboard's security-review rating and assess the two hotspots in review-priority order. Scope is a read-only technical assessment. No Sonar review status, comment, assignment, permission, application code or build configuration was changed. Salesync was not consulted. Applied the installed Spring Boot security skill and the local shared/wire workflow; primary Spring guidance takes precedence over any simplified advice that stateless applications are automatically immune to CSRF.

#### Live evidence and interpretation

Fresh anonymous reads of the authorised local server's `api/hotspots/search`, `api/hotspots/show` and `api/measures/component` confirm **two TO_REVIEW hotspots, 0.0% reviewed, security_review_rating 5.0 (E), and zero reported vulnerabilities**. E reflects review completion on this 25.12 instance; it does not establish two confirmed exploits. Review priority identifies which context to inspect first, not the severity of a confirmed application vulnerability. The earlier initial-gate OK result does not resolve these reviews.

| Order | Sonar review priority | Rule / location | Technical assessment and proposed disposition |
| --- | --- | --- | --- |
| 1 | HIGH | `java:S4502`, `SecurityConfiguration.java:76`; hotspot `cfbdb73e-bbab-49f7-806b-9e617e3c794b` | The CSRF exception is scoped to the bearer-token API/management chain. Source and existing tests support that design; a documented Safe disposition is a candidate for owner approval, not an automatic scanner dismissal. Preserve browser CSRF. |
| 2 | LOW | Project-level `kotlin:S6474`; hotspot `b62465ee-dc21-48cf-b753-444432bfc4a8` | `gradle/verification-metadata.xml` is absent. This is a real dependency-integrity configuration gap. It needs trusted checksums/signatures and executable verification before a Fixed disposition; Safe is not supported by the inspected setup. |

#### CSRF review: protected boundary and existing evidence

The affected data are authenticated account, tenant and logout mutations. The realistic CSRF attempt is a malicious site causing the browser to issue a state-changing CEREMO request using automatically supplied cookies or credentials. `shared/authentication/infrastructure/primary/SecurityConfiguration.filterChain` matches `/api/**` and `/management/**`, uses STATELESS session management and the resource server's bearer JWT mechanism. The public allowlist covers GET discovery/health and OPTIONS; other operations require authentication. The separate `browserFilterChain` uses OIDC sessions and keeps default CSRF protection, including POST `/logout`.

A fresh source search found no custom API cookie bearer resolver, form/body or query-token enablement, HTTP Basic/form login, or alternate session security-context repository. The existing `CustomClaimConverter` uses DefaultBearerTokenResolver to read a request; it is not an alternate cookie-authentication filter. Official Spring documentation identifies Authorization-header resolution as the resource-server default. Spring Security 7.1.1 `SessionManagementConfigurer.init` supplies request-scoped/null repositories for its stateless path rather than the session repository. These source observations support the narrow exception; the label JWT or STATELESS alone would not justify disabling CSRF.

Reopened [SecurityConfigurationTest](../../../src/test/java/io/ceremo/ceremobackend/shared/authentication/infrastructure/primary/SecurityConfigurationTest.java): `doesNotUseBrowserSessionToAuthenticateApi` submits a saved authenticated admin security context in an HTTP session and expects 401 for the API. `keepsCsrfProtectionForBrowserSession` expects 403 for a browser POST without a CSRF token. Provider detail tests also reject a session-only request. `requiresBearerAuthenticationForPrivateApi` includes an unauthenticated API POST rejection.

Reopened [AccountTenantSecurityIntegrationTest](../../../src/integrationTest/java/io/ceremo/ceremobackend/security/AccountTenantSecurityIntegrationTest.java): the real-signed-token API logout succeeds without a CSRF token and rejects unauthenticated logout; browser logout rejects missing CSRF and accepts the CSRF-protected request while verifying persistent revocation and the registered identity redirect. The browser login principal in that test is synthetic, not a live Keycloak login.

Read existing JUnit XML with external DTD resolution disabled: the security configuration suite records **16 tests, zero failures/errors/skips**, timestamp `2026-11-05T18:43:48.676Z`; the full-stack security suite records **13 tests, zero failures/errors/skips**, timestamp `2026-11-05T18:48:18.775Z`. Their named relevant cases are present. SecurityConfiguration source timestamp precedes those results. No tests were rerun during this review, and no prior coverage inputs were overwritten with a filtered test run. Before final status disposition or any future authentication change, confirm the maintained regressions against that release revision. A focused session-only unsafe API request remains useful additional evidence; the inspected session-only regression is a GET. Reopen the assessment if cookie authentication, Basic authentication, token resolution, browser proxying or the frontend trust boundary changes.

#### Dependency integrity: actual gap and bounded remediation

The build uses Maven Central and Kotlin Gradle scripts, explaining the `kotlin:` rule in a Java application. The missing file was checked directly. HTTPS and named dependency versions do not supply Gradle's artifact checksum/signature verification. No evidence of equivalent enforced verification was found in the inspected build. An adjacent observation is that `gradle/wrapper/gradle-wrapper.properties` also lacks `distributionSha256Sum`; wrapper archive verification is a separate control, not supplied by dependency metadata.

Proposed implementation, requiring the user's normal build-change approval: establish trusted artifact checksums/publisher keys, produce and review the dependency-verification baseline, retain metadata verification and use SHA-256 or stronger checksums plus verified signatures where suitable. Cover plugins and the regular/integration build configurations. Treat Gradle's generated checksums as a starting point: bootstrapping trusts the artifacts currently served, so generation alone does not authenticate their origin. Investigate mismatches rather than accepting every new checksum or adding broad trusted-artifact exceptions. Independently pin and validate the wrapper distribution checksum if that adjacent control is included in the approved work. Then run the complete existing check/Sonar workflow without reducing scope or thresholds, verify dependency failure handling, and decide the hotspot status from the resulting evidence. No dependency download, baseline generation, key import or code change was performed here.

#### Primary references and limits

Reviewed 6 November 2026; no external source code was copied or new skill installed:

- [SonarQube managing security hotspots](https://docs.sonarsource.com/sonarqube-community-build/user-guide/security-hotspots): context review, HIGH-first ordering and the Safe/Fixed/Acknowledged lifecycle. The live manual now mentions hotspot phase-out; this review uses the observed 25.12 server's actual hotspot API, not an assumption that its upgrade/migration has occurred.
- [SonarQube 10.7 metric definitions](https://docs.sonarsource.com/sonarqube-server/10.7/user-guide/code-metrics/metrics-definition): legacy security-review grade definitions, used only to interpret the older-style metric shown by our server. This is not a recommendation to run that unsupported version. Local API and screenshot confirm E and zero reviewed.
- [Spring CSRF threat model](https://docs.spring.io/spring-security/reference/features/exploits/csrf.html) and [resource-server bearer resolution](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/bearer-tokens.html): stateless cookie/Basic authentication can remain CSRF-vulnerable; the resource server defaults to the Authorization header. Live manuals are 7.1.2. The [7.1.1 session configurer source](https://github.com/spring-projects/spring-security/blob/7.1.1/config/src/main/java/org/springframework/security/config/annotation/web/configurers/SessionManagementConfigurer.java) gives a version-pinned implementation reference for `init`; it does not replace resolving dependencies and running current release tests before a change.
- [Gradle dependency verification](https://docs.gradle.org/current/userguide/dependency_verification.html): the metadata file, checksums/signatures, maintenance and bootstrapping trust limitation. Applies to the configured Gradle build, not a claim that a dependency-vulnerability or provenance scan ran.

Reuse: existing shared security chains and their tests supply the assessed controls; wire CORS remains separate and is not a CSRF defence. No new shared/wire component is needed for this review. Outcome: review order and evidence recorded; both Sonar statuses remain TO_REVIEW. No security certification, fresh test pass, runtime exploit or complete dependency audit is claimed. Local prose review completed; Grammarly remains unavailable/pending, material submitted none. No product document was added.

## Registered profile access, 5 November 2026

Approved scope: free registration before full provider profiles, detailed reviews and contact destinations; preserve accepted-membership/owner/staff controls for private tenants. See [ADR 11.10](../../../docs/03-technical-requirements.md#1110-registered-profile-and-contact-access) and [source/adaptation evidence](../product/customer-contact-retention.md#applied-registered-profile-boundary-5-november-2026). This checkpoint supersedes the earlier consolidation build for the current revision.

Focused authorisation/security-chain/tenant-wiring/architecture tests passed. Final command: `.\gradlew.bat check bootJar --console=plain`, exit 0, BUILD SUCCESSFUL; reported elapsed 1h 33m 34s, 11 actionable tasks, eight executed and three up-to-date. Regular and PostgreSQL test tasks executed against the same compiled application; input verification, combined report, per-class coverage verification and executable JAR completed successfully.

| Fresh report | Result |
| --- | --- |
| Regular JUnit XML | 532 tests; failures 0, errors 0, skipped 0 |
| PostgreSQL JUnit XML | 50 tests across four suites; failures 0, errors 0, skipped 0 |
| Combined LINE | 2155 covered, 0 missed |
| Combined BRANCH | 449 covered, 0 missed |
| Combined INSTRUCTION | 9244 covered, 0 missed |
| Application class inspection | No class has a missed line or branch counter |
| Combined XML written UTC | 2026-11-05T18:49:38.0847817Z |

Read `build/reports/jacoco/combined/combined.xml` with external DTD resolution disabled. CLASS/MISSEDCOUNT zero limits, report input guards and coverage scope remain unchanged. No dependency, exclusion, generated marker, disabled test or lower threshold was added. The fresh JAR includes ActiveAccountAuthorizationManager and excludes the test-only ProviderBoundaryFixture.

New tests verify anonymous/no-auth storage avoidance, known account denials and unexpected failure without a grant. Full-stack PostgreSQL 18.0 checks use actual RSA-signed JWTs through the production discovery/JWKS decoder, missing/PENDING/ACTIVE/SUSPENDED/ANONYMISED local accounts, and independent customers/unrelated admins attempting private tenant reads. Existing accepted-invitation/owner/revocation, transaction/audit rollback and concurrency checks remain passing. Provider HTTP handlers in this suite are fixtures; successful boundary tests do not implement provider profiles, contact releases, DTO/media protection or subscription eligibility.

Local documentation validation: 31 Markdown files, zero missing relative file targets, consistent table-column counts in nine public documents. Actual provider DTO/media leak tests, contact persistence/abuse controls, frontend state integration and retention features remain feature work. SONAR_TOKEN was unset; analysis and server gate were not run. Live Keycloak/SMTP/MFA and remote CI remain unverified. No live realm, business database or customer record changed. Existing JDK/Ehcache warnings remain; no dependency-scan result is claimed.

Editorial record: documentation version 3.4, reviewed locally 5 November 2026 using humanizer guidance. Grammarly unavailable/pending; no company text uploaded, score fabricated or authorship certification claimed. After the successful application build, remaining edits were documentation/context only.

## Account consolidation, 5 November 2026

Approval: consolidate usermanagement into account, preserving HTTP contracts, database schema, security policies and transaction boundaries. Source and adaptation evidence is in the [consolidation record](../ports-and-adapters/provider-account-security.md#account-module-consolidation-5-november-2026). This checkpoint supersedes the earlier same-day build for the current source layout.

Final command: `.\gradlew.bat clean check bootJar --console=plain`. BUILD SUCCESSFUL in 5m 55s; all 12 actionable tasks executed. Fresh XML records **525 regular tests and 48 PostgreSQL integration tests**, with zero failures, errors or skips. PostgreSQL 18.0 ran in disposable containers. Both suites used the same freshly compiled application.

Combined report: `build/reports/jacoco/combined/combined.xml`, UTC timestamp `2026-11-05T15:19:40.1103508Z`. Covered/missed counters: **2139/0 lines**, **443/0 branches**, **9186/0 instructions**. No application class has a missed line or branch. The per-CLASS zero-missed rule and input checks passed unchanged; no exclusions, lower thresholds or disabled tests were introduced. Removing the duplicate configuration reduces the executable-line count from the earlier checkpoint.

The focused account configuration, architecture, application and activation tests also passed before the clean build. The architecture guard rejects a production usermanagement package. Inspection of the fresh executable JAR confirmed the account domain and both application services plus AccountConfiguration, with no usermanagement/UserManagement entry. The source/test roots are removed; wire contains 16 Java files across nine areas. Local validation checked 30 Markdown files with zero missing relative file targets; nine public numbered documents remain. Historical snippets retain their dated context.

The existing real-RSA-token, loopback discovery/JWKS security suite remains passing. It does not verify a live Keycloak realm, SMTP, staff MFA or client SSO logout. SONAR_TOKEN was unset, so Sonar analysis/server gate was not run. Remote CI and operational release checks remain unverified. No live realm, business database or customer record was changed.

Editorial record: affected documentation version 3.3 and local research notes reviewed on 5 November 2026. Grammarly tooling unavailable; verification remains pending. No company text was uploaded, and no score or authorship certification is claimed. Documentation-only updates after this build require link/content checks rather than another application build.

## Provider account security, 5 November 2026

Scope: the user explicitly authorised adapting Salesync account/logout/tenant structure, then confirmed one provider business per tenant and independent customer accounts. See [source evidence and implementation](../ports-and-adapters/provider-account-security.md).

Command: `.\gradlew.bat check bootJar --console=plain`. Final result: BUILD SUCCESSFUL in 4m 33s, 11 actionable tasks. Java 25/Boot 4.1.1; PostgreSQL 18.0 disposable containers. The regular suite passed **525 tests** and PostgreSQL suite passed **48 tests**, with zero failures, errors or skips. The latter includes 11 full-stack account/tenant security cases using a loopback OIDC discovery/JWKS server and actual RSA-signed JWTs through the production decoder; the decoder is not mocked in that suite.

Fresh combined XML: `build/reports/jacoco/combined/combined.xml`, UTC timestamp `2026-11-05T05:22:19.2131473Z`. Counters: **2141/2141 lines**, **443/443 branches**, **9189/9189 instructions**. No class has a missed line or branch counter. The existing CLASS/MISSEDCOUNT maximum-zero rule passed unchanged, with both suites from the same compiled revision. No dependency, coverage exclusion, generated-code marker, disabled test or lower threshold was introduced. BootJar completed successfully.

Coverage gaps exposed missing-member acceptance and missing-aggregate status-update guards; additional PostgreSQL assertions now cover their errors and lack of replacement inserts. Earlier failed attempts are superseded by this final run, not reported as passes. The full suite also preserves user-account migration uniqueness, rollback, activation concurrency, shared/wire behaviour and plain-core/HTTP dependency rules.

Local checks: 12 affected Markdown files had zero missing relative file links; the public docs root retains nine files. Keycloak import JSON parses; new-import verification/brute-force/rotation/event settings and disabled unused admin client were inspected. New tenant error keys have ten EN and ten FR title/detail entries. The unused application admin credential block is removed. No existing realm, volume or deployed credential was reset or changed.

Limits: live Keycloak/SMTP/MFA, real client refresh/SSO logout, IdP-initiated back-channel processing, production database version, request/resource limits, dependency/key-rotation and operational release checks remain outstanding. SONAR_TOKEN was not set, so Sonar analysis/server quality gate was not run; no code upload or pass is claimed. GitHub Actions remains a local template, not verified remote CI. Existing Ehcache/JDK warnings remain and do not establish a security failure or a clean dependency scan.

Editorial record: implementation notes v1.0 and affected product docs v3.2; locally reviewed 5 November 2026. Grammarly tooling was unavailable in the exposed tools; check remains pending. No company text was sent externally and no detector/grammar score or human-authorship claim is made.
