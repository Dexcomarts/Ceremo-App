package io.ceremo.ceremobackend.shared.multitenancy.infrastructure.secondary;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantContext;

import io.ceremo.ceremobackend.shared.multitenancy.application.SystemOperation;
import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantScopedEntity;
import jakarta.persistence.EntityManager;

@Aspect
@Component
@ConditionalOnProperty(name = "application.multitenancy.enabled", havingValue = "true")
public class SystemOperationAspect {

  private static final Logger log = LoggerFactory.getLogger(SystemOperationAspect.class);

  private final EntityManager entityManager;

  public SystemOperationAspect(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Around("@annotation(systemOperation)")
  public Object disableTenantFilter(ProceedingJoinPoint joinPoint, SystemOperation systemOperation)
      throws Throwable {
    log.debug("Disabling tenant filter for system operation: {}", systemOperation.reason());

    Session session = entityManager.unwrap(Session.class);
    boolean wasEnabled = session.getEnabledFilter(TenantScopedEntity.TENANT_FILTER_NAME) != null;
    var tenantId = TenantContext.getCurrentTenantId();
    if (wasEnabled && tenantId.isEmpty()) {
      throw new IllegalStateException("An enabled tenant filter requires tenant context");
    }
    session.disableFilter(TenantScopedEntity.TENANT_FILTER_NAME);

    try {
      return joinPoint.proceed();
    } finally {
      if (wasEnabled) {
        session.enableFilter(TenantScopedEntity.TENANT_FILTER_NAME)
            .setParameter("tenantId", tenantId.orElseThrow().get());
      }
      log.debug("System operation completed: {}", systemOperation.reason());
    }
  }
}
