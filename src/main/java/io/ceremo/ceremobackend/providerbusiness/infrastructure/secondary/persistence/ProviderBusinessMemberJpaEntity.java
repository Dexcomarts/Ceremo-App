package io.ceremo.ceremobackend.providerbusiness.infrastructure.secondary.persistence;

import io.ceremo.ceremobackend.providerbusiness.domain.model.MemberRole;
import io.ceremo.ceremobackend.providerbusiness.domain.model.MembershipStatus;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusiness;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusinessMember;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderMembership;
import io.ceremo.ceremobackend.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(name = "provider_business_member", uniqueConstraints = @UniqueConstraint(
    name = "uq_provider_business_member", columnNames = {"provider_business_id", "user_id"}))
class ProviderBusinessMemberJpaEntity extends BaseEntity {
  @Column(name = "provider_business_id", nullable = false, updatable = false)
  private UUID providerBusinessId;
  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MemberRole role;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MembershipStatus status;

  protected ProviderBusinessMemberJpaEntity() {}

  ProviderBusinessMemberJpaEntity(UUID id, UUID providerBusinessId, ProviderBusinessMember member) {
    super(id);
    this.providerBusinessId = providerBusinessId;
    userId = member.userId();
    role = member.role();
    status = member.status();
  }

  ProviderBusinessMember toDomain() { return new ProviderBusinessMember(userId, role, status); }

  ProviderMembership toMembership(ProviderBusiness business) {
    return new ProviderMembership(business.id(), business.name(), business.status(), role, status);
  }

  void accept() { status = MembershipStatus.ACTIVE; }

  // Identity is the assigned id only (BaseEntity); mutable mapped fields are deliberately excluded.
  @Override
  public boolean equals(Object other) { return super.equals(other); }

  @Override
  public int hashCode() { return super.hashCode(); }
}
