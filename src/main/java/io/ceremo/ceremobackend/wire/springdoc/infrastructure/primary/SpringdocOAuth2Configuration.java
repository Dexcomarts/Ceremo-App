package io.ceremo.ceremobackend.wire.springdoc.infrastructure.primary;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.util.StringUtils;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureBefore(SpringdocConfiguration.class)
class SpringdocOAuth2Configuration {

  @Value("${springdoc.oauth2.authorization-url:}")
  private String authorizationUrl;

  @Value("${springdoc.oauth2.token-url:}")
  private String tokenUrl;

  @Bean
  GlobalOpenApiCustomizer oauthOpenApi(ClientRegistrationRepository registrations) {
    var registration = registrations.findByRegistrationId("oidc");
    if (registration == null) {
      throw new IllegalStateException("The oidc client registration is required for API documentation");
    }
    String authorizationEndpoint = StringUtils.hasText(authorizationUrl)
        ? authorizationUrl : registration.getProviderDetails().getAuthorizationUri();
    String tokenEndpoint = StringUtils.hasText(tokenUrl)
        ? tokenUrl : registration.getProviderDetails().getTokenUri();
    return openApi -> openApi
        .components(oauthComponents(openApi.getComponents(), authorizationEndpoint, tokenEndpoint))
        .addSecurityItem(new SecurityRequirement().addList("security_auth", List.of("openid", "profile", "email")));
  }

  private Components oauthComponents(Components existingComponents, String authorizationEndpoint, String tokenEndpoint) {
    Components components = existingComponents == null ? new Components() : existingComponents;
    return components.addSecuritySchemes(
      "security_auth",
      new SecurityScheme()
        .type(SecurityScheme.Type.OAUTH2)
        .flows(new OAuthFlows().authorizationCode(new OAuthFlow()
            .authorizationUrl(authorizationEndpoint).tokenUrl(tokenEndpoint)
            .scopes(new io.swagger.v3.oas.models.security.Scopes()
                .addString("openid", "OpenID identity")
                .addString("profile", "Basic profile")
                .addString("email", "Email address"))))
    );
  }
}
