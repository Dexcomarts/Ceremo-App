# Documentation suite: decisions, evidence and release acceptance

- Entry ID: product/documentation-suite
- Reviewed: 4 November 2026
- Status: researched; documentation revised and locally verified; Grammarly pending
- Question: how to apply the revised PRD's professional standard across CEREMO's remaining documents and remove every flowchart
- Scope: documents 01 and 03–09, root README, this local research entry and the index; no application, migration, build or runtime changes

## Authority and presentation rules

The user explicitly requested the same improvements across the remaining documentation and removal of all flowcharts. This authorises the document revisions and local research record. The approved product set remains nine files; no extra product document, installation, remote memory or upload is required.

Use investor-readable decisions and evidence at the start of each subject, with CTO/developer detail retained where it belongs. Make outcomes, constraints, tradeoffs, failure behaviour and acceptance testable. Keep proposed policy, source observations, implemented code and dated verification distinct. No universal Silicon Valley format or investor approval is claimed.

For the product documentation set, express workflows, transitions and relationships in prose/tables. Removed all five Mermaid blocks: customer flow, subscription sequence and entitlement state diagram in document 04, and two database relationship diagrams in document 06. A directory tree is a file inventory, not a runtime flowchart. Do not reintroduce diagrams by default in future revisions of this set.

## Evidence and skills

Primary sources reviewed on 4 November 2026:

| Source | Finding applied | Limit |
| --- | --- | --- |
| Team Sequoia, [Writing a Business Plan](https://sequoiacap.com/article/writing-a-business-plan), 15 March 2019 | Clear purpose, customer pain/current alternatives, market, competition and business model make a commercial case assessable. | Pitch guidance, not a prescribed architecture/specification format. Do not invent traction, market size, team credentials or financial projections. |
| arc42, [Template overview](https://arc42.org/overview/) and [quality requirements](https://docs.arc42.org/section-10/), living guidance | Separate goals/constraints/context, strategy, decisions, quality scenarios and risks; describe observable responses and acceptance. | Inform structure without copying the template. The pages identify CC BY-SA 4.0; no template text/code is reproduced or licensed into CEREMO. |
| AWS, [Consistent operational readiness review](https://docs.aws.amazon.com/wellarchitected/latest/operational-excellence-pillar/ops_ready_to_support_const_orr.html) and [ORR tool](https://docs.aws.amazon.com/wellarchitected/latest/operational-readiness-reviews/the-orr-tool.html), living guidance | Review release quality, operational risks, procedures and ownership before exposure; tailor review questions to real failure modes. | A published Amazon/AWS mechanism, not an AWS hosting choice, external certification or evidence that CEREMO has passed. |
| W3C WAI, [Form notifications](https://www.w3.org/WAI/tutorials/forms/notifications/), living tutorial | Provide clear submission feedback and practical field-error recovery. | Supports proposed UI acceptance; no assembled frontend or accessibility conformance result exists. |
| GOV.UK, [In-depth interviews](https://www.gov.uk/service-manual/user-research/using-in-depth-interviews), living service manual | Investigate users' circumstances/work and combine interviews with observed tasks where suitable. | Method precedent; proposed CEREMO sample sizes and findings are not supplied by this page. |
| Spring Security, [JWT resource server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html), current reference | Token validation and downstream business permission are separate responsibilities. | Reference mechanisms do not prove a live CEREMO issuer/JWKS exchange. |
| PostgreSQL, [18 constraints](https://www.postgresql.org/docs/18/ddl-constraints.html) | Constraints provide database-enforced integrity; cross-record rules need appropriate design rather than arbitrary row CHECK expressions. | The version-17 production proposal remains unresolved against local/test 18.0. |
| Gradle, [JaCoCo plugin](https://docs.gradle.org/current/userguide/jacoco_plugin.html) | Reporting/verification need explicit task wiring and evidence. | CEREMO's 100% per-class threshold is user policy; it is not imposed by Gradle or proof of security. |
| Paystack, [Webhooks](https://paystack.com/docs/payments/webhooks/) and [subscriptions](https://paystack.com/docs/payments/subscriptions/) | Authenticate notifications and distinguish automatic subscription support from Ghana MoMo one-off collection. | No live merchant/payment flow tested; proposed manual renewal terms remain subject to approval. |
| Ghana DPC, [Organisation guidance](https://dpc.gov.gh/for-organisations/) and official CSA [Act 843 copy](https://csa.gov.gh/resources/Data_Protection_Act_2012.pdf) | Registration, transparent purpose/optional information and appropriate security inform the existing privacy work. | No legal clearance, current registration or compliance certification claimed. The current BoG directory timed out; no fresh PSP-licence confirmation was obtained. |

A guessed AWS readiness URL was inaccessible; the correct OPS07-BP02 and ORR-tool pages were subsequently opened. Existing cultural, competitor and technical-source registers retain their dated qualifications rather than being silently represented as a full fresh audit.

Applied installed humanizer, investor-materials and documentation-and-ADRs guidance. The reviewed standalone humanizer is MIT-licensed and locally pinned as recorded in its SOURCE.json. Read the local shared/wire skill, inventories, security rules and relevant research context. No new skill, remote script or sub-agent was installed/run. No company draft was transferred to an external writing service.

## Local source and reuse checks

Inspected build.gradle.kts, wrapper configuration, UserService, UserManagementConfiguration, BaseEntity, initial migration, local PostgreSQL Compose and relevant resource settings. The current configured Sonar key is ceremo_backend; the earlier documentation's hyphenated key was incorrect. Current Hibernate ddl-auto is none; schema validation is a requirement where explicitly configured, not an enabled production fact.

User Management already has the repository adapter, outer transactional composition, required terms binding and shared UUID use. Activation, the production account HTTP boundary and wider onboarding remain open. Corrected stale descriptions that still called implemented persistence/tooling pending. No code was changed to make documentation true.

The first attempted account configuration filename did not exist; source discovery identified UserManagementConfiguration. A PowerShell wildcard passed literally to rg failed; searching the directory established the actual settings. Neither failed lookup was treated as evidence of a missing implementation.

## Changes by document

| File | Decision-oriented revision |
| --- | --- |
| 01 | Commercial hypothesis/evidence summary, market-sizing inputs without invented numbers, experiment protocol, risks and clearer account/business distinction. Preserved cultural citations, field questions, recruitment counts and limits. Moved the earlier detailed account engineering notes below. |
| 03 | Architecture strategy/tradeoffs, external trust boundaries, current-vs-planned status, measurable proposed quality scenarios and architecture risk responses. Preserved API and ADR history; corrected runtime-schema-validation wording. |
| 04 | Complete text/table journeys, billing steps, entitlement transitions, module acceptance and recovery. All three Mermaid blocks removed. |
| 05 | Experience priorities, design decision responsibilities, public/private handling, proposed component states and usable handoff criteria. Selected stack and every open JSON value retained. |
| 06 | Relationship/cardinality table, authoritative-data ownership, consistency boundaries and migration/recovery acceptance. All schema table rows retained; both Mermaid diagrams removed. |
| 07 | Milestone exit evidence/current status, task outcomes, delivery controls and policy dependencies. All 15 task IDs retained; no effort/date/budget invented. |
| 08 | Assurance status, release/security scenarios, operational responsibilities and verification provenance. Corrected Sonar key and stale adapter/composition/tooling claims. Kept dated coverage history and exact 100% rule. |
| 09 | All 110 original functional rows retained; added module acceptance and six end-to-end release cases. Did not restore the rejected introduction/permissions table/diagram. |
| Root README | Review paths, updated document descriptions, actual account/test directory entries and concise current-source description. Local commands remain available as documented; none executed here. |

Document 02 version 3.0 remains the product reference and is unchanged by this suite revision. Product, schema and contract names remain consistent. Existing database/API definitions are requirements for future features, not a claim of implemented marketplace operations.

## Verification and remaining work

Local PowerShell verification passed on 4 November for all nine product documents, root README, research index and this entry: 12 files, 133 relative links/anchors, consistent table columns, balanced code fences, sequential numbered product sections and no remaining diagram blocks. The product set remains nine files. A separate source comparison passed for the unchanged PRD, complete API table, all 59 original database table lines, all 15 task IDs, identical selected/open frontend JSON and unchanged superseded ADR 11.2/11.3 history. All 110 original function rows remain identical; all 157 original functional-document table lines are retained. Exactly five Mermaid blocks were removed. Retained account-research links were rebased for this local memory location.

Checked the stale-status corrections against actual build/source/configuration. No application classes, tests, migrations, dependencies, runtime configuration or coverage thresholds changed. No build, test, Sonar analysis, live service, deployment or recovery action was run. These checks establish local documentation consistency, not market demand, legal clearance, accessibility conformance, security or production readiness.

Grammarly is **pending** for the full revised versions of 01/03–09 and root README, dated 4 November 2026. No checker/upload was authorised or used, no score exists and no full language-check claim is made. A later approved public/anonymised excerpt check must state exactly what it covers.

Remaining decisions: field evidence and commercial thresholds; offer/trial/pricing; trust/review/appeal rules; account activation/contact; browser sessions/design; production database/integrations; privacy/retention; numeric operating commitments and named responsible people. The revision does not approve these choices.

## Retained account research

The following preserves document 01's earlier detailed section 18 for traceability. It is historical research, superseded for presentation by the concise product/account section and current owning research entries. In particular, any earlier pending persistence/composition statements are not current implementation status.

<details>
<summary>Original account-module evidence and learning references, reviewed 20 October 2026</summary>

#### 18. Account ownership and the first business module

Reviewed 20 October 2026. The question at this stage is how to connect a person who signs in to their CEREMO records and permissions before building provider profiles. A business module is the part of the backend responsible for a business concern and its rules. In CEREMO, User Management owns the local account; Provider Management owns a business profile and its members; Subscription Management owns listing access periods. These are feature packages in the same backend, not separate deployed services.

#### Existing applications and what they establish

| Source | Demonstrated behaviour | CEREMO implication |
| --- | --- | --- |
| [Airbnb: switching between hosting and travelling](https://www.airbnb.co.uk/help/article/3546) | The same account can switch between being a guest and managing hosting | A photographer can also organise their own occasion. Keep the person separate from their provider business; do not require another account to become a customer. |
| [Sharetribe user types](https://www.sharetribe.com/help/en/articles/9495315-setup-tutorial-1-add-or-edit-user-types) | Marketplace onboarding can differ for providers and customers; peer marketplaces may have people acting as both | Ask for business details when someone creates a provider profile. Avoid forcing every browsing customer through provider onboarding. |
| [Sharetribe user API](https://www.sharetribe.com/api-reference/integration.html#users) | User identity-provider links, account states and effective permissions are explicit; denied listing permissions produce a forbidden response | A valid login is only one condition for an allowed action. Apply account status and business permissions in the backend. |
| [The Knot vendor explanation](https://www.theknot.com/content/the-knot-for-wedding-vendors) and [WeddingPro](https://www.theknotpro.com/) | Vendors manage storefronts, enquiries and visibility through a vendor account; WeddingPro advertises Basic and Premium options | A vendor account and listing are useful precedents for provider discovery funded by commercial visibility. Their exact terms and payment features are not CEREMO requirements. The WeddingPro home page required JavaScript on direct retrieval; Basic/Premium details were visible in indexed official text. |

These sources establish comparable product patterns. They do not establish that these companies use Keycloak, our schema, our package names or the same development order. Starting with a small local account module is our dependency-based recommendation. No market-wide adoption percentage or Ghanaian conversion evidence was found in this investigation.

#### Proposed CEREMO workflows and rules

A customer browses public providers without an account. When the customer chooses an account feature, Keycloak handles registration or login. CEREMO validates the access token, establishes or finds the local account using the trusted issuer and subject, and checks its status before returning the private profile or performing an action. Provisioning must be explicit and safe to retry; a profile read must not silently recreate a suspended or deleted account. Never join accounts merely because their email or phone matches. The optional preferences guide follows account setup, can be skipped and belongs to discovery.

A provider follows the same identity flow, then creates a separate provider business profile. Provider Management records who owns or belongs to that business. Editing a profile requires active account access and membership of that specific provider. Choosing "provider" at signup cannot grant staff privileges or ownership of someone else's business. Listing approval, trial/subscription entitlement, check results and reviews have their own rules. Login or subscription payment cannot grant all of them.

For example, Ama manages a catering company in Accra and later looks for a photographer for her own birthday. One personal account supports both activities. Her catering business has its own profile, members and subscription. If its subscription expires, discovery eligibility changes under the subscription rules; her personal account is not deleted. This is a hypothetical workflow, not an interview result.

Implement the smallest account-to-provider flow first: local account migration and identity uniqueness, safe account provisioning, current-user response, local status checks and ownership/security tests; then provider creation and membership. Existing shared authentication code already handles part of token validation. Broader recovery, phone verification and staff operations remain explicit planned work in UM-T02, not capabilities proven by the first profile endpoint. See [implementation tasks](../../../docs/07-implementation-plan.md).

#### Code to learn from

| Reference | Inspected behaviour | Use and limits |
| --- | --- | --- |
| CEREMO `shared/authentication/infrastructure/primary/SecurityConfiguration.java`, `AudienceValidator.java` and `application/AuthenticatedUser.java` | Existing security foundation identified in the repository | Extend the existing foundation with local account status and identity lookup; avoid a second login implementation. Recheck the actual methods and tests when implementing. |
| [Sharetribe `src/ducks/user.duck.js`](https://github.com/sharetribe/web-template/blob/main/src/ducks/user.duck.js) | `fetchCurrentUserPayloadCreator` calls `sdk.currentUser.show(...)`, requests effective permissions and handles inactive users before fetching related data | Learn the current-user response and state-handling pattern. This is JavaScript client code dependent on Sharetribe services, not a reusable Spring backend. Its transaction and Stripe sections are outside CEREMO's MVP. Review the repository's [licence](https://github.com/sharetribe/web-template/blob/main/LICENSE) before any reuse. |
| [Spring Security resource-server sample](https://github.com/spring-projects/spring-security-samples/tree/main/servlet/spring-boot/java/oauth2/resource-server/hello-security) | `OAuth2ResourceServerSecurityConfiguration.java` demonstrates bearer-token route permissions; the sample includes integration tests | Use as a Java security reference alongside [current JWT documentation](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html). Adapt to CEREMO's Keycloak issuer, audience and existing chains. Check build versions and Apache-2.0 notices before reuse. It does not implement marketplace accounts, ownership or subscription entitlement. |

No external source code was copied or executed during this research. Public vendor documentation cannot substitute for proprietary backend source. The first-module scope is a CEREMO design decision informed by these precedents.

#### Cultural and original-source notes

Research used official Ghanaian sources, academic records, public competitor pages and payment-provider documentation. Full official pages, selected thesis sections and competitor homepages were opened. The naming extract and funeral repository record were available without every academic full text. BoG retrieval limits are stated above.

The work did not include interviews, accounts, enquiries, payments or provider audits. Historical dates are retained. Cultural examples describe selected communities and studies, not rules for every Ghanaian household. Festival dates need confirmation for the locality and year concerned.

</details>
