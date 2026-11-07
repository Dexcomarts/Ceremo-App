package io.ceremo.ceremobackend.shared.authentication.infrastructure.primary;

import static org.assertj.core.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakAccessTokenValidatorTest {
  private static final Instant NOW = Instant.parse("2026-11-04T00:00:00Z");
  private final KeycloakAccessTokenValidator validator = new KeycloakAccessTokenValidator(
      Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void requiresAnInteractiveAccessTokenWithBoundedIdentitySessionAndLifetime() {
    assertThat(validator.validate(token(builder -> {})).hasErrors()).isFalse();
    List<Consumer<Jwt.Builder>> invalid = List.of(
        builder -> builder.claim("typ", "ID"),
        builder -> builder.claims(claims -> claims.remove("typ")),
        builder -> builder.claims(claims -> claims.remove("sub")),
        builder -> builder.claim("sub", 123),
        builder -> builder.subject(" "),
        builder -> builder.subject("x".repeat(256)),
        builder -> builder.claims(claims -> claims.remove("sid")),
        builder -> builder.claim("sid", 123),
        builder -> builder.claim("sid", " "),
        builder -> builder.claim("sid", "x".repeat(256)),
        builder -> builder.claims(claims -> claims.remove("iat")),
        builder -> builder.claims(claims -> claims.remove("exp")),
        builder -> builder.issuedAt(NOW.plusSeconds(61)));
    for (Consumer<Jwt.Builder> change : invalid) {
      assertThat(validator.validate(token(change)).hasErrors()).isTrue();
    }
    // Jwt's builder rejects reversed bounds itself. A mocked decoded claim set exercises our guard.
    Jwt reversed = org.mockito.Mockito.mock(Jwt.class);
    org.mockito.Mockito.when(reversed.getClaims()).thenReturn(java.util.Map.of(
        "typ", "Bearer", "sub", "user", "sid", "session"));
    org.mockito.Mockito.when(reversed.getIssuedAt()).thenReturn(NOW);
    org.mockito.Mockito.when(reversed.getExpiresAt()).thenReturn(NOW);
    assertThat(validator.validate(reversed).hasErrors()).isTrue();
    assertThatThrownBy(() -> new KeycloakAccessTokenValidator(null)).isInstanceOf(NullPointerException.class);
  }

  private static Jwt token(Consumer<Jwt.Builder> change) {
    Jwt.Builder builder = Jwt.withTokenValue("synthetic").header("alg", "RS256")
        .subject("person").claim("sid", "session").claim("typ", "Bearer")
        .issuedAt(NOW).expiresAt(NOW.plusSeconds(300));
    change.accept(builder);
    return builder.build();
  }
}
