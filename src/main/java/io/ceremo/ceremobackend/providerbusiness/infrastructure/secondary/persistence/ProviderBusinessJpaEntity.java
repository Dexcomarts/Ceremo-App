package io.ceremo.ceremobackend.providerbusiness.infrastructure.secondary.persistence;

import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusiness;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusinessId;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusinessStatus;
import io.ceremo.ceremobackend.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "provider_business")
class ProviderBusinessJpaEntity extends BaseEntity {
  @Column(nullable = false, length = 160)
  private String name;
  @Column(name = "owner_id", nullable = false, updatable = false)
  private UUID ownerId;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ProviderBusinessStatus status;

  protected ProviderBusinessJpaEntity() {}

  ProviderBusinessJpaEntity(ProviderBusiness business) {
    super(business.id().value());
    name = business.name();
    ownerId = business.ownerId();
    status = business.status();
  }

  ProviderBusiness toDomain() { return new ProviderBusiness(ProviderBusinessId.of(getId()), name, ownerId, status); }
  void updateStatus(ProviderBusinessStatus status) { this.status = status; }
}
