package io.ceremo.ceremobackend.shared.authentication.infrastructure.primary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;


class OAuth2ConfigurationTest {

  @Test
  void shouldCreateAuthorizedClientManager() {
    OAuth2Configuration configuration = new OAuth2Configuration();

    OAuth2AuthorizedClientManager manager =
        configuration.authorizedClientManager(
            mock(ClientRegistrationRepository.class), mock(OAuth2AuthorizedClientRepository.class));

    assertThat(manager).isInstanceOf(DefaultOAuth2AuthorizedClientManager.class);
  }
}
