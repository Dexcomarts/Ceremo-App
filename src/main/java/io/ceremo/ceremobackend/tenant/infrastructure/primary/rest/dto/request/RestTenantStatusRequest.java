package io.ceremo.ceremobackend.tenant.infrastructure.primary.rest.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RestTenantStatusRequest(@NotBlank @Size(max = 500) String reason) {
  @JsonAnySetter
  public void rejectUnknownField(String name, Object value) {
    throw new IllegalArgumentException("Unknown provider status field");
  }
}
