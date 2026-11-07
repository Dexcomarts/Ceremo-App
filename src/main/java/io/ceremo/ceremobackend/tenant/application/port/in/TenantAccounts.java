package io.ceremo.ceremobackend.tenant.application.port.in;

import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantId;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPage;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPageable;
import io.ceremo.ceremobackend.tenant.domain.model.MemberRole;
import io.ceremo.ceremobackend.tenant.domain.model.Tenant;
import io.ceremo.ceremobackend.tenant.domain.model.TenantMember;
import java.util.UUID;

public interface TenantAccounts {
  Tenant create(String name);
  Tenant get(TenantId id);
  CeremoPage<Tenant> mine(CeremoPageable pageable);
  CeremoPage<TenantMember> members(TenantId id, CeremoPageable pageable);
  void invite(TenantId id, UUID userId, MemberRole role);
  Tenant accept(TenantId id);
  void revoke(TenantId id, UUID userId);
  Tenant suspend(TenantId id, String reason);
  Tenant reactivate(TenantId id, String reason);
}
