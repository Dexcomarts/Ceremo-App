package io.ceremo.ceremobackend.wire.logout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.ceremo.ceremobackend.account.application.port.out.CurrentUser;
import io.ceremo.ceremobackend.logout.application.port.in.Logout;
import io.ceremo.ceremobackend.logout.application.port.in.RevocationRetention;
import io.ceremo.ceremobackend.logout.infrastructure.primary.schedule.RevocationPurgeJob;
import io.ceremo.ceremobackend.shared.error.domain.AssertionException;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;

class LogoutConfigurationTest {
  private static final String CRON = "application.logout.revocation-purge-cron=0 */15 * * * *";
  private static final String RETENTION = "application.logout.revocation-retention=P91D";
  private final ApplicationContextRunner runner = new ApplicationContextRunner()
      .withUserConfiguration(LogoutConfiguration.class)
      .withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class))
      .withBean(CurrentUser.class, () -> mock(CurrentUser.class))
      .withBean(Clock.class, Clock::systemUTC);

  @Configuration(proxyBeanMethods = false)
  @EnableScheduling
  static class Scheduling {}

  @Test
  void wiresLogoutRetentionAndRegistersThePurgeOnTheConfiguredCron() {
    runner.withUserConfiguration(Scheduling.class).withPropertyValues(RETENTION, CRON).run(context -> {
      assertThat(context).hasNotFailed().hasSingleBean(Logout.class)
          .hasSingleBean(RevocationRetention.class).hasSingleBean(RevocationPurgeJob.class);
      assertThat(context.getBean(LogoutProperties.class).revocationRetention()).isEqualTo(Duration.ofDays(91));
      assertThat(context.getBeansOfType(ScheduledTaskHolder.class).values().stream()
          .flatMap(holder -> holder.getScheduledTasks().stream())
          .map(scheduled -> scheduled.getTask())
          .filter(CronTask.class::isInstance).map(task -> ((CronTask) task).getExpression()))
          .containsExactly("0 */15 * * * *");
    });
  }

  @Test
  void acceptsADisabledScheduleAndALongerRetention() {
    runner.withUserConfiguration(Scheduling.class)
        .withPropertyValues("application.logout.revocation-retention=P120D", "application.logout.revocation-purge-cron=-")
        .run(context -> {
          assertThat(context).hasNotFailed();
          assertThat(context.getBeansOfType(ScheduledTaskHolder.class).values().stream()
              .flatMap(holder -> holder.getScheduledTasks().stream())).isEmpty();
        });
  }

  @ParameterizedTest
  @ValueSource(strings = {"P90D", "PT24H", "PT0S", "-PT1H", "P121D"})
  void rejectsARetentionOutsideTheSafeWindow(String retention) {
    runner.withPropertyValues("application.logout.revocation-retention=" + retention, CRON)
        .run(context -> assertThat(context).hasFailed()
            .getFailure().hasRootCauseInstanceOf(AssertionException.class));
  }

  @Test
  void rejectsAnInvalidBlankOrMissingConfiguration() {
    runner.withPropertyValues(RETENTION, "application.logout.revocation-purge-cron=every quarter hour")
        .run(context -> assertThat(context).hasFailed()
            .getFailure().hasRootCauseInstanceOf(IllegalArgumentException.class));
    runner.withPropertyValues(RETENTION, "application.logout.revocation-purge-cron= ")
        .run(context -> assertThat(context).hasFailed()
            .getFailure().hasRootCauseInstanceOf(AssertionException.class));
    runner.withPropertyValues(CRON)
        .run(context -> assertThat(context).hasFailed()
            .getFailure().hasRootCauseInstanceOf(AssertionException.class));
    runner.withPropertyValues(RETENTION)
        .run(context -> assertThat(context).hasFailed()
            .getFailure().hasRootCauseInstanceOf(AssertionException.class));
  }
}
