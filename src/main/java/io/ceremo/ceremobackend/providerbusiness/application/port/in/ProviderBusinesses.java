package io.ceremo.ceremobackend.providerbusiness.application.port.in;

import io.ceremo.ceremobackend.providerbusiness.domain.model.MemberRole;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusiness;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusinessId;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusinessMember;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderMembership;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPage;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPageable;
import java.util.UUID;

public interface ProviderBusinesses {
  ProviderBusiness create(String name);
  ProviderBusiness get(ProviderBusinessId id);
  CeremoPage<ProviderBusiness> mine(CeremoPageable pageable);
  CeremoPage<ProviderBusinessMember> members(ProviderBusinessId id, CeremoPageable pageable);
  CeremoPage<ProviderBusinessMember> invitations(ProviderBusinessId id, CeremoPageable pageable);
  void invite(ProviderBusinessId id, UUID userId, MemberRole role);
  void removeMember(ProviderBusinessId id, UUID userId);
  void cancelInvitation(ProviderBusinessId id, UUID userId);
  CeremoPage<ProviderMembership> myMemberships(CeremoPageable pageable);
  ProviderBusiness accept(ProviderBusinessId id);
  ProviderBusiness suspend(ProviderBusinessId id, String reason);
  ProviderBusiness reactivate(ProviderBusinessId id, String reason);
}
