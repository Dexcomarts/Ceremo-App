package io.ceremo.ceremobackend.account.infrastructure.secondary.security;

import io.ceremo.ceremobackend.account.application.port.out.StaffAccess;
import io.ceremo.ceremobackend.account.domain.exception.UserErrorKey;
import io.ceremo.ceremobackend.shared.authentication.application.AuthenticatedUser;
import io.ceremo.ceremobackend.shared.authentication.domain.Role;
import io.ceremo.ceremobackend.shared.error.domain.CeremoException;
import io.ceremo.ceremobackend.shared.error.domain.ErrorStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;

/**
 * Staff actions need the ADMIN role, a token issued to a staff web client and a recent Keycloak sign-in. No MFA step
 * is required (founder decision, 16 Nov 2026). {@code auth_time} comes from Keycloak's {@code basic} client scope and
 * changes only on a real sign-in, never on refresh or SSO reuse (AuthenticationManager 26.4.2), so the client
 * re-authenticates with {@code prompt=login} when this adapter returns {@code recent-authentication-required}.
 */
public final class SpringStaffAccess implements StaffAccess {
  // Same tolerance as KeycloakAccessTokenValidator applies to iat.
  private static final Duration CLOCK_SKEW = Duration.ofSeconds(60);

  private final Clock clock;
  private final Duration maxAge;
  private final Set<String> staffClients;

  public SpringStaffAccess(Clock clock, Duration maxAge, Collection<String> staffClients) {
    this.clock = Objects.requireNonNull(clock, "clock");
    this.maxAge = Objects.requireNonNull(maxAge, "maxAge");
    this.staffClients = Set.copyOf(Objects.requireNonNull(staffClients, "staffClients"));
  }

  @Override
  public void requireAdministrator() {
    Map<String, Object> claims = AuthenticatedUser.attributes();
    if (!AuthenticatedUser.roles().hasRole(Role.ADMIN)) {
      throw new AccessDeniedException("Administrator access is required.");
    }
    // Re-authenticating cannot fix these, so they stay a plain 403 rather than the re-authentication problem.
    if (!(claims.get("azp") instanceof String client) || !staffClients.contains(client)
        || grantsOfflineAccess(claims.get("scope"))) {
      throw new AccessDeniedException("Staff actions are only available from the staff web clients.");
    }
    Instant now = clock.instant();
    if (!(claims.get("auth_time") instanceof Long authenticatedAt)
        || authenticatedAt < now.minus(maxAge).getEpochSecond()
        || authenticatedAt > now.plus(CLOCK_SKEW).getEpochSecond()) {
      throw CeremoException
          .builder(UserErrorKey.RECENT_AUTHENTICATION_REQUIRED)
          .status(ErrorStatus.FORBIDDEN)
          .message("Staff actions need a sign-in within the configured re-authentication window.")
          .build();
    }
  }

  private static boolean grantsOfflineAccess(Object scope) {
    return scope instanceof String text && List.of(text.split(" ")).contains("offline_access");
  }
}
