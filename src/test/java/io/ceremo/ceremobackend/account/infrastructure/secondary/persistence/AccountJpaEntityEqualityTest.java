package io.ceremo.ceremobackend.account.infrastructure.secondary.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.ceremo.ceremobackend.account.domain.model.UserIdentity;
import io.ceremo.ceremobackend.account.domain.model.UserStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountJpaEntityEqualityTest {
  private static final UUID ID = UUID.fromString("01940d11-7390-7000-8000-000000000001");
  private static final UUID OTHER_ID = UUID.fromString("01940d11-7390-7000-8000-000000000002");
  private static final Instant ACCEPTED_AT = Instant.parse("2026-10-20T12:00:00Z");

  @Test
  void accountRowsAreEqualByIdOnlyWhateverTheirMappedFields() {
    UserJpaEntity row = user(ID, "Ama Mensah", UserStatus.PENDING);
    UserJpaEntity sameIdChangedFields = user(ID, "Ama M.", UserStatus.ACTIVE);

    assertThat(row).isEqualTo(sameIdChangedFields).hasSameHashCodeAs(sameIdChangedFields)
        .isNotEqualTo(user(OTHER_ID, "Ama Mensah", UserStatus.PENDING));
  }

  @Test
  void identityRowsAreEqualByIdOnlyWhateverTheirMappedFields() {
    UserJpaEntity account = user(ID, "Ama Mensah", UserStatus.ACTIVE);
    UserIdentityJpaEntity row = identity(ID, account, "subject-1");
    UserIdentityJpaEntity sameIdChangedFields = identity(ID, account, "subject-2");

    assertThat(row).isEqualTo(sameIdChangedFields).hasSameHashCodeAs(sameIdChangedFields)
        .isNotEqualTo(identity(OTHER_ID, account, "subject-1"));
  }

  private static UserJpaEntity user(UUID id, String displayName, UserStatus status) {
    return new UserJpaEntity(id, displayName, "terms-v1", ACCEPTED_AT, status);
  }

  private static UserIdentityJpaEntity identity(UUID id, UserJpaEntity account, String subject) {
    return new UserIdentityJpaEntity(id, account, new UserIdentity("https://identity.example/realms/ceremo", subject),
        ACCEPTED_AT);
  }
}
