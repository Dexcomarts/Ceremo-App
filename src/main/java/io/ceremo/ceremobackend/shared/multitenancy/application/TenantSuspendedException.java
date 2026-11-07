package io.ceremo.ceremobackend.shared.multitenancy.application;

import io.ceremo.ceremobackend.shared.error.domain.ErrorStatus;
import io.ceremo.ceremobackend.shared.error.domain.CeremoException;
import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantId;

public final class TenantSuspendedException extends CeremoException {

  private TenantSuspendedException(CeremoExceptionBuilder builder) {
    super(builder);
  }

  public static TenantSuspendedException suspended(TenantId tenantId) {
    return new TenantSuspendedException(
        CeremoException.builder(MultitenancyErrorKey.TENANT_SUSPENDED)
            .message("Tenant " + tenantId + " is suspended")
            .status(ErrorStatus.FORBIDDEN)
            .addParameter("tenantId", tenantId.toString()));
  }
}
