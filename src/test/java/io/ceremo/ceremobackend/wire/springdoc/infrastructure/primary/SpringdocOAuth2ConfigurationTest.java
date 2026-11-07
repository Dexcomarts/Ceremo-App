package io.ceremo.ceremobackend.wire.springdoc.infrastructure.primary;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

class SpringdocOAuth2ConfigurationTest {
  @Test
  void derivesOAuthEndpointsAndHandlesMissingComponents() {
    var registration = ClientRegistration.withRegistrationId("oidc").clientId("ceremo-test")
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("https://app.example/callback").authorizationUri("https://identity.example/auth")
        .tokenUri("https://identity.example/token").build();
    var customizer = new SpringdocOAuth2Configuration()
        .oauthOpenApi(new InMemoryClientRegistrationRepository(registration));
    OpenAPI api = new OpenAPI();
    customizer.customise(api);
    var flow = api.getComponents().getSecuritySchemes().get("security_auth").getFlows();
    assertThat(flow.getImplicit()).isNull();
    assertThat(flow.getAuthorizationCode().getAuthorizationUrl()).isEqualTo("https://identity.example/auth");
    assertThat(flow.getAuthorizationCode().getTokenUrl()).isEqualTo("https://identity.example/token");
    assertThat(flow.getAuthorizationCode().getScopes()).containsKeys("openid", "profile", "email");
    assertThat(api.getSecurity().getFirst().get("security_auth")).containsExactly("openid", "profile", "email");
    OpenAPI existing = new OpenAPI().components(new Components().addSecuritySchemes(
        "existing", new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")));
    customizer.customise(existing);
    assertThat(existing.getComponents().getSecuritySchemes()).containsKeys("existing", "security_auth");
  }
}
