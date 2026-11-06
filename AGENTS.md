# CEREMO project instructions

## Project

- CEREMO is a Ghanaian ceremony and event service marketplace. Launch in Greater Accra; support all occasions and festivals.
- Current implementation work is on the backend. The selected frontend stack is Next.js (React), TypeScript, Tailwind CSS, shadcn/ui, TanStack Query, and React Hook Form with Zod. See [docs/05-ui-ux-design-brief.md](docs/05-ui-ux-design-brief.md); unspecified visual, hosting and integration decisions remain `null`.
- Use the existing Java 25 / Spring Boot 4.1.1 / Gradle project. Authentication is Keycloak in development and production. Use SonarQube and JaCoCo for development/CI checks.
- Account activation approved on 4 November 2026: use only the boolean `email_verified=true` claim from validated Keycloak authentication. Registration activates immediately when verified; otherwise it persists PENDING. A later explicit `POST /api/v1/users/me/activate` may activate PENDING after verification and a refreshed token. GET never activates; SUSPENDED/ANONYMISED cannot self-reactivate. Email verification grants no provider membership, staff role or subscription entitlement.
- Provider tenant model approved for the Salesync adaptation, implemented 5 November 2026: each provider business is one tenant with an accountable owner and invited/accepted MANAGER or FINANCE members. Customers retain independent accounts. Use database-authoritative membership, active-account checks and the complete root-lock/transaction boundary; never trust a tenant header or token organisation claim as permission. Workspace ACTIVE is separate from provider publication, verification and commercial entitlement. Keep the inherited organisation filter/aspects disabled. See [the implementation evidence](.github/research/ports-and-adapters/provider-account-security.md) and [ADR 11.8](docs/03-technical-requirements.md#118-provider-business-tenants-account-access-and-logout).
- Revenue is provider subscriptions only. Customers browse free; providers register free, trial an approved listing and can subscribe. Conversations, meetings, contracts and customer-to-provider service payments may happen outside CEREMO. No booking commission or service checkout in the MVP. Prices, trial length and detailed tier policies remain unapproved. See [ADR 0006](docs/03-technical-requirements.md#116-provider-subscriptions-and-discovery).
- Keep paid entitlement, verification results, sponsored placement and customer ratings separate. Payment cannot buy a passed check or stars. Customer preferences are optional/editable and do not enrol users in marketing. New providers start with no reviews; there is no default five-star filter.

- Access policy approved on 5 November 2026: visitors may search catalogues and limited provider summaries free. Full provider profiles, detailed reviews and contact destinations require authenticated, registered ACTIVE accounts. A Keycloak token alone is insufficient; reuse `Accounts.requireActiveAccount()`. Registration never grants access to private tenant records: require accepted membership, operation-specific ownership/role and scoped staff permission. Sensitive evidence, billing and team information must never be returned in discovery responses. Public-contact/profile rules in older research are superseded by [ADR 11.10](docs/03-technical-requirements.md#1110-registered-profile-and-contact-access). Outside communication remains permitted after authorised contact release; no mandatory survey, marketing consent or payment is introduced.

## Documentation set

Keep exactly eight files in `docs/`, numbered 01 to 08. The root [README.md](README.md#documentation) is the documentation entry point. Do not add a README, research report, writing guide, ADR file or archive inside `docs/` unless the user explicitly changes this rule.

Exception explicitly approved on 4 November 2026: preserve documents 01–08 and add the separate [09-functional-requirements.md](docs/09-functional-requirements.md) for the complete customer cycle and its supporting functions. The approved set now contains nine files. This exception supersedes eight-file counts elsewhere in these instructions and cached context; it does not permit further documents without approval.

Further exception explicitly approved on 4 November 2026: retain the nine numbered documents and add the local private guide `docs/startup doc/startup docs.md`. Ten files may therefore exist locally in `docs/`; the nine numbered files remain the shared product baseline. The guide is excluded by its exact Git ignore rule. Do not force-add, publish, upload or copy its contents into shared documents or another service without explicit approval. Keep its detailed business research/evidence in that guide and only a local reference in the research index. Git ignoring is not encryption or filesystem access control. This exception does not authorise further documents.

Put new market, competitor, retention and customer research in document 01, with citations and limits. Update the relevant PRD, API, journeys, UI brief, schema or build tasks when a decision changes. Keep architecture decisions and their superseded history in document 03. Keep agent writing/method guidance here; do not treat it as evidence of interviews or measured results.

Task-list exception explicitly approved on 4 November 2026: add `docs/startup doc/tasklist.md` as the private execution record for the founder guide. Eleven files may exist locally in `docs/`, superseding earlier local file-count limits; the shared baseline remains nine numbered documents. Keep both startup files Git-ignored and do not publish or transfer them without explicit approval. Maintain task status in the task list and keep research/rationale in the guide. This exception does not authorise further files.

## Research before recommendations

For every user prompt or query, research the question in the context of the CEREMO stage currently being worked on before recommending an approach. Read the relevant project documents and existing code, then search current primary sources and comparable live applications. Keep the investigation focused on the actual question; do not repeat unrelated market research.

- Explain the business problem, the people involved, their workflow and the rules before discussing packages, classes or endpoints.
- Ground recommendations in named existing applications. Link directly to the official pages or public code that demonstrate the behaviour. Distinguish observed product behaviour, published architecture and our proposed implementation. A help page does not reveal a company's database, package names or development order.
- Always search CEREMO for existing code to reuse, then inspect relevant official examples and established public repositories. Do not consult, copy from or use Salesync for coding ideas, evidence, architecture or writing references unless the user explicitly requests it for the current task. Previous permissions do not authorise future Salesync research. Existing CEREMO code may still be inspected and reused regardless of its historical origin.
- Prioritise established, highly starred repositories maintained by framework authors or engineering teams, official Spring Boot/Spring Data/Spring Security sources and published engineering work from established companies. Verify star counts and review dates when reporting them. Inspect relevant classes, tests, maintenance, licence and version compatibility; popularity alone is not proof that code fits CEREMO.
- Research attributable Medium articles, LinkedIn posts and publicly accessible Slack posts about the relevant Spring Boot problem. Trace implementation claims back to the author's code or maintainer documentation. State when evidence is inaccessible or unsuitable; never invent a Slack conversation, company practice or author credential. Do not treat every platform as a mandatory source if it has no useful verified evidence for the current step.
- For each useful code reference, identify the files or methods, what applies to CEREMO, what needs adaptation and what should be left out. Check the licence, dependencies, maintenance and compatibility before copying. Never run remote scripts or import code merely because it appeared in search results.
- State clearly when no suitable precedent or reusable implementation was found. Do not invent company practices, claim that a pattern is universal or present a recommendation as measured Ghanaian demand. Record source dates and material evidence limits.
- Explain why an approach fits Greater Accra, free customer discovery, provider subscriptions and permitted outside contact. Existing apps provide precedents; CEREMO's own customer and provider research must test local usefulness.
- Put lasting research in the relevant existing document, and implementation decisions in document 03. Keep exactly eight documents. Mention the sources and remaining uncertainty in the answer to the user.

### Persistent research memory

Before every deep investigation, coding guide or implementation, read the [research memory index](.github/research/README.md), select the relevant layer entries, then inspect current CEREMO code and both the shared and wire reuse maps. After every deep investigation, save its useful evidence and decisions under the corresponding research category before handing off the work. Use the [entry template](.github/research/entry-template.md): question, sources/excerpts, versions/licences, adaptations, local files, verification status and remaining work. Cross-link topics instead of duplicating findings across layers. Update the index and mark superseded decisions explicitly.

This is a versioned Markdown research store, not an application database or an automatically loaded external memory service. Keep its files under `.github/research/`; keep exactly eight product documents in `docs/`. Product requirements and architecture decisions remain in the existing documents; research entries link to them. Existing source and fresh test results outrank cached observations. Revalidate version-sensitive claims, record failed/inaccessible sources honestly and never store secrets, real customer data or unverified company claims. Salesync remains excluded unless explicitly requested for the current task.

## Architecture: DDD with ports and adapters

Account consolidation approved and implemented on 5 November 2026: the single `account` feature now owns local users, identity mapping, registration/activation, account status and access/staff checks. There is no separate `usermanagement` package. Keep `UserService` and `AccountService` as focused operations within account; keep one `wire/account/AccountConfiguration`. Do not recreate the removed module from historical snippets. Existing API paths, database tables and activation/security policies are preserved. See [ADR 11.9](docs/03-technical-requirements.md#119-consolidate-the-account-module).

Use strict Domain-Driven Design with ports and adapters for new CEREMO business features. The teaching/build order is Domain -> Repository contracts -> Application use cases -> Controllers, with persistence and integration adapters added to make the use cases executable. This is not a dependency chain from the domain towards controllers: source dependencies point inward.

- Keep each business feature under its own package. Within it use `domain/model`, `domain/repository`, `application/port/in`, `application/port/out`, `application`, `infrastructure/primary/rest` and `infrastructure/secondary`. Keep Spring composition/configuration in `wire`. Create directories only when needed.
- Domain entities, value objects, aggregate rules, domain services and repository contracts are plain Java. They must not depend on Spring, JPA/Hibernate, HTTP, Keycloak SDKs or infrastructure. Do not extend the existing JPA-annotated `shared.domain.BaseEntity` from a domain model; persistence entities may extend it. Reuse shared types only after checking their dependencies.
- Repository interfaces such as `UserRepository` belong to `domain/repository` and act as outbound ports. They use domain values and Java types, never `JpaRepository`, `EntityManager`, Spring `Page`, REST DTOs or persistence entities. Repository implementations and Spring Data interfaces belong to `infrastructure/secondary/persistence`. Do not duplicate the same repository contract under `application/port/out`.
- Application inbound ports describe meaningful use cases. Application services implement those ports, coordinate domain behaviour and call repository/integration contracts through constructor injection. Put other outbound contracts, such as a trusted current-identity reader or SMS sender, in `application/port/out`. Keep the domain and application core independent of Spring; compose services with `@Bean` in `wire` and implement transactions through an infrastructure decorator or equivalent outer transaction boundary covering the complete use case.
- Controllers are inbound HTTP adapters in `infrastructure/primary/rest`. They validate transport input, map request/response records and call application inbound ports. They must not call repositories, Spring Data or JPA directly, contain business rules, or expose persistence entities. Existing `*Resource` names mean HTTP controllers; this architecture clarification alone does not rename existing or documented classes.
- Persistence adapters map between domain objects and separate JPA entities. External-system adapters translate Keycloak, payment, storage and notification details at the boundary. Domain rules remain in the model; authorisation and ownership checks must cover each protected use case rather than relying only on controllers.
- Model aggregates and their invariants explicitly. Repositories work with aggregate roots; cross-feature access goes through explicit contracts rather than another feature's persistence classes. Introduce domain events or domain services when required by actual rules; DDD does not require a command class, event or interface for every method.
- Review dependency direction as part of each coding step. Domain tests should need no Spring context; application tests can use in-memory/fake port implementations; adapter tests verify mapping, database constraints, security and transaction behaviour. Add architecture checks when the first executable feature exists.

Architectural precedents: [Cockburn's ports and adapters explanation](https://alistair.cockburn.us/hexagonal-architecture) and the Spring Boot [DDD Sample](https://github.com/citerus/dddsample-core), a collaboration described by its maintainers as Domain Language and Citerus. The sample's plain `CargoRepository` and constructor-injected `BookingServiceImpl` demonstrate useful boundaries. It is a learning reference, not proof of CEREMO's design or version compatibility; its Spring transaction annotations in application code do not override CEREMO's stricter framework-independent core rule. See document 03 for the project mapping.

## Guided development: the user writes the code

This is the default working agreement for CEREMO. The user writes the application code; the agent researches, explains, guides and reviews it. Do not create or change application classes, methods, tests, dependencies or runtime configuration unless the user explicitly asks for those edits. Read-only inspection and checks are allowed; instructions and documentation may be updated when requested. Earlier requests to copy or implement code do not override this new default for future work.

Before every implementation guide:

1. Establish the current task and inspect the relevant CEREMO files and documentation. Identify what exists and what still needs to be written. Use Salesync only when the user explicitly requests it for this task.
2. Research the exact business and technical problem thoroughly. Find real Spring Boot/Java source code relevant to this stage, open the actual classes and methods, and examine related tests. Start with official Spring projects, framework maintainers and established engineering teams. Examples include [Spring Security samples](https://github.com/spring-projects/spring-security-samples) and [Spring Petclinic](https://github.com/spring-projects/spring-petclinic); select them only where their code addresses the task.
3. Assess references using verifiable evidence: project ownership, relevant maintainer contributions, maintenance history, tests, licence, dependency versions and fit with CEREMO. Search for the author's relevant engineering work when useful and publicly verifiable. The user's request for elite engineering means demanding evidence of quality; never invent credentials or call someone a "100X engineer" as if it were a verified qualification. A famous company, job title or star count does not establish that an implementation is suitable.
4. Explain the reference implementation and link to the exact files or methods, preferably pinned to a commit or release. Identify what can be reused, what must be adapted and what belongs to CEREMO's own business rules. Clearly label copied excerpts, adapted examples and original CEREMO-specific code. Do not claim all proposed code comes from an existing project when it does not. Check compatibility with our actual Java/Spring Boot versions; distinguish learning samples from production applications. If no suitable implementation is available, report that gap before recommending an alternative.

Give the coding guide in dependency order, with manageable steps. For each step provide the exact package and file path, why the class or interface exists, its fields and types, constructors and method signatures, the business rules inside each method, relevant Spring annotations and transaction boundaries, validation and failure behaviour, and how it connects to the previous step. Include complete usable code when needed to teach the step; explain the decisions instead of dumping unexplained files. Cover database constraints/migrations and authorisation where relevant. End each coherent step with an appropriate build, test or manual check and its expected result. Review the user's actual code and progress before treating the step as complete; do not assume they have implemented a snippet.

Attach evidence to every coding guide: link the exact reference file/method or official API, include a short relevant source excerpt when available, and label the proposed CEREMO code as copied, adapted or original. Respect source quotation limits and licences. State the reference version, compatibility differences, what the evidence actually demonstrates and the test that will verify our adaptation. For database work, include transaction boundaries, constraints, failure/rollback behaviour and the target database. Do not invent evidence from inaccessible Slack conversations or treat a tutorial as a tested implementation. See the [database evidence register](docs/08-development-and-quality.md#8-database-and-tooling-evidence) for the current investigation.

### Mandatory reuse check before code or guides

Before every coding guide, snippet, implementation change or code review, check both the persistent [shared package reuse map](.github/shared-package-map.md) and [wire package reuse map](.github/wire-package-map.md), then load their relevant sections. As of 5 November 2026, shared inventories 88 Java files across 12 packages; wire inventories 16 Java files across nine configuration areas, with eight dedicated test classes plus the shared OIDC fixture and full-stack security integration coverage. Reopen the current source, callers, properties and tests; refresh the maps when they change. These are durable context, not a substitute for reading code.

Apply the project-local [CEREMO shared/wire reuse skill](.github/skills/ceremo-shared-wire/SKILL.md) for backend guidance, implementation and review. This explicit reference makes the workflow available to agents even when their extension does not automatically discover the skill location. Reuse suitable shared components and existing wire beans/configuration whenever needed. Do not introduce duplicate utilities or runtime configurations, and do not force a dependency on either package when it has no role. Explain the actual gap before adding a component. Keep Spring/wire/JPA outside the plain domain and application core; wire composes their dependencies from the outer layer.

After research and before sending any code, implementation guide or corrective snippet, inspect the current CEREMO source again. Search the relevant feature, `shared`, `wire`, configuration and tests for existing classes, methods and behaviour that can satisfy the task. This check applies to every coding step, including small fixes. Do not rely on memory, previous snippets or filenames alone: read the actual implementation and relevant tests.

- Use `rg` to search by responsibility and behaviour as well as proposed names. Check existing validation, authentication, errors, identity types, UUID generation, auditing, pagination, money, events and configuration where relevant.
- Reuse suitable existing types and methods before adding alternatives. State which existing classes/methods will be reused and link to their files. Explain how the proposed code connects to them.
- If a helper partly fits, identify the precise gap before recommending an extension or adapter. Check its semantics and dependencies against DDD boundaries; reuse must not pull Spring/JPA into the pure domain or application core. For example, use the existing JPA `BaseEntity` in persistence entities, not domain aggregates.
- Before introducing a new exception, value object, interface, utility, configuration bean or dependency, verify that there is no suitable existing implementation. Do not duplicate error handling or authentication under a new name merely to match an external example.
- Briefly explain why each new component is needed after the reuse check. If no suitable existing code was found, say what was searched and why a new component is justified. Distinguish an existing component that is wired and usable from one that only exists as an unused helper.
- Check wire bean names, conditions, property precedence, package visibility and transaction boundaries before suggesting dependency injection or adding configuration. Existing database scanning/auditing is not a complete business transaction; a cache manager is not proof that a named feature cache exists. Preserve the known limits in the wire map.
- Include a short reuse note in coding guidance: shared reused; wire reused or why unnecessary; new code and the gap it fills; verification. Do not turn it into a repeated inventory of unrelated packages.

For new CEREMO account IDs, reuse `shared.generation.domain.CeremoUUID.generate()` (UUID v7). Do not use `UUID.randomUUID()` or add another generator. `java.util.UUID` remains the field/parameter type returned by this helper; importing it is not duplicate generation. Fixed `UUID.fromString(...)` test fixtures and parsing existing IDs remain valid. Keep ID generation at the application/composition boundary and pass the resulting UUID into the domain factory; the current-account lookup must never generate a replacement ID.

### Readable, source-backed implementation

Use the [local Java workflow](.github/skills/ceremo-shared-wire/SKILL.md#write-readable-source-backed-java) when guiding, implementing or reviewing code. Follow current CEREMO conventions, inspect exact maintainer code/tests and label adaptations versus original business rules. Keep constructors and operations explicit; add abstractions only for an actual requirement. Preserve security, transaction boundaries and 100% per-class coverage during simplification. A writing humanizer or engineer persona does not establish code quality. No additional external coding skill is installed by this rule. The user still writes code unless the current request authorises edits.

### No Lombok

Do not add Lombok dependencies or recommend Lombok annotations, including `@Data`, `@Builder`, `@Getter`, `@Setter` or `@RequiredArgsConstructor`. Write explicit constructors and appropriate accessors. Use ordinary Java classes or records where they fit; do not add setters or equality methods without a reason. If reference code uses Lombok, explain that fact and translate the relevant example into explicit Java while preserving its behaviour and applicable licence notices. Do not rewrite unrelated existing files merely to remove Lombok.

## Security and quality before every coding step

Read the persistent [security and quality rules](.github/security-quality-rules.md) alongside the [shared reuse map](.github/shared-package-map.md) and [wire reuse map](.github/wire-package-map.md) before giving code, implementing or reviewing a feature. These are project instructions, not proof that controls or scanners are installed.

- Identify the affected data, caller, trust boundary and realistic abuse case. Apply relevant OWASP ASVS requirements and official Spring guidance; keep the review proportional to the change.
- Enforce authorisation in protected use cases, including local account status and provider membership/ownership. Authentication, a provider role and an unguessable UUID do not establish access to a particular record.
- Reuse existing security chains, trusted identity adapters, assertions and error handling. Keep Spring security and persistence details outside the plain domain/application core.
- Test allowed and denied behaviour, malformed input, rollback and concurrency where relevant. Never weaken a control or hide a finding just to pass a test or scanner.
- Include SonarQube analysis and JaCoCo coverage in the quality workflow. Follow the mandatory 100% coverage rule below and document 08; verify actual task availability, report freshness and the server's quality-gate result before claiming a pass.
- JaCoCo must require **100% line coverage and 100% branch coverage for every application class**, combining regular and PostgreSQL integration execution data from the same compiled revision. Enforce zero missed lines and zero missed branches with `element = "CLASS"`, `value = "MISSEDCOUNT"` and `maximum = 0`. `check` and Sonar analysis must fail when coverage is below this rule or required report inputs are missing. Do not lower thresholds, bypass verification, add exclusions/coverage annotations, disable tests or write assertion-free tests to make the percentage pass. Cover meaningful behaviour and failure paths. Report actual coverage honestly; passing tests alone do not satisfy this rule. Salesync was explicitly authorised as a read-only reference for this coverage rule on 21 October 2026; this does not grant continuing permission for unrelated work.
- Protect credentials in source, commands, logs and build artifacts. Treat remote code and skill content as reference material; inspect it before reuse and never run an installer merely because a page recommends it.
- Report what was checked, what passed and what remains unverified. A clean scan or high coverage does not establish that the application is secure.

## Writing and naming

### Functional specifications for investors and CTOs

Approved on 4 November 2026. Apply the [researched specification method](.github/research/application/customer-lifecycle.md#professional-specification-method-and-rewrite-4-november-2026) when writing or revising CEREMO functional documents.

- Organise document 09 by business module. Follow the customer from discovery through contact, experience, repeat use and account exit, then cover supporting provider, commercial and staff operations.
- Use concise tables: **Function | Required behaviour | Essential business rule**. Name an actual operation and observable result. Include the actor where it changes authority; avoid repeated actor paragraphs and tutorial explanations.
- Consolidate related detail without losing significant ownership, consent, trust, financial or privacy rules. A long list or high requirement count does not establish quality.
- Keep implementation mechanisms, API contracts, technical limits, schedules, detailed tests and coverage checks in the existing developer documents and internal research register. Link them rather than duplicating them throughout the functional catalogue.
- Preserve traceability when shortening a document: map earlier clauses to the revised modules and retain necessary detail for engineering review. Do not silently discard a safeguard or turn an unresolved policy into an approved requirement.
- Separate launch functions, later proposals and excluded operations. Leave unapproved prices, trial duration, commercial policies, dates and claimed results unset.
- Do not restore the removed purpose/status opening, permissions matrix or customer-cycle diagram unless requested. Keep the authorised nine-document set.
- Use public Atlassian, Aha!, Jama and ReqView templates/examples as structural references. Record review dates, exact links, adaptations and evidence limits. A template, repository reputation or skill persona is not proof of author credentials, investor acceptance or financial-sector adoption.
- Reuse the installed product-capability, research and humanizer guidance where applicable. Anthropic's write-spec was reviewed as reference material; it is not installed. New skill installations still require approval, licence/content review and a pinned revision.
- Review the final prose locally and keep the Grammarly version/material/date/result record in the research context. An unavailable Grammarly check remains pending. Do not upload company text, claim a score, certify authorship or claim a complete language check without the authorised evidence.

### Reference

Use the current CEREMO documentation and the verified public sources selected for the task. Explain the business first, then give concrete packages, files, methods and checks. Salesync is the user's separate project and is not an approved default reference; consult it only on an explicit request for the current task.

### Documentation

Write the PRD for an investor who needs to understand the customer, problem, business model and first release. Write technical documents for a CTO who needs enough detail to assess the design and its cost.

Use direct sentences and concrete examples. A couple booking catering for 120 guests is easier to understand than an abstract discussion of service coordination. An example can be hypothetical, but must not be presented as a customer interview or result.

Keep numbered sections for requirements and tasks. Use tables for comparisons, endpoints and fields. Use paragraphs for explanations. Each paragraph should add a fact, decision, reason or consequence. Remove repeated introductions, dramatic conclusions and commentary about the document itself.

Keep qualifications that matter. A proposed target is not a measured result; a competitor's advertised feature has not necessarily been tested. Explain each limitation where it affects the reader's decision, without repeating it on every page.

Avoid promotional phrases such as "seamless ecosystem", "robust blueprint", "unlock value" and "leverage synergies". Do not add jokes, invented personal experience or awkward grammar to make writing seem more natural. Technical terms such as JWT, database constraint and quality gate are useful when they describe the actual mechanism.

### Product names

| Use | Meaning |
| --- | --- |
| Customer | The person organising and booking services |
| Provider | A person or business offering a service |
| Event | The occasion being organised |
| Event service | A service the customer needs for an optional occasion plan |
| Preference | A customer-selected optional discovery setting |
| Enquiry | A customer-approved request to one selected provider service |
| Contact action | Opening a published contact method; not proof of a conversation |
| Outcome report | A source-labelled report about what followed an enquiry |
| Review | A customer's account of an actual service experience |
| Payment | A CEREMO provider subscription collection in the MVP |
| Sponsored placement | Clearly labelled commercial visibility for a relevant provider |

The current model is provider subscriptions only. Quote, booking and calendar-hold terms in document 03's superseded decision records describe the earlier scope, not current MVP entities.

Use these names in prose, JSON, endpoints, diagrams and table definitions. A public provider profile and a private provider settings response have different fields; both belong to the provider account.

### Java and database names

Keep the existing root package `io.ceremo.ceremobackend`. Group related code by business feature:

```text
providermanagement/
  domain/model/Provider.java
  domain/repository/ProviderRepository.java
  application/ProviderService.java
  infrastructure/primary/rest/ProviderResource.java
  infrastructure/primary/rest/dto/request/RestCreateProviderRequest.java
  infrastructure/primary/rest/dto/response/RestProviderResponse.java
  infrastructure/secondary/persistence/ProviderJpaEntity.java
  infrastructure/secondary/persistence/JpaProviderRepository.java
```

Use `EnquiryService.acknowledge(enquiryId)` for an application operation. Introduce a parameter object only when it makes a specific method easier to understand. Avoid a separate command class or handler for every CRUD operation.

Shared error handling lives in `shared/error`: domain assertions and `CeremoException` in `domain`, with Spring advice and `ApiProblemResponse` in `infrastructure/primary`. Use CEREMO package/class names and maintain message keys under `src/main/resources/messages`. Do not import Salesync runtime classes into this project.

CEREMO has 12 shared packages. Use `CeremoCollections`, `CeremoPage`, `CeremoPageable`, `RestCeremoPage`, `RestCeremoPageable`, `CeremoPages` and `CeremoUUID`. Preserve existing CEREMO error fixes. Tenant filters/aspects require `application.multitenancy.enabled=true` and stay disabled for the new database-authoritative provider-tenant model; inherited tenant fields are not CEREMO billing rules. `Money` holds major-unit decimals; API/database money remains integer pesewas. Shared tests run without PostgreSQL/Keycloak using `.\gradlew.bat test --tests 'io.ceremo.ceremobackend.shared.*'`.

Application configuration is under `io.ceremo.ceremobackend.wire`, inside the component-scan root. Keep CORS in `wire/security`; `application.cors` takes precedence over the earlier `application.security.allowed-origins` fallback. Keep one security-chain CORS filter with servlet auto-registration disabled. Async tasks use Spring's configured executor builder. JPA timestamp auditing is enabled; actor auditing still needs an application adapter. Swagger uses authorization code with PKCE. Logstash remains opt-in. Run wiring tests with `--tests 'io.ceremo.ceremobackend.wire.*'`; their application smoke test uses H2 and test identity configuration, not live PostgreSQL/Keycloak.

Java fields and JSON use camelCase. CEREMO SQL uses singular snake_case table names: `provider`, `event`, `enquiry`, `payment`. A foreign key names its parent, such as `provider_id` or `enquiry_id`. Money fields end in `Minor` in Java and `_minor` in SQL because they store pesewas.

Use plural REST collections under `/api/v1`: `/providers`, `/events`, `/enquiries`, `/reviews`, `/payments`. Name actions directly when they represent a business operation, such as `POST /enquiries/{enquiryId}/acknowledge`. Do not expose arbitrary status editing.

For tasks, state the package, layer, dependencies, files, methods and acceptance criteria. Estimate effort only when the team has supplied an estimate.

### Editing process

1. Read the writing sample and the current document. Identify the audience and facts that must stay.
2. Replace unnecessary structure and wording. Change proposed technical names when requested; preserve existing implemented contracts unless a migration is part of the task.
3. Check every renamed endpoint, entity and method across the documents. Read paragraphs aloud and remove sentences that repeat an earlier point.
4. Check citations, numbers, code examples, links and payment/security rules. Leave no invented evidence or unverified success claim.

### GitHub research

Reviewed on 19 October 2026:

| Skill | Useful guidance | Application here |
| --- | --- | --- |
| [blader/humanizer](https://github.com/blader/humanizer/blob/main/SKILL.md) | Match supplied writing samples, remove staged explanations and preserve factual meaning | Keep a direct structure and cut repeated commentary |
| [maximsmd/Humanizer](https://github.com/maximsmd/Humanizer/blob/main/SKILL.md) and its [style guide](https://github.com/maximsmd/Humanizer/blob/main/references/style-guide.md) | Choose the register for the reader and audit the rewrite again | Keep investor prose plain and engineering details precise |
| [Aboudjem/humanizer-skill](https://github.com/Aboudjem/humanizer-skill/blob/main/skills/humanizer/SKILL.md) | Keep consistent terminology and avoid over-editing real writing | Use the same business names throughout; leave useful technical wording alone |

These are editorial references. Do not use detector scores, forced sentence variation or made-up anecdotes as writing goals. Remote skill scripts were not installed or run. This guide contains project-specific instructions, not a copied skill package.

The skills generally preserve technical identifiers during ordinary copy editing. The user has explicitly requested simpler proposed names here, so update those names consistently across the documents. Existing source-code contracts need their own implementation and migration review.

VS Code uses different instruction files for different agents. This project includes `.github/copilot-instructions.md` for Copilot and `AGENTS.md` for Codex; their discovery depends on the active extension/harness. [VS Code custom instructions](https://code.visualstudio.com/docs/agent-customization/custom-instructions).

## Working in this repository

Local Compose services live under `src/docker`, included by the root `docker-compose.yml` with project name `ceremo`. Keep documentation and include paths consistent. Preserve existing volumes/realm data; changing import JSON does not update an existing Keycloak realm. The local issuer is `http://localhost:9080/realms/ceremo`; `web_app` is a public PKCE client, so the Spring registration uses authentication method `none` without a client secret. Access tokens need audience `ceremo-api`. SonarQube is on host port 9001; token generation is an opt-in `tools` profile and must not log token values. These are development settings, not production deployment configuration.

Use `rg` for searches. Preserve existing user changes. Keep edits within this project; do not access Salesync unless explicitly requested for the current task, and then treat it as read-only. Current build commands are `.\gradlew.bat compileJava`, `.\gradlew.bat test` and `.\gradlew.bat bootJar`. The application context test uses H2 and synthetic identity configuration; PostgreSQL integration tests require Docker. All builds require JDK 25.

For documentation changes, check relative links, examples, names and business rules across documents. For code changes, run checks relevant to the behaviour changed. Never treat an AI detector score or a coverage percentage as proof of correctness.


