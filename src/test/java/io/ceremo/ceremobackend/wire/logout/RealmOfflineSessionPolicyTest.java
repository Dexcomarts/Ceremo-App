package io.ceremo.ceremobackend.wire.logout;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Guards the development realm import against drift from ADR 11.13. The revocation retention (LogoutProperties) is
 * only safe while no Keycloak session that can carry a revoked sid outlives it. This file does not update an existing
 * realm; apply the same settings there through the admin console or a realm re-import.
 */
class RealmOfflineSessionPolicyTest {
  private static final JsonNode REALM = new ObjectMapper()
      .readTree(Path.of("src/docker/keycloak-realm-config/ceremo-realm.json").toFile());

  @Test
  void capsEverySessionThatCanCarryARevokedSidWithinTheRetention() {
    assertThat(REALM.get("offlineSessionMaxLifespanEnabled").asBoolean()).isTrue();
    long offlineMaximum = REALM.get("offlineSessionMaxLifespan").asLong();
    assertThat(offlineMaximum).isPositive();
    assertThat(Duration.ofSeconds(offlineMaximum).plusMinutes(15).plusSeconds(60))
        .isLessThan(LogoutProperties.MINIMUM_RETENTION);
    assertThat(Duration.ofSeconds(REALM.get("ssoSessionMaxLifespan").asLong()))
        .isLessThan(LogoutProperties.MINIMUM_RETENTION);
    for (JsonNode client : REALM.get("clients")) {
      JsonNode override = client.path("attributes").path("client.offline.session.max.lifespan");
      assertThat(override.isMissingNode() || override.asLong() <= offlineMaximum)
          .as("offline lifespan override on %s", client.get("clientId").asString()).isTrue();
    }
  }

  @Test
  void letsOnlyTheAndroidClientRequestOfflineTokensTheApiAccepts() {
    assertThat(names(REALM.get("defaultOptionalClientScopes"))).doesNotContain("offline_access");
    assertThat(names(REALM.get("defaultDefaultClientScopes"))).doesNotContain("offline_access");
    // Keycloak's built-in clients (account, admin-cli, ...) keep offline_access, but the API's AudienceValidator
    // rejects their tokens: only client-level mappers add the ceremo-api audience, never a shared client scope.
    for (JsonNode scope : REALM.get("clientScopes")) {
      assertThat(addsApiAudience(scope)).as("ceremo-api audience on client scope %s", scope.get("name").asString())
          .isFalse();
    }
    for (JsonNode client : REALM.get("clients")) {
      String clientId = client.get("clientId").asString();
      boolean offline = names(client.path("optionalClientScopes")).contains("offline_access")
          || names(client.path("defaultClientScopes")).contains("offline_access");
      if (addsApiAudience(client)) {
        assertThat(offline).as("offline_access on API client %s", clientId).isEqualTo("android_app".equals(clientId));
      }
    }
  }

  private static boolean addsApiAudience(JsonNode clientOrScope) {
    return StreamSupport.stream(clientOrScope.path("protocolMappers").spliterator(), false)
        .anyMatch(mapper -> "oidc-audience-mapper".equals(mapper.path("protocolMapper").asString())
            && "ceremo-api".equals(mapper.path("config").path("included.custom.audience").asString()));
  }

  @Test
  void configuresTheAndroidClientAsANativePkceClient() {
    JsonNode android = StreamSupport.stream(REALM.get("clients").spliterator(), false)
        .filter(client -> "android_app".equals(client.get("clientId").asString())).findFirst().orElseThrow();
    assertThat(android.get("publicClient").asBoolean()).isTrue();
    assertThat(android.get("standardFlowEnabled").asBoolean()).isTrue();
    assertThat(android.get("implicitFlowEnabled").asBoolean()).isFalse();
    assertThat(android.get("directAccessGrantsEnabled").asBoolean()).isFalse();
    assertThat(android.get("serviceAccountsEnabled").asBoolean()).isFalse();
    assertThat(android.get("attributes").get("pkce.code.challenge.method").asString()).isEqualTo("S256");
    assertThat(names(android.get("redirectUris"))).containsExactly("io.ceremo.android:/oauth2redirect");
  }

  private static List<String> names(JsonNode array) {
    return StreamSupport.stream(array.spliterator(), false).map(JsonNode::asString).toList();
  }
}
