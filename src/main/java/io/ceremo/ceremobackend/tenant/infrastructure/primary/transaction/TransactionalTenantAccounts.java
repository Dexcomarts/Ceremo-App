package io.ceremo.ceremobackend.tenant.infrastructure.primary.transaction;

import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantId;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPage;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPageable;
import io.ceremo.ceremobackend.tenant.application.port.in.TenantAccounts;
import io.ceremo.ceremobackend.tenant.domain.model.MemberRole;
import io.ceremo.ceremobackend.tenant.domain.model.Tenant;
import io.ceremo.ceremobackend.tenant.domain.model.TenantMember;
import java.util.Objects;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** Locks, membership changes and their audit entry share one transaction. */
@Transactional
public class TransactionalTenantAccounts implements TenantAccounts {
  private final TenantAccounts delegate;

  public TransactionalTenantAccounts(TenantAccounts delegate) {
    this.delegate = Objects.requireNonNull(delegate, "delegate");
  }

  @Override public Tenant create(String name) { return delegate.create(name); }
  @Override public Tenant get(TenantId id) { return delegate.get(id); }
  @Override @Transactional(readOnly = true)
  public CeremoPage<Tenant> mine(CeremoPageable page) { return delegate.mine(page); }
  @Override public CeremoPage<TenantMember> members(TenantId id, CeremoPageable page) { return delegate.members(id, page); }
  @Override public void invite(TenantId id, UUID userId, MemberRole role) { delegate.invite(id, userId, role); }
  @Override public Tenant accept(TenantId id) { return delegate.accept(id); }
  @Override public void revoke(TenantId id, UUID userId) { delegate.revoke(id, userId); }
  @Override public Tenant suspend(TenantId id, String reason) { return delegate.suspend(id, reason); }
  @Override public Tenant reactivate(TenantId id, String reason) { return delegate.reactivate(id, reason); }
}
