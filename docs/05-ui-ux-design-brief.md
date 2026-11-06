# CEREMO Frontend and UI/UX Brief

Version 3.4 · 5 November 2026 · Design and integration requirements draft

## 1. Selected frontend stack

The web interface must help an organiser compare providers and make an informed contact decision, while giving providers and staff clear management tools. Current implementation remains on the backend; no frontend application has been created. The stack is selected, while the visual system and browser-session design are still open. [Frontend decision](03-technical-requirements.md#115-frontend-stack).

| Area | Selection | Use in CEREMO |
| --- | --- | --- |
| Framework | Next.js (React) | Public provider pages and customer, provider and staff interfaces; supports server rendering and static generation where appropriate. [Next.js](https://nextjs.org/docs/app/getting-started/server-and-client-components) |
| Language | TypeScript | Typed components, forms and API contracts. [TypeScript handbook](https://www.typescriptlang.org/docs/handbook/intro.html) |
| Styling | Tailwind CSS | Shared styling utilities and responsive layouts. [Responsive design](https://tailwindcss.com/docs/responsive-design) |
| UI components | shadcn/ui | Component source maintained and adapted within the frontend. [Introduction](https://ui.shadcn.com/docs) |
| Data fetching and caching | TanStack Query | Interactive API queries, mutations and server-state caching. [Server rendering](https://tanstack.com/query/latest/docs/framework/react/guides/advanced-ssr) |
| Forms | React Hook Form | Form values, submission state and field errors, connected to Zod through its resolver. [Official resolvers](https://github.com/react-hook-form/resolvers#zod) |
| Validation | Zod | Runtime input schemas and inferred TypeScript types. [Zod](https://zod.dev/) |

Pin compatible versions together when the frontend is created. Server rendering, a component library and TypeScript do not establish search ranking, accessibility or runtime data validity.

### Experience priorities

| Priority | Required user outcome | Design acceptance |
| --- | --- | --- |
| Understand relevance | Know why a provider fits the service and area. | Current criteria and coverage are visible; unknown availability is explicit. |
| Compare honestly | Understand prices, checks and customer feedback. | Currency/unit, check scope/date and review count are readable and independent. |
| Reach contact clearly | Browse limited summaries free; explain the free account gate before full profile/contact access. | Return to the selected provider after registration/activation; no compulsory guide or marketing consent. |
| Control disclosure | Decide what is sent and to whom. | Preview the selected-provider brief and shared contacts before submission. |
| Recover safely | Understand whether an enquiry or subscription payment succeeded. | Keep the original reference and distinguish pending, failed and completed states. |
| Understand the provider offer | Know trial end, paid benefits and renewal obligations. | Show actual terms before purchase and retained access after expiry. |

## 2. Decisions still open

The values below remain unselected. They are not defaults to infer from a framework, copied template or competitor. Visual tokens need approval before production components are styled; session design needs security review before authenticated client work.

```json
{
  "frontendFramework": "Next.js (React)",
  "frontendLanguage": "TypeScript",
  "stylingFramework": "Tailwind CSS",
  "componentLibrary": "shadcn/ui",
  "serverStateLibrary": "TanStack Query",
  "formLibrary": "React Hook Form",
  "validationLibrary": "Zod",
  "applicationPlatforms": ["Web"],
  "nativeMobileFramework": null,
  "dependencyVersions": null,
  "nodeVersion": null,
  "packageManager": null,
  "frontendHosting": null,
  "routingModel": null,
  "authenticationClientLibrary": null,
  "browserSessionStrategy": null,
  "screenInventory": null,
  "clientRoutes": null,
  "navigation": null,
  "brandLogo": null,
  "colourPalette": null,
  "typographyFamilies": null,
  "typographyScale": null,
  "spacingScale": null,
  "gridAndBreakpoints": null,
  "responsiveBehaviour": null,
  "iconSet": null,
  "designTokens": null,
  "interactionAndMotion": null,
  "accessibilityConformanceTarget": null,
  "designFile": null
}
```

| Decision group | Required deliverable | Review responsibility |
| --- | --- | --- |
| Brand and visual system | Logo, palette, type families/scale, spacing, icon set and semantic state tokens. | Founder/product with design. |
| Information architecture | Screen inventory, routes, navigation and action-to-destination mapping. | Product/design with engineering. |
| Responsive behaviour | Content priorities, layout rules, breakpoints, media treatment and large-table behaviour. | Design/frontend engineering. |
| Identity and runtime | Session strategy, client library, Node/package versions, hosting and public/private cache policy. | Engineering/security. |
| Accessibility | Conformance target, test coverage and required assistive-technology checks. | Design/engineering with product. |

Roles are proposed responsibilities; named owners are not yet assigned. Do not replace open values with speculative colours, fonts, dimensions or native technology.

## 3. Rendering, data and forms

| Surface | Required handling |
| --- | --- |
| Public catalogue and provider summaries | Choose rendering/freshness policies for limited summaries. Exclude full profile content, gated contact destinations and private tenant fields from HTML, metadata, JSON-LD and public caches. |
| Registered provider profiles and contact | Require an ACTIVE local account, show registration/verification/denied states, preserve the intended provider and use private no-store responses. Recheck provider contact eligibility. |
| Private customer/provider/staff work | Require authorised API access and private cache policies. Do not emit private data in public HTML, shared caches or client props. |
| Interactive queries | Use TanStack Query where it manages interactive server state. Scope prefetched server caches to the request; clear private state on logout/account change. |
| Writes | Invalidate affected data after confirmed success. Preserve record references and use backend idempotency when a result is uncertain. |
| Forms | Use React Hook Form with the Zod resolver for values, validation and submission state. Preserve recoverable input and show field-level plus submission feedback. |
| API responses | Apply runtime schemas at selected boundaries; TypeScript types alone do not validate JSON. The backend remains responsible for validation, permissions and business rules. |

Use approved thumbnails for anonymous summaries. Full portfolio assets must follow registered profile access through authorised media delivery; knowing an object key is not permission. Private verification evidence and billing attachments require their own record-level permission. A blurred button or image does not protect data already delivered to the browser.

Use client components for interaction that requires them. Server credentials stay server-side. See [Next.js rendering](https://nextjs.org/docs/app/getting-started/server-and-client-components), [TanStack Query SSR](https://tanstack.com/query/latest/docs/framework/react/guides/advanced-ssr), [form resolvers](https://github.com/react-hook-form/resolvers#zod) and [Zod](https://zod.dev/).

A failed notification must not make a saved enquiry appear unsaved. A browser checkout return must not make an unverified payment appear paid. Reopening a screen must read authoritative status rather than replay a purchase.

## 4. Backend and login integration

The frontend consumes Spring Boot REST APIs under `/api/v1`. Java owns provider publication, entitlement, ranking, moderation and payment verification. Keycloak remains the development and production identity provider. The web stack supports mobile browsers; native mobile technology remains `null`.

A server-held token approach with an opaque HttpOnly browser session is proposed in the [authentication requirements](03-technical-requirements.md#4-authentication-and-permissions). The library, storage and session strategy are not selected. Review PKCE, exact callbacks, protected session storage, CSRF/origin policy, refresh coordination and logout before implementation. [Next.js authentication](https://nextjs.org/docs/app/guides/authentication).

Only intentionally public configuration may use `NEXT_PUBLIC_`; these values enter browser code. Identity/session secrets and payment keys must stay in server configuration. Define how the browser reaches the API before classifying a backend URL as server-only or public. [Environment variables](https://nextjs.org/docs/app/guides/environment-variables).

No customer ceremony-service checkout, booking confirmation or calendar reservation should appear in the interface. Provider subscription checkout must identify CEREMO as the subscription merchant and explain the purchased platform benefits.

## 5. Information the API must provide

| Task | Minimum information and interaction requirement |
| --- | --- |
| Compare providers | Service, coverage, indicative price/unit, portfolio, actual checks and published rating/count. |
| Use optional guide | Unknown answers, active filters, skip/back/edit/reset and clear information purpose. |
| Plan an occasion | Separate service needs, optional dates/locality and unit-based budgets. |
| Recognise promotion | Explicit Sponsored label, distinct from checks and customer stars. |
| Contact a provider | Current eligibility and an approved business destination, or a previewable enquiry brief. |
| Read/review experience | Published experience, evidence status, provider reply, report and appeal routes. |
| Manage subscription | Offer price/benefits/terms, trial/paid-through dates, expiry and cancellation effects. |
| Check payment | Invoice/reference, authoritative status, entitlement and any billing case. |
| Read local notice | Source, affected locality, effective dates and review date. |
| Correct an error | Existing problem-response key, field/message details and a meaningful recovery action. |
| Resume after disconnection | Existing record/reference, current status and retry-safe available actions. |

Private fields must be excluded by the API, rather than merely hidden on screen. State text must remain understandable without colour or an icon.

### Reusable components and states

Use a shared provider card/profile summary, price/unit display, check summary, rating/count, filter panel, optional-question form, enquiry preview, status panel, evidence upload, case history and subscription offer/invoice view where the same behaviour recurs. These are proposed responsibilities, not implemented components.

Each component needs relevant loading, empty, success, validation, denied, unavailable and retry states. Keep account, listing, enquiry and payment states distinct. Use shadcn/ui as maintained source; test assembled components after customisation.

## 6. Later design work

Design validation must precede a claim that the interface is ready. Research with family, committee and business organisers, providers and relevant staff, including varied device access and digital confidence.

| Review | Evidence needed before acceptance |
| --- | --- |
| Discovery task | Users can select a service/area, understand filters and choose a suitable candidate without completing the guide. |
| Trust comprehension | Users distinguish a paid placement, a completed check and customer ratings, including zero reviews. |
| Price comprehension | Users distinguish per-guest and whole-service prices and recognise quote-needed information. |
| Enquiry/privacy | Users identify the recipient and shared fields; skipping optional answers does not block ordinary contact. |
| Trial/billing | Providers understand when trial access begins/ends, what a payment buys and what happens after uncertain checkout or expiry. |
| Responsive use | Core content/actions remain usable on small and large screens, with text zoom and without obstructed controls or misleading truncation. |
| Accessibility | Test keyboard operation, focus, names/labels, screen-reader feedback, contrast, error recovery and status announcements against the approved target. |

[WCAG 2.2](https://www.w3.org/TR/WCAG22/) is the reference for selecting the conformance target. W3C recommends clear submission results, field feedback and practical error correction; apply these to enquiries and billing status. [W3C form notifications](https://www.w3.org/WAI/tutorials/forms/notifications/). No conformance result is claimed.

The implementation handoff requires approved screen/routes, tokens, responsive rules, component states, authentication design and testable acceptance evidence. Use [journeys](04-app-flow-and-user-journeys.md), [functional requirements](09-functional-requirements.md) and [technical requirements](03-technical-requirements.md) as the behaviour contract.

Editorial verification: version 3.4, 5 November 2026; Grammarly pending.
