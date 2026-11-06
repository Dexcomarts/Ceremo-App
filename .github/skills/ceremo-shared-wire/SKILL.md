---
name: ceremo-shared-wire
description: Reuse CEREMO shared utilities and Spring wire configuration when guiding, implementing or reviewing backend Java features, while preserving DDD and ports-and-adapters boundaries.
---

# Reuse CEREMO shared and wire

This project-local skill is loaded through the explicit links in AGENTS.md and Copilot instructions. Its location alone does not guarantee automatic skill discovery by an extension. It does not grant permission to edit application code; the user writes code unless the current request authorises edits.

## Load the relevant context

Read [project rules](../../../AGENTS.md), the [research index](../../research/README.md), [shared map](../../shared-package-map.md), [wire map](../../wire-package-map.md) and [security rules](../../security-quality-rules.md). Use the maps as responsibility indexes, then reopen the corresponding current classes, callers, configuration and tests. Source outranks memory. Research the exact public maintainer APIs when recommendations depend on them; Salesync remains excluded unless explicitly requested for the current task.

## Decide what belongs where

| Work | Reuse decision |
| --- | --- |
| Pure domain/application behaviour | Use suitable pure shared assertions, errors and values; keep Spring/JPA/wire imports out. Pass IDs/time through constructors or factories. |
| Persistence adapter | Reuse BaseEntity, existing UUID generation, mapper, migrations and database wiring. Cover the complete use case with an outer transaction; scanning/auditing do not supply that boundary. |
| HTTP/security adapter | Reuse shared authentication/advice and central wire CORS/OpenAPI. Check ownership/status in use cases; CORS and API documentation are not authorisation. |
| Async adapter | Inject the configured executor; verify context and transaction behaviour. Tenant decoration is not propagation of JWT/security context or durable delivery. |
| Cache or logging integration | Check whether a named cache/appender is actually configured and needed. Preserve expiry/invalidation, opt-in lifecycle and secret hygiene; do not add these to unrelated features. |
| Spring composition | Reuse existing beans and property bindings; create missing feature composition in the outer layer. Respect package-private helpers and choose bean visibility/qualifiers deliberately. |

Check both maps for every coding step; apply only components with a demonstrated role. Do not force direct dependencies on both packages or add duplicate configurations merely to satisfy a checklist.

## Before giving or changing code

1. Search the feature, shared, wire and tests for the required behaviour and already registered beans. Identify default properties, conditions, package visibility and any missing composition.
2. Choose reuse, a narrow extension or new code based on the actual gap. Do not recreate UUID generation, errors, authentication, executors, CORS chains, auditing or an OpenAPI document already present.
3. Check trust boundaries and DDD dependency direction. Keep business rules in the core and Spring composition/adapters outside it. No Lombok.
4. Cite exact local files/methods and relevant maintainer evidence. Label adapted versus original examples; do not present proposed wiring as active.
5. Give a short reuse note: **Shared reused; Wire reused or why unnecessary; New code and gap; Verification.** Keep it proportional to the task.
6. If edits are authorised, change only the necessary files, run affected checks and record actual outcomes. If guidance is requested, provide the check for the user without claiming proposed code passed.

## Preserve known defaults

- Since the 5 November 2026 access decision, anonymous discovery is limited to safe catalogue/provider summaries. Full profiles, detailed reviews and contact release require the existing ACTIVE-account check. Keep tenant member/owner/operation checks in use cases; a registered customer or role claim grants no private business access. Reuse the account HTTP authorisation adapter when relevant; do not restore the historical public-profile allowlist.

- Repository defaults are disabled; atomic registration still needs explicit outer composition.
- Embedded JPA tests replacing Hikari must not inherit an incorrect provider-disables-auto-commit promise. Preserve the existing rollback regression checks.
- Cache customizer is empty; `createCache` clears an existing cache without changing its configuration. A manager bean does not mean a feature cache exists.
- Keep one CORS integration, servlet duplicate registration disabled, explicit origins and browser CSRF protection. Reuse validated identity adapters rather than accepting identity keys from request bodies.
- Timestamp auditing exists; a production actor auditor is still missing. Do not invent configured actors.
- Provider businesses now use the `tenant` module with explicit database ownership/membership checks. Optional inherited organisation-claim filters stay disabled; their tier/location claims are not permissions or billing rules. Logstash remains opt-in. Read [the account/logout/tenant evidence](../../research/ports-and-adapters/provider-account-security.md) before adding protected business operations.

## Write readable, source-backed Java

Reviewed: 4 November 2026. Apply this workflow when the user requests implementation or guidance. These are CEREMO instructions; they do not certify authorship or engineer seniority.

1. Start with one observable business operation and its failure cases. Reopen the closest current CEREMO class and test before selecting a public reference. Preserve established package, DTO and method names unless a reviewed migration changes them.
2. Read exact maintainer source and related tests. Record release/commit, licence, dependencies and differences from Java 25/Boot 4.1.1. Explain what is adapted and what is original CEREMO policy. A popular repository is a candidate, not approval to copy it.
3. Use explicit constructors, narrow methods and ordinary control flow. Prefer named business operations such as `activate()` to arbitrary status setters. Use transport records for declared fields; keep persistence entities and security claims outside the public response. No Lombok, extra commands, generic factories or layers without a concrete need.
4. Put invariants in the domain, orchestration and caller checks in the application, and HTTP/JPA/Spring mechanisms in adapters and wire. Reuse shared UUIDs, assertions, errors, identity reading, JPA auditing/versioning and existing transaction composition where suitable.
5. Test the actual rule, including denied access and meaningful failures. Use PostgreSQL for constraints, commit/rollback and concurrency. Distinguish mocked-principal security tests from signature validation or live Keycloak. Preserve framework error headers and committed-response handling; a generic fallback must not consume Spring Security denials.
6. Review changed code for unnecessary indirection, repeated logic and comments that merely restate it. Keep helpful names and explanations of non-obvious constraints. Readability changes must preserve authorisation, validation, transaction behaviour and the mandatory coverage scope.
7. Run affected checks, then the complete `check bootJar` when the feature is ready. Record same-revision regular/PostgreSQL results and per-class JaCoCo counters. Report SonarQube separately. Update the owning research entries, maps and API documentation.

References: [Spring Petclinic controller and tests](https://github.com/spring-projects/spring-petclinic/tree/main/src), [Spring MVC exception resolution](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html), and [Anthropic code-simplifier guidance](https://github.com/anthropics/claude-plugins-official/blob/main/plugins/code-simplifier/agents/code-simplifier.md), reviewed as reference material only. Petclinic's HTML/entity binding and direct repository calls do not fit CEREMO's inbound-port boundary. The simplifier's language-specific conventions and persona claims are not adopted. These evolving references were observed on the review date; no external plugin, hook or remote script was installed. See the [HTTP evidence record](../../research/controllers/http-validation.md#account-http-implementation-4-november-2026) for the exact source assessment and verification.

## Keep memory honest

When source/behaviour changes, update the relevant map and owning research entry with date, evidence and actual checks. Keep inventories outside the approved product documentation set. Never store secrets or turn an inspected test into a claimed execution result.
