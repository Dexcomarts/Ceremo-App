package io.ceremo.ceremobackend.shared.multitenancy.application;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.ceremo.ceremobackend.shared.error.domain.ErrorStatus;

class NoTenantContextExceptionTest {

  @Test
  void shouldCreateMissingTenantException() {
    NoTenantContextException exception = NoTenantContextException.missingTenant();

    assertThat(exception.key()).isEqualTo(MultitenancyErrorKey.NO_TENANT_CONTEXT);
    assertThat(exception.status()).isEqualTo(ErrorStatus.BAD_REQUEST);
    assertThat(exception.getMessage()).contains("No tenant context available");
  }
}
