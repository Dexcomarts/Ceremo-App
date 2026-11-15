package io.ceremo.ceremobackend.wire.logout;

import io.ceremo.ceremobackend.logout.application.LogoutService;
import io.ceremo.ceremobackend.logout.application.RevocationRetentionService;
import io.ceremo.ceremobackend.logout.application.port.in.Logout;
import io.ceremo.ceremobackend.logout.application.port.in.RevocationRetention;
import io.ceremo.ceremobackend.logout.application.port.out.CurrentSession;
import io.ceremo.ceremobackend.logout.domain.repository.SessionRevocations;
import io.ceremo.ceremobackend.logout.infrastructure.primary.schedule.RevocationPurgeJob;
import io.ceremo.ceremobackend.logout.infrastructure.secondary.persistence.JdbcSessionRevocations;
import io.ceremo.ceremobackend.logout.infrastructure.secondary.security.SpringCurrentSession;
import io.ceremo.ceremobackend.account.application.port.out.CurrentUser;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LogoutProperties.class)
public class LogoutConfiguration {
  @Bean
  SessionRevocations sessionRevocations(JdbcTemplate jdbc) {
    return new JdbcSessionRevocations(jdbc);
  }

  @Bean
  CurrentSession currentSession(CurrentUser currentUser) {
    return new SpringCurrentSession(currentUser);
  }

  @Bean
  Logout logout(CurrentSession currentSession, SessionRevocations revocations, Clock clock) {
    return new LogoutService(currentSession, revocations, clock);
  }

  @Bean
  RevocationRetention revocationRetention(SessionRevocations revocations, Clock clock, LogoutProperties properties) {
    return new RevocationRetentionService(revocations, clock, properties.revocationRetention());
  }

  @Bean
  RevocationPurgeJob revocationPurgeJob(RevocationRetention retention, LogoutProperties properties) {
    return new RevocationPurgeJob(retention, properties.revocationPurgeCron());
  }
}
