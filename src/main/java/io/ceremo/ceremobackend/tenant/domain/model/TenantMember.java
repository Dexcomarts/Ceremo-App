package io.ceremo.ceremobackend.tenant.domain.model;

import io.ceremo.ceremobackend.shared.error.domain.Assert;
import java.util.UUID;

public record TenantMember(UUID userId, MemberRole role, MembershipStatus status) {
  public TenantMember {
    Assert.notNull("userId", userId);
    Assert.notNull("role", role);
    Assert.notNull("status", status);
  }
}
