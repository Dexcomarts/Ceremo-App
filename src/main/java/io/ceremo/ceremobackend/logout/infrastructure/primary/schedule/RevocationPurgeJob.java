package io.ceremo.ceremobackend.logout.infrastructure.primary.schedule;

import io.ceremo.ceremobackend.logout.application.port.in.RevocationRetention;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Scheduled inbound adapter. It registers the purge from the validated LogoutProperties cron, as Spring Session
 * JDBC schedules cleanUpExpiredSessions from its validated cleanupCron (spring-session 4.1.1). Every instance may run
 * it: the purge is one idempotent DELETE. A failure is logged by Spring's scheduler and the next run retries.
 */
public class RevocationPurgeJob implements SchedulingConfigurer {
  private static final Logger log = LoggerFactory.getLogger(RevocationPurgeJob.class);
  private final RevocationRetention retention;
  private final String cron;

  public RevocationPurgeJob(RevocationRetention retention, String cron) {
    this.retention = Objects.requireNonNull(retention, "retention");
    this.cron = Objects.requireNonNull(cron, "cron");
  }

  @Override
  public void configureTasks(ScheduledTaskRegistrar registrar) {
    if (!Scheduled.CRON_DISABLED.equals(cron)) {
      registrar.addCronTask(this::purge, cron);
    }
  }

  public void purge() {
    int deleted = retention.purgeExpired();
    if (deleted > 0) {
      log.info("Purged {} expired session revocations", deleted);
    } else {
      log.debug("No expired session revocations to purge");
    }
  }
}
