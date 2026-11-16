package io.ceremo.ceremobackend.account.infrastructure.secondary.security;

import static org.assertj.core.api.Assertions.*;

import io.ceremo.ceremobackend.account.domain.exception.UserErrorKey;
import io.ceremo.ceremobackend.shared.error.domain.CeremoException;
import io.ceremo.ceremobackend.shared.error.domain.ErrorStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class SpringStaffAccessTest {
  private static final long NOW = 100_000;
  private static final long WINDOW = 7_200;

  private final SpringStaffAccess staff = new SpringStaffAccess(
      Clock.fixed(Instant.ofEpochSecond(NOW), ZoneOffset.UTC), Duration.ofSeconds(WINDOW),
      List.of("web_app", "ceremo-admin"));

  @AfterEach
  void clear() { SecurityContextHolder.clearContext(); }

  @ParameterizedTest
  @ValueSource(longs = {NOW, NOW - WINDOW, NOW + 60})
  void allowsAnAdministratorWhoSignedInWithinTheWindowWithoutMfa(long authenticatedAt) {
    authenticate("ROLE_ADMIN", claims("web_app", authenticatedAt));

    assertThatCode(staff::requireAdministrator).doesNotThrowAnyException();
  }

  @Test
  void allowsTheDedicatedAdminClient() {
    authenticate("ROLE_ADMIN", claims("ceremo-admin", NOW));

    assertThatCode(staff::requireAdministrator).doesNotThrowAnyException();
  }

  @Test
  void refusesANonAdministratorWithAPlainDenial() {
    authenticate("ROLE_CUSTOMER", claims("web_app", NOW));

    assertThatThrownBy(staff::requireAdministrator).isInstanceOf(AccessDeniedException.class)
        .hasMessage("Administrator access is required.");
  }

  @Test
  void refusesTokensFromOtherClientsBecauseReauthenticatingCannotHelp() {
    authenticate("ROLE_ADMIN", claims("android_app", NOW));
    staffClientDenied();

    Map<String, Object> noClient = claims("web_app", NOW);
    noClient.remove("azp");
    authenticate("ROLE_ADMIN", noClient);
    staffClientDenied();

    Map<String, Object> nonTextClient = claims("web_app", NOW);
    nonTextClient.put("azp", List.of("web_app"));
    authenticate("ROLE_ADMIN", nonTextClient);
    staffClientDenied();
  }

  @Test
  void refusesOfflineTokensEvenFromAStaffClient() {
    Map<String, Object> offline = claims("web_app", NOW);
    offline.put("scope", "openid offline_access");
    authenticate("ROLE_ADMIN", offline);

    staffClientDenied();
  }

  @Test
  void acceptsOrdinaryScopesAndIgnoresANonTextScope() {
    Map<String, Object> scoped = claims("web_app", NOW);
    scoped.put("scope", "openid profile email");
    authenticate("ROLE_ADMIN", scoped);
    assertThatCode(staff::requireAdministrator).doesNotThrowAnyException();

    scoped.put("scope", List.of("offline_access"));
    authenticate("ROLE_ADMIN", scoped);
    assertThatCode(staff::requireAdministrator).doesNotThrowAnyException();
  }

  @ParameterizedTest
  @ValueSource(longs = {NOW - WINDOW - 1, NOW + 61})
  void asksForReauthenticationWhenTheSignInIsTooOldOrInTheFuture(long authenticatedAt) {
    authenticate("ROLE_ADMIN", claims("web_app", authenticatedAt));

    reauthenticationRequired();
  }

  @Test
  void asksForReauthenticationWhenAuthTimeIsMissingOrNotANumberOfSeconds() {
    for (Object time : List.of("100000", 100_000)) {
      Map<String, Object> wrongType = claims("web_app", NOW);
      wrongType.put("auth_time", time);
      authenticate("ROLE_ADMIN", wrongType);
      reauthenticationRequired();
    }
    Map<String, Object> missing = claims("web_app", NOW);
    missing.remove("auth_time");
    authenticate("ROLE_ADMIN", missing);
    reauthenticationRequired();
  }

  @Test
  void requiresItsCollaborators() {
    Clock clock = Clock.systemUTC();
    Duration window = Duration.ofHours(2);
    List<String> clients = List.of("web_app");

    assertThatThrownBy(() -> new SpringStaffAccess(null, window, clients)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new SpringStaffAccess(clock, null, clients)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new SpringStaffAccess(clock, window, null)).isInstanceOf(NullPointerException.class);
  }

  private void staffClientDenied() {
    assertThatThrownBy(staff::requireAdministrator).isInstanceOf(AccessDeniedException.class)
        .hasMessage("Staff actions are only available from the staff web clients.");
  }

  private void reauthenticationRequired() {
    assertThatThrownBy(staff::requireAdministrator).isInstanceOfSatisfying(CeremoException.class, error -> {
      assertThat(error.key()).isEqualTo(UserErrorKey.RECENT_AUTHENTICATION_REQUIRED);
      assertThat(error.status()).isEqualTo(ErrorStatus.FORBIDDEN);
    });
  }

  private static Map<String, Object> claims(String client, long authenticatedAt) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("azp", client);
    claims.put("auth_time", authenticatedAt);
    return claims;
  }

  private static void authenticate(String role, Map<String, Object> claims) {
    Jwt token = Jwt.withTokenValue("synthetic").header("alg", "RS256").subject("staff")
        .claims(values -> values.putAll(claims)).build();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(token,
        List.of(new SimpleGrantedAuthority(role))));
  }
}
