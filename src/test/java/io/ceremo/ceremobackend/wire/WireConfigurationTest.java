package io.ceremo.ceremobackend.wire;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import io.ceremo.ceremobackend.CeremoBackendApplication;
import io.ceremo.ceremobackend.shared.domain.BaseEntity;
import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantContext;
import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantId;
import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantInfo;
import io.ceremo.ceremobackend.shared.multitenancy.infrastructure.primary.TenantFilter;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Table;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = {CeremoBackendApplication.class, TestOidcConfiguration.class}, properties = {
    "spring.datasource.url=jdbc:h2:mem:ceremo-wire;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
    "application.registration.terms-version=terms-v1",
    "application.logging.logstash.tcp.enabled=false", "application.multitenancy.enabled=false",
    "spring.task.execution.pool.core-size=2", "spring.task.execution.pool.max-size=4",
    "spring.task.execution.pool.queue-capacity=5", "spring.task.execution.thread-name-prefix=ceremo-test-",
    "springdoc.oauth2.authorization-url=https://identity.example/authorize",
    "springdoc.oauth2.token-url=https://identity.example/token"})
@AutoConfigureMockMvc
class WireConfigurationTest {
  @Autowired ApplicationContext context;
  @Autowired ThreadPoolTaskExecutor taskExecutor;
  @Autowired CacheManager cacheManager;
  @Autowired PlatformTransactionManager transactionManager;
  @Autowired MockMvc mvc;
  @PersistenceContext EntityManager entityManager;
  @MockitoBean(name = "jwtDecoder") JwtDecoder decoder;

  @AfterEach
  void clearContext() { TenantContext.clear(); }

  @Test
  void startsAllWiringWithOptionalTenantAndLogstashComponentsDisabled() {
    assertThat(context.getBeansOfType(TenantFilter.class)).isEmpty();
    assertThat(context.containsBean("logstashTcpLifeCycle")).isFalse();
    assertThat(context.containsBean("cacheManagerCustomizer")).isTrue();
    assertThat(cacheManager).isNotNull();
  }

  @Test
  void usesConfiguredExecutorAndPropagatesTenantWithoutLeakingToNextTask() throws Exception {
    assertThat(taskExecutor.getCorePoolSize()).isEqualTo(2);
    assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(4);
    assertThat(taskExecutor.getQueueCapacity()).isEqualTo(5);
    TenantInfo tenant = new TenantInfo(TenantId.of(UUID.randomUUID()), "Test provider", "FREE", 1);
    TenantContext.setCurrentTenant(tenant);
    var inherited = CompletableFuture.supplyAsync(TenantContext::requireCurrentTenant, taskExecutor)
        .get(10, TimeUnit.SECONDS);
    assertThat(inherited).isEqualTo(tenant);
    TenantContext.clear();
    assertThat(CompletableFuture.supplyAsync(TenantContext::hasCurrentTenant, taskExecutor)
        .get(10, TimeUnit.SECONDS)).isFalse();
  }

  @Test
  void fillsAuditTimestampsAndUpdatesOptimisticVersion() {
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    UUID id = UUID.randomUUID();
    transaction.executeWithoutResult(status -> entityManager.persist(new AuditRecord(id)));
    transaction.executeWithoutResult(status -> {
      AuditRecord record = entityManager.find(AuditRecord.class, id);
      assertThat(record.getCreatedAt()).isNotNull();
      assertThat(record.getUpdatedAt()).isNotNull();
      record.description = "updated";
    });
    transaction.executeWithoutResult(status -> {
      AuditRecord record = entityManager.find(AuditRecord.class, id);
      assertThat(record.getVersion()).isEqualTo(1L);
      assertThat(record.getUpdatedAt()).isAfterOrEqualTo(record.getCreatedAt());
    });
  }

  @Test
  void publishesCeremoOpenApiWithAuthorizationCodeFlow() throws Exception {
    mvc.perform(get("/v3/api-docs/all").with(user("developer")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.info.title").value("CEREMO API"))
        .andExpect(jsonPath("$.externalDocs").doesNotExist())
        .andExpect(jsonPath("$.components.securitySchemes.security_auth.flows.authorizationCode.authorizationUrl")
            .value("https://identity.example/authorize"))
        .andExpect(jsonPath("$.components.securitySchemes.security_auth.flows.authorizationCode.tokenUrl")
            .value("https://identity.example/token"))
        .andExpect(jsonPath("$.components.securitySchemes.security_auth.flows.implicit").doesNotExist());
  }

  @Entity(name = "WireAuditRecord")
  @Table(name = "wire_audit_record")
  static class AuditRecord extends BaseEntity {
    private String description;
    protected AuditRecord() {}
    AuditRecord(UUID id) { super(id); }
  }

}


