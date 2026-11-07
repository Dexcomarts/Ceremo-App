package io.ceremo.ceremobackend.shared.authentication.infrastructure.primary;

import static org.assertj.core.api.Assertions.*;

import io.ceremo.ceremobackend.shared.authentication.domain.Role;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class ClaimsTest {

  @Test
  void ignoresMalformedRolesAndKeycloakAdministrationRoles() {
    assertThat(Claims.extractAuthorityFromClaims(Map.of("roles", "ROLE_ADMIN"))).isEmpty();
    assertThat(Claims.extractAuthorityFromClaims(Map.of("roles", List.of(123, "ROLE_CUSTOMER"))))
        .containsExactly(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    assertThat(Claims.extractAuthorityFromClaims(Map.of(
        "realm_access", Map.of("roles", List.of("admin")),
        "resource_access", Map.of("realm-management", Map.of("roles", List.of("realm-admin"))))))
        .isEmpty();
  }

  private static final String CLAIMS_NAMESPACE = "urn:ceremo:";

  @Test
  void shouldExtractAuthorityFromClaims() {
    Map<String, Object> claims = Map.of("groups", List.of(Role.ADMIN.key(), Role.CUSTOMER.key()));

    List<GrantedAuthority> authorities = Claims.extractAuthorityFromClaims(claims);

    assertThat(authorities).containsExactly(new SimpleGrantedAuthority(Role.ADMIN.key()), new SimpleGrantedAuthority(Role.CUSTOMER.key()));
  }

  @Test
  void shouldExtractAuthorityFromClaimsNamespacedRoles() {
    Map<String, Object> claims = Map.of(CLAIMS_NAMESPACE + "roles", List.of(Role.ADMIN.key(), Role.CUSTOMER.key()));

    List<GrantedAuthority> authorities = Claims.extractAuthorityFromClaims(claims);

    assertThat(authorities).containsExactly(new SimpleGrantedAuthority(Role.ADMIN.key()), new SimpleGrantedAuthority(Role.CUSTOMER.key()));
  }
}
