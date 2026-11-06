package io.ceremo.ceremobackend.logout.infrastructure.secondary.security;

import static org.assertj.core.api.Assertions.*;

import io.ceremo.ceremobackend.shared.authentication.application.NotAuthenticatedUserException;
import io.ceremo.ceremobackend.account.infrastructure.secondary.security.SpringCurrentUser;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class SpringCurrentSessionTest {
  private final SpringCurrentSession session = new SpringCurrentSession(new SpringCurrentUser("https://id.example"));

  @AfterEach void clear() { SecurityContextHolder.clearContext(); }

  @Test
  void readsOnlyTheAuthenticatedSessionAndRejectsMissingOrMalformedIds() {
    assertThatThrownBy(session::requireSession).isInstanceOf(NotAuthenticatedUserException.class);
    authenticate(Map.of());
    assertThatThrownBy(session::requireSession).isInstanceOf(NotAuthenticatedUserException.class);
    for (Object sid : List.of(123, " ", "x".repeat(256))) {
      authenticate(Map.of("sid", sid));
      assertThatThrownBy(session::requireSession).isInstanceOf(NotAuthenticatedUserException.class);
    }
    authenticate(Map.of("sid", "session"));
    assertThat(session.requireSession().id()).isEqualTo("session");
    assertThat(session.requireSession().identity().subject()).isEqualTo("person");
    assertThatThrownBy(() -> new SpringCurrentSession(null)).isInstanceOf(NullPointerException.class);
  }

  private static void authenticate(Map<String, Object> claims) {
    Jwt jwt = Jwt.withTokenValue("synthetic").header("alg", "RS256")
        .issuer("https://id.example").subject("person").claims(values -> values.putAll(claims)).build();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
  }
}
