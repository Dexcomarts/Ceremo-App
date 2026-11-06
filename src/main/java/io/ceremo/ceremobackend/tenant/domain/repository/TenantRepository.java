package io.ceremo.ceremobackend.tenant.domain.repository;

import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantId;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPage;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPageable;
import io.ceremo.ceremobackend.tenant.domain.model.Tenant;
import io.ceremo.ceremobackend.tenant.domain.model.TenantMember;
import java.util.Optional;
import java.util.UUID;

public interface TenantRepository {
  void create(Tenant tenant);
  // Point lookups lock the root until the complete use case commits, including membership changes.
  Optional<Tenant> findForMember(TenantId id, UUID userId);
  Optional<Tenant> findForInvitee(TenantId id, UUID userId);
  Optional<Tenant> findById(TenantId id);
  CeremoPage<Tenant> findForUser(UUID userId, CeremoPageable pageable);
  CeremoPage<TenantMember> members(TenantId id, CeremoPageable pageable);
  void invite(TenantId id, TenantMember member);
  void accept(TenantId id, UUID userId);
  void revoke(TenantId id, UUID userId);
  void updateStatus(Tenant tenant);
  void recordChange(TenantId id, UUID actorId, String action, String reason);
}
