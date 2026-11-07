package io.ceremo.ceremobackend.shared.multitenancy.domain;

import java.util.Optional;
import java.util.UUID;

import io.ceremo.ceremobackend.shared.error.domain.Assert;

public record TenantId(UUID id) {

  public TenantId {
    Assert.notNull("id", id);
  }

  public static TenantId of(UUID id) {
    return new TenantId(id);
  }

  public static TenantId of(String id) {
    Assert.notBlank("id", id);
    return new TenantId(UUID.fromString(id));
  }

  public static Optional<TenantId> ofNullable(UUID id) {
    return Optional.ofNullable(id).map(TenantId::new);
  }

  public static Optional<TenantId> ofNullable(String id) {
    return Optional.ofNullable(id).filter(s -> !s.isBlank()).map(TenantId::of);
  }

  public UUID get() {
    return id();
  }

  @Override
  public String toString() {
    return id.toString();
  }
}
