package io.ceremo.ceremobackend.shared.multitenancy.infrastructure.secondary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.aspectj.lang.ProceedingJoinPoint;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantContext;
import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantId;
import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantInfo;
import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantScopedEntity;
import jakarta.persistence.EntityManager;

class TenantFilterAspectTest {

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void shouldEnableTenantFilterWhenTenantContextExists() throws Throwable {
    TenantId tenantId = TenantId.of(UUID.randomUUID());
    TenantContext.setCurrentTenant(new TenantInfo(tenantId, "Tenant", "FREE", 1));
    EntityManager entityManager = mock(EntityManager.class);
    Session session = mock(Session.class);
    Filter filter = mock(Filter.class);
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    when(entityManager.unwrap(Session.class)).thenReturn(session);
    when(session.enableFilter(TenantScopedEntity.TENANT_FILTER_NAME)).thenReturn(filter);
    when(joinPoint.proceed()).thenReturn("done");

    Object result = new TenantFilterAspect(entityManager).enableTenantFilter(joinPoint);

    assertThat(result).isEqualTo("done");
    verify(filter).setParameter("tenantId", tenantId.get());
  }

  @Test
  void shouldNotEnableTenantFilterWhenAlreadyEnabled() throws Throwable {
    TenantContext.setCurrentTenant(new TenantInfo(TenantId.of(UUID.randomUUID()), "Tenant", "FREE", 1));
    EntityManager entityManager = mock(EntityManager.class);
    Session session = mock(Session.class);
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    when(entityManager.unwrap(Session.class)).thenReturn(session);
    when(session.getEnabledFilter(TenantScopedEntity.TENANT_FILTER_NAME)).thenReturn(mock(Filter.class));
    when(joinPoint.proceed()).thenReturn("done");

    Object result = new TenantFilterAspect(entityManager).enableTenantFilter(joinPoint);

    assertThat(result).isEqualTo("done");
    verify(session, never()).enableFilter(TenantScopedEntity.TENANT_FILTER_NAME);
  }

  @Test
  void shouldCallPointcutMethods() {
    TenantFilterAspect aspect = new TenantFilterAspect(mock(EntityManager.class));

    assertThatCode(
            () -> {
              aspect.serviceClasses();
              aspect.publicMethods();
            })
        .doesNotThrowAnyException();
  }
}
