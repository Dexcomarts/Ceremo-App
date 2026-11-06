package io.ceremo.ceremobackend.tenant.infrastructure.secondary.persistence;

import io.ceremo.ceremobackend.shared.domain.BaseEntity;
import io.ceremo.ceremobackend.tenant.domain.model.MemberRole;
import io.ceremo.ceremobackend.tenant.domain.model.MembershipStatus;
import io.ceremo.ceremobackend.tenant.domain.model.TenantMember;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(name = "tenant_member", uniqueConstraints = @UniqueConstraint(
    name = "uq_tenant_member", columnNames = {"tenant_id", "user_id"}))
class TenantMemberJpaEntity extends BaseEntity {
  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;
  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MemberRole role;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MembershipStatus status;

  protected TenantMemberJpaEntity() {}

  TenantMemberJpaEntity(UUID id, UUID tenantId, TenantMember member) {
    super(id);
    this.tenantId = tenantId;
    userId = member.userId();
    role = member.role();
    status = member.status();
  }

  TenantMember toDomain() { return new TenantMember(userId, role, status); }
  void accept() { status = MembershipStatus.ACTIVE; }
}
