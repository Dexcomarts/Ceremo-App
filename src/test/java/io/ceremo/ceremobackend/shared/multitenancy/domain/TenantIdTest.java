package io.ceremo.ceremobackend.shared.multitenancy.domain;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.ceremo.ceremobackend.shared.error.domain.MissingMandatoryValueException;

class TenantIdTest {

  @Test
  void shouldNotBuildWithoutId() {
    assertThatThrownBy(() -> new TenantId(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id");
  }

  @Test
  void shouldCreateWithUUID() {
    UUID id = UUID.randomUUID();
    TenantId tenantId = TenantId.of(id);
    assertThat(tenantId.id()).isEqualTo(id);
    assertThat(tenantId.get()).isEqualTo(id);
  }

  @Test
  void shouldCreateWithString() {
    UUID id = UUID.randomUUID();
    TenantId tenantId = TenantId.of(id.toString());
    assertThat(tenantId.id()).isEqualTo(id);
  }

  @Test
  void shouldNotCreateWithBlankString() {
    assertThatThrownBy(() -> TenantId.of("  "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class);
  }

  @Test
  void shouldCreateOptionalFromUUID() {
    UUID id = UUID.randomUUID();
    assertThat(TenantId.ofNullable(id)).isPresent().hasValueSatisfying(t -> assertThat(t.id()).isEqualTo(id));
  }

  @Test
  void shouldReturnEmptyOptionalFromNullUUID() {
    assertThat(TenantId.ofNullable((UUID) null)).isEmpty();
  }

  @Test
  void shouldCreateOptionalFromString() {
    UUID id = UUID.randomUUID();
    assertThat(TenantId.ofNullable(id.toString())).isPresent();
  }

  @Test
  void shouldReturnEmptyOptionalFromNullString() {
    assertThat(TenantId.ofNullable((String) null)).isEmpty();
  }

  @Test
  void shouldReturnEmptyOptionalFromBlankString() {
    assertThat(TenantId.ofNullable("  ")).isEmpty();
  }

  @Test
  void shouldHaveToString() {
    UUID id = UUID.randomUUID();
    TenantId tenantId = TenantId.of(id);
    assertThat(tenantId).hasToString(id.toString());
  }
}
