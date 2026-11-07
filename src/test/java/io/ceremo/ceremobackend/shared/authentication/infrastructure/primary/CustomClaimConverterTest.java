package io.ceremo.ceremobackend.shared.authentication.infrastructure.primary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

class CustomClaimConverterTest {
  private final RestTemplate rest = mock(RestTemplate.class);
  private final CustomClaimConverter converter = new CustomClaimConverter(
      ClientRegistration.withRegistrationId("oidc").clientId("ceremo")
          .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
          .tokenUri("https://identity.example/token")
          .userInfoUri("https://identity.example/userinfo").build(), rest);

  @AfterEach
  void clearRequest() { RequestContextHolder.resetRequestAttributes(); }

  @Test
  void leavesClaimsUnchangedOutsideHttpRequest() {
    assertThat(converter.convert(Map.of("sub", "customer-1"))).containsEntry("sub", "customer-1");
    verifyNoInteractions(rest);
  }

  @Test
  void appendsOnlyMatchingUserAndDoesNotReusePriorRequestData() {
    useRequest();
    var json = new ObjectMapper();
    ObjectNode first = json.createObjectNode().put("sub", "customer-1").put("preferred_username", "old-name");
    ObjectNode updated = json.createObjectNode().put("sub", "customer-1").put("preferred_username", "new-name");
    stub(first, updated);
    assertThat(converter.convert(Map.of("sub", "customer-1")))
        .containsEntry("preferred_username", "old-name");
    assertThat(converter.convert(Map.of("sub", "customer-1")))
        .containsEntry("preferred_username", "new-name");
  }

  @Test
  void rejectsUserInfoForDifferentSubject() {
    useRequest();
    stub(new ObjectMapper().createObjectNode().put("sub", "other-customer"));
    assertThatThrownBy(() -> converter.convert(Map.of("sub", "customer-1")))
        .isInstanceOf(OAuth2AuthenticationException.class);
  }

  @Test
  void leavesSignedClaimsIntactWhenUserInfoHasNoBody() {
    useRequest();
    when(rest.exchange(eq("https://identity.example/userinfo"), eq(HttpMethod.GET),
        any(HttpEntity.class), eq(ObjectNode.class))).thenReturn(ResponseEntity.ok().build());
    assertThat(converter.convert(Map.of("sub", "customer-1", "given_name", "Signed name")))
        .containsEntry("given_name", "Signed name");
  }

  @Test
  void rejectsMissingAndNullUserInfoSubjects() {
    useRequest();
    var json = new ObjectMapper();
    stub(json.createObjectNode(), json.createObjectNode().putNull("sub"));
    assertThatThrownBy(() -> converter.convert(Map.of("sub", "customer-1")))
        .isInstanceOf(OAuth2AuthenticationException.class);
    assertThatThrownBy(() -> converter.convert(Map.of("sub", "customer-1")))
        .isInstanceOf(OAuth2AuthenticationException.class);
  }

  @Test
  void appendsMatchedNamesGroupsAndNamespacedRoles() {
    useRequest();
    var user = new ObjectMapper().createObjectNode().put("sub", "customer-1")
        .put("given_name", "Original").put("family_name", "Original")
        .put("email", "ama@example.com").put("name", "Ama Akua Mensah");
    user.putArray("groups").add("ROLE_CUSTOMER");
    user.putArray(Claims.CLAIMS_NAMESPACE + "roles").add("ROLE_PROVIDER");
    stub(user);
    assertThat(converter.convert(Map.of("sub", "customer-1")))
        .containsEntry("given_name", "Ama").containsEntry("family_name", "Akua Mensah")
        .containsEntry("email", "ama@example.com")
        .containsEntry("groups", java.util.List.of("ROLE_CUSTOMER"))
        .containsEntry("roles", java.util.List.of("ROLE_PROVIDER"));
  }

  @Test
  void ignoresAWhitespaceOnlyFullNameInsteadOfErasingExistingNames() {
    useRequest();
    stub(new ObjectMapper().createObjectNode().put("sub", "customer-1").put("name", "   "));
    assertThat(converter.convert(Map.of("sub", "customer-1", "given_name", "Ama")))
        .containsEntry("given_name", "Ama").doesNotContainKey("family_name");
  }

  private void useRequest() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer test-token");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
  }

  private void stub(ObjectNode first, ObjectNode... restOfResponses) {
    var responses = when(rest.exchange(eq("https://identity.example/userinfo"), eq(HttpMethod.GET),
        any(HttpEntity.class), eq(ObjectNode.class))).thenReturn(ResponseEntity.ok(first));
    for (ObjectNode next : restOfResponses) { responses = responses.thenReturn(ResponseEntity.ok(next)); }
  }
}
