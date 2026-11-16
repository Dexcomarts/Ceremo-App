package io.ceremo.ceremobackend.providerbusiness.infrastructure.secondary.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.ceremo.ceremobackend.providerbusiness.domain.model.MemberRole;
import io.ceremo.ceremobackend.providerbusiness.domain.model.MembershipStatus;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusiness;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusinessId;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusinessMember;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusinessStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProviderBusinessJpaEntityEqualityTest {
  private static final UUID ID = UUID.fromString("01940d11-7390-7000-8000-000000000001");
  private static final UUID OTHER_ID = UUID.fromString("01940d11-7390-7000-8000-000000000002");
  private static final UUID OWNER = UUID.fromString("01940d11-7390-7000-8000-000000000010");
  private static final UUID MEMBER = UUID.fromString("01940d11-7390-7000-8000-000000000011");

  @Test
  void businessRowsAreEqualByIdOnlyWhateverTheirMappedFields() {
    ProviderBusinessJpaEntity row = business(ID, "Ama Catering", ProviderBusinessStatus.ACTIVE);
    ProviderBusinessJpaEntity sameIdChangedFields = business(ID, "Renamed", ProviderBusinessStatus.SUSPENDED);

    assertThat(row).isEqualTo(sameIdChangedFields).hasSameHashCodeAs(sameIdChangedFields)
        .isNotEqualTo(business(OTHER_ID, "Ama Catering", ProviderBusinessStatus.ACTIVE));
  }

  @Test
  void memberRowsAreEqualByIdOnlyWhateverTheirMappedFields() {
    ProviderBusinessMemberJpaEntity row = member(ID, MemberRole.MANAGER, MembershipStatus.INVITED);
    ProviderBusinessMemberJpaEntity sameIdChangedFields = member(ID, MemberRole.FINANCE, MembershipStatus.ACTIVE);

    assertThat(row).isEqualTo(sameIdChangedFields).hasSameHashCodeAs(sameIdChangedFields)
        .isNotEqualTo(member(OTHER_ID, MemberRole.MANAGER, MembershipStatus.INVITED));
  }

  private static ProviderBusinessJpaEntity business(UUID id, String name, ProviderBusinessStatus status) {
    return new ProviderBusinessJpaEntity(new ProviderBusiness(ProviderBusinessId.of(id), name, OWNER, status));
  }

  private static ProviderBusinessMemberJpaEntity member(UUID id, MemberRole role, MembershipStatus status) {
    return new ProviderBusinessMemberJpaEntity(id, ID, new ProviderBusinessMember(MEMBER, role, status));
  }
}
