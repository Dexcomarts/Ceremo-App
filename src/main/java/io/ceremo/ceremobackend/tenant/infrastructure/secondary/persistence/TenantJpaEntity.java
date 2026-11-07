package io.ceremo.ceremobackend.tenant.infrastructure.secondary.persistence;

import io.ceremo.ceremobackend.shared.domain.BaseEntity;
import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantId;
import io.ceremo.ceremobackend.tenant.domain.model.Tenant;
import io.ceremo.ceremobackend.tenant.domain.model.TenantStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "tenant")
class TenantJpaEntity extends BaseEntity {
  @Column(nullable = false, length = 160)
  private String name;
  @Column(name = "owner_id", nullable = false, updatable = false)
  private UUID ownerId;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TenantStatus status;

  protected TenantJpaEntity() {}

  TenantJpaEntity(Tenant tenant) {
    super(tenant.id().get());
    name = tenant.name();
    ownerId = tenant.ownerId();
    status = tenant.status();
  }

  Tenant toDomain() { return new Tenant(TenantId.of(getId()), name, ownerId, status); }
  void updateStatus(TenantStatus status) { this.status = status; }
}
