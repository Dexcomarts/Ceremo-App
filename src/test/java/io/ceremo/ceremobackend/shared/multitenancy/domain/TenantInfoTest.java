package io.ceremo.ceremobackend.shared.multitenancy.domain;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.ceremo.ceremobackend.shared.error.domain.MissingMandatoryValueException;
import io.ceremo.ceremobackend.shared.error.domain.NumberValueTooLowException;
import io.ceremo.ceremobackend.shared.error.domain.StringTooLongException;

class TenantInfoTest {

  @Test
  void shouldNotBuildWithoutId() {
    assertThatThrownBy(() -> new TenantInfo(null, "Test", "FREE", 1))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id");
  }

  @Test
  void shouldNotBuildWithoutName() {
    TenantId id = TenantId.of(UUID.randomUUID());
    assertThatThrownBy(() -> new TenantInfo(id, null, "FREE", 1))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("name");
  }

  @Test
  void shouldNotBuildWithBlankName() {
    TenantId id = TenantId.of(UUID.randomUUID());
    assertThatThrownBy(() -> new TenantInfo(id, "  ", "FREE", 1))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("name");
  }

  @Test
  void shouldNotBuildWithTooLongName() {
    TenantId id = TenantId.of(UUID.randomUUID());
    assertThatThrownBy(() -> new TenantInfo(id, "a".repeat(256), "FREE", 1))
      .isExactlyInstanceOf(StringTooLongException.class);
  }

  @Test
  void shouldNotBuildWithoutSubscriptionTier() {
    TenantId id = TenantId.of(UUID.randomUUID());
    assertThatThrownBy(() -> new TenantInfo(id, "Test", null, 1))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("subscriptionTier");
  }

  @Test
  void shouldNotBuildWithNegativeMaxLocations() {
    TenantId id = TenantId.of(UUID.randomUUID());
    assertThatThrownBy(() -> new TenantInfo(id, "Test", "FREE", -1))
      .isExactlyInstanceOf(NumberValueTooLowException.class);
  }

  @Test
  void shouldBuildTenantInfo() {
    TenantId id = TenantId.of(UUID.randomUUID());
    TenantInfo info = new TenantInfo(id, "Test Tenant", "PROFESSIONAL", 10);

    assertThat(info.id()).isEqualTo(id);
    assertThat(info.name()).isEqualTo("Test Tenant");
    assertThat(info.subscriptionTier()).isEqualTo("PROFESSIONAL");
    assertThat(info.maxLocations()).isEqualTo(10);
  }
}
