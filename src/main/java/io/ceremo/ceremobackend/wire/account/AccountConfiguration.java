package io.ceremo.ceremobackend.wire.account;

import io.ceremo.ceremobackend.account.application.AccountService;
import io.ceremo.ceremobackend.account.application.port.in.Accounts;
import io.ceremo.ceremobackend.account.application.port.in.UserAccounts;
import io.ceremo.ceremobackend.account.application.port.in.UserDirectory;
import io.ceremo.ceremobackend.account.application.port.out.CurrentUser;
import io.ceremo.ceremobackend.account.application.port.out.StaffAccess;
import io.ceremo.ceremobackend.account.application.UserService;
import io.ceremo.ceremobackend.account.domain.repository.UserRepository;
import io.ceremo.ceremobackend.account.infrastructure.primary.transaction.TransactionalUserAccounts;
import io.ceremo.ceremobackend.account.infrastructure.secondary.persistence.JpaUserDirectory;
import io.ceremo.ceremobackend.account.infrastructure.secondary.persistence.JpaUserRepository;
import io.ceremo.ceremobackend.account.infrastructure.secondary.security.SpringStaffAccess;
import io.ceremo.ceremobackend.shared.generation.domain.CeremoUUID;
import jakarta.persistence.EntityManagerFactory;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(UserRegistrationProperties.class)
public class AccountConfiguration {
  private static final Logger log = LoggerFactory.getLogger(AccountConfiguration.class);

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  UserRepository userRepository(EntityManagerFactory entityManagerFactory) {
    return new JpaUserRepository(
        SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory),
        CeremoUUID::generate);
  }

  @Bean
  UserDirectory userDirectory(EntityManagerFactory entityManagerFactory) {
    return new JpaUserDirectory(
        SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory));
  }

  @Bean
  UserAccounts userAccounts(
      UserRepository users,
      CurrentUser currentUser,
      Clock clock,
      UserRegistrationProperties registration,
      PlatformTransactionManager transactionManager) {
    if (registration.termsVersion() == null) {
      // Not silent: the API still runs, so operators must see why every registration returns 403.
      log.warn("application.registration.terms-version is not set: registration is closed until it is configured");
    }
    UserService service = new UserService(users, currentUser, clock, registration.termsVersion());
    return new TransactionalUserAccounts(service, transactionManager);
  }

  @Bean
  StaffAccess staffAccess(Clock clock) {
    return new SpringStaffAccess(clock);
  }

  @Bean
  Accounts accounts(UserAccounts users, StaffAccess staff) {
    return new AccountService(users, staff);
  }
}
