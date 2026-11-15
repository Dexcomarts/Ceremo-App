package io.ceremo.ceremobackend.wire.logout;

import io.ceremo.ceremobackend.shared.error.domain.Assert;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;

/**
 * A revoked sid must stay blocked while Keycloak could still issue a token for it. Since ADR 11.13 the longest such
 * session is the Android client's offline session: 90 days (realm offlineSessionMaxLifespan 7776000 with
 * offlineSessionMaxLifespanEnabled), plus the 15-minute validator token cap and 60 s skew. Keycloak gives an offline
 * session the same sid as its online session. Raise the minimum before deploying if that realm maximum grows.
 */
@ConfigurationProperties(prefix = "application.logout", ignoreUnknownFields = false)
public record LogoutProperties(Duration revocationRetention, String revocationPurgeCron) {
  static final Duration MINIMUM_RETENTION = Duration.ofDays(91);
  // An upper bound keeps clock.instant().minus(retention) representable, so the purge cannot fail on every run.
  static final Duration MAXIMUM_RETENTION = Duration.ofDays(120);

  public LogoutProperties {
    Assert.notNull("revocationRetention", revocationRetention);
    Assert.field("revocationRetentionSeconds", revocationRetention.toSeconds()).min(MINIMUM_RETENTION.toSeconds())
        .max(MAXIMUM_RETENTION.toSeconds());
    Assert.field("revocationPurgeCron", revocationPurgeCron).notBlank();
    // Same check as Spring Session's setCleanupCron: a valid cron expression, or "-" to disable the job.
    // Assert has no cron asserter, so this one rule uses IllegalArgumentException, as Spring's Assert.isTrue does.
    if (!Scheduled.CRON_DISABLED.equals(revocationPurgeCron) && !CronExpression.isValidExpression(revocationPurgeCron)) {
      throw new IllegalArgumentException("application.logout.revocation-purge-cron must be a valid cron or \"-\"");
    }
  }
}
