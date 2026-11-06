package io.ceremo.ceremobackend.shared.multitenancy.domain;

import io.ceremo.ceremobackend.shared.error.domain.Assert;

public record TenantInfo(TenantId id, String name, String subscriptionTier, int maxLocations) {

  public TenantInfo {
    Assert.notNull("id", id);
    Assert.field("name", name).notBlank().maxLength(255);
    Assert.field("subscriptionTier", subscriptionTier).notBlank();
    Assert.field("maxLocations", maxLocations).positive();
  }
}
