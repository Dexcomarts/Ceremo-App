package io.ceremo.ceremobackend.wire.tenant;

import io.ceremo.ceremobackend.account.application.port.in.Accounts;
import io.ceremo.ceremobackend.tenant.application.TenantService;
import io.ceremo.ceremobackend.tenant.application.port.in.TenantAccounts;
import io.ceremo.ceremobackend.tenant.domain.repository.TenantRepository;
import io.ceremo.ceremobackend.tenant.infrastructure.primary.transaction.TransactionalTenantAccounts;
import io.ceremo.ceremobackend.tenant.infrastructure.secondary.persistence.JpaTenantRepository;
import io.ceremo.ceremobackend.account.application.port.in.UserDirectory;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.SharedEntityManagerCreator;

@Configuration(proxyBeanMethods = false)
public class TenantConfiguration {
  @Bean
  TenantRepository tenantRepository(EntityManagerFactory entityManagerFactory) {
    return new JpaTenantRepository(SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory));
  }

  @Bean
  TenantAccounts tenantAccounts(Accounts accounts, UserDirectory users, TenantRepository tenants) {
    return new TransactionalTenantAccounts(new TenantService(accounts, users, tenants));
  }
}
