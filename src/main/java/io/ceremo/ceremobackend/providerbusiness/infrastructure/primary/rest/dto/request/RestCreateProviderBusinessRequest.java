package io.ceremo.ceremobackend.providerbusiness.infrastructure.primary.rest.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RestCreateProviderBusinessRequest(@NotBlank @Size(max = 160) String name) {
  @JsonAnySetter
  public void rejectUnknownField(String name, Object value) {
    throw new IllegalArgumentException("Unknown provider business field");
  }
}
