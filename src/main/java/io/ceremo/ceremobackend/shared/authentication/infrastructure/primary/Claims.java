package io.ceremo.ceremobackend.shared.authentication.infrastructure.primary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

final class Claims {

  static final String CLAIMS_NAMESPACE = "urn:ceremo:";

  private Claims() {}

  static List<GrantedAuthority> extractAuthorityFromClaims(Map<String, Object> claims) {
    return mapRolesToGrantedAuthorities(getRolesFromClaims(claims));
  }

  private static Collection<String> getRolesFromClaims(Map<String, Object> claims) {
    Object roles = claims.getOrDefault(
      "groups",
      claims.getOrDefault("roles", claims.getOrDefault(CLAIMS_NAMESPACE + "roles", new ArrayList<>()))
    );
    if (!(roles instanceof Collection<?> values)) {
      return List.of();
    }
    return values.stream().filter(String.class::isInstance).map(String.class::cast).toList();
  }
  private static List<GrantedAuthority> mapRolesToGrantedAuthorities(Collection<String> roles) {
    return roles.stream().filter(role -> role.startsWith("ROLE_")).map(SimpleGrantedAuthority::new).collect(Collectors.toList());
  }
}
