package io.ceremo.ceremobackend.wire.springdoc.infrastructure.primary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

class SpringdocConfigurationTest {
    @Test
    void includesAnExplicitDocumentationLinkAndVersion() {
        new ApplicationContextRunner().withUserConfiguration(SpringdocConfiguration.class)
            .withPropertyValues("application.version=1.2.3", "application.documentation-url=https://docs.example/ceremo")
            .run(context -> {
                var api = context.getBean(OpenAPI.class);
                assertThat(api.getInfo().getVersion()).isEqualTo("1.2.3");
                assertThat(api.getExternalDocs().getUrl()).isEqualTo("https://docs.example/ceremo");
                assertThat(api.getExternalDocs().getDescription()).isEqualTo("Project Documentation");
            });
    }

    @Test
    void refusesToPublishOAuthDocumentationWithoutItsClientRegistration() {
        var registrations = mock(ClientRegistrationRepository.class);
        assertThatThrownBy(() -> new SpringdocOAuth2Configuration().oauthOpenApi(registrations))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("oidc client registration");
    }
}
