package io.ceremo.ceremobackend.shared.multitenancy.application;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.ceremo.ceremobackend.shared.error.domain.ErrorStatus;
import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantId;

class TenantSuspendedExceptionTest {

  @Test
  void shouldCreateSuspendedException() {
    TenantId tenantId = TenantId.of(UUID.randomUUID());

    TenantSuspendedException exception = TenantSuspendedException.suspended(tenantId);

    assertThat(exception.key()).isEqualTo(MultitenancyErrorKey.TENANT_SUSPENDED);
    assertThat(exception.status()).isEqualTo(ErrorStatus.FORBIDDEN);
    assertThat(exception.parameters()).containsEntry("tenantId", tenantId.toString());
    assertThat(exception.getMessage()).contains(tenantId.toString()).contains("suspended");
  }
}
