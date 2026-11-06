package io.ceremo.ceremobackend.shared.authentication.infrastructure.primary;

import static org.assertj.core.api.Assertions.assertThat;

import io.ceremo.ceremobackend.shared.multitenancy.infrastructure.primary.TenantFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

@SpringJUnitWebConfig({SecurityConfigurationTest.TestConfiguration.class,
    TenantSecurityWiringTest.TenantConfiguration.class})
@TestPropertySource(properties = {
    "spring.security.oauth2.client.provider.oidc.issuer-uri=https://identity.example/realms/ceremo",
    "application.multitenancy.enabled=true"})
class TenantSecurityWiringTest {
    @MockitoBean(name = "jwtDecoder") private JwtDecoder decoder;
    @MockitoBean private io.ceremo.ceremobackend.logout.application.port.in.Logout logout;
    @MockitoBean private io.ceremo.ceremobackend.account.application.port.in.Accounts accounts;
    @Autowired private TenantFilter tenantFilter;
    @Autowired @Qualifier("filterChain") private SecurityFilterChain api;
    @Autowired @Qualifier("tenantFilterRegistration") private FilterRegistrationBean<TenantFilter> registration;

    @Test
    void addsTheTenantFilterToSecurityWithoutRegisteringItTwice() {
        assertThat(api.getFilters().stream().filter(filter -> filter == tenantFilter).count()).isEqualTo(1);
        assertThat(registration.getFilter()).isSameAs(tenantFilter);
        assertThat(registration.isEnabled()).isFalse();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TenantConfiguration {
        @Bean TenantFilter tenantFilter() { return new TenantFilter(); }
    }
}
