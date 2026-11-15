package io.ceremo.ceremobackend.providerbusiness.infrastructure.primary.rest.dto.response;

import io.ceremo.ceremobackend.providerbusiness.domain.model.MemberRole;
import io.ceremo.ceremobackend.providerbusiness.domain.model.MembershipStatus;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusinessMember;
import java.util.UUID;

public record RestProviderBusinessMemberResponse(UUID userId, MemberRole role, MembershipStatus status) {
  public static RestProviderBusinessMemberResponse from(ProviderBusinessMember member) {
    return new RestProviderBusinessMemberResponse(member.userId(), member.role(), member.status());
  }
}
