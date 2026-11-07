package io.ceremo.ceremobackend.shared.authentication.infrastructure.primary;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/** CEREMO accepts interactive Keycloak user access tokens, not ID or service-account tokens. */
final class KeycloakAccessTokenValidator implements OAuth2TokenValidator<Jwt> {
  private final Clock clock;

  KeycloakAccessTokenValidator(Clock clock) {
    this.clock = Objects.requireNonNull(clock, "clock");
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
        || !expiresAt.isAfter(issuedAt)) {
      return OAuth2TokenValidatorResult.failure(new OAuth2Error(
          "invalid_token", "A valid Keycloak user access token is required.", null));
    }
    return OAuth2TokenValidatorResult.success();
  }

  private static boolean validText(Object value, int maximum) {
    return value instanceof String text && !text.isBlank() && text.length() <= maximum;
  }
}
