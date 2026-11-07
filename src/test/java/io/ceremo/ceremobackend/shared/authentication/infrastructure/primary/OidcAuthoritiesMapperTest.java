package io.ceremo.ceremobackend.shared.authentication.infrastructure.primary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.ceremo.ceremobackend.shared.multitenancy.infrastructure.primary.TenantFilter;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

class OidcAuthoritiesMapperTest {
    private final SecurityConfiguration configuration = new SecurityConfiguration(
        new ApplicationSecurityProperties(), mock(CustomAuthenticationSuccessHandler.class),
        new StaticListableBeanFactory().getBeanProvider(TenantFilter.class));

    @Test
    void mapsIdTokenRolesWithoutUserInfoAndIgnoresUnrelatedAuthorities() {
        var mapped = configuration.userAuthoritiesMapper().mapAuthorities(List.of(
            new OidcUserAuthority(idToken()), new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertThat(mapped.stream().map(GrantedAuthority::getAuthority).toList()).containsExactly("ROLE_CUSTOMER");
    }

    @Test
    void combinesIdTokenAndMatchedUserInfoRolesWithoutDuplicates() {
        var info = new OidcUserInfo(Map.of("sub", "person-123",
            "roles", List.of("ROLE_CUSTOMER", "ROLE_PROVIDER")));
        var mapped = configuration.userAuthoritiesMapper().mapAuthorities(
            List.of(new OidcUserAuthority(idToken(), info)));
        assertThat(mapped.stream().map(GrantedAuthority::getAuthority).toList())
            .containsExactlyInAnyOrder("ROLE_CUSTOMER", "ROLE_PROVIDER");
    }

    private static OidcIdToken idToken() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new OidcIdToken("synthetic-id-token", now, now.plusSeconds(300),
            Map.of("sub", "person-123", "roles", List.of("ROLE_CUSTOMER")));
    }
}

