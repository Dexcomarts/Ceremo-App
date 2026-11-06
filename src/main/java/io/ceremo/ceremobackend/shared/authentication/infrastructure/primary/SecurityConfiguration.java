package io.ceremo.ceremobackend.shared.authentication.infrastructure.primary;

import io.ceremo.ceremobackend.account.application.port.in.Accounts;
import io.ceremo.ceremobackend.account.infrastructure.primary.security.ActiveAccountAuthorizationManager;

import static org.springframework.security.config.Customizer.withDefaults;

import io.ceremo.ceremobackend.shared.authentication.domain.Role;
import io.ceremo.ceremobackend.logout.application.port.in.Logout;
import io.ceremo.ceremobackend.logout.domain.model.UserSession;
import io.ceremo.ceremobackend.logout.domain.repository.SessionRevocations;
import io.ceremo.ceremobackend.account.domain.model.UserIdentity;
import java.time.Clock;
import io.ceremo.ceremobackend.shared.multitenancy.infrastructure.primary.TenantFilter;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.dao.DataAccessException;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
class SecurityConfiguration {

  private final ApplicationSecurityProperties properties;
  private final CustomAuthenticationSuccessHandler authenticationSuccessHandler;
  private final ObjectProvider<TenantFilter> tenantFilter;

  @Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}")
  private String issuerUri;

  SecurityConfiguration(
      ApplicationSecurityProperties properties,
      CustomAuthenticationSuccessHandler authenticationSuccessHandler,
      ObjectProvider<TenantFilter> tenantFilter) {
    this.properties = properties;
    this.authenticationSuccessHandler = authenticationSuccessHandler;
    this.tenantFilter = tenantFilter;
  }

  @Bean
  @Order(1)
  public SecurityFilterChain filterChain(HttpSecurity http, Accounts accounts) {
    http.securityMatcher("/api/**", "/management/**")
        .cors(withDefaults())
        // This chain ignores browser sessions and cookies; clients supply the bearer header.
        // Keep CSRF enabled in browserFilterChain and reassess this exception if credentials change.
        .csrf(CsrfConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers(HttpMethod.GET,
                "/api/v1/providers", "/api/v1/occasion-types",
                "/api/v1/service-categories", "/api/v1/localities", "/api/v1/advisories",
                "/api/v1/subscription-plans",
                "/management/health", "/management/health/liveness",
                "/management/health/readiness").permitAll()
            .requestMatchers("/api/v1/providers/*", "/api/v1/providers/*/**")
                .access(new ActiveAccountAuthorizationManager(accounts))
            .requestMatchers("/api/v1/admin/**", "/management/**").hasAuthority(Role.ADMIN.key())
            .anyRequest().authenticated())
        .oauth2ResourceServer(oauth -> oauth.jwt(jwt ->
            jwt.jwtAuthenticationConverter(authenticationConverter())));
    tenantFilter.ifAvailable(filter -> http.addFilterAfter(filter, BearerTokenAuthenticationFilter.class));
    configureHeaders(http);
    return http.build();
  }

  // Browser login has its own session and CSRF protection. API calls require bearer tokens.
  @Bean
  @Order(2)
  public SecurityFilterChain browserFilterChain(
      HttpSecurity http, ClientRegistrationRepository registrations, Logout logout) {
    http.cors(withDefaults()).authorizeHttpRequests(auth -> auth
            .requestMatchers("/error").permitAll()
            .anyRequest().authenticated())
        .oauth2Login(oauth -> oauth.successHandler(authenticationSuccessHandler))
        .oauth2Client(withDefaults())
        .logout(configuration -> configuration
            .addLogoutHandler((request, response, authentication) -> {
              if (authentication != null) {
                logout.logout();
              }
            })
            .logoutSuccessHandler(new OidcClientInitiatedLogoutSuccessHandler(registrations)));
    configureHeaders(http);
    return http.build();
  }

  private void configureHeaders(HttpSecurity http) {
    http.headers(headers -> headers
        .contentSecurityPolicy(csp -> csp.policyDirectives(properties.getContentSecurityPolicy()))
        .frameOptions(frame -> frame.sameOrigin())
        .referrerPolicy(referrer -> referrer.policy(
            ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
        .permissionsPolicyHeader(permissions -> permissions.policy(
            "camera=(), geolocation=(), microphone=(), payment=()")));
  }

  private Converter<Jwt, AbstractAuthenticationToken> authenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(new JwtGrantedAuthorityConverter());
    return converter;
  }

  @Bean
  public GrantedAuthoritiesMapper userAuthoritiesMapper() {
    return authorities -> {
      Set<GrantedAuthority> mapped = new HashSet<>();
      authorities.forEach(authority -> {
        if (authority instanceof OidcUserAuthority oidc) {
          mapped.addAll(Claims.extractAuthorityFromClaims(oidc.getIdToken().getClaims()));
          if (oidc.getUserInfo() != null) {
            mapped.addAll(Claims.extractAuthorityFromClaims(oidc.getUserInfo().getClaims()));
          }
        }
      });
      return mapped;
    };
  }

  @Bean
  public JwtDecoder jwtDecoder(SessionRevocations revocations, Clock clock) {
    NimbusJwtDecoder decoder = JwtDecoders.fromOidcIssuerLocation(issuerUri);
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
        JwtValidators.createDefaultWithIssuer(issuerUri),
        new AudienceValidator(properties.getOauth2().getAudience()),
        new KeycloakAccessTokenValidator(clock)));
    // Read signed token claims locally; do not fetch/cache userinfo while decoding a token.
    return token -> {
      Jwt jwt = decoder.decode(token);
      UserSession session = new UserSession(
          new UserIdentity(jwt.getIssuer().toString(), jwt.getSubject()), jwt.getClaimAsString("sid"));
      try {
        if (revocations.isRevoked(session)) {
          throw new BadJwtException("This session has been logged out.");
        }
      } catch (DataAccessException exception) {
        throw new BadJwtException("The session could not be validated.", exception);
      }
      return jwt;
    };
  }

  @Bean
  @ConditionalOnProperty(name = "application.multitenancy.enabled", havingValue = "true")
  public FilterRegistrationBean<TenantFilter> tenantFilterRegistration(TenantFilter filter) {
    FilterRegistrationBean<TenantFilter> registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }
}
