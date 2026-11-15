package io.ceremo.ceremobackend.providerbusiness.infrastructure.primary.rest.dto.response;

import io.ceremo.ceremobackend.providerbusiness.domain.model.MemberRole;
import io.ceremo.ceremobackend.providerbusiness.domain.model.MembershipStatus;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusinessStatus;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderMembership;
import java.util.UUID;

public record RestProviderMembershipResponse(UUID businessId, String businessName,
    ProviderBusinessStatus businessStatus, MemberRole role, MembershipStatus status) {
  public static RestProviderMembershipResponse from(ProviderMembership membership) {
    return new RestProviderMembershipResponse(membership.businessId().value(), membership.businessName(),
        membership.businessStatus(), membership.role(), membership.status());
  }
}
