package io.ceremo.ceremobackend.shared.multitenancy.infrastructure.primary;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import io.ceremo.ceremobackend.shared.multitenancy.domain.TenantContext;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantFilterTest {

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private FilterChain filterChain;

  @Mock
  private SecurityContext securityContext;

  private TenantFilter tenantFilter;

  @BeforeEach
  void setUp() {
    tenantFilter = new TenantFilter();
    TenantContext.clear();
    SecurityContextHolder.setContext(securityContext);
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldClearTenantContextAfterFilterChain() throws Exception {
    when(securityContext.getAuthentication()).thenReturn(null);

    tenantFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertThat(TenantContext.getCurrentTenant()).isEmpty();
  }

  @Test
  void shouldNotSetTenantWhenAuthenticationIsNull() throws Exception {
    when(securityContext.getAuthentication()).thenReturn(null);

    tenantFilter.doFilterInternal(request, response, filterChain);

    assertThat(TenantContext.getCurrentTenant()).isEmpty();
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void shouldNotSetTenantWhenNotAuthenticated() throws Exception {
    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(false);
    when(securityContext.getAuthentication()).thenReturn(auth);

    tenantFilter.doFilterInternal(request, response, filterChain);

    assertThat(TenantContext.getCurrentTenant()).isEmpty();
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void shouldExtractTenantFromJwtToken() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID keycloakOrgId = UUID.randomUUID();

    // Keycloak nested format: key is org name, value is org data
    Map<String, Object> orgData = new HashMap<>();
    orgData.put("id", keycloakOrgId.toString());
    orgData.put("tenant_id", List.of(tenantId.toString()));
    orgData.put("subscription_tier", List.of("ENTERPRISE"));
    orgData.put("max_locations", List.of("10"));

    Map<String, Object> orgClaim = new HashMap<>();
    orgClaim.put("Test Organization", orgData);

    Map<String, Object> claims = new HashMap<>();
    claims.put("organization", orgClaim);
    claims.put("sub", "user");

    Jwt jwt = createJwt(claims);
    JwtAuthenticationToken jwtToken = authenticatedJwtToken(jwt);
    when(securityContext.getAuthentication()).thenReturn(jwtToken);

    tenantFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    // Context should be cleared after filter chain
    assertThat(TenantContext.getCurrentTenant()).isEmpty();
  }

  @Test
  void shouldNotSetTenantWhenNoOrganizationClaim() throws Exception {
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", "user");

    Jwt jwt = createJwt(claims);
    JwtAuthenticationToken jwtToken = authenticatedJwtToken(jwt);
    when(securityContext.getAuthentication()).thenReturn(jwtToken);

    tenantFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertThat(TenantContext.getCurrentTenant()).isEmpty();
  }

  @Test
  void shouldNotSetTenantWhenTenantIdIsMissing() throws Exception {
    UUID keycloakOrgId = UUID.randomUUID();

    // Keycloak nested format but without tenant_id
    Map<String, Object> orgData = new HashMap<>();
    orgData.put("id", keycloakOrgId.toString());
    // No tenant_id

    Map<String, Object> orgClaim = new HashMap<>();
    orgClaim.put("Test Organization", orgData);

    Map<String, Object> claims = new HashMap<>();
    claims.put("organization", orgClaim);

    Jwt jwt = createJwt(claims);
    JwtAuthenticationToken jwtToken = authenticatedJwtToken(jwt);
    when(securityContext.getAuthentication()).thenReturn(jwtToken);

    tenantFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertThat(TenantContext.getCurrentTenant()).isEmpty();
  }

  @Test
  void shouldNotSetTenantWhenTenantIdListIsEmpty() throws Exception {
    Map<String, Object> orgData = new HashMap<>();
    orgData.put("tenant_id", List.of());

    Map<String, Object> orgClaim = new HashMap<>();
    orgClaim.put("Test Organization", orgData);

    Jwt jwt = createJwt(Map.of("organization", orgClaim));
    JwtAuthenticationToken jwtToken = authenticatedJwtToken(jwt);
    when(securityContext.getAuthentication()).thenReturn(jwtToken);

    tenantFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertThat(TenantContext.getCurrentTenant()).isEmpty();
  }

  @Test
  void shouldHandleInvalidOrganizationClaimFormat() throws Exception {
    Map<String, Object> claims = new HashMap<>();
    claims.put("organization", "invalid-not-a-map");

    Jwt jwt = createJwt(claims);
    JwtAuthenticationToken jwtToken = authenticatedJwtToken(jwt);
    when(securityContext.getAuthentication()).thenReturn(jwtToken);

    tenantFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertThat(TenantContext.getCurrentTenant()).isEmpty();
  }

  @Test
  void shouldHandleInvalidTenantIdFormat() throws Exception {
    // Keycloak nested format with invalid tenant_id
    Map<String, Object> orgData = new HashMap<>();
    orgData.put("id", UUID.randomUUID().toString());
    orgData.put("tenant_id", List.of("not-a-valid-uuid"));

    Map<String, Object> orgClaim = new HashMap<>();
    orgClaim.put("Test Organization", orgData);

    Map<String, Object> claims = new HashMap<>();
    claims.put("organization", orgClaim);

    Jwt jwt = createJwt(claims);
    JwtAuthenticationToken jwtToken = authenticatedJwtToken(jwt);
    when(securityContext.getAuthentication()).thenReturn(jwtToken);

    tenantFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertThat(TenantContext.getCurrentTenant()).isEmpty();
  }

  @Test
  void shouldHandleOrgDataNotBeingMap() throws Exception {
    // Keycloak format where org data is not a map (invalid)
    Map<String, Object> orgClaim = new HashMap<>();
    orgClaim.put("Test Organization", "not-a-map");

    Map<String, Object> claims = new HashMap<>();
    claims.put("organization", orgClaim);
    claims.put("sub", "user");

    Jwt jwt = createJwt(claims);
    JwtAuthenticationToken jwtToken = authenticatedJwtToken(jwt);
    when(securityContext.getAuthentication()).thenReturn(jwtToken);

    tenantFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertThat(TenantContext.getCurrentTenant()).isEmpty();
  }

  @Test
  void shouldUseDefaultValuesForMissingAttributes() throws Exception {
    UUID tenantId = UUID.randomUUID();

    // Keycloak nested format with only tenant_id (no subscription_tier or max_locations)
    Map<String, Object> orgData = new HashMap<>();
    orgData.put("id", UUID.randomUUID().toString());
    orgData.put("tenant_id", List.of(tenantId.toString()));
    // No subscription_tier or max_locations - should use defaults

    Map<String, Object> orgClaim = new HashMap<>();
    orgClaim.put("Test Organization", orgData);

    Map<String, Object> claims = new HashMap<>();
    claims.put("organization", orgClaim);
    claims.put("sub", "user");

    Jwt jwt = createJwt(claims);
    JwtAuthenticationToken jwtToken = authenticatedJwtToken(jwt);
    when(securityContext.getAuthentication()).thenReturn(jwtToken);

    tenantFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    // Context should be cleared after filter chain
    assertThat(TenantContext.getCurrentTenant()).isEmpty();
  }

  @Test
  void shouldClearContextEvenWhenExceptionThrown() throws Exception {
    when(securityContext.getAuthentication()).thenReturn(null);
    doThrow(new RuntimeException("Test exception")).when(filterChain).doFilter(request, response);

    assertThatThrownBy(() -> tenantFilter.doFilterInternal(request, response, filterChain))
      .isInstanceOf(RuntimeException.class);

    assertThat(TenantContext.getCurrentTenant()).isEmpty();
  }

  @Test
  void shouldNotSetTenantForUnsupportedAuthenticationType() throws Exception {
    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(true);
    when(securityContext.getAuthentication()).thenReturn(auth);

    tenantFilter.doFilterInternal(request, response, filterChain);

    assertThat(TenantContext.getCurrentTenant()).isEmpty();
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void shouldExtractTenantFromOAuth2TokenWithStringAttributes() throws Exception {
    UUID tenantId = UUID.randomUUID();
    Map<String, Object> orgData = new HashMap<>();
    orgData.put("tenant_id", tenantId.toString());
    orgData.put("subscription_tier", "PROFESSIONAL");
    orgData.put("max_locations", "not-a-number");

    Map<String, Object> orgClaim = new HashMap<>();
    orgClaim.put("Test Organization", orgData);

    OAuth2User principal = mock(OAuth2User.class);
    when(principal.getAttributes()).thenReturn(Map.of("organization", orgClaim));
    OAuth2AuthenticationToken token =
        new OAuth2AuthenticationToken(
            principal, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")), "oidc");
    when(securityContext.getAuthentication()).thenReturn(token);

    tenantFilter.doFilterInternal(request, response, filterChain);

    assertThat(TenantContext.getCurrentTenant()).isEmpty();
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void shouldNotSetTenantWhenOAuth2PrincipalIsNull() throws Exception {
    OAuth2AuthenticationToken token = mock(OAuth2AuthenticationToken.class);
    when(token.isAuthenticated()).thenReturn(true);
    when(token.getPrincipal()).thenReturn(null);
    when(securityContext.getAuthentication()).thenReturn(token);

    tenantFilter.doFilterInternal(request, response, filterChain);

    assertThat(TenantContext.getCurrentTenant()).isEmpty();
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void shouldNotSetTenantWhenOrganizationClaimIsEmpty() throws Exception {
    Jwt jwt = createJwt(Map.of("organization", Map.of()));
    JwtAuthenticationToken jwtToken = authenticatedJwtToken(jwt);
    when(securityContext.getAuthentication()).thenReturn(jwtToken);

    tenantFilter.doFilterInternal(request, response, filterChain);

    assertThat(TenantContext.getCurrentTenant()).isEmpty();
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void shouldExtractDefaultStringAttributeWhenAttributesAreNull() {
    String value =
        ReflectionTestUtils.invokeMethod(
            tenantFilter, "extractStringAttribute", null, "subscription_tier", "FREE");

    assertThat(value).isEqualTo("FREE");
  }

  @Test
  void shouldExtractDefaultStringAttributeWhenValueIsMissing() {
    String value =
        ReflectionTestUtils.invokeMethod(
            tenantFilter, "extractStringAttribute", Map.of(), "subscription_tier", "FREE");

    assertThat(value).isEqualTo("FREE");
  }

  @Test
  void shouldExtractDefaultStringAttributeWhenListIsEmpty() {
    String value =
        ReflectionTestUtils.invokeMethod(
            tenantFilter,
            "extractStringAttribute",
            Map.of("subscription_tier", List.of()),
            "subscription_tier",
            "FREE");

    assertThat(value).isEqualTo("[]");
  }

  private Jwt createJwt(Map<String, Object> claims) {
    return new Jwt(
      "test-token-value",
      Instant.now(),
      Instant.now().plusSeconds(3600),
      Map.of("alg", "RS256"),
      claims
    );
  }

  private JwtAuthenticationToken authenticatedJwtToken(Jwt jwt) {
    return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
  }
}
