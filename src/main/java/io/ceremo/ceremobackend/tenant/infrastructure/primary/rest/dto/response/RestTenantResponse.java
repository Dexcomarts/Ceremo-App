package io.ceremo.ceremobackend.tenant.infrastructure.primary.rest.dto.response;

import io.ceremo.ceremobackend.tenant.domain.model.Tenant;
import io.ceremo.ceremobackend.tenant.domain.model.TenantStatus;
import java.util.UUID;

public record RestTenantResponse(UUID id, String name, UUID ownerId, TenantStatus status) {
  public static RestTenantResponse from(Tenant tenant) {
    return new RestTenantResponse(tenant.id().get(), tenant.name(), tenant.ownerId(), tenant.status());
  }
}
