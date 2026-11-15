package io.ceremo.ceremobackend.providerbusiness.infrastructure.primary.transaction;

import io.ceremo.ceremobackend.providerbusiness.application.port.in.ProviderBusinesses;
import io.ceremo.ceremobackend.providerbusiness.domain.model.MemberRole;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusiness;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusinessId;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusinessMember;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderMembership;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPage;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPageable;
import java.util.Objects;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Locks, membership changes and their audit entry share one transaction. Only reads that take no row
 * lock are read-only, because PostgreSQL rejects SELECT ... FOR UPDATE in a read-only transaction.
 */
@Transactional
public class TransactionalProviderBusinesses implements ProviderBusinesses {
  private final ProviderBusinesses delegate;

  public TransactionalProviderBusinesses(ProviderBusinesses delegate) {
    this.delegate = Objects.requireNonNull(delegate, "delegate");
  }

  @Override public ProviderBusiness create(String name) { return delegate.create(name); }
  @Override public ProviderBusiness get(ProviderBusinessId id) { return delegate.get(id); }
  @Override @Transactional(readOnly = true)
  public CeremoPage<ProviderBusiness> mine(CeremoPageable page) { return delegate.mine(page); }
  @Override public CeremoPage<ProviderBusinessMember> members(ProviderBusinessId id, CeremoPageable page) {
    return delegate.members(id, page);
  }
  @Override public CeremoPage<ProviderBusinessMember> invitations(ProviderBusinessId id, CeremoPageable page) {
    return delegate.invitations(id, page);
  }
  @Override public void invite(ProviderBusinessId id, UUID userId, MemberRole role) { delegate.invite(id, userId, role); }
  @Override public void removeMember(ProviderBusinessId id, UUID userId) { delegate.removeMember(id, userId); }
  @Override public void cancelInvitation(ProviderBusinessId id, UUID userId) { delegate.cancelInvitation(id, userId); }
  @Override @Transactional(readOnly = true)
  public CeremoPage<ProviderMembership> myMemberships(CeremoPageable page) { return delegate.myMemberships(page); }
  @Override public ProviderBusiness accept(ProviderBusinessId id) { return delegate.accept(id); }
  @Override public ProviderBusiness suspend(ProviderBusinessId id, String reason) { return delegate.suspend(id, reason); }
  @Override public ProviderBusiness reactivate(ProviderBusinessId id, String reason) { return delegate.reactivate(id, reason); }
}
