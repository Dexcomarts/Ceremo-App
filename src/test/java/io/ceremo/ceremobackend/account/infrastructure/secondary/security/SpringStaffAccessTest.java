package io.ceremo.ceremobackend.account.infrastructure.secondary.security;

import static org.assertj.core.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class SpringStaffAccessTest {
  private final SpringStaffAccess staff = new SpringStaffAccess(
      Clock.fixed(Instant.ofEpochSecond(1000), ZoneOffset.UTC));

  @AfterEach
  void clear() { SecurityContextHolder.clearContext(); }

  @Test
  void requiresAdminMfaAndFreshServerIssuedAuthenticationTime() {
    authenticate("ROLE_CUSTOMER", Map.of());
    denied();
    authenticate("ROLE_ADMIN", Map.of());
    denied();
    authenticate("ROLE_ADMIN", Map.of("amr", "mfa"));
    denied();
    authenticate("ROLE_ADMIN", Map.of("amr", List.of("pwd")));
    denied();
    authenticate("ROLE_ADMIN", Map.of("amr", List.of("mfa")));
    denied();
    for (Object time : List.of("1000", 1000, 699L, 1001L)) {
      authenticate("ROLE_ADMIN", Map.of("amr", List.of("mfa"), "auth_time", time));
      denied();
    }
    for (long time : List.of(700L, 1000L)) {
      authenticate("ROLE_ADMIN", Map.of("amr", List.of("mfa"), "auth_time", time));
      assertThatCode(staff::requireAdministrator).doesNotThrowAnyException();
    }
    assertThatThrownBy(() -> new SpringStaffAccess(null)).isInstanceOf(NullPointerException.class);
  }

  private void denied() {
    assertThatThrownBy(staff::requireAdministrator).isInstanceOf(AccessDeniedException.class);
  }

  private static void authenticate(String role, Map<String, Object> claims) {
    Jwt token = Jwt.withTokenValue("synthetic").header("alg", "RS256").subject("staff")
        .claims(values -> values.putAll(claims)).build();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(token,
        List.of(new SimpleGrantedAuthority(role))));
  }
}
