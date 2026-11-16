package io.ceremo.ceremobackend.shared.authentication.infrastructure.primary;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakAccessTokenValidatorTest {
  private static final Instant NOW = Instant.parse("2026-11-04T00:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
  private final KeycloakAccessTokenValidator validator = new KeycloakAccessTokenValidator(CLOCK, List.of("android_app"));

  @Test
  void acceptsAnInteractiveUserAccessToken() {
    assertThat(validator.validate(token(builder -> {})).hasErrors()).isFalse();
    assertThat(validator.validate(token(builder -> builder.claim("scope", "openid profile email"))).hasErrors()).isFalse();
  }

  @Test
  void rejectsTokensThatAreNotInteractiveUserAccessTokens() {
    assertRejected(List.of(
        builder -> builder.claim("typ", "ID"),
        builder -> builder.claims(claims -> claims.remove("typ")),
        builder -> builder.claims(claims -> claims.remove("sub")),
        builder -> builder.claim("sub", 123),
        builder -> builder.subject(" "),
        builder -> builder.subject("x".repeat(256)),
        builder -> builder.claims(claims -> claims.remove("sid")),
        builder -> builder.claim("sid", 123),
        builder -> builder.claim("sid", " "),
        builder -> builder.claim("sid", "x".repeat(256))));
  }

  @Test
  void boundsTheTokenLifetimeThatTheRevocationRetentionAssumes() {
    assertThat(validator.validate(token(builder -> builder.expiresAt(NOW.plusSeconds(900)))).hasErrors()).isFalse();
    assertRejected(List.of(
        builder -> builder.claims(claims -> claims.remove("iat")),
        builder -> builder.claims(claims -> claims.remove("exp")),
        builder -> builder.issuedAt(NOW.plusSeconds(61)),
        builder -> builder.expiresAt(NOW.plusSeconds(901))));
    // Jwt's builder rejects reversed bounds itself. A mocked decoded claim set exercises our guard.
    Jwt reversed = mock(Jwt.class);
    when(reversed.getClaims()).thenReturn(Map.of("typ", "Bearer", "sub", "user", "sid", "session"));
    when(reversed.getIssuedAt()).thenReturn(NOW);
    when(reversed.getExpiresAt()).thenReturn(NOW);
    assertThat(validator.validate(reversed).hasErrors()).isTrue();
  }

  @Test
  void acceptsOfflineSessionTokensOnlyFromConfiguredClients() {
    // ADR 11.13: only the Android client may present offline-session tokens.
    assertThat(validator.validate(token(builder -> builder.claim("scope", "openid offline_access")
        .claim("azp", "android_app"))).hasErrors()).isFalse();
    assertRejected(List.of(
        builder -> builder.claim("scope", "openid offline_access profile"),
        builder -> builder.claim("scope", "openid offline_access").claim("azp", "web_app"),
        builder -> builder.claim("scope", "offline_access").claim("azp", List.of("android_app"))));
    KeycloakAccessTokenValidator noOfflineClients = new KeycloakAccessTokenValidator(CLOCK, List.of());
    assertThat(noOfflineClients.validate(token(builder -> builder.claim("scope", "offline_access")
        .claim("azp", "android_app"))).hasErrors()).isTrue();
  }

  @Test
  void rejectsMissingCollaborators() {
    List<String> noOfflineClients = List.of();
    assertThatThrownBy(() -> new KeycloakAccessTokenValidator(null, noOfflineClients)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new KeycloakAccessTokenValidator(CLOCK, null)).isInstanceOf(NullPointerException.class);
  }

  private void assertRejected(List<Consumer<Jwt.Builder>> changes) {
    for (Consumer<Jwt.Builder> change : changes) {
      assertThat(validator.validate(token(change)).hasErrors()).isTrue();
    }
  }

  private static Jwt token(Consumer<Jwt.Builder> change) {
    Jwt.Builder builder = Jwt.withTokenValue("synthetic").header("alg", "RS256")
        .subject("person").claim("sid", "session").claim("typ", "Bearer")
        .issuedAt(NOW).expiresAt(NOW.plusSeconds(300));
    change.accept(builder);
    return builder.build();
  }
}
