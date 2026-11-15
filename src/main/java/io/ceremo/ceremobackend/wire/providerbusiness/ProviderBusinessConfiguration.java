package io.ceremo.ceremobackend.wire.providerbusiness;

import io.ceremo.ceremobackend.account.application.port.in.Accounts;
import io.ceremo.ceremobackend.account.application.port.in.UserDirectory;
import io.ceremo.ceremobackend.providerbusiness.application.ProviderBusinessService;
import io.ceremo.ceremobackend.providerbusiness.application.port.in.ProviderBusinesses;
import io.ceremo.ceremobackend.providerbusiness.domain.repository.ProviderBusinessRepository;
import io.ceremo.ceremobackend.providerbusiness.infrastructure.primary.transaction.TransactionalProviderBusinesses;
import io.ceremo.ceremobackend.providerbusiness.infrastructure.secondary.persistence.JpaProviderBusinessRepository;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.SharedEntityManagerCreator;

@Configuration(proxyBeanMethods = false)
public class ProviderBusinessConfiguration {
  @Bean
  ProviderBusinessRepository providerBusinessRepository(EntityManagerFactory entityManagerFactory) {
    return new JpaProviderBusinessRepository(SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory));
  }

  @Bean
  ProviderBusinesses providerBusinesses(Accounts accounts, UserDirectory users, ProviderBusinessRepository businesses) {
    return new TransactionalProviderBusinesses(new ProviderBusinessService(accounts, users, businesses));
  }
}
