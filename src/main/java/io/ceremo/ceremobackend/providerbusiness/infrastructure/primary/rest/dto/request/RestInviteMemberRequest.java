package io.ceremo.ceremobackend.providerbusiness.infrastructure.primary.rest.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.ceremo.ceremobackend.providerbusiness.domain.model.MemberRole;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RestInviteMemberRequest(@NotNull UUID userId, @NotNull MemberRole role) {
  @JsonAnySetter
  public void rejectUnknownField(String name, Object value) {
    throw new IllegalArgumentException("Unknown team invitation field");
  }
}
