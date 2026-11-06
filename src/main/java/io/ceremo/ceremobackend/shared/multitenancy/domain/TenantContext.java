package io.ceremo.ceremobackend.shared.multitenancy.domain;

import java.util.Optional;

import io.ceremo.ceremobackend.shared.multitenancy.application.NoTenantContextException;

public final class TenantContext {

  private static final ThreadLocal<TenantInfo> CURRENT_TENANT = new ThreadLocal<>();

  private TenantContext() {}

  public static void setCurrentTenant(TenantInfo tenant) {
    CURRENT_TENANT.set(tenant);
  }

  public static Optional<TenantInfo> getCurrentTenant() {
    return Optional.ofNullable(CURRENT_TENANT.get());
  }

  public static Optional<TenantId> getCurrentTenantId() {
    return getCurrentTenant().map(TenantInfo::id);
  }

  public static TenantInfo requireCurrentTenant() {
    return getCurrentTenant().orElseThrow(NoTenantContextException::missingTenant);
  }

  public static TenantId requireCurrentTenantId() {
    return getCurrentTenantId().orElseThrow(NoTenantContextException::missingTenant);
  }

  public static void clear() {
    CURRENT_TENANT.remove();
  }

  public static boolean hasCurrentTenant() {
    return CURRENT_TENANT.get() != null;
  }
}
