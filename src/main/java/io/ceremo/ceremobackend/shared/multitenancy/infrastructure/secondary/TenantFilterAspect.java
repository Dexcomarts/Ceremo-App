package io.ceremo.ceremobackend.shared.multitenancy.infrastructure.secondary;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantContext;
import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantScopedEntity;
import jakarta.persistence.EntityManager;

@Aspect
@Component
@ConditionalOnProperty(name = "application.multitenancy.enabled", havingValue = "true")
public class TenantFilterAspect {

  private static final Logger log = LoggerFactory.getLogger(TenantFilterAspect.class);

  private final EntityManager entityManager;

  public TenantFilterAspect(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Pointcut("@within(org.springframework.stereotype.Service)")
  public void serviceClasses() {}

  @Pointcut("execution(public * *(..))")
  public void publicMethods() {}

  @Around("serviceClasses() && publicMethods()")
  public Object enableTenantFilter(ProceedingJoinPoint joinPoint) throws Throwable {
    TenantContext.getCurrentTenantId()
        .ifPresent(
            tenantId -> {
              Session session = entityManager.unwrap(Session.class);
              if (session.getEnabledFilter(TenantScopedEntity.TENANT_FILTER_NAME) == null) {
                log.debug("Enabling tenant filter for tenant: {}", tenantId);
                session
                    .enableFilter(TenantScopedEntity.TENANT_FILTER_NAME)
                    .setParameter("tenantId", tenantId.get());
              }
            });

    return joinPoint.proceed();
  }
}
