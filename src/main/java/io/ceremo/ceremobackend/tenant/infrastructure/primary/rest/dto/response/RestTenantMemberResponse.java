package io.ceremo.ceremobackend.tenant.infrastructure.primary.rest.dto.response;

import io.ceremo.ceremobackend.tenant.domain.model.MemberRole;
import io.ceremo.ceremobackend.tenant.domain.model.MembershipStatus;
import io.ceremo.ceremobackend.tenant.domain.model.TenantMember;
import java.util.UUID;

public record RestTenantMemberResponse(UUID userId, MemberRole role, MembershipStatus status) {
  public static RestTenantMemberResponse from(TenantMember member) {
    return new RestTenantMemberResponse(member.userId(), member.role(), member.status());
  }
}
