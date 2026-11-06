package io.ceremo.ceremobackend.tenant.domain.model;

import io.ceremo.ceremobackend.shared.error.domain.Assert;
import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantId;
import java.util.UUID;

/** A provider business workspace. ACTIVE does not mean an approved or paid listing. */
public record Tenant(TenantId id, String name, UUID ownerId, TenantStatus status) {
  public Tenant {
    Assert.notNull("id", id);
    Assert.field("name", name).notBlank().maxLength(160);
    Assert.notNull("ownerId", ownerId);
    Assert.notNull("status", status);
  }

  public static Tenant create(TenantId id, String name, UUID ownerId) {
    return new Tenant(id, name, ownerId, TenantStatus.ACTIVE);
  }

  public Tenant suspend() { return new Tenant(id, name, ownerId, TenantStatus.SUSPENDED); }
  public Tenant reactivate() { return new Tenant(id, name, ownerId, TenantStatus.ACTIVE); }
}
