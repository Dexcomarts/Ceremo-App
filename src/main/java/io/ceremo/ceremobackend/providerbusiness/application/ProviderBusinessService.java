package io.ceremo.ceremobackend.providerbusiness.application;

import io.ceremo.ceremobackend.account.application.port.in.Accounts;
import io.ceremo.ceremobackend.account.application.port.in.UserDirectory;
import io.ceremo.ceremobackend.account.domain.exception.UserErrorKey;
import io.ceremo.ceremobackend.providerbusiness.application.port.in.ProviderBusinesses;
import io.ceremo.ceremobackend.providerbusiness.domain.model.MemberRole;
import io.ceremo.ceremobackend.providerbusiness.domain.model.MembershipStatus;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusiness;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusinessErrorKey;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusinessId;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusinessMember;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusinessStatus;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderMembership;
import io.ceremo.ceremobackend.providerbusiness.domain.repository.ProviderBusinessRepository;
import io.ceremo.ceremobackend.shared.error.domain.Assert;
import io.ceremo.ceremobackend.shared.error.domain.CeremoException;
import io.ceremo.ceremobackend.shared.error.domain.ErrorStatus;
import io.ceremo.ceremobackend.shared.generation.domain.CeremoUUID;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPage;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPageable;
import java.util.Objects;
import java.util.UUID;

public final class ProviderBusinessService implements ProviderBusinesses {
  private final Accounts accounts;
  private final UserDirectory users;
  private final ProviderBusinessRepository businesses;

  public ProviderBusinessService(Accounts accounts, UserDirectory users, ProviderBusinessRepository businesses) {
    this.accounts = Objects.requireNonNull(accounts, "accounts");
    this.users = Objects.requireNonNull(users, "users");
    this.businesses = Objects.requireNonNull(businesses, "businesses");
  }

  @Override
  public ProviderBusiness create(String name) {
    UUID actor = accounts.requireActiveAccount().id();
    ProviderBusiness business = ProviderBusiness.create(ProviderBusinessId.of(CeremoUUID.generate()), name, actor);
    businesses.create(business);
    businesses.recordChange(business.id(), actor, "CREATED", "Provider business created");
    return business;
  }

  @Override
  public ProviderBusiness get(ProviderBusinessId id) {
    return memberBusiness(id, accounts.requireActiveAccount().id());
  }

  @Override
  public CeremoPage<ProviderBusiness> mine(CeremoPageable pageable) {
    return businesses.findForUser(accounts.requireActiveAccount().id(), pageable);
  }

  @Override
  public CeremoPage<ProviderBusinessMember> members(ProviderBusinessId id, CeremoPageable pageable) {
    ownerBusiness(id, accounts.requireActiveAccount().id());
    return businesses.members(id, MembershipStatus.ACTIVE, pageable);
  }

  @Override
  public CeremoPage<ProviderBusinessMember> invitations(ProviderBusinessId id, CeremoPageable pageable) {
    ownerBusiness(id, accounts.requireActiveAccount().id());
    return businesses.members(id, MembershipStatus.INVITED, pageable);
  }

  @Override
  public void invite(ProviderBusinessId id, UUID userId, MemberRole role) {
    UUID actor = accounts.requireActiveAccount().id();
    ProviderBusiness business = ownerBusiness(id, actor);
    requireActive(business);
    protectOwner(business, userId);
    if (!users.isRegistered(userId)) {
      throw CeremoException.builder(UserErrorKey.USER_NOT_FOUND).status(ErrorStatus.NOT_FOUND)
          .message("The invited account is not registered.").build();
    }
    businesses.invite(id, new ProviderBusinessMember(userId, role, MembershipStatus.INVITED));
    businesses.recordChange(id, actor, "MEMBER_INVITED", userId.toString());
  }

  @Override
  public void removeMember(ProviderBusinessId id, UUID userId) {
    removeTeamRow(id, userId, MembershipStatus.ACTIVE, "MEMBER_REVOKED");
  }

  @Override
  public void cancelInvitation(ProviderBusinessId id, UUID userId) {
    removeTeamRow(id, userId, MembershipStatus.INVITED, "INVITATION_CANCELLED");
  }

  @Override
  public CeremoPage<ProviderMembership> myMemberships(CeremoPageable pageable) {
    return businesses.memberships(accounts.requireActiveAccount().id(), pageable);
  }

  @Override
  public ProviderBusiness accept(ProviderBusinessId id) {
    UUID actor = accounts.requireActiveAccount().id();
    ProviderBusiness business = businesses.findForInvitee(id, actor).orElseThrow(ProviderBusinessService::notFound);
    requireActive(business);
    if (business.ownerId().equals(actor)) {
      return business; // The owner's row is already ACTIVE; accepting again changes nothing and is not audited.
    }
    businesses.accept(id, actor);
    businesses.recordChange(id, actor, "INVITATION_ACCEPTED", actor.toString());
    return business;
  }

  @Override
  public ProviderBusiness suspend(ProviderBusinessId id, String reason) {
    UUID actor = accounts.requireAdministrator().id();
    validateReason(reason);
    ProviderBusiness business = businesses.findById(id).orElseThrow(ProviderBusinessService::notFound).suspend();
    businesses.updateStatus(business);
    businesses.recordChange(id, actor, "SUSPENDED", reason);
    return business;
  }

  @Override
  public ProviderBusiness reactivate(ProviderBusinessId id, String reason) {
    UUID actor = accounts.requireAdministrator().id();
    validateReason(reason);
    ProviderBusiness business = businesses.findById(id).orElseThrow(ProviderBusinessService::notFound).reactivate();
    businesses.updateStatus(business);
    businesses.recordChange(id, actor, "REACTIVATED", reason);
    return business;
  }

  private void removeTeamRow(ProviderBusinessId id, UUID userId, MembershipStatus status, String action) {
    UUID actor = accounts.requireActiveAccount().id();
    ProviderBusiness business = ownerBusiness(id, actor);
    protectOwner(business, userId);
    // Owners may remove access or cancel invitations even while the business is suspended.
    businesses.remove(id, userId, status);
    businesses.recordChange(id, actor, action, userId.toString());
  }

  private ProviderBusiness memberBusiness(ProviderBusinessId id, UUID actor) {
    return businesses.findForMember(id, actor).orElseThrow(ProviderBusinessService::notFound);
  }

  private ProviderBusiness ownerBusiness(ProviderBusinessId id, UUID actor) {
    ProviderBusiness business = memberBusiness(id, actor);
    if (!business.ownerId().equals(actor)) {
      throw CeremoException.builder(ProviderBusinessErrorKey.PROVIDER_BUSINESS_OWNER_REQUIRED)
          .status(ErrorStatus.FORBIDDEN).message("Only the provider owner may manage this team.").build();
    }
    return business;
  }

  private static void requireActive(ProviderBusiness business) {
    if (business.status() != ProviderBusinessStatus.ACTIVE) {
      throw CeremoException.builder(ProviderBusinessErrorKey.PROVIDER_BUSINESS_SUSPENDED)
          .status(ErrorStatus.FORBIDDEN).message("The provider business is suspended.").build();
    }
  }

  private static void protectOwner(ProviderBusiness business, UUID userId) {
    Assert.notNull("userId", userId);
    if (business.ownerId().equals(userId)) {
      throw CeremoException.builder(ProviderBusinessErrorKey.PROVIDER_BUSINESS_OWNER_PROTECTED)
          .status(ErrorStatus.BAD_REQUEST)
          .message("The accountable owner cannot be replaced through team membership.").build();
    }
  }

  private static void validateReason(String reason) {
    Assert.field("reason", reason).notBlank().maxLength(500);
  }

  private static CeremoException notFound() {
    return CeremoException.builder(ProviderBusinessErrorKey.PROVIDER_BUSINESS_NOT_FOUND)
        .status(ErrorStatus.NOT_FOUND).message("The provider business is unavailable.").build();
  }
}
