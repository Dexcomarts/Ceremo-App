package io.ceremo.ceremobackend.shared.multitenancy.infrastructure.secondary;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantContext;
import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantId;
import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantInfo;

class TenantAwareTaskDecoratorTest {

  private TenantAwareTaskDecorator decorator;

  @BeforeEach
  void setUp() {
    decorator = new TenantAwareTaskDecorator();
    TenantContext.clear();
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void shouldPropagateTenantContextToDecoratedRunnable() {
    TenantId tenantId = TenantId.of(UUID.randomUUID());
    TenantInfo tenantInfo = new TenantInfo(tenantId, "Test Tenant", "PROFESSIONAL", 5);
    TenantContext.setCurrentTenant(tenantInfo);

    AtomicReference<TenantInfo> capturedTenant = new AtomicReference<>();

    Runnable originalTask = () -> {
      capturedTenant.set(TenantContext.getCurrentTenant().orElse(null));
    };

    Runnable decoratedTask = decorator.decorate(originalTask);

    TenantContext.clear();

    decoratedTask.run();

    assertThat(capturedTenant.get()).isNotNull();
    assertThat(capturedTenant.get().id()).isEqualTo(tenantId);
    assertThat(capturedTenant.get().name()).isEqualTo("Test Tenant");
  }

  @Test
  void shouldRestoreCallingThreadContextAfterDecoratedRunnableCompletes() {
    TenantId tenantId = TenantId.of(UUID.randomUUID());
    TenantInfo tenantInfo = new TenantInfo(tenantId, "Test Tenant", "FREE", 1);
    TenantContext.setCurrentTenant(tenantInfo);

    Runnable originalTask = () -> {};

    Runnable decoratedTask = decorator.decorate(originalTask);
    decoratedTask.run();

    assertThat(TenantContext.getCurrentTenant()).contains(tenantInfo);
  }

  @Test
  void shouldRunWithoutTenantContextWhenNoneSet() {
    AtomicReference<TenantInfo> capturedTenant = new AtomicReference<>();

    Runnable originalTask = () -> {
      capturedTenant.set(TenantContext.getCurrentTenant().orElse(null));
    };

    Runnable decoratedTask = decorator.decorate(originalTask);
    decoratedTask.run();

    assertThat(capturedTenant.get()).isNull();
  }

  @Test
  void shouldRestoreCallingThreadContextEvenWhenTaskThrowsException() {
    TenantId tenantId = TenantId.of(UUID.randomUUID());
    TenantInfo tenantInfo = new TenantInfo(tenantId, "Test Tenant", "ENTERPRISE", 100);
    TenantContext.setCurrentTenant(tenantInfo);

    Runnable originalTask = () -> {
      throw new RuntimeException("Task failed");
    };

    Runnable decoratedTask = decorator.decorate(originalTask);

    assertThatThrownBy(decoratedTask::run)
      .isInstanceOf(RuntimeException.class)
      .hasMessage("Task failed");

    assertThat(TenantContext.getCurrentTenant()).contains(tenantInfo);
  }
}
