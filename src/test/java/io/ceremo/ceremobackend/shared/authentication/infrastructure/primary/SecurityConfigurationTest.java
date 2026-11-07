package io.ceremo.ceremobackend.shared.authentication.infrastructure.primary;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.List;
import java.time.Instant;
import java.util.UUID;
import io.ceremo.ceremobackend.account.application.port.in.Accounts;
import io.ceremo.ceremobackend.account.domain.exception.UserErrorKey;
import io.ceremo.ceremobackend.account.domain.model.User;
import io.ceremo.ceremobackend.account.domain.model.UserStatus;
import io.ceremo.ceremobackend.shared.error.domain.CeremoException;
import io.ceremo.ceremobackend.shared.error.domain.ErrorStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringJUnitWebConfig(SecurityConfigurationTest.TestConfiguration.class)
@TestPropertySource(properties = {
    "spring.security.oauth2.client.provider.oidc.issuer-uri=https://identity.example/realms/ceremo",
    "application.security.allowed-origins[0]=https://app.example"})
class SecurityConfigurationTest {

  @Autowired WebApplicationContext context;
  @MockitoBean(name = "jwtDecoder") JwtDecoder decoder;
  @MockitoBean io.ceremo.ceremobackend.logout.application.port.in.Logout logout;
  @MockitoBean Accounts accounts;
  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    when(decoder.decode("customer-token")).thenReturn(token("ROLE_CUSTOMER"));
    when(decoder.decode("admin-token")).thenReturn(token("ROLE_ADMIN"));
    when(decoder.decode("invalid-token")).thenThrow(new BadJwtException("Invalid signature"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"/api/v1/providers", "/api/v1/occasion-types", "/api/v1/service-categories",
      "/api/v1/localities", "/api/v1/advisories", "/api/v1/subscription-plans"})
  void allowsPublicDiscovery(String path) throws Exception {
    mvc.perform(get(path)).andExpect(status().isOk());
  }

  @Test
  void requiresBearerAuthenticationForPrivateApi() throws Exception {
    mvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
    mvc.perform(post("/api/v1/providers")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/providers/123/settings")).andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @ValueSource(strings = {"/api/v1/providers/123", "/api/v1/providers/123/reviews",
      "/api/v1/providers/123/contact", "/api/v1/providers/123/settings"})
  void providerDetailsRequireAuthenticationAndAnActiveLocalAccount(String path) throws Exception {
    mvc.perform(get(path)).andExpect(status().isUnauthorized());
    verifyNoInteractions(accounts);
    when(accounts.requireActiveAccount()).thenThrow(
        CeremoException.builder(UserErrorKey.USER_NOT_FOUND).status(ErrorStatus.NOT_FOUND).build());
    mvc.perform(get(path).header("Authorization", "Bearer customer-token"))
        .andExpect(status().isForbidden());
    doReturn(User.restore(UUID.fromString("01940d11-7390-7000-8000-000000000001"),
        "Ama", "terms-v1", Instant.EPOCH, UserStatus.ACTIVE)).when(accounts).requireActiveAccount();
    mvc.perform(get(path).header("Authorization", "Bearer customer-token"))
        .andExpect(status().isOk());
    mvc.perform(get(path).session(adminSession())).andExpect(status().isUnauthorized());
  }

  @Test
  void acceptsCustomerTokenButRejectsInvalidToken() throws Exception {
    mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer customer-token"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer invalid-token"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void protectsStaffAndMonitoringRoutes() throws Exception {
    mvc.perform(get("/api/v1/admin/check").header("Authorization", "Bearer customer-token"))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/admin/check").header("Authorization", "Bearer admin-token"))
        .andExpect(status().isOk());
    mvc.perform(get("/management/prometheus")).andExpect(status().isUnauthorized());
  }

  @Test
  void doesNotUseBrowserSessionToAuthenticateApi() throws Exception {
    mvc.perform(get("/api/v1/users/me").session(adminSession()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void browserSessionsAndTokenCookiesCannotAuthenticateApiWrites() throws Exception {
    mvc.perform(post("/api/v1/providers").session(adminSession()))
        .andExpect(status().isUnauthorized());
    mvc.perform(post("/api/v1/providers").cookie(new Cookie("access_token", "customer-token")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void acceptsAnExplicitBearerHeaderForApiWritesWithoutACsrfToken() throws Exception {
    mvc.perform(post("/api/v1/providers").header("Authorization", "Bearer customer-token"))
        .andExpect(status().isOk());
  }

  @Test
  void keepsCsrfProtectionForBrowserSession() throws Exception {
    mvc.perform(post("/web/change").session(adminSession())).andExpect(status().isForbidden());
    mvc.perform(get("/swagger-ui/index.html")).andExpect(status().is3xxRedirection());
  }

  @Test
  void restrictsCorsToConfiguredOrigins() throws Exception {
    mvc.perform(options("/api/v1/providers").header("Origin", "https://app.example")
        .header("Access-Control-Request-Method", "GET")).andExpect(status().isOk());
    mvc.perform(options("/api/v1/providers").header("Origin", "https://other.example")
        .header("Access-Control-Request-Method", "GET")).andExpect(status().isForbidden());
  }

  private static MockHttpSession adminSession() {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("SPRING_SECURITY_CONTEXT", new SecurityContextImpl(
        UsernamePasswordAuthenticationToken.authenticated("admin", "unused",
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))));
    return session;
  }

  private static Jwt token(String role) {
    return Jwt.withTokenValue("test-token").header("alg", "RS256")
        .subject("test-user").claim("preferred_username", "test-user")
        .claim("roles", List.of(role)).build();
  }

  @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
  @EnableWebMvc
  @ComponentScan(basePackages = "io.ceremo.ceremobackend.wire.security", useDefaultFilters = false,
      includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Configuration.class))
  @Import({SecurityConfiguration.class, OAuth2Configuration.class,
      CustomAuthenticationSuccessHandler.class, TestResource.class})
  static class TestConfiguration {
    @Bean
    OAuth2AuthorizedClientRepository authorizedClientRepository() {
      return new HttpSessionOAuth2AuthorizedClientRepository();
    }

    @Bean
    ApplicationSecurityProperties applicationSecurityProperties() {
      ApplicationSecurityProperties properties = new ApplicationSecurityProperties();
      properties.getOauth2().setAudience(List.of("ceremo-api"));
      properties.setAllowedOrigins(List.of("https://app.example"));
      return properties;
    }

    @Bean
    ClientRegistrationRepository clientRegistrationRepository() {
      return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("oidc")
          .clientId("ceremo-web").clientSecret("test-only")
          .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
          .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
          .scope("openid", "profile")
          .authorizationUri("https://identity.example/authorize")
          .tokenUri("https://identity.example/token")
          .jwkSetUri("https://identity.example/certs")
          .userInfoUri("https://identity.example/userinfo").userNameAttributeName("sub").build());
    }
  }

  @RestController
  @TestComponent
  static class TestResource {
    @GetMapping({"/api/v1/providers", "/api/v1/providers/{id}",
        "/api/v1/providers/{id}/reviews", "/api/v1/providers/{id}/contact", "/api/v1/providers/{id}/settings",
        "/api/v1/occasion-types", "/api/v1/service-categories", "/api/v1/localities",
        "/api/v1/advisories", "/api/v1/subscription-plans", "/api/v1/users/me",
        "/api/v1/admin/check"})
    String read() { return "ok"; }

    @PostMapping({"/api/v1/providers", "/web/change"})
    String write() { return "ok"; }
  }
}
