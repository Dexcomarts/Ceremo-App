package io.ceremo.ceremobackend.shared.multitenancy.application;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;


class MultitenancyErrorKeyTest {

  @Test
  void shouldHaveCorrectKeyForNoTenantContext() {
    assertThat(MultitenancyErrorKey.NO_TENANT_CONTEXT.get()).isEqualTo("no-tenant-context");
  }

  @Test
  void shouldHaveCorrectKeyForTenantSuspended() {
    assertThat(MultitenancyErrorKey.TENANT_SUSPENDED.get()).isEqualTo("tenant-suspended");
  }

  @Test
  void shouldHaveAllExpectedValues() {
    assertThat(MultitenancyErrorKey.values())
      .containsExactlyInAnyOrder(MultitenancyErrorKey.NO_TENANT_CONTEXT, MultitenancyErrorKey.TENANT_SUSPENDED);
  }
}
