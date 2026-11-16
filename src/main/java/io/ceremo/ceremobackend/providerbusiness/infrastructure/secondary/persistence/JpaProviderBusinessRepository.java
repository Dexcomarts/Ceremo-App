package io.ceremo.ceremobackend.providerbusiness.infrastructure.secondary.persistence;

import io.ceremo.ceremobackend.providerbusiness.domain.model.MembershipStatus;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusiness;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusinessErrorKey;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusinessId;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusinessMember;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderMembership;
import io.ceremo.ceremobackend.providerbusiness.domain.repository.ProviderBusinessRepository;
import io.ceremo.ceremobackend.shared.error.domain.CeremoException;
import io.ceremo.ceremobackend.shared.error.domain.ErrorStatus;
import io.ceremo.ceremobackend.shared.generation.domain.CeremoUUID;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPage;
import io.ceremo.ceremobackend.shared.pagination.domain.CeremoPageable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.MANDATORY)
public class JpaProviderBusinessRepository implements ProviderBusinessRepository {
  private static final String MEMBER_SCOPE = """
      (b.ownerId = :userId or exists (select m.id from ProviderBusinessMemberJpaEntity m
       where m.providerBusinessId = b.id and m.userId = :userId and m.status = 'ACTIVE'))
      """;
  private static final String USER_ID = "userId";
  private final EntityManager entityManager;

  public JpaProviderBusinessRepository(EntityManager entityManager) {
    this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
  }

  @Override
  public void create(ProviderBusiness business) {
    entityManager.persist(new ProviderBusinessJpaEntity(business));
    // The member row has no mapped association, so order_inserts cannot see the foreign key: write the root first.
    entityManager.flush();
    entityManager.persist(new ProviderBusinessMemberJpaEntity(CeremoUUID.generate(), business.id().value(),
        business.ownerMember()));
    entityManager.flush();
  }

  @Override
  public Optional<ProviderBusiness> findForMember(ProviderBusinessId id, UUID userId) {
    // Check membership in a fresh statement after the root lock is acquired. A query's earlier
    // READ COMMITTED snapshot must not authorise a member revoked while it waited for the lock.
    return findById(id).filter(business -> business.ownerId().equals(userId)
        || findMember(id, userId).filter(member -> member.toDomain().status() == MembershipStatus.ACTIVE).isPresent());
  }

  @Override
  public Optional<ProviderBusiness> findForInvitee(ProviderBusinessId id, UUID userId) {
    return findById(id).filter(business -> findMember(id, userId).isPresent());
  }

  @Override
  public Optional<ProviderBusiness> findById(ProviderBusinessId id) {
    return Optional.ofNullable(entityManager.find(ProviderBusinessJpaEntity.class, id.value(),
        LockModeType.PESSIMISTIC_WRITE)).map(ProviderBusinessJpaEntity::toDomain);
  }

  @Override
  public CeremoPage<ProviderBusiness> findForUser(UUID userId, CeremoPageable pageable) {
    List<ProviderBusiness> content = entityManager.createQuery(
            "select b from ProviderBusinessJpaEntity b where " + MEMBER_SCOPE + " order by b.id",
            ProviderBusinessJpaEntity.class)
        .setParameter(USER_ID, userId).setFirstResult(pageable.offset()).setMaxResults(pageable.pageSize())
        .getResultList().stream().map(ProviderBusinessJpaEntity::toDomain).toList();
    long count = entityManager.createQuery(
            "select count(b) from ProviderBusinessJpaEntity b where " + MEMBER_SCOPE, Long.class)
        .setParameter(USER_ID, userId).getSingleResult();
    return page(content, pageable, count);
  }

  @Override
  public CeremoPage<ProviderBusinessMember> members(ProviderBusinessId id, MembershipStatus status,
      CeremoPageable pageable) {
    List<ProviderBusinessMember> content = entityManager.createQuery("""
            select m from ProviderBusinessMemberJpaEntity m
            where m.providerBusinessId = :id and m.status = :status order by m.userId
            """, ProviderBusinessMemberJpaEntity.class)
        .setParameter("id", id.value()).setParameter("status", status)
        .setFirstResult(pageable.offset()).setMaxResults(pageable.pageSize())
        .getResultList().stream().map(ProviderBusinessMemberJpaEntity::toDomain).toList();
    long count = entityManager.createQuery("""
            select count(m) from ProviderBusinessMemberJpaEntity m
            where m.providerBusinessId = :id and m.status = :status
            """, Long.class)
        .setParameter("id", id.value()).setParameter("status", status).getSingleResult();
    return page(content, pageable, count);
  }

  @Override
  public CeremoPage<ProviderMembership> memberships(UUID userId, CeremoPageable pageable) {
    List<ProviderMembership> content = entityManager.createQuery("""
            select m, b from ProviderBusinessMemberJpaEntity m, ProviderBusinessJpaEntity b
            where b.id = m.providerBusinessId and m.userId = :userId order by b.id
            """, Object[].class)
        .setParameter(USER_ID, userId).setFirstResult(pageable.offset()).setMaxResults(pageable.pageSize())
        .getResultList().stream()
        .map(row -> ((ProviderBusinessMemberJpaEntity) row[0]).toMembership(((ProviderBusinessJpaEntity) row[1]).toDomain()))
        .toList();
    long count = entityManager.createQuery(
            "select count(m) from ProviderBusinessMemberJpaEntity m where m.userId = :userId", Long.class)
        .setParameter(USER_ID, userId).getSingleResult();
    return page(content, pageable, count);
  }

  @Override
  public void invite(ProviderBusinessId id, ProviderBusinessMember member) {
    if (findMember(id, member.userId()).isPresent()) {
      throw failure(ProviderBusinessErrorKey.PROVIDER_BUSINESS_MEMBER_EXISTS, ErrorStatus.CONFLICT);
    }
    entityManager.persist(new ProviderBusinessMemberJpaEntity(CeremoUUID.generate(), id.value(), member));
    entityManager.flush();
  }

  @Override
  public void accept(ProviderBusinessId id, UUID userId) {
    ProviderBusinessMemberJpaEntity member = findMember(id, userId).orElseThrow(() ->
        failure(ProviderBusinessErrorKey.PROVIDER_BUSINESS_MEMBER_NOT_FOUND, ErrorStatus.NOT_FOUND));
    member.accept();
    entityManager.flush();
  }

  @Override
  public void remove(ProviderBusinessId id, UUID userId, MembershipStatus status) {
    ProviderBusinessMemberJpaEntity row = findMember(id, userId)
        .filter(member -> member.toDomain().status() == status)
        .orElseThrow(() -> failure(status == MembershipStatus.ACTIVE
            ? ProviderBusinessErrorKey.PROVIDER_BUSINESS_MEMBER_NOT_FOUND
            : ProviderBusinessErrorKey.PROVIDER_BUSINESS_INVITATION_NOT_FOUND, ErrorStatus.NOT_FOUND));
    entityManager.remove(row);
    entityManager.flush();
  }

  @Override
  public void updateStatus(ProviderBusiness business) {
    ProviderBusinessJpaEntity entity = entityManager.find(ProviderBusinessJpaEntity.class, business.id().value());
    if (entity == null) {
      throw failure(ProviderBusinessErrorKey.PROVIDER_BUSINESS_NOT_FOUND, ErrorStatus.NOT_FOUND);
    }
    entity.updateStatus(business.status());
    entityManager.flush();
  }

  @Override
  public void recordChange(ProviderBusinessId id, UUID actorId, String action, String reason) {
    entityManager.createNativeQuery("""
        insert into provider_business_audit(id, provider_business_id, actor_id, action, reason, occurred_at)
        values (:id, :businessId, :actorId, :action, :reason, current_timestamp)
        """).setParameter("id", CeremoUUID.generate()).setParameter("businessId", id.value())
        .setParameter("actorId", actorId).setParameter("action", action).setParameter("reason", reason).executeUpdate();
  }

  private Optional<ProviderBusinessMemberJpaEntity> findMember(ProviderBusinessId id, UUID userId) {
    return entityManager.createQuery("""
        select m from ProviderBusinessMemberJpaEntity m where m.providerBusinessId = :id and m.userId = :userId
        """, ProviderBusinessMemberJpaEntity.class).setParameter("id", id.value()).setParameter(USER_ID, userId)
        .getResultStream().findFirst();
  }

  private static <T> CeremoPage<T> page(List<T> content, CeremoPageable pageable, long count) {
    return CeremoPage.builder(content).currentPage(pageable.page()).pageSize(pageable.pageSize())
        .totalElementsCount(count).build();
  }

  private static CeremoException failure(ProviderBusinessErrorKey key, ErrorStatus status) {
    return CeremoException.builder(key).status(status).message("The requested team operation is unavailable.").build();
  }
}
