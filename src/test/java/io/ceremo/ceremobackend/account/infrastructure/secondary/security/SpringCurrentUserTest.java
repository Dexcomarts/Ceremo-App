package io.ceremo.ceremobackend.account.infrastructure.secondary.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.ceremo.ceremobackend.shared.authentication.application.NotAuthenticatedUserException;
import io.ceremo.ceremobackend.shared.generation.domain.CeremoUUID;
import io.ceremo.ceremobackend.account.application.UserService;
import io.ceremo.ceremobackend.account.domain.model.User;
import io.ceremo.ceremobackend.account.domain.model.UserIdentity;
import io.ceremo.ceremobackend.account.domain.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class SpringCurrentUserTest {

  private static final String ISSUER = "https://identity.example/realms/ceremo";
  private static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");
  private final SpringCurrentUser currentUser = new SpringCurrentUser(ISSUER);

  @BeforeEach
  @AfterEach
  void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void usesExactIssuerAndSubjectRatherThanUsername() {
    authenticateJwt(Map.of(
        "iss", ISSUER, "sub", "Person-123", "preferred_username", "different-name"));

    assertEquals(new UserIdentity(ISSUER, "Person-123"), currentUser.requireIdentity());
  }

  @Test
  void readsOidcBrowserIdentityThroughSharedAuthentication() {
    var idToken = new OidcIdToken(
        "test-id-token", NOW, NOW.plusSeconds(300), Map.of("iss", ISSUER, "sub", "person-123"));
    var principal = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")), idToken);
    SecurityContextHolder.getContext().setAuthentication(new OAuth2AuthenticationToken(
        principal, principal.getAuthorities(), "oidc"));

    assertEquals(new UserIdentity(ISSUER, "person-123"), currentUser.requireIdentity());
  }

  @Test
  void readsTheFrameworkConvertedOidcIssuerWithoutAcceptingArbitraryObjects() {
    var converter = org.springframework.security.oauth2.client.oidc.authentication
        .OidcIdTokenDecoderFactory.createDefaultClaimTypeConverter();
    Map<String, Object> claims = converter.convert(Map.of("iss", ISSUER, "sub", "person-123"));
    assertEquals(java.net.URL.class, claims.get("iss").getClass());
    var idToken = new OidcIdToken("synthetic", NOW, NOW.plusSeconds(300), claims);
    var principal = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")), idToken);
    SecurityContextHolder.getContext().setAuthentication(new OAuth2AuthenticationToken(
        principal, principal.getAuthorities(), "oidc"));
    assertEquals(new UserIdentity(ISSUER, "person-123"), currentUser.requireIdentity());
  }

  @Test
  void rejectsMissingAuthentication() {
    assertThrows(NotAuthenticatedUserException.class, currentUser::requireIdentity);
  }

  @Test
  void rejectsAnonymousAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
        "test-key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

    assertThrows(NotAuthenticatedUserException.class, currentUser::requireIdentity);
  }

  @Test
  void rejectsUnauthenticatedPrincipal() {
    SecurityContextHolder.getContext().setAuthentication(
        UsernamePasswordAuthenticationToken.unauthenticated("person-123", "unused"));

    assertThrows(NotAuthenticatedUserException.class, currentUser::requireIdentity);
  }

  @ParameterizedTest
  @MethodSource("invalidIdentityClaims")
  void rejectsIncompleteForeignOrMalformedIdentity(Map<String, Object> claims) {
    authenticateJwt(claims);

    assertThrows(NotAuthenticatedUserException.class, currentUser::requireIdentity);
  }

  @Test
  void connectsSharedAuthenticationToAccountLookup() {
    authenticateJwt(Map.of("iss", ISSUER, "sub", "person-123"));
    var identity = new UserIdentity(ISSUER, "person-123");
    var account = User.register(CeremoUUID.generate(), "Ama Mensah", "terms-v1", NOW);
    var users = mock(UserRepository.class);
    when(users.findByIdentity(identity)).thenReturn(Optional.of(account));

    assertEquals(Optional.of(account), new UserService(
        users, currentUser, Clock.fixed(NOW, ZoneOffset.UTC), "terms-v1").currentUser());
  }

  @Test
  void verificationMustComeFromTheBooleanClaimOfTheTrustedIdentity() {
    authenticateJwt(Map.of("iss", ISSUER, "sub", "person-123", "email_verified", true));
    assertEquals(true, currentUser.isEmailVerified());
    for (Object claim : List.of(false, "true", "false", 1, Map.of("value", true))) {
      authenticateJwt(Map.of("iss", ISSUER, "sub", "person-123", "email_verified", claim));
      assertEquals(false, currentUser.isEmailVerified());
    }
    authenticateJwt(Map.of("iss", ISSUER, "sub", "person-123"));
    assertEquals(false, currentUser.isEmailVerified());
  }

  @Test
  void verificationCannotBypassTheExistingAuthenticationChecks() {
    assertThrows(NotAuthenticatedUserException.class, currentUser::isEmailVerified);
    authenticateJwt(Map.of("iss", "https://identity.example/realms/other",
        "sub", "person-123", "email_verified", true));
    assertThrows(NotAuthenticatedUserException.class, currentUser::isEmailVerified);
  }

  static Stream<Map<String, Object>> invalidIdentityClaims() {
    return Stream.of(
        Map.of("sub", "person-123"),
        Map.of("iss", ISSUER),
        Map.of("iss", "https://identity.example/realms/other", "sub", "person-123"),
        Map.of("iss", ISSUER, "sub", " "),
        Map.of("iss", ISSUER, "sub", 123),
        Map.of("iss", 123, "sub", "person-123"),
        Map.of("iss", ISSUER, "sub", "x".repeat(256)));
  }

  private void authenticateJwt(Map<String, Object> claims) {
    var jwt = Jwt.withTokenValue("test-access-token")
        .header("alg", "RS256")
        .claims(values -> values.putAll(claims))
        .build();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
  }
}
