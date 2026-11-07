package io.ceremo.ceremobackend.shared.multitenancy.domain;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.ceremo.ceremobackend.shared.multitenancy.application.NoTenantContextException;

class TenantScopedEntityTest {

  @Test
  void persistenceConstructorAllowsTenantHydrationBeforePersist() {
    var entity = new TestTenantScopedEntity();
    UUID tenant = io.ceremo.ceremobackend.shared.generation.domain.CeremoUUID.generate();
    entity.setTenantIdPublic(tenant);
    entity.prePersistPublic();
    assertThat(entity.getTenantId()).isEqualTo(tenant);
  }

  @BeforeEach
  void setUp() {
    TenantContext.clear();
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void shouldCreateEntityWithId() {
    UUID id = UUID.randomUUID();
    TestTenantScopedEntity entity = new TestTenantScopedEntity(id);

    assertThat(entity.getId()).isEqualTo(id);
    assertThat(entity.getTenantId()).isNull();
  }

  @Test
  void shouldSetTenantIdWithUUID() {
    UUID id = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    TestTenantScopedEntity entity = new TestTenantScopedEntity(id);

    entity.setTenantIdPublic(tenantId);

    assertThat(entity.getTenantId()).isEqualTo(tenantId);
  }

  @Test
  void shouldSetTenantIdWithTenantIdValueObject() {
    UUID id = UUID.randomUUID();
    TenantId tenantId = TenantId.of(UUID.randomUUID());
    TestTenantScopedEntity entity = new TestTenantScopedEntity(id);

    entity.setTenantIdPublic(tenantId);

    assertThat(entity.getTenantId()).isEqualTo(tenantId.get());
  }

  @Test
  void shouldSetNullWhenTenantIdValueObjectIsNull() {
    UUID id = UUID.randomUUID();
    TestTenantScopedEntity entity = new TestTenantScopedEntity(id);
    entity.setTenantIdPublic(UUID.randomUUID());

    entity.setTenantIdPublic((TenantId) null);

    assertThat(entity.getTenantId()).isNull();
  }

  @Test
  void shouldSetTenantIdFromContextOnPrePersist() {
    UUID id = UUID.randomUUID();
    TenantId tenantId = TenantId.of(UUID.randomUUID());
    TenantInfo tenantInfo = new TenantInfo(tenantId, "Test Tenant", "FREE", 1);
    TenantContext.setCurrentTenant(tenantInfo);

    TestTenantScopedEntity entity = new TestTenantScopedEntity(id);
    entity.prePersistPublic();

    assertThat(entity.getTenantId()).isEqualTo(tenantId.get());
  }

  @Test
  void shouldNotOverrideTenantIdOnPrePersistWhenAlreadySet() {
    UUID id = UUID.randomUUID();
    UUID existingTenantId = UUID.randomUUID();
    TenantId contextTenantId = TenantId.of(UUID.randomUUID());
    TenantInfo tenantInfo = new TenantInfo(contextTenantId, "Test Tenant", "FREE", 1);
    TenantContext.setCurrentTenant(tenantInfo);

    TestTenantScopedEntity entity = new TestTenantScopedEntity(id);
    entity.setTenantIdPublic(existingTenantId);
    entity.prePersistPublic();

    assertThat(entity.getTenantId()).isEqualTo(existingTenantId);
  }

  @Test
  void shouldThrowWhenPrePersistWithNoTenantContext() {
    UUID id = UUID.randomUUID();
    TestTenantScopedEntity entity = new TestTenantScopedEntity(id);

    assertThatThrownBy(entity::prePersistPublic)
      .isInstanceOf(NoTenantContextException.class)
      .hasMessageContaining("No tenant context available");
  }

  @Test
  void shouldHaveCorrectFilterName() {
    assertThat(TenantScopedEntity.TENANT_FILTER_NAME).isEqualTo("tenantFilter");
  }

  private static class TestTenantScopedEntity extends TenantScopedEntity {
    TestTenantScopedEntity() { super(); }
    TestTenantScopedEntity(UUID id) {
      super(id);
    }

    void setTenantIdPublic(UUID tenantId) {
      setTenantId(tenantId);
    }

    void setTenantIdPublic(TenantId tenantId) {
      setTenantId(tenantId);
    }

    void prePersistPublic() {
      prePersist();
    }
  }
}
