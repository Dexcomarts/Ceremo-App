package io.ceremo.ceremobackend.shared.multitenancy.application;

import io.ceremo.ceremobackend.shared.error.domain.ErrorStatus;
import io.ceremo.ceremobackend.shared.error.domain.CeremoException;

public final class NoTenantContextException extends CeremoException {

  private NoTenantContextException(CeremoExceptionBuilder builder) {
    super(builder);
  }

  public static NoTenantContextException missingTenant() {
    return new NoTenantContextException(
        CeremoException.builder(MultitenancyErrorKey.NO_TENANT_CONTEXT)
            .message("No tenant context available for this operation")
            .status(ErrorStatus.BAD_REQUEST));
  }
}
