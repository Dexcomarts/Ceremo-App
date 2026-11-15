package io.ceremo.ceremobackend.logout.application;

import io.ceremo.ceremobackend.logout.application.port.in.RevocationRetention;
import io.ceremo.ceremobackend.logout.domain.repository.SessionRevocations;
import io.ceremo.ceremobackend.shared.error.domain.Assert;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

/**
 * Keeps each revocation for the whole retention period, then deletes it. Adapted from Spring Session JDBC's
 * expired-session cleanup (spring-session 4.1.1, Apache-2.0). The retention must outlive every Keycloak session
 * that could still carry the revoked sid; LogoutProperties enforces the minimum.
 */
public final class RevocationRetentionService implements RevocationRetention {
  private final SessionRevocations revocations;
  private final Clock clock;
  private final Duration retention;

  public RevocationRetentionService(SessionRevocations revocations, Clock clock, Duration retention) {
    this.revocations = Objects.requireNonNull(revocations, "revocations");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.retention = Objects.requireNonNull(retention, "retention");
    // A zero or negative retention would make the cutoff now or later and delete every revocation.
    // min(1), not positive(): the shared LongAsserter.positive() is min(0) and accepts zero.
    Assert.field("retentionSeconds", retention.toSeconds()).min(1);
  }

  @Override
  public int purgeExpired() {
    return revocations.purgeRevokedBefore(clock.instant().minus(retention));
  }
}
