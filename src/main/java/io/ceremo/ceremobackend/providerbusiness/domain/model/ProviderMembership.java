package io.ceremo.ceremobackend.providerbusiness.domain.model;

import io.ceremo.ceremobackend.shared.error.domain.Assert;

/** The caller's own invitation or team membership in one provider business. The owner appears with role OWNER. */
public record ProviderMembership(ProviderBusinessId businessId, String businessName,
    ProviderBusinessStatus businessStatus, MemberRole role, MembershipStatus status) {
  public ProviderMembership {
    Assert.notNull("businessId", businessId);
    Assert.notBlank("businessName", businessName);
    Assert.notNull("businessStatus", businessStatus);
    Assert.notNull("role", role);
    Assert.notNull("status", status);
  }
}
