package io.ceremo.ceremobackend.logout.application;

import io.ceremo.ceremobackend.logout.application.port.in.Logout;
import io.ceremo.ceremobackend.logout.application.port.out.CurrentSession;
import io.ceremo.ceremobackend.logout.domain.repository.SessionRevocations;
import java.time.Clock;
import java.util.Objects;

public final class LogoutService implements Logout {
  private final CurrentSession currentSession;
  private final SessionRevocations revocations;
  private final Clock clock;

  public LogoutService(CurrentSession currentSession, SessionRevocations revocations, Clock clock) {
    this.currentSession = Objects.requireNonNull(currentSession, "currentSession");
    this.revocations = Objects.requireNonNull(revocations, "revocations");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  @Override
  public void logout() {
    revocations.revoke(currentSession.requireSession(), clock.instant());
  }
}
