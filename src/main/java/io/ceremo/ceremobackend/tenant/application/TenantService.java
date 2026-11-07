package io.ceremo.ceremobackend.tenant.application;

import io.ceremo.ceremobackend.account.application.port.in.Accounts;
import io.ceremo.ceremobackend.shared.error.domain.Assert;
import io.ceremo.ceremobackend.shared.error.domain.CeremoException;
import io.ceremo.ceremobackend.shared.error.domain.ErrorStatus;
import io.ceremo.ceremobackend.shared.generation.domain.CeremoUUID;
import io.ceremo.ceremobackend.shared.multitenancy.application.TenantSuspendedException;
import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantId;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPage;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPageable;
import io.ceremo.ceremobackend.tenant.application.port.in.TenantAccounts;
import io.ceremo.ceremobackend.tenant.domain.model.MemberRole;
import io.ceremo.ceremobackend.tenant.domain.model.MembershipStatus;
import io.ceremo.ceremobackend.tenant.domain.model.Tenant;
import io.ceremo.ceremobackend.tenant.domain.model.TenantErrorKey;
import io.ceremo.ceremobackend.tenant.domain.model.TenantMember;
import io.ceremo.ceremobackend.tenant.domain.model.TenantStatus;
import io.ceremo.ceremobackend.tenant.domain.repository.TenantRepository;
import io.ceremo.ceremobackend.account.application.port.in.UserDirectory;
import io.ceremo.ceremobackend.account.domain.exception.UserErrorKey;
import java.util.Objects;
import java.util.UUID;

public final class TenantService implements TenantAccounts {
  private final Accounts accounts;
  private final UserDirectory users;
  private final TenantRepository tenants;

  public TenantService(Accounts accounts, UserDirectory users, TenantRepository tenants) {
    this.accounts = Objects.requireNonNull(accounts, "accounts");
    this.users = Objects.requireNonNull(users, "users");
    this.tenants = Objects.requireNonNull(tenants, "tenants");
  }

  @Override
  public Tenant create(String name) {
    UUID actor = accounts.requireActiveAccount().id();
    Tenant tenant = Tenant.create(TenantId.of(CeremoUUID.generate()), name, actor);
    tenants.create(tenant);
    tenants.recordChange(tenant.id(), actor, "CREATED", "Provider workspace created");
    return tenant;
  }

  @Override
  public Tenant get(TenantId id) {
    return memberTenant(id, accounts.requireActiveAccount().id());
  }

  @Override
  public CeremoPage<Tenant> mine(CeremoPageable pageable) {
    return tenants.findForUser(accounts.requireActiveAccount().id(), pageable);
  }

  @Override
  public CeremoPage<TenantMember> members(TenantId id, CeremoPageable pageable) {
    UUID actor = accounts.requireActiveAccount().id();
    ownerTenant(id, actor);
    return tenants.members(id, pageable);
  }

  @Override
  public void invite(TenantId id, UUID userId, MemberRole role) {
    UUID actor = accounts.requireActiveAccount().id();
    Tenant tenant = ownerTenant(id, actor);
    requireActive(tenant);
    protectOwner(tenant, userId);
    if (!users.isRegistered(userId)) {
      throw CeremoException.builder(UserErrorKey.USER_NOT_FOUND).status(ErrorStatus.NOT_FOUND)
          .message("The invited account is not registered.").build();
    }
    tenants.invite(id, new TenantMember(userId, role, MembershipStatus.INVITED));
    tenants.recordChange(id, actor, "MEMBER_INVITED", userId.toString());
  }

  @Override
  public Tenant accept(TenantId id) {
    UUID actor = accounts.requireActiveAccount().id();
    Tenant tenant = tenants.findForInvitee(id, actor).orElseThrow(TenantService::notFound);
    requireActive(tenant);
    tenants.accept(id, actor);
    tenants.recordChange(id, actor, "INVITATION_ACCEPTED", actor.toString());
    return tenant;
  }

  @Override
  public void revoke(TenantId id, UUID userId) {
    UUID actor = accounts.requireActiveAccount().id();
    Tenant tenant = ownerTenant(id, actor);
    protectOwner(tenant, userId);
    // Owners may remove access even while the business is suspended.
    tenants.revoke(id, userId);
    tenants.recordChange(id, actor, "MEMBER_REVOKED", userId.toString());
  }

  @Override
  public Tenant suspend(TenantId id, String reason) {
    UUID actor = accounts.requireAdministrator().id();
    validateReason(reason);
    Tenant tenant = tenants.findById(id).orElseThrow(TenantService::notFound).suspend();
    tenants.updateStatus(tenant);
    tenants.recordChange(id, actor, "SUSPENDED", reason);
    return tenant;
  }

  @Override
  public Tenant reactivate(TenantId id, String reason) {
    UUID actor = accounts.requireAdministrator().id();
    validateReason(reason);
    Tenant tenant = tenants.findById(id).orElseThrow(TenantService::notFound).reactivate();
    tenants.updateStatus(tenant);
    tenants.recordChange(id, actor, "REACTIVATED", reason);
    return tenant;
  }

  private Tenant memberTenant(TenantId id, UUID actor) {
    return tenants.findForMember(id, actor).orElseThrow(TenantService::notFound);
  }

  private Tenant ownerTenant(TenantId id, UUID actor) {
    Tenant tenant = memberTenant(id, actor);
    if (!tenant.ownerId().equals(actor)) {
      throw CeremoException.builder(TenantErrorKey.TENANT_OWNER_REQUIRED).status(ErrorStatus.FORBIDDEN)
          .message("Only the provider owner may manage this team.").build();
    }
    return tenant;
  }

  private static void requireActive(Tenant tenant) {
    if (tenant.status() != TenantStatus.ACTIVE) {
      throw TenantSuspendedException.suspended(tenant.id());
    }
  }

  private static void protectOwner(Tenant tenant, UUID userId) {
    Assert.notNull("userId", userId);
    if (tenant.ownerId().equals(userId)) {
      throw CeremoException.builder(TenantErrorKey.TENANT_OWNER_PROTECTED).status(ErrorStatus.BAD_REQUEST)
          .message("The accountable owner cannot be replaced through team membership.").build();
    }
  }

  private static void validateReason(String reason) {
    Assert.field("reason", reason).notBlank().maxLength(500);
  }

  private static CeremoException notFound() {
    return CeremoException.builder(TenantErrorKey.TENANT_NOT_FOUND).status(ErrorStatus.NOT_FOUND)
        .message("The provider business is unavailable.").build();
  }
}
