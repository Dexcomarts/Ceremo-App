package io.ceremo.ceremobackend.providerbusiness.domain.model;

import io.ceremo.ceremobackend.shared.error.domain.Assert;
import io.ceremo.ceremobackend.shared.error.domain.CeremoException;
import io.ceremo.ceremobackend.shared.error.domain.ErrorStatus;
import java.util.UUID;

public record ProviderBusinessMember(UUID userId, MemberRole role, MembershipStatus status) {
  public ProviderBusinessMember {
    Assert.notNull("userId", userId);
    Assert.notNull("role", role);
    Assert.notNull("status", status);
    // An OWNER row exists only as the owner's ACTIVE row, so an invitation can never carry the OWNER role.
    if (role == MemberRole.OWNER && status != MembershipStatus.ACTIVE) {
      throw CeremoException.builder(ProviderBusinessErrorKey.PROVIDER_BUSINESS_OWNER_PROTECTED)
          .status(ErrorStatus.BAD_REQUEST)
          .message("The accountable owner cannot be replaced through team membership.").build();
    }
  }

  static ProviderBusinessMember owner(UUID userId) {
    return new ProviderBusinessMember(userId, MemberRole.OWNER, MembershipStatus.ACTIVE);
  }
}
