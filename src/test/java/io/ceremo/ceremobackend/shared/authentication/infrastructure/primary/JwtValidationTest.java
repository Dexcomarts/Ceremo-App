package io.ceremo.ceremobackend.shared.authentication.infrastructure.primary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.ceremo.ceremobackend.shared.multitenancy.infrastructure.primary.TenantFilter;
import java.time.Instant;
import java.time.Clock;
import io.ceremo.ceremobackend.logout.domain.repository.SessionRevocations;
import org.springframework.dao.DataAccessResourceFailureException;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;

class JwtValidationTest {
  private static final String ISSUER = "https://identity.example/realms/ceremo";
  private static RSAKey signingKey;

  @BeforeAll
  static void createKey() throws JOSEException {
    signingKey = new RSAKeyGenerator(2048).generate();
  }

  @Test
  void acceptsSignedTokenWithMatchingIssuerAndAudience() throws Exception {
    assertThat(decoder().decode(signedToken(ISSUER, "ceremo-api", Instant.now().plusSeconds(300), signingKey))
        .getSubject()).isEqualTo("test-user");
  }

  @Test
  void rejectsWrongIssuerAudienceExpiryAndSignature() throws Exception {
    JwtDecoder decoder = decoder();
    for (String invalid : List.of(
        signedToken("https://foreign.example", "ceremo-api", Instant.now().plusSeconds(300), signingKey),
        signedToken(ISSUER, "other-api", Instant.now().plusSeconds(300), signingKey),
        signedToken(ISSUER, "ceremo-api", Instant.now().minusSeconds(300), signingKey),
        signedToken(ISSUER, "ceremo-api", Instant.now().plusSeconds(300), new RSAKeyGenerator(2048).generate()))) {
      assertThatThrownBy(() -> decoder.decode(invalid)).isInstanceOf(JwtException.class);
    }
  }

  private static JwtDecoder decoder() throws JOSEException {
    return decoder(mock(SessionRevocations.class));
  }

  @Test
  void rejectsRevokedSessionsAndFailsClosedWhenStorageIsUnavailable() throws Exception {
    SessionRevocations revocations = mock(SessionRevocations.class);
    JwtDecoder decoder = decoder(revocations);
    String token = signedToken(ISSUER, "ceremo-api", Instant.now().plusSeconds(300), signingKey);
    when(revocations.isRevoked(any())).thenReturn(true);
    assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class)
        .hasMessage("This session has been logged out.");
    when(revocations.isRevoked(any())).thenThrow(new DataAccessResourceFailureException("Private DB detail"));
    assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class)
        .hasMessage("The session could not be validated.");
  }

  private static JwtDecoder decoder(SessionRevocations revocations) throws JOSEException {
    NimbusJwtDecoder realDecoder = NimbusJwtDecoder.withPublicKey(signingKey.toRSAPublicKey()).build();
    ApplicationSecurityProperties properties = new ApplicationSecurityProperties();
    properties.getOauth2().setAudience(List.of("ceremo-api"));
    SecurityConfiguration config = new SecurityConfiguration(properties,
        new CustomAuthenticationSuccessHandler(),
        new DefaultListableBeanFactory().getBeanProvider(TenantFilter.class));
    ReflectionTestUtils.setField(config, "issuerUri", ISSUER);
    try (var factory = mockStatic(JwtDecoders.class)) {
      factory.when(() -> JwtDecoders.fromOidcIssuerLocation(ISSUER)).thenReturn(realDecoder);
      return config.jwtDecoder(revocations, Clock.systemUTC());
    }
  }

  private static String signedToken(String issuer, String audience, Instant expiry, RSAKey key)
      throws JOSEException {
    SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), new JWTClaimsSet.Builder()
        .issuer(issuer).subject("test-user").audience(audience).claim("typ", "Bearer").claim("sid", "session-123")
        .issueTime(Date.from(Instant.now().minusSeconds(600)))
        .expirationTime(Date.from(expiry)).build());
    jwt.sign(new RSASSASigner(key));
    return jwt.serialize();
  }
}
