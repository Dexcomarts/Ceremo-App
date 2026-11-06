# User account domain

- Entry ID: domain/user-accounts
- Researched: 20 October 2026
- Status: implemented; verification is recorded in local test reports, not inferred from this label
- Question: how to create and restore an account without losing saved status or coupling the core to persistence

## Evidence and limits

[Cockburn's ports and adapters](https://alistair.cockburn.us/hexagonal-architecture/) explains separating application behaviour from external technology. [Jakarta entity requirements](https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/Entity.html) describe persistence classes; those requirements do not require the CEREMO domain model to be a JPA entity. These sources support boundaries, not CEREMO's specific account policy.

## CEREMO decisions and implementation

[User](../../../src/main/java/io/ceremo/ceremobackend/account/domain/model/User.java) is immutable plain Java. `register` starts PENDING; `restore` preserves the saved status. Neither generates IDs or reads authentication. Reuse shared `Assert`; generate new account IDs through `CeremoUUID` in the application layer. Do not use a registration factory to load existing ACTIVE/SUSPENDED/ANONYMISED records.

[UserIdentity](../../../src/main/java/io/ceremo/ceremobackend/account/domain/model/UserIdentity.java) preserves exact issuer and subject strings. Shared `Name`, `Username` and `Email` have different semantics and do not replace it. A saved status is not itself an authorisation decision.

## Verification and next work

[UserTest](../../../src/test/java/io/ceremo/ceremobackend/account/domain/model/UserTest.java) covers initial state and invalid mandatory values. [Mapping tests](../../../src/test/java/io/ceremo/ceremobackend/account/infrastructure/secondary/persistence/UserMapperTest.java) cover all four statuses. The 4 November activation increment adds [UserActivationTest](../../../src/test/java/io/ceremo/ceremobackend/account/domain/model/UserActivationTest.java): PENDING becomes ACTIVE with unchanged identity/terms fields, ACTIVE returns the same snapshot, and restricted statuses fail. `activate()` is plain domain behaviour; trusted email verification is checked by the application through a port. Other status operations/contact verification remain unimplemented; `restore` must not be exposed as arbitrary status editing. See the [activation decision and execution record](../application/user-registration.md#verified-email-activation-4-november-2026).

Related: [application](../application/user-registration.md), [repositories](../repositories/user-persistence.md), [database](../database/user-schema.md).

## Provider account security, 5 November 2026

Account identity remains independent of provider businesses. The new Tenant root retains the accountable owner; ordinary team records grant no owner or platform staff role. Workspace ACTIVE grants no verification, publication or subscription. See [the approved tenant model](../ports-and-adapters/provider-account-security.md).


Consolidation, 5 November 2026: current source, ports, adapters and tests are under `account`; composition is `wire/account/AccountConfiguration`. Existing domain behaviour, transactions, HTTP routes and database schema continue. Older package names in dated examples/commands are historical. See [the approved consolidation](../ports-and-adapters/provider-account-security.md#account-module-consolidation-5-november-2026).
