package io.ceremo.ceremobackend.logout.domain.repository;

import io.ceremo.ceremobackend.logout.domain.model.UserSession;
import java.time.Instant;

public interface SessionRevocations {
  boolean isRevoked(UserSession session);
  void revoke(UserSession session, Instant revokedAt);

  /** Deletes revocations recorded strictly before {@code cutoff} and returns how many were removed. */
  int purgeRevokedBefore(Instant cutoff);
}
