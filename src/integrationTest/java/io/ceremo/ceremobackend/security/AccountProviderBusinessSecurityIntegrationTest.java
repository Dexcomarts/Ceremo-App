package io.ceremo.ceremobackend.security;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import io.ceremo.ceremobackend.CeremoBackendApplication;
import io.ceremo.ceremobackend.logout.domain.model.UserSession;
import io.ceremo.ceremobackend.logout.infrastructure.primary.schedule.RevocationPurgeJob;
import io.ceremo.ceremobackend.logout.domain.repository.SessionRevocations;
import io.ceremo.ceremobackend.shared.error.domain.CeremoException;
import io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusinessId;
import io.ceremo.ceremobackend.providerbusiness.application.port.in.ProviderBusinesses;
import io.ceremo.ceremobackend.providerbusiness.domain.repository.ProviderBusinessRepository;
import io.ceremo.ceremobackend.account.domain.model.UserIdentity;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Real JWT signature/discovery, production security wiring, migrations and disposable PostgreSQL. */
@SpringBootTest(classes = {CeremoBackendApplication.class, AccountProviderBusinessSecurityIntegrationTest.IdentityRegistration.class,
    AccountProviderBusinessSecurityIntegrationTest.ProviderBoundaryFixture.class}, properties = {
    "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true",
    "spring.jpa.properties.hibernate.cache.use_second_level_cache=false",
    "application.registration.terms-version=terms-v1", "application.multitenancy.enabled=false",
    "application.logging.logstash.tcp.enabled=false", "application.security.oauth2.audience[0]=ceremo-api"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Timeout(45)
class AccountProviderBusinessSecurityIntegrationTest {
  /** Exercises reserved profile routes; this is not a production provider implementation. */
  @RestController
  @TestComponent
  static class ProviderBoundaryFixture {
    @GetMapping({"/api/v1/providers/{id}", "/api/v1/providers/{id}/reviews", "/api/v1/providers/{id}/contact"})
    ResponseEntity<String> detail() {
      return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body("fixture-provider-detail");
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class IdentityRegistration {
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
  private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.0");
  private static HttpServer identityServer;
  private static RSAKey signingKey;
  private static String issuer;

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired PlatformTransactionManager transactionManager;
  @Autowired SessionRevocations revocations;
  @Autowired RevocationPurgeJob purgeJob;
  @Autowired ProviderBusinesses businesses;
  @Autowired ProviderBusinessRepository repository;
  @Autowired JwtDecoder decoder;
  @Autowired ClientRegistrationRepository registrations;

  @DynamicPropertySource
  static void fixtures(DynamicPropertyRegistry properties) throws Exception {
    POSTGRES.start();
    signingKey = new RSAKeyGenerator(2048).generate();
    identityServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    String base = "http://127.0.0.1:" + identityServer.getAddress().getPort();
    issuer = base + "/realms/ceremo";
    serve("/realms/ceremo/.well-known/openid-configuration", """
        {"issuer":"%s","jwks_uri":"%s/certs","authorization_endpoint":"%s/auth",
         "token_endpoint":"%s/token","response_types_supported":["code"],
         "subject_types_supported":["public"],"id_token_signing_alg_values_supported":["RS256"]}
        """.formatted(issuer, base, base, base));
    serve("/certs", "{\"keys\":[" + signingKey.toPublicJWK().toJSONString() + "]}");
    identityServer.start();
    properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    properties.add("spring.datasource.username", POSTGRES::getUsername);
    properties.add("spring.datasource.password", POSTGRES::getPassword);
    properties.add("spring.security.oauth2.client.provider.oidc.issuer-uri", () -> issuer);
  }

  private static void serve(String path, String content) {
    identityServer.createContext(path, exchange -> {
      byte[] response = content.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, response.length);
      try (var output = exchange.getResponseBody()) { output.write(response); }
    });
  }

  @AfterAll
  static void stop() {
    if (identityServer != null) { identityServer.stop(0); }
    POSTGRES.stop();
  }

  @BeforeEach
  void reset() {
    new TransactionTemplate(transactionManager).executeWithoutResult(status ->
        jdbc.execute("truncate provider_business_audit, provider_business_member, provider_business, revoked_session, user_identity, app_user"));
  }

  @AfterEach void clearIdentity() { SecurityContextHolder.clearContext(); }

  @Test
  void providerProfilesAndContactsRequireALiveActiveRegisteredAccount() throws Exception {
    String registered = token("profile-customer");
    UUID account = register(registered);
    String unknown = token("not-registered", "unknown-session", List.of("ROLE_ADMIN"), true, claims -> {});
    String pending = token("pending-profile", "pending-session", List.of("ROLE_CUSTOMER"), false,
        claims -> claims.claim("email_verified", false));
    register(pending);
    for (String path : List.of("/api/v1/providers/123", "/api/v1/providers/123/reviews", "/api/v1/providers/123/contact")) {
      mvc.perform(get(path)).andExpect(status().isUnauthorized());
      var denied = mvc.perform(auth(get(path), unknown)).andExpect(status().isForbidden()).andReturn();
      assertThat(denied.getResponse().getContentAsString()).doesNotContain("fixture-provider-detail");
      mvc.perform(auth(get(path), pending)).andExpect(status().isForbidden());
      mvc.perform(auth(get(path), registered)).andExpect(status().isOk())
          .andExpect(content().string("fixture-provider-detail"))
          .andExpect(header().string("Cache-Control", "no-store"));
    }
    for (String state : List.of("SUSPENDED", "ANONYMISED")) {
      new TransactionTemplate(transactionManager).executeWithoutResult(status ->
          jdbc.update("update app_user set status = ?, version = version + 1 where id = ?", state, account));
      mvc.perform(auth(get("/api/v1/providers/123"), registered)).andExpect(status().isForbidden());
      mvc.perform(auth(get("/api/v1/providers/123/contact"), registered)).andExpect(status().isForbidden());
    }
  }

  @Test
  void registrationAndAnAdminTokenDoNotExposeAnotherBusinessesPrivateRecords() throws Exception {
    String owner = token("private-owner");
    register(owner);
    UUID tenant = create(owner, "Private provider business");
    String customer = token("independent-customer");
    register(customer);
    String admin = token("outside-admin", "admin-session", List.of("ROLE_ADMIN"), true, claims -> {});
    register(admin);
    for (String suffix : List.of("", "/members", "/invitations")) {
      String path = "/api/v1/provider-businesses/" + tenant + suffix;
      mvc.perform(get(path)).andExpect(status().isUnauthorized());
      mvc.perform(auth(get(path), token("unregistered-private"))).andExpect(status().isNotFound());
      for (String outsider : List.of(customer, admin)) {
        var denied = mvc.perform(auth(get(path), outsider)).andExpect(status().isNotFound()).andReturn();
        assertThat(denied.getResponse().getContentAsString())
            .doesNotContain("Private provider business", "\"ownerId\"", "\"userId\"", "\"content\"");
      }
    }
    mvc.perform(auth(get("/api/v1/provider-businesses/" + tenant), owner)).andExpect(status().isOk());
  }

  @Test
  void customersAreIndependentAndForeignBusinessIdsNeverGrantMembership() throws Exception {
    String ama = token("ama");
    String kojo = token("kojo");
    UUID amaId = register(ama);
    register(kojo);
    UUID first = create(ama, "Ama Catering");
    UUID second = create(kojo, "Kojo Photos");
    mvc.perform(auth(get("/api/v1/users/me"), ama)).andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.id").value(amaId.toString()))
        .andExpect(jsonPath("$.roles").doesNotExist());
    mvc.perform(auth(get("/api/v1/provider-businesses?pageSize=1"), ama)).andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElementsCount").value(1))
        .andExpect(jsonPath("$.content[0].id").value(first.toString()));
    mvc.perform(auth(get("/api/v1/provider-businesses/" + second), ama)).andExpect(status().isNotFound());
    mvc.perform(auth(get("/api/v1/provider-businesses/" + first + "/members"), kojo)).andExpect(status().isNotFound());
    mvc.perform(auth(delete("/api/v1/provider-businesses/" + second + "/members/" + amaId), ama)).andExpect(status().isNotFound());
    mvc.perform(auth(get("/api/v1/provider-businesses?pageSize=101"), ama)).andExpect(status().isBadRequest());
    mvc.perform(auth(get("/api/v1/provider-businesses/not-a-uuid"), ama)).andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/provider-businesses")).andExpect(status().isUnauthorized());
  }

  @Test
  void invitationsRequireAcceptanceAndOnlyOwnersCanManageATeam() throws Exception {
    String owner = token("owner");
    String member = token("member");
    UUID ownerId = register(owner);
    UUID memberId = register(member);
    UUID tenant = create(owner, "Event Team");
    invite(owner, tenant, memberId, "MANAGER", 204);
    invite(owner, tenant, memberId, "FINANCE", 409);
    mvc.perform(auth(get("/api/v1/provider-businesses/" + tenant), member)).andExpect(status().isNotFound());
    mvc.perform(auth(get("/api/v1/provider-businesses"), member)).andExpect(jsonPath("$.totalElementsCount").value(0));
    mvc.perform(auth(get("/api/v1/provider-businesses/" + tenant + "/invitations"), owner)).andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].status").value("INVITED"))
        .andExpect(jsonPath("$.content[0].role").value("MANAGER"));
    for (int retry = 0; retry < 2; retry++) {
      mvc.perform(auth(post("/api/v1/users/me/provider-memberships/" + tenant + "/accept"), member)).andExpect(status().isOk());
    }
    mvc.perform(auth(get("/api/v1/provider-businesses/" + tenant), member)).andExpect(status().isOk());
    mvc.perform(auth(get("/api/v1/provider-businesses"), member)).andExpect(jsonPath("$.totalElementsCount").value(1));
    mvc.perform(auth(get("/api/v1/provider-businesses/" + tenant + "/members"), member)).andExpect(status().isForbidden());
    invite(member, tenant, ownerId, "MANAGER", 403);
    invite(owner, tenant, ownerId, "MANAGER", 400);
    mvc.perform(auth(delete("/api/v1/provider-businesses/" + tenant + "/members/" + ownerId), owner)).andExpect(status().isBadRequest());
    mvc.perform(auth(delete("/api/v1/provider-businesses/" + tenant + "/members/" + memberId), owner)).andExpect(status().isNoContent());
    mvc.perform(auth(get("/api/v1/provider-businesses/" + tenant), member)).andExpect(status().isNotFound());
    mvc.perform(auth(post("/api/v1/users/me/provider-memberships/" + tenant + "/accept"), member)).andExpect(status().isNotFound());
    mvc.perform(auth(delete("/api/v1/provider-businesses/" + tenant + "/members/" + memberId), owner)).andExpect(status().isNotFound());
    mvc.perform(auth(post("/api/v1/users/me/provider-memberships/" + tenant + "/accept"), owner)).andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(tenant.toString()));
    assertThat(jdbc.queryForObject("select count(*) from provider_business_audit where actor_id = ?", Long.class, ownerId)).isEqualTo(3L);
  }

  @Test
  void inviteesSeeTheirOwnInvitationsAndOwnersManageInvitationsSeparatelyFromMembers() throws Exception {
    String owner = token("owner");
    String member = token("member");
    String outsider = token("outsider");
    UUID ownerId = register(owner);
    UUID memberId = register(member);
    register(outsider);
    UUID business = create(owner, "Kente Events");
    String mine = "/api/v1/users/me/provider-memberships";
    String base = "/api/v1/provider-businesses/" + business;
    mvc.perform(get(mine)).andExpect(status().isUnauthorized());
    mvc.perform(auth(get(mine), token("unregistered-invitee"))).andExpect(status().isNotFound());
    invite(owner, business, memberId, "MANAGER", 204);
    mvc.perform(auth(get(mine), member)).andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.totalElementsCount").value(1))
        .andExpect(jsonPath("$.content[0].businessId").value(business.toString()))
        .andExpect(jsonPath("$.content[0].businessName").value("Kente Events"))
        .andExpect(jsonPath("$.content[0].role").value("MANAGER"))
        .andExpect(jsonPath("$.content[0].status").value("INVITED"));
    mvc.perform(auth(get(mine), outsider)).andExpect(jsonPath("$.totalElementsCount").value(0));
    mvc.perform(auth(get(base + "/members"), owner)).andExpect(jsonPath("$.totalElementsCount").value(1))
        .andExpect(jsonPath("$.content[0].userId").value(ownerId.toString()))
        .andExpect(jsonPath("$.content[0].role").value("OWNER"))
        .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
    mvc.perform(auth(get(mine), owner)).andExpect(jsonPath("$.totalElementsCount").value(1))
        .andExpect(jsonPath("$.content[0].businessId").value(business.toString()))
        .andExpect(jsonPath("$.content[0].role").value("OWNER"))
        .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
    mvc.perform(auth(get(base + "/invitations"), owner)).andExpect(jsonPath("$.totalElementsCount").value(1));
    mvc.perform(auth(delete(base + "/members/" + memberId), owner)).andExpect(status().isNotFound())
        .andExpect(jsonPath("$.key").value("provider-business-member-not-found"));
    mvc.perform(auth(delete(base + "/invitations/" + memberId), owner)).andExpect(status().isNoContent());
    mvc.perform(auth(get(mine), member)).andExpect(jsonPath("$.totalElementsCount").value(0));
    mvc.perform(auth(delete(base + "/invitations/" + memberId), owner)).andExpect(status().isNotFound())
        .andExpect(jsonPath("$.key").value("provider-business-invitation-not-found"));
    invite(owner, business, memberId, "FINANCE", 204);
    mvc.perform(auth(post(mine + "/" + business + "/accept"), member)).andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(business.toString()));
    mvc.perform(auth(get(mine), member)).andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
        .andExpect(jsonPath("$.content[0].role").value("FINANCE"));
    mvc.perform(auth(get(base + "/members"), owner)).andExpect(jsonPath("$.totalElementsCount").value(2))
        .andExpect(jsonPath("$.content[*].userId",
            org.hamcrest.Matchers.containsInAnyOrder(ownerId.toString(), memberId.toString())));
    mvc.perform(auth(get(base + "/invitations"), owner)).andExpect(jsonPath("$.totalElementsCount").value(0));
    mvc.perform(auth(get(base + "/invitations"), member)).andExpect(status().isForbidden());
    mvc.perform(auth(delete(base + "/invitations/" + memberId), owner)).andExpect(status().isNotFound());
    mvc.perform(auth(delete(base + "/members/" + memberId), owner)).andExpect(status().isNoContent());
    assertThat(jdbc.queryForList("select action from provider_business_audit order by occurred_at, id", String.class))
        .containsExactlyInAnyOrder("CREATED", "MEMBER_INVITED", "INVITATION_CANCELLED", "MEMBER_INVITED",
            "INVITATION_ACCEPTED", "MEMBER_REVOKED");
  }

  @Test
  void suspendedBusinessesCannotAddOrAcceptMembersAndOnlyRecentlySignedInStaffCanRestoreThem() throws Exception {
    String owner = token("owner");
    String member = token("member");
    String admin = token("admin", "admin-session", List.of("ROLE_ADMIN"), true, claims -> {});
    UUID ownerId = register(owner);
    UUID memberId = register(member);
    register(admin);
    UUID tenant = create(owner, "Provider");
    invite(owner, tenant, memberId, "FINANCE", 204);
    String base = "/api/v1/admin/provider-businesses/" + tenant;
    mvc.perform(auth(post(base + "/suspend"), owner).contentType(MediaType.APPLICATION_JSON)
        .content("{\"reason\":\"Investigation\"}")).andExpect(status().isForbidden());
    String staleSignIn = token("admin", "admin-session", List.of("ROLE_ADMIN"), true,
        claims -> claims.claim("auth_time", Instant.now().minusSeconds(7_201).getEpochSecond()));
    mvc.perform(auth(post(base + "/suspend"), staleSignIn).contentType(MediaType.APPLICATION_JSON)
        .content("{\"reason\":\"Investigation\"}")).andExpect(status().isForbidden())
        .andExpect(jsonPath("$.key").value("recent-authentication-required"));
    String noSignInTime = token("admin", "admin-session", List.of("ROLE_ADMIN"), false,
        claims -> claims.claim("azp", "web_app"));
    mvc.perform(auth(post(base + "/suspend"), noSignInTime).contentType(MediaType.APPLICATION_JSON)
        .content("{\"reason\":\"Investigation\"}")).andExpect(status().isForbidden())
        .andExpect(jsonPath("$.key").value("recent-authentication-required"));
    String androidAdmin = token("admin", "admin-session", List.of("ROLE_ADMIN"), true,
        claims -> claims.claim("azp", "android_app"));
    mvc.perform(auth(post(base + "/suspend"), androidAdmin).contentType(MediaType.APPLICATION_JSON)
        .content("{\"reason\":\"Investigation\"}")).andExpect(status().isForbidden())
        .andExpect(content().string(""));
    mvc.perform(auth(post(base + "/suspend"), admin).contentType(MediaType.APPLICATION_JSON)
        .content("{\"reason\":\"Investigation\"}")).andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUSPENDED"));
    invite(owner, tenant, memberId, "MANAGER", 403);
    mvc.perform(auth(post("/api/v1/users/me/provider-memberships/" + tenant + "/accept"), member)).andExpect(status().isForbidden());
    mvc.perform(auth(get("/api/v1/provider-businesses/" + tenant), owner)).andExpect(status().isOk());
    mvc.perform(auth(delete("/api/v1/provider-businesses/" + tenant + "/invitations/" + memberId), owner)).andExpect(status().isNoContent());
    mvc.perform(auth(post(base + "/reactivate"), admin).contentType(MediaType.APPLICATION_JSON)
        .content("{\"reason\":\"Review completed\"}")).andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));
    mvc.perform(auth(post(base + "/reactivate"), admin).contentType(MediaType.APPLICATION_JSON)
        .content("{\"reason\":\"\"}")).andExpect(status().isBadRequest());
    mvc.perform(auth(post(base + "/suspend"), admin).contentType(MediaType.APPLICATION_JSON)
        .content("{\"reason\":\"Review\",\"ownerId\":\"" + ownerId + "\"}")).andExpect(status().isBadRequest());
    mvc.perform(auth(post("/api/v1/admin/provider-businesses/" + UUID.fromString("01940d11-7390-7000-8000-000000000099") + "/suspend"), admin)
        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Review\"}")).andExpect(status().isNotFound());
  }

  @Test
  void pendingOrLocallyRestrictedAccountsCannotCreateBusinessesOrSelfPromote() throws Exception {
    String pending = token("pending", "pending-session", List.of("ROLE_CUSTOMER"), false,
        claims -> claims.claim("email_verified", false));
    UUID user = register(pending);
    mvc.perform(auth(post("/api/v1/provider-businesses"), pending).contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"Business\"}")).andExpect(status().isForbidden());
    mvc.perform(auth(get("/api/v1/users/me"), pending)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING"));
    String verified = token("pending");
    mvc.perform(auth(post("/api/v1/users/me/activate"), verified)).andExpect(status().isOk());
    for (String state : List.of("SUSPENDED", "ANONYMISED")) {
      new TransactionTemplate(transactionManager).executeWithoutResult(status ->
          jdbc.update("update app_user set status = ?, version = version + 1 where id = ?", state, user));
      mvc.perform(auth(post("/api/v1/provider-businesses"), verified).contentType(MediaType.APPLICATION_JSON)
          .content("{\"name\":\"Business\"}")).andExpect(status().isForbidden());
    }
    mvc.perform(auth(get("/api/v1/users/me"), token("unregistered"))).andExpect(status().isNotFound());
  }

  @Test
  void requestBodiesCannotGrantOwnershipMembershipStatusOrStaffRoles() throws Exception {
    String owner = token("owner");
    UUID user = register(owner);
    for (String field : List.of("ownerId", "status", "roles", "subscriptionTier")) {
      mvc.perform(auth(post("/api/v1/provider-businesses"), owner).contentType(MediaType.APPLICATION_JSON)
          .content("{\"name\":\"Business\",\"" + field + "\":\"injected\"}")).andExpect(status().isBadRequest());
    }
    UUID tenant = create(owner, "Business");
    mvc.perform(auth(post("/api/v1/provider-businesses/" + tenant + "/invitations"), owner).contentType(MediaType.APPLICATION_JSON)
        .content("{\"userId\":\"" + user + "\",\"role\":\"OWNER\"}")).andExpect(status().isBadRequest());
    UUID other = register(token("other"));
    mvc.perform(auth(post("/api/v1/provider-businesses/" + tenant + "/invitations"), owner).contentType(MediaType.APPLICATION_JSON)
        .content("{\"userId\":\"" + other + "\",\"role\":\"OWNER\"}")).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.key").value("provider-business-owner-protected"));
    mvc.perform(auth(post("/api/v1/provider-businesses/" + tenant + "/invitations"), owner).contentType(MediaType.APPLICATION_JSON)
        .content("{\"userId\":\"" + user + "\",\"role\":\"MANAGER\",\"status\":\"ACTIVE\"}")).andExpect(status().isBadRequest());
    invite(owner, tenant, UUID.fromString("01940d11-7390-7000-8000-000000000099"), "MANAGER", 404);
    assertThat(jdbc.queryForObject("select count(*) from provider_business_member where role <> 'OWNER'", Long.class)).isZero();
    assertThat(jdbc.queryForObject("select count(*) from provider_business_member where user_id = ? and role = 'OWNER'",
        Long.class, user)).isEqualTo(1L);
  }

  @Test
  void signedIdTokensAndMissingOrForeignUserTokenClaimsCannotAuthenticateToTheApi() throws Exception {
    List<Consumer<JWTClaimsSet.Builder>> changes = List.of(
        claims -> claims.claim("typ", "ID"),
        claims -> claims.claim("sid", null),
        claims -> claims.expirationTime(null),
        claims -> claims.issueTime(null),
        claims -> claims.issuer("https://foreign.example"),
        claims -> claims.audience("different-api"),
        claims -> claims.issueTime(Date.from(Instant.now().plusSeconds(120))));
    for (Consumer<JWTClaimsSet.Builder> change : changes) {
      String invalid = token("person", "session", List.of("ROLE_CUSTOMER"), false, change);
      mvc.perform(auth(get("/api/v1/users/me"), invalid)).andExpect(status().isUnauthorized());
    }
    assertThat(jdbc.queryForObject("select count(*) from app_user", Long.class)).isZero();
  }

  @Test
  void logoutRejectsEveryTokenFromTheSameSessionAcrossRefreshAndAllowsANewSession() throws Exception {
    String original = token("ama");
    register(original);
    mvc.perform(auth(post("/api/v1/logout"), original)).andExpect(status().isNoContent())
        .andExpect(header().string("Cache-Control", "no-store"));
    mvc.perform(auth(get("/api/v1/users/me"), original)).andExpect(status().isUnauthorized());
    mvc.perform(auth(get("/api/v1/users/me"), token("ama"))).andExpect(status().isUnauthorized());
    String nextSession = token("ama", "new-session", List.of("ROLE_CUSTOMER"), false, claims -> {});
    mvc.perform(auth(get("/api/v1/users/me"), nextSession)).andExpect(status().isOk());
    UserSession session = new UserSession(new UserIdentity(issuer, "ama"), "ama-session");
    revocations.revoke(session, Instant.now());
    assertThat(revocations.isRevoked(session)).isTrue();
    assertThat(jdbc.queryForObject("select count(*) from revoked_session", Long.class)).isEqualTo(1L);
    assertThat(revocations.isRevoked(new UserSession(new UserIdentity(issuer + "/other", "ama"), "ama-session"))).isFalse();
    mvc.perform(post("/api/v1/logout")).andExpect(status().isUnauthorized());
  }

  @Test
  void onlyTheAndroidClientMayUseOfflineTokensAndLogoutStillBlocksTheirSession() throws Exception {
    // ADR 11.13. Keycloak 26.4.2 creates the offline session with the online session id
    // (PersistentUserSessionProvider: new UserSessionEntity(userSession.getId())) and TokenManager copies it into sid,
    // so one sid revocation covers both. These synthetic tokens test ceremo's checks, not Keycloak itself.
    Consumer<JWTClaimsSet.Builder> androidOffline = claims -> claims.claim("azp", "android_app")
        .claim("scope", "openid offline_access profile email");
    String offline = token("kofi", "kofi-session", List.of("ROLE_CUSTOMER"), false, androidOffline);
    register(offline);
    mvc.perform(auth(get("/api/v1/users/me"), offline)).andExpect(status().isOk());
    String webOffline = token("kofi", "kofi-session", List.of("ROLE_CUSTOMER"), false,
        claims -> claims.claim("azp", "web_app").claim("scope", "openid offline_access"));
    mvc.perform(auth(get("/api/v1/users/me"), webOffline)).andExpect(status().isUnauthorized());
    mvc.perform(auth(post("/api/v1/logout"), offline)).andExpect(status().isNoContent());
    String refreshedOffline = token("kofi", "kofi-session", List.of("ROLE_CUSTOMER"), false, androidOffline);
    mvc.perform(auth(get("/api/v1/users/me"), refreshedOffline)).andExpect(status().isUnauthorized());
  }

  @Test
  void scheduledPurgeKeepsRevocationsForTheWholeRetentionAndRemovesOnlyOlderOnes() {
    Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    UserSession expired = new UserSession(new UserIdentity(issuer, "expired"), "expired-session");
    UserSession retained = new UserSession(new UserIdentity(issuer, "retained"), "retained-session");
    revocations.revoke(expired, now.minus(Duration.ofDays(93)));
    revocations.revoke(retained, now.minus(Duration.ofDays(91)));
    purgeJob.purge();
    assertThat(revocations.isRevoked(expired)).isFalse();
    assertThat(revocations.isRevoked(retained)).isTrue();
  }

  @Test
  void purgeDeletesOnlyRowsStrictlyBeforeTheCutoffAndIsIdempotent() {
    Instant revokedAt = Instant.parse("2026-11-14T00:00:00.123Z");
    UserSession atCutoff = new UserSession(new UserIdentity(issuer, "edge"), "edge-session");
    revocations.revoke(atCutoff, revokedAt);
    assertThat(revocations.purgeRevokedBefore(revokedAt)).isZero();
    assertThat(revocations.isRevoked(atCutoff)).isTrue();
    assertThat(revocations.purgeRevokedBefore(revokedAt.plusMillis(1))).isEqualTo(1);
    assertThat(revocations.purgeRevokedBefore(revokedAt.plusMillis(1))).isZero();
  }

  @Test
  void migrationIndexesTheRevocationTimeUsedByThePurge() {
    assertThat(jdbc.queryForObject("""
        select count(*) from pg_indexes where schemaname = current_schema()
        and tablename = 'revoked_session' and indexname = 'idx_revoked_session_revoked_at'
        """, Long.class)).isEqualTo(1L);
  }

  @Test
  void browserLogoutRequiresCsrfRevokesTheSessionAndUsesTheRegisteredIdentityEndpoint() throws Exception {
    mvc.perform(post("/logout").with(oidcLogin().clientRegistration(registrations.findByRegistrationId("oidc")).idToken(token -> token.issuer(issuer)
        .subject("browser").claim("sid", "browser-session")))).andExpect(status().isForbidden());
    mvc.perform(post("/logout").with(csrf()).with(oidcLogin().clientRegistration(registrations.findByRegistrationId("oidc")).idToken(token -> token.issuer(issuer)
            .subject("browser").claim("sid", "browser-session"))))
        .andExpect(status().is3xxRedirection())
        .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("https://identity.example/logout?id_token_hint=")));
    assertThat(revocations.isRevoked(new UserSession(new UserIdentity(issuer, "browser"), "browser-session"))).isTrue();
    mvc.perform(post("/logout").with(csrf())).andExpect(status().is3xxRedirection());
  }

  @Test
  void theDatabaseAllowsExactlyOneActiveOwnerRowPerBusiness() throws Exception {
    String owner = token("owner");
    UUID ownerId = register(owner);
    UUID otherId = register(token("other"));
    UUID business = create(owner, "Guarded");
    String insert = "insert into provider_business_member (id, provider_business_id, user_id, role, status, created_at, updated_at, version) "
        + "values (uuidv7(), ?, ?, ?, ?, current_timestamp, current_timestamp, 0)";
    assertThatThrownBy(() -> jdbc.update(insert, business, otherId, "OWNER", "ACTIVE"))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class)
        .hasMessageContaining("uq_provider_business_member_owner");
    assertThatThrownBy(() -> jdbc.update(insert, business, otherId, "OWNER", "INVITED"))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class)
        .hasMessageContaining("ck_provider_business_member_owner_active");
    assertThatThrownBy(() -> jdbc.update(insert, business, otherId, "ADMIN", "ACTIVE"))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class)
        .hasMessageContaining("ck_provider_business_member_role");
    assertThat(jdbc.queryForList("select user_id from provider_business_member where provider_business_id = ?", UUID.class, business))
        .containsExactly(ownerId);
  }

  @Test
  void auditFailureRollsBackTheEntireBusinessMutation() throws Exception {
    String owner = token("owner");
    register(owner);
    assume(owner);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
      businesses.create("Rolled back");
      throw new IllegalStateException("Later operation failed");
    })).isInstanceOf(IllegalStateException.class);
    assertThat(jdbc.queryForObject("select count(*) from provider_business", Long.class)).isZero();
    assertThat(jdbc.queryForObject("select count(*) from provider_business_member", Long.class)).isZero();
    assertThat(jdbc.queryForObject("select count(*) from provider_business_audit", Long.class)).isZero();
    UUID tenant = create(owner, "Kept");
    ProviderBusinessId keptBusiness = ProviderBusinessId.of(tenant);
    UUID unknownActor = UUID.fromString("01940d11-7390-7000-8000-000000000099");
    assertThatThrownBy(() -> transaction.executeWithoutResult(status ->
        repository.recordChange(keptBusiness, unknownActor, "CREATED", "Invalid actor")))
        .isInstanceOf(RuntimeException.class);
    assertThat(jdbc.queryForObject("select count(*) from provider_business_audit", Long.class)).isEqualTo(1L);
  }

  @Test
  void missingAggregateStatusUpdatesFailWithoutCreatingAReplacementBusiness() throws Exception {
    UUID owner = register(token("owner"));
    var missing = io.ceremo.ceremobackend.providerbusiness.domain.model.ProviderBusiness.create(
        ProviderBusinessId.of(UUID.fromString("01940d11-7390-7000-8000-000000000099")), "Missing", owner);
    assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status ->
        repository.updateStatus(missing.suspend())))
        .isInstanceOfSatisfying(CeremoException.class,
            error -> assertThat(error.key().get()).isEqualTo("provider-business-not-found"));
    assertThat(jdbc.queryForObject("select count(*) from provider_business", Long.class)).isZero();
    assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status ->
        repository.accept(missing.id(), owner)))
        .isInstanceOfSatisfying(CeremoException.class,
            error -> assertThat(error.key().get()).isEqualTo("provider-business-member-not-found"));
  }

  @Test
  void membershipIsRecheckedAfterWaitingForAConcurrentRevocationToCommit() throws Exception {
    String owner = token("owner");
    String member = token("member");
    register(owner);
    UUID memberId = register(member);
    UUID tenant = create(owner, "Team");
    invite(owner, tenant, memberId, "MANAGER", 204);
    mvc.perform(auth(post("/api/v1/users/me/provider-memberships/" + tenant + "/accept"), member)).andExpect(status().isOk());
    try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
      CountDownLatch attempting = new CountDownLatch(1);
      Future<?>[] lookup = new Future<?>[1];
      assume(owner);
      new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
        businesses.get(ProviderBusinessId.of(tenant)); // Owner holds the root lock.
        lookup[0] = executor.submit(() -> {
          try {
            assume(member);
            attempting.countDown();
            businesses.get(ProviderBusinessId.of(tenant));
          } finally { SecurityContextHolder.clearContext(); }
        });
        try {
          assertThat(attempting.await(5, TimeUnit.SECONDS)).isTrue();
          assertThatThrownBy(() -> lookup[0].get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
        } catch (InterruptedException error) { throw new IllegalStateException(error); }
        businesses.removeMember(ProviderBusinessId.of(tenant), memberId);
      });
      assertThatThrownBy(() -> lookup[0].get(10, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class)
          .hasCauseInstanceOf(CeremoException.class);
    }
  }

  private void assume(String rawToken) {
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(decoder.decode(rawToken), List.of()));
  }

  private UUID register(String token) throws Exception {
    String response = mvc.perform(auth(post("/api/v1/users/me"), token).contentType(MediaType.APPLICATION_JSON)
        .content("{\"displayName\":\"Test organiser\",\"termsVersion\":\"terms-v1\",\"termsAccepted\":true}"))
        .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    return UUID.fromString(JsonPath.read(response, "$.id"));
  }

  private UUID create(String token, String name) throws Exception {
    String response = mvc.perform(auth(post("/api/v1/provider-businesses"), token).contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"" + name + "\"}")).andExpect(status().isCreated())
        .andExpect(header().string("Cache-Control", "no-store")).andReturn().getResponse().getContentAsString();
    return UUID.fromString(JsonPath.read(response, "$.id"));
  }

  private void invite(String token, UUID tenant, UUID user, String role, int expected) throws Exception {
    mvc.perform(auth(post("/api/v1/provider-businesses/" + tenant + "/invitations"), token).contentType(MediaType.APPLICATION_JSON)
        .content("{\"userId\":\"" + user + "\",\"role\":\"" + role + "\"}"))
        .andExpect(status().is(expected));
  }

  private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, String token) {
    return request.header("Authorization", "Bearer " + token);
  }

  private static String token(String subject) throws Exception {
    return token(subject, subject + "-session", List.of("ROLE_CUSTOMER"), false, claims -> {});
  }

  private static String token(String subject, String sid, List<String> roles, boolean recentStaffSignIn,
      Consumer<JWTClaimsSet.Builder> change) throws Exception {
    Instant now = Instant.now();
    JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder().issuer(issuer).subject(subject).audience("ceremo-api")
        .claim("typ", "Bearer").claim("sid", sid).claim("roles", roles).claim("email_verified", true)
        .issueTime(Date.from(now.minusSeconds(1))).expirationTime(Date.from(now.plusSeconds(300)));
    // No amr: staff need only a recent sign-in from a staff web client (founder decision, 16 Nov 2026).
    if (recentStaffSignIn) { claims.claim("azp", "web_app").claim("auth_time", now.getEpochSecond()); }
    change.accept(claims);
    SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims.build());
    jwt.sign(new RSASSASigner(signingKey));
    return jwt.serialize();
  }
}
