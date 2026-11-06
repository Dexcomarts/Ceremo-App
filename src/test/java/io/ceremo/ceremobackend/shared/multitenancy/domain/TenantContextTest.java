package io.ceremo.ceremobackend.shared.multitenancy.domain;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.ceremo.ceremobackend.shared.multitenancy.application.NoTenantContextException;

class TenantContextTest {

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void shouldSetAndGetCurrentTenant() {
    TenantId id = TenantId.of(UUID.randomUUID());
    TenantInfo info = new TenantInfo(id, "Test", "FREE", 1);

    TenantContext.setCurrentTenant(info);

    assertThat(TenantContext.getCurrentTenant()).isPresent().contains(info);
    assertThat(TenantContext.getCurrentTenantId()).isPresent().contains(id);
  }

  @Test
  void shouldReturnEmptyWhenNoTenantSet() {
    assertThat(TenantContext.getCurrentTenant()).isEmpty();
    assertThat(TenantContext.getCurrentTenantId()).isEmpty();
  }

  @Test
  void shouldClearCurrentTenant() {
    TenantId id = TenantId.of(UUID.randomUUID());
    TenantInfo info = new TenantInfo(id, "Test", "FREE", 1);

    TenantContext.setCurrentTenant(info);
    TenantContext.clear();

    assertThat(TenantContext.getCurrentTenant()).isEmpty();
  }

  @Test
  void shouldRequireCurrentTenant() {
    TenantId id = TenantId.of(UUID.randomUUID());
    TenantInfo info = new TenantInfo(id, "Test", "FREE", 1);

    TenantContext.setCurrentTenant(info);

    assertThat(TenantContext.requireCurrentTenant()).isEqualTo(info);
    assertThat(TenantContext.requireCurrentTenantId()).isEqualTo(id);
  }

  @Test
  void shouldThrowWhenRequireCurrentTenantNotSet() {
    assertThatThrownBy(TenantContext::requireCurrentTenant)
      .isExactlyInstanceOf(NoTenantContextException.class)
      .hasMessageContaining("No tenant context available");
  }

  @Test
  void shouldThrowWhenRequireCurrentTenantIdNotSet() {
    assertThatThrownBy(TenantContext::requireCurrentTenantId)
      .isExactlyInstanceOf(NoTenantContextException.class)
      .hasMessageContaining("No tenant context available");
  }

  @Test
  void shouldCheckHasCurrentTenant() {
    assertThat(TenantContext.hasCurrentTenant()).isFalse();

    TenantId id = TenantId.of(UUID.randomUUID());
    TenantInfo info = new TenantInfo(id, "Test", "FREE", 1);
    TenantContext.setCurrentTenant(info);

    assertThat(TenantContext.hasCurrentTenant()).isTrue();
  }
}
