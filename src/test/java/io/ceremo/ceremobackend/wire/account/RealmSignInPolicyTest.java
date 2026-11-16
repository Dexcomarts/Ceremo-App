package io.ceremo.ceremobackend.wire.account;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Guards the development realm import against drift from the sign-in decision of 16 November 2026: Google, email and
 * password with verification, optional passkeys, no MFA, and staff re-authentication checked through auth_time. The
 * import does not update an existing realm; apply the same settings there by re-import or the admin console.
 */
class RealmSignInPolicyTest {
  private static final JsonNode REALM = new ObjectMapper()
      .readTree(Path.of("src/docker/keycloak-realm-config/ceremo-realm.json").toFile());

  @Test
  void issuesAuthTimeToEveryClientWhoseTokensTheApiAccepts() {
    JsonNode basic = find(REALM.get("clientScopes"), "name", "basic");
    JsonNode authTime = find(basic.get("protocolMappers"), "name", "auth_time");
    assertThat(authTime.get("protocolMapper").asString()).isEqualTo("oidc-usersessionmodel-note-mapper");
    assertThat(authTime.get("config").get("user.session.note").asString()).isEqualTo("AUTH_TIME");
    assertThat(authTime.get("config").get("claim.name").asString()).isEqualTo("auth_time");
    assertThat(authTime.get("config").get("jsonType.label").asString()).isEqualTo("long");
    assertThat(authTime.get("config").get("access.token.claim").asString()).isEqualTo("true");
    assertThat(texts(REALM.get("defaultDefaultClientScopes"))).contains("basic");
    for (String clientId : List.of("web_app", "android_app", "ceremo-admin")) {
      assertThat(texts(client(clientId).get("defaultClientScopes"))).as("basic on %s", clientId).contains("basic");
    }
  }

  @Test
  void opensSelfServiceEmailSignUpWithVerificationAndALongPassword() {
    assertThat(REALM.get("registrationAllowed").asBoolean()).isTrue();
    assertThat(REALM.get("resetPasswordAllowed").asBoolean()).isTrue();
    assertThat(REALM.get("verifyEmail").asBoolean()).isTrue();
    assertThat(REALM.get("loginWithEmailAllowed").asBoolean()).isTrue();
    // Sign-up asks for email, password and first name only: the email is the username (founder decision, 16 Nov 2026).
    assertThat(REALM.get("registrationEmailAsUsername").asBoolean()).isTrue();
    assertThat(REALM.get("duplicateEmailsAllowed").asBoolean()).isFalse();
    assertThat(REALM.get("bruteForceProtected").asBoolean()).isTrue();
    // NIST SP 800-63B-4 section 3.1.1.2: at least 15 characters when a password is the only factor.
    assertThat(REALM.get("passwordPolicy").asString()).contains("length(15)", "maxLength(64)");
    assertThat(REALM.get("smtpServer").get("host").asString()).isEqualTo("mailpit");
    // Keycloak 24 removed registration-profile-action; an import that still lists it crashes the Register page
    // with a NullPointerException. These are the form actions Keycloak 26.4.2 itself creates
    // (DefaultAuthenticationFlows.registrationFlow).
    JsonNode form = find(REALM.get("authenticationFlows"), "alias", "registration form");
    assertThat(StreamSupport.stream(form.get("authenticationExecutions").spliterator(), false)
        .map(execution -> execution.get("authenticator").asString()).toList())
        .containsExactly("registration-user-creation", "registration-password-action", "registration-recaptcha-action");
  }

  @Test
  void asksOnlyForEmailPasswordAndFirstNameAtSignUp() {
    // Keycloak 26.4.2's default user profile (keycloak-default-user-profile.json) with lastName made optional.
    JsonNode profile = new ObjectMapper().readTree(REALM.get("components")
        .get("org.keycloak.userprofile.UserProfileProvider").get(0).get("config").get("kc.user.profile.config").get(0)
        .asString());
    assertThat(find(profile.get("attributes"), "name", "email").has("required")).isTrue();
    assertThat(find(profile.get("attributes"), "name", "firstName").has("required")).isTrue();
    assertThat(find(profile.get("attributes"), "name", "lastName").has("required")).isFalse();
  }

  @Test
  void offersGoogleWithTheSecretInTheVaultAndNeverLinksAccountsAutomatically() {
    JsonNode google = find(REALM.get("identityProviders"), "alias", "google");
    assertThat(google.get("providerId").asString()).isEqualTo("google");
    // Off unless a client ID is configured (Cal.com, Outline and Discourse gate optional Google login the same way).
    // Disabled, not hideOnLogin: kc_idp_hint still redirects to an enabled but hidden provider.
    assertThat(google.get("enabled").asString()).isEqualTo("${CEREMO_GOOGLE_ENABLED:false}");
    assertThat(google.get("hideOnLogin").asBoolean()).isFalse();
    assertThat(google.get("trustEmail").asBoolean()).isTrue();
    assertThat(google.get("storeToken").asBoolean()).isFalse();
    assertThat(google.get("config").get("clientSecret").asString()).isEqualTo("${vault.googleClientSecret}");
    assertThat(google.get("config").get("clientId").asString()).startsWith("${CEREMO_GOOGLE_CLIENT_ID:");
    assertThat(google.get("firstBrokerLoginFlowAlias").asString()).isEqualTo("first broker login");
    // Pre-account-hijack defence: linking a Google sign-in to an existing account by an emailed link would keep a
    // password an attacker set when pre-registering the victim's address. Keycloak 26.4.2's
    // IdpEmailVerificationAuthenticator ignores trustEmail, so the step itself is disabled; the owner proves the
    // account by re-authentication instead (or resets its password by email first).
    JsonNode options = find(REALM.get("authenticationFlows"), "alias", "Account verification options");
    assertThat(find(options.get("authenticationExecutions"), "authenticator", "idp-email-verification")
        .get("requirement").asString()).isEqualTo("DISABLED");
    assertThat(find(options.get("authenticationExecutions"), "flowAlias",
        "Verify Existing Account by Re-authentication").get("requirement").asString()).isEqualTo("ALTERNATIVE");
    for (JsonNode flow : REALM.get("authenticationFlows")) {
      for (JsonNode execution : flow.path("authenticationExecutions")) {
        assertThat(execution.path("authenticator").asString()).as("flow %s", flow.get("alias").asString())
            .isNotEqualTo("idp-auto-link");
      }
    }
  }

  @Test
  void offersPasskeysWithoutForcingThemAndDoesNotOfferOtp() {
    assertThat(REALM.get("webAuthnPolicyPasswordlessPasskeysEnabled").asBoolean()).isTrue();
    JsonNode passkey = find(REALM.get("requiredActions"), "alias", "webauthn-register-passwordless");
    assertThat(passkey.get("enabled").asBoolean()).isTrue();
    assertThat(passkey.get("defaultAction").asBoolean()).isFalse();
    assertThat(find(REALM.get("requiredActions"), "alias", "CONFIGURE_TOTP").get("enabled").asBoolean()).isFalse();
  }

  @Test
  void keepsTheStaffConsoleAShortSessionPkceClientWithoutPasswordOrOfflineGrants() {
    JsonNode admin = client("ceremo-admin");
    assertThat(admin.get("publicClient").asBoolean()).isTrue();
    assertThat(admin.get("implicitFlowEnabled").asBoolean()).isFalse();
    assertThat(admin.get("directAccessGrantsEnabled").asBoolean()).isFalse();
    assertThat(admin.get("serviceAccountsEnabled").asBoolean()).isFalse();
    assertThat(admin.get("attributes").get("pkce.code.challenge.method").asString()).isEqualTo("S256");
    assertThat(admin.get("attributes").get("client.session.idle.timeout").asLong()).isEqualTo(900);
    assertThat(admin.get("attributes").get("client.session.max.lifespan").asLong()).isEqualTo(14_400);
    assertThat(texts(admin.get("defaultClientScopes"))).doesNotContain("offline_access");
    assertThat(texts(admin.get("optionalClientScopes"))).doesNotContain("offline_access");
    assertThat(client("web_app").get("directAccessGrantsEnabled").asBoolean()).isFalse();
  }

  private static JsonNode client(String clientId) {
    return find(REALM.get("clients"), "clientId", clientId);
  }

  private static JsonNode find(JsonNode array, String field, String value) {
    return StreamSupport.stream(array.spliterator(), false)
        .filter(node -> value.equals(node.path(field).asString())).findFirst().orElseThrow();
  }

  private static List<String> texts(JsonNode array) {
    return StreamSupport.stream(array.spliterator(), false).map(JsonNode::asString).toList();
  }
}
