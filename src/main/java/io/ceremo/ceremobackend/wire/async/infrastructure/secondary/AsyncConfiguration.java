package io.ceremo.ceremobackend.wire.async.infrastructure.secondary;

import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import io.ceremo.ceremobackend.shared.multitenancy.infrastructure.secondary.TenantAwareTaskDecorator;

@Configuration
@EnableAsync
@EnableScheduling
class AsyncConfiguration {

  private final TenantAwareTaskDecorator tenantAwareTaskDecorator;

  AsyncConfiguration(TenantAwareTaskDecorator tenantAwareTaskDecorator) {
    this.tenantAwareTaskDecorator = tenantAwareTaskDecorator;
  }

  @Bean
  public ThreadPoolTaskExecutor taskExecutor(ThreadPoolTaskExecutorBuilder builder) {
    return builder.taskDecorator(tenantAwareTaskDecorator).build();
  }
}
