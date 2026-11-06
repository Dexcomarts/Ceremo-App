# User schema and migrations

- Entry ID: database/user-schema
- Researched: 20 October 2026
- Status: initial migration, adapter and registration rollback/concurrency verified on 21 October
- Question: how to introduce account/identity tables and prove their constraints on isolated PostgreSQL
- Versions: Java 25, Boot 4.1.1, Gradle 9.8.1; managed Flyway 12.4.0 and Testcontainers 2.0.5; local PostgreSQL 18.0

## Evidence and limits

Latest 22 October run passed all 33 PostgreSQL cases: 14 schema, nine adapter and ten use-case checks, including atomic rollback, unrelated CHECK failure and competing registration. The new constraint-failure check uses DDL within its failed disposable transaction and rolls it back; no migration was added or edited. See [coverage completion](../quality/tests-and-analysis.md#full-coverage-completion-22-october-2026) and [the original composition record](../application/user-registration.md#applied-checkpoint-and-verification).

[Boot 4.1.1 Flyway initializer](https://github.com/spring-projects/spring-boot/blob/v4.1.1/module/spring-boot-flyway/src/main/java/org/springframework/boot/flyway/autoconfigure/FlywayMigrationInitializer.java) invokes migration; use the existing integration rather than another startup runner. [Boot dependency guidance](https://docs.spring.io/spring-boot/how-to/data-initialization.html) requires the Flyway starter and PostgreSQL support module.

[Testcontainers 2.0.5 PostgreSQL test](https://github.com/testcontainers/testcontainers-java/blob/2.0.5/modules/postgresql/src/test/java/org/testcontainers/postgresql/PostgreSQLContainerTest.java) demonstrates a real container/query. [PostgreSQL constraints](https://www.postgresql.org/docs/18/ddl-constraints.html) and [collations](https://www.postgresql.org/docs/18/collation.html) define the schema mechanisms. [Flyway migration history](https://documentation.red-gate.com/fd/versioned-migrations-273973333.html) explains ordered, checksummed versions. Exact source excerpts and licences are linked from [document 08](../../../docs/08-development-and-quality.md#85-short-source-excerpts-for-the-first-guides).

## CEREMO decisions

Match `BaseEntity` columns: UUID ID, TIMESTAMPTZ creation/update dates, nullable actor strings and BIGINT version. Keep UUID generation in existing Java code. Use named identity uniqueness, non-null issuer/subject, a restricted foreign key to `app_user` and an index on `user_id`. Compare identifiers with explicit C collation; never normalise or merge them by email/phone. SQL required-value checks supplement domain validation rather than claiming identical Unicode whitespace rules.

Use only implemented fields plus identity metadata; contact verification and revocation migrations arrive with those features. Initial migration SQL is original CEREMO schema code, not copied company code. Applied shared migrations are immutable; no automatic clean/repair/baseline on unexplained failure.

## Local implementation and verification

Implemented: [initial migration](../../../src/main/resources/db/migration/V202610200001__create_user_accounts.sql) and [UserSchemaIntegrationTest](../../../src/integrationTest/java/io/ceremo/ceremobackend/account/infrastructure/secondary/persistence/UserSchemaIntegrationTest.java). `integrationTest` connects directly to its disposable PostgreSQL container, never to the local shared database. Docker unavailability is a failed required check, not a skip. H2 mapping tests retain their existing isolated schema and do not prove these migrations.

Verified on 20 October 2026: `integrationTest --tests '*UserSchemaIntegrationTest'` passed all 14 cases on disposable PostgreSQL 18.0, with zero failures/errors/skips. Cases cover migration/validation/re-run, unique identity pair, absent parent, distinct issuer/subject/case, restricted deletion, missing/blank identity values, invalid account status and audit/version defaults. The migration is packaged in the executable JAR. No existing application database was migrated. Adapter atomicity, JPA schema validation and concurrent registration remain for the repository stage.

Resolved verification finding: the first run passed 13/14 cases because the restricted-deletion test expected SQLSTATE `23503` instead of the observed `23001`. [PostgreSQL's error reference](https://www.postgresql.org/docs/18/errcodes-appendix.html) distinguishes `restrict_violation` (23001) from `foreign_key_violation` (23503). The assertion was corrected without changing the constraint; the second run passed 14/14. Missing-parent insertion still expects 23503. Preserve this distinction in future tests.

Related: [repositories](../repositories/user-persistence.md), [quality](../quality/tests-and-analysis.md), [database design](../../../docs/06-database-architecture.md).

## Provider account security, 5 November 2026

The initial migration remains unchanged. V202611040001 adds persistent issuer/session revocations; V202611040002 adds tenant, tenant_member and tenant_audit, with FKs, role/status constraints and unique membership. Provider publication/billing tables remain planned. See [the migration and isolation evidence](../ports-and-adapters/provider-account-security.md) and document 06 for the current implementation boundary.


Consolidation, 5 November 2026: current source, ports, adapters and tests are under `account`; composition is `wire/account/AccountConfiguration`. Existing domain behaviour, transactions, HTTP routes and database schema continue. Older package names in dated examples/commands are historical. See [the approved consolidation](../ports-and-adapters/provider-account-security.md#account-module-consolidation-5-november-2026).
