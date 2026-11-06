# Feature ports and adapters

- Entry ID: ports-and-adapters/feature-boundaries
- Researched: 20 October 2026
- Status: architecture rule adopted; implementation is partial
- Question: where authentication, persistence and transactions belong under strict DDD

## Evidence

[Cockburn's architecture](https://alistair.cockburn.us/hexagonal-architecture/) separates application logic from external interaction. [Spring transaction templates](https://docs.spring.io/spring-framework/reference/data-access/transaction/programmatic.html) provide imperative transaction boundaries, but couple their caller to Spring. Our choice is an outer infrastructure decorator rather than introducing Spring into the core.

## CEREMO boundaries

`UserAccounts` is the application inbound port. `CurrentUser` is a plain outbound identity port. The [SpringCurrentUser adapter](../../../src/main/java/io/ceremo/ceremobackend/account/infrastructure/secondary/security/SpringCurrentUser.java) reads authentication already established by shared security. It does not replace JWT signature/issuer/audience validation or perform a second login.

`UserRepository` already acts as an outbound domain port; do not duplicate it under application ports. JPA types stay in persistence adapters. Compose services and transaction boundaries in infrastructure/`wire`. The existing database configuration disables default repository transactions, so an explicit complete use-case boundary is essential.

## Verification and next work

Registration composition and an ArchUnit dependency rule are now implemented and verified. Principal-based adapter tests exist; a real issuer/JWKS flow and the HTTP boundary remain pending. See [the applied checkpoint](../application/user-registration.md#applied-checkpoint-and-verification). Continue checking domain/application dependencies after changes.

Related: [application](../application/user-registration.md), [repositories](../repositories/user-persistence.md), [controllers](../controllers/http-validation.md).

## Runtime wiring inventory and reuse workflow

Reviewed on 21 October 2026. The [wire map](../../wire-package-map.md) inventories all 12 current wire Java files (11 classes plus security package metadata), six configuration areas and five test classes. It records actual beans, property precedence, defaults, package visibility, optional components, source evidence and test limits. Use it alongside the [shared map](../../shared-package-map.md), not as permission to import configuration into the pure core.

The [project-local reuse skill](../../skills/ceremo-shared-wire/SKILL.md) provides the implementation workflow: inspect both maps/current source, select only relevant existing behaviour, identify a real gap, compose externally, then verify. It is explicitly linked from AGENTS.md and Copilot instructions; this is not a claim that every extension automatically discovers or enforces it.

Latest source/test review supersedes the earlier composition gap: the adapter, complete transaction wrapper and production wiring exist and pass PostgreSQL use-case checks. The wire inventory now has 14 main Java files across seven areas and six test classes plus an OIDC fixture. Production actor auditing and named feature caches remain gaps. Async tenant decoration does not propagate a transaction or security principal. OpenAPI requirements and CORS do not enforce record ownership. See [the applied checkpoint](../application/user-registration.md#applied-checkpoint-and-verification) for fresh verification and limits; no Salesync source was consulted.

Context verification on 21 October 2026: all local links in the eight changed/new context files resolve; the wire map covers every one of the 12 main Java files and five wire test classes; `docs/` still contains exactly eight files. The skill's simple name/description frontmatter was checked with PowerShell. The bundled Python skill validator could not run because no Python runtime is installed; no dependency was installed to work around it. No application source, tests, dependencies or runtime configuration changed for this task.

Consolidation, 5 November 2026: current source, ports, adapters and tests are under `account`; composition is `wire/account/AccountConfiguration`. Existing domain behaviour, transactions, HTTP routes and database schema continue. Older package names in dated examples/commands are historical. See [the approved consolidation](../ports-and-adapters/provider-account-security.md#account-module-consolidation-5-november-2026).
