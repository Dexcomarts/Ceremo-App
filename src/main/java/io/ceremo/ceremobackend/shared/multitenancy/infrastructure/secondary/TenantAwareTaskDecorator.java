package io.ceremo.ceremobackend.shared.multitenancy.infrastructure.secondary;

import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantContext;
import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantInfo;

@Component
public class TenantAwareTaskDecorator implements TaskDecorator {

  @Override
  public Runnable decorate(Runnable runnable) {
    TenantInfo currentTenant = TenantContext.getCurrentTenant().orElse(null);

    return () -> {
      TenantInfo previousTenant = TenantContext.getCurrentTenant().orElse(null);
      try {
        TenantContext.clear();
        if (currentTenant != null) {
          TenantContext.setCurrentTenant(currentTenant);
        }
        runnable.run();
      } finally {
        TenantContext.clear();
        if (previousTenant != null) {
          TenantContext.setCurrentTenant(previousTenant);
        }
      }
    };
  }
}
