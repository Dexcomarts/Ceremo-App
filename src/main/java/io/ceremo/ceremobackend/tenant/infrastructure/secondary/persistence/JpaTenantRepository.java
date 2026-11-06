package io.ceremo.ceremobackend.tenant.infrastructure.secondary.persistence;

import io.ceremo.ceremobackend.shared.error.domain.CeremoException;
import io.ceremo.ceremobackend.shared.error.domain.ErrorStatus;
import io.ceremo.ceremobackend.shared.generation.domain.CeremoUUID;
import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantId;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPage;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPageable;
import io.ceremo.ceremobackend.tenant.domain.model.MembershipStatus;
import io.ceremo.ceremobackend.tenant.domain.model.Tenant;
import io.ceremo.ceremobackend.tenant.domain.model.TenantErrorKey;
import io.ceremo.ceremobackend.tenant.domain.model.TenantMember;
import io.ceremo.ceremobackend.tenant.domain.repository.TenantRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.MANDATORY)
public class JpaTenantRepository implements TenantRepository {
  private static final String MEMBER_SCOPE = """
      (t.ownerId = :userId or exists (select m.id from TenantMemberJpaEntity m
       where m.tenantId = t.id and m.userId = :userId and m.status = 'ACTIVE'))
      """;
  private final EntityManager entityManager;

  public JpaTenantRepository(EntityManager entityManager) {
    this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
  }

  @Override
  public void create(Tenant tenant) {
    entityManager.persist(new TenantJpaEntity(tenant));
    entityManager.flush();
  }

  @Override
  public Optional<Tenant> findForMember(TenantId id, UUID userId) {
    // Check membership in a fresh statement after the root lock is acquired. A query's earlier
    // READ COMMITTED snapshot must not authorise a member revoked while it waited for the lock.
    return findById(id).filter(tenant -> tenant.ownerId().equals(userId)
        || findMember(id, userId).filter(member -> member.toDomain().status() == MembershipStatus.ACTIVE).isPresent());
  }

  @Override
  public Optional<Tenant> findForInvitee(TenantId id, UUID userId) {
    return findById(id).filter(tenant -> findMember(id, userId).isPresent());
  }

  @Override
  public Optional<Tenant> findById(TenantId id) {
    return Optional.ofNullable(entityManager.find(TenantJpaEntity.class, id.get(), LockModeType.PESSIMISTIC_WRITE))
        .map(TenantJpaEntity::toDomain);
  }

  @Override
  public CeremoPage<Tenant> findForUser(UUID userId, CeremoPageable pageable) {
    List<Tenant> content = entityManager.createQuery(
            "select t from TenantJpaEntity t where " + MEMBER_SCOPE + " order by t.id", TenantJpaEntity.class)
        .setParameter("userId", userId).setFirstResult(pageable.offset()).setMaxResults(pageable.pageSize())
        .getResultList().stream().map(TenantJpaEntity::toDomain).toList();
    long count = entityManager.createQuery("select count(t) from TenantJpaEntity t where " + MEMBER_SCOPE, Long.class)
        .setParameter("userId", userId).getSingleResult();
    return CeremoPage.builder(content).currentPage(pageable.page()).pageSize(pageable.pageSize())
        .totalElementsCount(count).build();
  }

  @Override
  public CeremoPage<TenantMember> members(TenantId id, CeremoPageable pageable) {
    List<TenantMember> content = entityManager.createQuery(
            "select m from TenantMemberJpaEntity m where m.tenantId = :id order by m.userId", TenantMemberJpaEntity.class)
        .setParameter("id", id.get()).setFirstResult(pageable.offset()).setMaxResults(pageable.pageSize())
        .getResultList().stream().map(TenantMemberJpaEntity::toDomain).toList();
    long count = entityManager.createQuery("select count(m) from TenantMemberJpaEntity m where m.tenantId = :id", Long.class)
        .setParameter("id", id.get()).getSingleResult();
    return CeremoPage.builder(content).currentPage(pageable.page()).pageSize(pageable.pageSize())
        .totalElementsCount(count).build();
  }

  @Override
  public void invite(TenantId id, TenantMember member) {
    if (findMember(id, member.userId()).isPresent()) {
      throw failure(TenantErrorKey.TENANT_MEMBER_EXISTS, ErrorStatus.CONFLICT);
    }
    entityManager.persist(new TenantMemberJpaEntity(CeremoUUID.generate(), id.get(), member));
    entityManager.flush();
  }

  @Override
  public void accept(TenantId id, UUID userId) {
    TenantMemberJpaEntity member = findMember(id, userId).orElseThrow(() ->
        failure(TenantErrorKey.TENANT_MEMBER_NOT_FOUND, ErrorStatus.NOT_FOUND));
    member.accept();
    entityManager.flush();
  }

  @Override
  public void revoke(TenantId id, UUID userId) {
    TenantMemberJpaEntity member = findMember(id, userId).orElseThrow(() ->
        failure(TenantErrorKey.TENANT_MEMBER_NOT_FOUND, ErrorStatus.NOT_FOUND));
    entityManager.remove(member);
    entityManager.flush();
  }

  @Override
  public void updateStatus(Tenant tenant) {
    TenantJpaEntity entity = entityManager.find(TenantJpaEntity.class, tenant.id().get());
    if (entity == null) {
      throw failure(TenantErrorKey.TENANT_NOT_FOUND, ErrorStatus.NOT_FOUND);
    }
    entity.updateStatus(tenant.status());
    entityManager.flush();
  }

  @Override
  public void recordChange(TenantId id, UUID actorId, String action, String reason) {
    entityManager.createNativeQuery("""
        insert into tenant_audit(id, tenant_id, actor_id, action, reason, occurred_at)
        values (:id, :tenantId, :actorId, :action, :reason, current_timestamp)
        """).setParameter("id", CeremoUUID.generate()).setParameter("tenantId", id.get())
        .setParameter("actorId", actorId).setParameter("action", action).setParameter("reason", reason).executeUpdate();
  }

  private Optional<TenantMemberJpaEntity> findMember(TenantId id, UUID userId) {
    return entityManager.createQuery("""
        select m from TenantMemberJpaEntity m where m.tenantId = :id and m.userId = :userId
        """, TenantMemberJpaEntity.class).setParameter("id", id.get()).setParameter("userId", userId)
        .getResultStream().findFirst();
  }

  private static CeremoException failure(TenantErrorKey key, ErrorStatus status) {
    return CeremoException.builder(key).status(status).message("The requested team operation is unavailable.").build();
  }
}
