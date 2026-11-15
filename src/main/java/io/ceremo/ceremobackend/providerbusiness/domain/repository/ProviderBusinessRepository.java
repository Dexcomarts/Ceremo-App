package io.ceremo.ceremobackend.providerbusiness.domain.repository;

import io.ceremo.ceremobackend.providerbusiness.domain.model.MembershipStatus;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusiness;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusinessId;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusinessMember;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderMembership;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPage;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPageable;
import java.util.Optional;
import java.util.UUID;

public interface ProviderBusinessRepository {
  /** Stores the business and its owner's ACTIVE OWNER team row together. */
  void create(ProviderBusiness business);
  // Point lookups lock the root until the complete use case commits, including membership changes.
  Optional<ProviderBusiness> findForMember(ProviderBusinessId id, UUID userId);
  Optional<ProviderBusiness> findForInvitee(ProviderBusinessId id, UUID userId);
  Optional<ProviderBusiness> findById(ProviderBusinessId id);
  CeremoPage<ProviderBusiness> findForUser(UUID userId, CeremoPageable pageable);
  /** Team rows of one business in the given state: ACTIVE members or INVITED invitations. */
  CeremoPage<ProviderBusinessMember> members(ProviderBusinessId id, MembershipStatus status, CeremoPageable pageable);
  /** The user's own invitations and memberships across businesses. */
  CeremoPage<ProviderMembership> memberships(UUID userId, CeremoPageable pageable);
  void invite(ProviderBusinessId id, ProviderBusinessMember member);
  void accept(ProviderBusinessId id, UUID userId);
  /** Removes the user's team row only when it is in the given state; otherwise reports it as unavailable. */
  void remove(ProviderBusinessId id, UUID userId, MembershipStatus status);
  void updateStatus(ProviderBusiness business);
  void recordChange(ProviderBusinessId id, UUID actorId, String action, String reason);
}
