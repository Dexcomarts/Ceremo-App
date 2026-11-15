package io.ceremo.ceremobackend.shared.authentication.infrastructure.primary;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/** ceremo accepts interactive Keycloak user access tokens, not ID or service-account tokens. */
final class KeycloakAccessTokenValidator implements OAuth2TokenValidator<Jwt> {
  // Upper bound on access-token life. The revocation retention (LogoutProperties) assumes it, so a realm
  // override cannot outlive a purged sid revocation. The dev realm issues 300 s tokens.
  static final Duration MAXIMUM_LIFETIME = Duration.ofMinutes(15);
  private final Clock clock;
  private final Set<String> offlineClients;

  /**
   * @param offlineClients client IDs (the token's {@code azp}) allowed to present offline-session tokens. ADR 11.13
   *     allows only the Android client; the realm must cap its offline sessions within the revocation retention.
   */
  KeycloakAccessTokenValidator(Clock clock, Collection<String> offlineClients) {
    this.clock = Objects.requireNonNull(clock, "clock");
    this.offlineClients = Set.copyOf(Objects.requireNonNull(offlineClients, "offlineClients"));
  }

  @Override
  public OAuth2TokenValidatorResult validate(Jwt jwt) {
    Instant issuedAt = jwt.getIssuedAt();
    Instant expiresAt = jwt.getExpiresAt();
    if (!"Bearer".equals(jwt.getClaims().get("typ"))
        || !validText(jwt.getClaims().get("sub"), 255)
        || !validText(jwt.getClaims().get("sid"), 255)
        || issuedAt == null || expiresAt == null
        || issuedAt.isAfter(clock.instant().plusSeconds(60))
        || !expiresAt.isAfter(issuedAt)
        || Duration.between(issuedAt, expiresAt).compareTo(MAXIMUM_LIFETIME) > 0
        || offlineTokenFromUnapprovedClient(jwt)) {
      return OAuth2TokenValidatorResult.failure(new OAuth2Error(
          "invalid_token", "A valid Keycloak user access token is required.", null));
    }
    return OAuth2TokenValidatorResult.success();
  }

  private boolean offlineTokenFromUnapprovedClient(Jwt jwt) {
    return grantsOfflineAccess(jwt.getClaims().get("scope")) && !isOfflineClient(jwt.getClaims().get("azp"));
  }

  // Keycloak sets azp to the requesting client ID (TokenManager 26.4.2: token.issuedFor(client.getClientId())).
  // The instanceof check matters: an immutable Set throws NullPointerException from contains(null).
  private boolean isOfflineClient(Object azp) {
    return azp instanceof String client && offlineClients.contains(client);
  }

  private static boolean grantsOfflineAccess(Object scope) {
    return scope instanceof String text && List.of(text.split(" ")).contains("offline_access");
  }

  private static boolean validText(Object value, int maximum) {
    return value instanceof String text && !text.isBlank() && text.length() <= maximum;
  }
}
