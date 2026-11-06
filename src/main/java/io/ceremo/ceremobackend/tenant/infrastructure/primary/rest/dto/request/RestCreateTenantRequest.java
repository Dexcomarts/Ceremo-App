package io.ceremo.ceremobackend.tenant.infrastructure.primary.rest.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RestCreateTenantRequest(@NotBlank @Size(max = 160) String name) {
  @JsonAnySetter
  public void rejectUnknownField(String name, Object value) {
    throw new IllegalArgumentException("Unknown provider workspace field");
  }
}
