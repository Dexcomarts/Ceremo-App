package io.ceremo.ceremobackend.shared.authentication.infrastructure.primary;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Test class for the {@link AudienceValidator} utility class.
 */
class AudienceValidatorTest {

  @Test
  void rejectsATokenWithoutAnAudienceList() {
    Jwt missingAudience = mock(Jwt.class);
    when(missingAudience.getAudience()).thenReturn(null);
    assertThat(new AudienceValidator(List.of("ceremo-api")).validate(missingAudience).hasErrors()).isTrue();
  }

  private final AudienceValidator validator = new AudienceValidator(List.of("api://default"));

  @Test
  void shouldInvalidAudience() {
    Jwt badJwt = mock(Jwt.class);
    when(badJwt.getAudience()).thenReturn(List.of("bar"));

    assertThat(validator.validate(badJwt).hasErrors()).isTrue();
  }

  @Test
  void shouldValidAudience() {
    Jwt jwt = mock(Jwt.class);
    when(jwt.getAudience()).thenReturn(List.of("api://default"));

    assertThat(validator.validate(jwt).hasErrors()).isFalse();
  }

  @Test
  void shouldSupportAnyMatchingAudience() {
    AudienceValidator localValidator = new AudienceValidator(List.of("api://default", "api://secondary"));
    Jwt jwt = mock(Jwt.class);
    when(jwt.getAudience()).thenReturn(List.of("other", "api://secondary"));

    OAuth2TokenValidatorResult result = localValidator.validate(jwt);

    assertThat(result.hasErrors()).isFalse();
  }

  @Test
  void shouldRejectEmptyAllowedAudience() {
    assertThatThrownBy(() -> new AudienceValidator(List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Allowed audience");
  }
}
