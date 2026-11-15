package io.ceremo.ceremobackend.providerbusiness.infrastructure.primary.rest.dto.response;

import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusiness;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusinessStatus;
import java.util.UUID;

public record RestProviderBusinessResponse(UUID id, String name, UUID ownerId, ProviderBusinessStatus status) {
  public static RestProviderBusinessResponse from(ProviderBusiness business) {
    return new RestProviderBusinessResponse(business.id().value(), business.name(), business.ownerId(), business.status());
  }
}
