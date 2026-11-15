package io.ceremo.ceremobackend.providerbusiness.domain.model;

import io.ceremo.ceremobackend.shared.error.domain.Assert;
import java.util.UUID;

/** A provider business workspace. ACTIVE does not mean an approved or paid listing. */
public record ProviderBusiness(ProviderBusinessId id, String name, UUID ownerId, ProviderBusinessStatus status) {
  public ProviderBusiness {
    Assert.notNull("id", id);
    Assert.field("name", name).notBlank().maxLength(160);
    Assert.notNull("ownerId", ownerId);
    Assert.notNull("status", status);
  }

  public static ProviderBusiness create(ProviderBusinessId id, String name, UUID ownerId) {
    return new ProviderBusiness(id, name, ownerId, ProviderBusinessStatus.ACTIVE);
  }

  public ProviderBusiness suspend() { return new ProviderBusiness(id, name, ownerId, ProviderBusinessStatus.SUSPENDED); }
  public ProviderBusiness reactivate() { return new ProviderBusiness(id, name, ownerId, ProviderBusinessStatus.ACTIVE); }

  /** The owner's own ACTIVE team row, stored with the business so team lists read one table. */
  public ProviderBusinessMember ownerMember() { return ProviderBusinessMember.owner(ownerId); }
}
