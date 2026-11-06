package io.ceremo.ceremobackend.wire;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/** Local test registration avoids discovery against a live Keycloak server. */
@TestConfiguration(proxyBeanMethods = false)
public class TestOidcConfiguration {

    @Bean
    ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("oidc")
            .clientId("ceremo-test").authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}").scope("openid", "profile", "email")
            .authorizationUri("https://identity.example/authorize").tokenUri("https://identity.example/token")
            .jwkSetUri("https://identity.example/certs").userInfoUri("https://identity.example/userinfo")
            .providerConfigurationMetadata(java.util.Map.of("end_session_endpoint", "https://identity.example/logout"))
            .userNameAttributeName("sub").build());
    }
}
