package io.ceremo.ceremobackend.shared.multitenancy.infrastructure.secondary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.ceremo.ceremobackend.shared.generation.domain.CeremoUUID;
import io.ceremo.ceremobackend.shared.multitenancy.application.SystemOperation;
import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantContext;
import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantId;
import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantInfo;
import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantScopedEntity;
import jakarta.persistence.EntityManager;
import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SystemOperationAspectTest {

  @AfterEach
  void clearTenant() {
    TenantContext.clear();
  }

  @Test
  void restoresTheOriginalTenantFilterEvenWhenTheSystemOperationFails() throws Throwable {
    var entityManager = mock(EntityManager.class);
    var session = mock(Session.class);
    var filter = mock(Filter.class);
    var joinPoint = mock(ProceedingJoinPoint.class);
    var tenantId = TenantId.of(
        CeremoUUID.generate());
    TenantContext.setCurrentTenant(
        new TenantInfo(tenantId, "Test", "FREE", 1));
    when(entityManager.unwrap(Session.class)).thenReturn(session);
    when(session.getEnabledFilter(TenantScopedEntity.TENANT_FILTER_NAME)).thenReturn(filter);
    when(session.enableFilter(TenantScopedEntity.TENANT_FILTER_NAME)).thenReturn(filter);
    var failure = new IllegalStateException("System operation failed");
    when(joinPoint.proceed()).thenThrow(failure);
    assertSame(failure, assertThrows(
        IllegalStateException.class, () -> new SystemOperationAspect(entityManager)
            .disableTenantFilter(joinPoint, annotatedMethod().getAnnotation(SystemOperation.class))));
    verify(filter).setParameter("tenantId", tenantId.get());
  }

  @Test
  void rejectsAnEnabledFilterWithoutTenantContextBeforeProceeding() throws Throwable {
    var entityManager = mock(EntityManager.class);
    var session = mock(Session.class);
    var joinPoint = mock(ProceedingJoinPoint.class);
    when(entityManager.unwrap(Session.class)).thenReturn(session);
    when(session.getEnabledFilter(TenantScopedEntity.TENANT_FILTER_NAME)).thenReturn(mock(Filter.class));
    assertThrows(IllegalStateException.class, () ->
        new SystemOperationAspect(entityManager).disableTenantFilter(
            joinPoint, annotatedMethod().getAnnotation(SystemOperation.class)));
    verifyNoInteractions(joinPoint);
    verify(session, never()).disableFilter(anyString());
  }

  @Test
  void shouldDisableTenantFilterAndProceed() throws Throwable {
    EntityManager entityManager = mock(EntityManager.class);
    Session session = mock(Session.class);
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    when(entityManager.unwrap(Session.class)).thenReturn(session);
    when(joinPoint.proceed()).thenReturn("done");

    SystemOperationAspect aspect = new SystemOperationAspect(entityManager);
    SystemOperation annotation = annotatedMethod().getAnnotation(SystemOperation.class);

    assertThat(aspect.disableTenantFilter(joinPoint, annotation)).isEqualTo("done");
    verify(session).disableFilter(TenantScopedEntity.TENANT_FILTER_NAME);
    verify(joinPoint).proceed();
  }

  @SystemOperation(reason = "test")
  private static void annotated() {
    // Intentionally empty: this helper exists only so the test can read its annotation.
  }

  private static Method annotatedMethod() throws NoSuchMethodException {
    return SystemOperationAspectTest.class.getDeclaredMethod("annotated");
  }
}


