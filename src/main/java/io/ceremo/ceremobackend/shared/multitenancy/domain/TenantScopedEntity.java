package io.ceremo.ceremobackend.shared.multitenancy.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import io.ceremo.ceremobackend.shared.domain.BaseEntity;

@MappedSuperclass
@FilterDef(
    name = TenantScopedEntity.TENANT_FILTER_NAME,
    parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = TenantScopedEntity.TENANT_FILTER_NAME, condition = "tenant_id = :tenantId")
@SuppressWarnings("squid:S2160")
public abstract class TenantScopedEntity extends BaseEntity {

  public static final String TENANT_FILTER_NAME = "tenantFilter";

  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  protected TenantScopedEntity() {}

  protected TenantScopedEntity(UUID id) {
    super(id);
  }

  @PrePersist
  protected void prePersist() {
    if (tenantId == null) {
      tenantId = TenantContext.requireCurrentTenantId().get();
    }
  }

  public UUID getTenantId() {
    return tenantId;
  }

  protected void setTenantId(UUID tenantId) {
    this.tenantId = tenantId;
  }

  protected void setTenantId(TenantId tenantId) {
    this.tenantId = tenantId != null ? tenantId.get() : null;
  }
}
