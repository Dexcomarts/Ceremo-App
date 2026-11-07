package io.ceremo.ceremobackend.account.infrastructure.secondary.security;

import io.ceremo.ceremobackend.account.application.port.out.StaffAccess;
import io.ceremo.ceremobackend.shared.authentication.application.AuthenticatedUser;
import io.ceremo.ceremobackend.shared.authentication.domain.Role;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;

/** These claims must be issued by the configured Keycloak MFA flow, never a user attribute. */
public final class SpringStaffAccess implements StaffAccess {
  private final Clock clock;

  public SpringStaffAccess(Clock clock) {
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  @Override
  public void requireAdministrator() {
    Map<String, Object> claims = AuthenticatedUser.attributes();
    if (!AuthenticatedUser.roles().hasRole(Role.ADMIN)) {
      throw new AccessDeniedException("Administrator access is required.");
    }
    if (!(claims.get("amr") instanceof Collection<?> methods) || !methods.contains("mfa")) {
      throw new AccessDeniedException("Multi-factor authentication is required.");
    }
    if (!(claims.get("auth_time") instanceof Long authenticatedAt)) {
      throw new AccessDeniedException("Recent authentication is required.");
    }
    Instant now = clock.instant();
    long earliest = now.minusSeconds(300).getEpochSecond();
    if (authenticatedAt < earliest || authenticatedAt > now.getEpochSecond()) {
      throw new AccessDeniedException("Recent authentication is required.");
    }
  }
}
