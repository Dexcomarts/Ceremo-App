package io.ceremo.ceremobackend.account.infrastructure.primary.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.ceremo.ceremobackend.CeremoBackendApplication;
import io.ceremo.ceremobackend.wire.TestOidcConfiguration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Real HTTP/security/application/persistence wiring with H2; PostgreSQL has a separate suite. */
@SpringBootTest(classes = {CeremoBackendApplication.class, TestOidcConfiguration.class}, properties = {
    "spring.datasource.url=jdbc:h2:mem:ceremo-http;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
    "spring.jpa.properties.hibernate.cache.use_second_level_cache=false",
    "application.registration.terms-version=terms-v1",
    "application.logging.logstash.tcp.enabled=false", "application.multitenancy.enabled=false",
    "spring.security.oauth2.client.provider.oidc.issuer-uri=https://identity.example/realms/ceremo"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserResourceTest {

    private static final String PATH = "/api/v1/users/me";
    private static final String ISSUER = "https://identity.example/realms/ceremo";
    private static final String VALID_REQUEST =
        "{\"displayName\":\"Ama Mensah\",\"termsVersion\":\"terms-v1\",\"termsAccepted\":true}";

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoBean(name = "jwtDecoder") private JwtDecoder decoder;

    @BeforeEach
    void prepare() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.update("delete from user_identity");
            jdbc.update("delete from app_user");
        });
        when(decoder.decode("pending")).thenReturn(token("person-123", false));
        when(decoder.decode("verified")).thenReturn(token("person-123", true));
        when(decoder.decode("other")).thenReturn(token("other-person", true));
        when(decoder.decode("invalid")).thenThrow(new BadJwtException("Untrusted token detail"));
    }

    @Test
    void registersTheAuthenticatedAccountAndReturnsItsSafeProfile() throws Exception {
        mvc.perform(authenticated(post(PATH), "verified")
                .contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", PATH))
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.displayName").value("Ama Mensah"))
            .andExpect(jsonPath("$.termsVersion").value("terms-v1"))
            .andExpect(jsonPath("$.termsAcceptedAt").isString())
            .andExpect(jsonPath("$.id").isString())
            .andExpect(jsonPath("$.*").value(org.hamcrest.Matchers.hasSize(5)))
            .andExpect(jsonPath("$.issuer").doesNotExist())
            .andExpect(jsonPath("$.subject").doesNotExist());
        assertThat(jdbc.queryForObject("select count(*) from app_user", Long.class)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("select count(*) from user_identity", Long.class)).isEqualTo(1L);
    }

    @Test
    void lookupOfAMissingAccountDoesNotCreateOne() throws Exception {
        mvc.perform(authenticated(get(PATH), "verified"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.key").value("user-not-found"));
        assertThat(jdbc.queryForObject("select count(*) from app_user", Long.class)).isZero();
    }

    @Test
    void readsOnlyTheCallersAccountAndDoesNotActivateDuringGet() throws Exception {
        register("pending");
        mvc.perform(authenticated(get(PATH), "verified"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.status").value("PENDING"));
        mvc.perform(authenticated(get(PATH), "other")).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select status from app_user", String.class)).isEqualTo("PENDING");
    }

    @Test
    void explicitlyActivatesAfterVerificationAndAllowsSafeRetries() throws Exception {
        register("pending");
        mvc.perform(authenticated(post(PATH + "/activate"), "pending"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.key").value("email-not-verified"));
        mvc.perform(authenticated(post(PATH + "/activate"), "verified"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
        long version = jdbc.queryForObject("select version from app_user", Long.class);
        mvc.perform(authenticated(post(PATH + "/activate"), "verified"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
        assertThat(jdbc.queryForObject("select version from app_user", Long.class)).isEqualTo(version);
    }

    @Test
    void cannotActivateAnotherPersonsAccount() throws Exception {
        register("pending");
        mvc.perform(authenticated(post(PATH + "/activate"), "other"))
            .andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select status from app_user", String.class)).isEqualTo("PENDING");
    }

    @ParameterizedTest
    @ValueSource(strings = {"SUSPENDED", "ANONYMISED"})
    void verifiedEmailCannotReactivateRestrictedAccounts(String accountStatus) throws Exception {
        register("pending");
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
            jdbc.update("update app_user set status = ?, version = version + 1", accountStatus));
        mvc.perform(authenticated(post(PATH + "/activate"), "verified"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.key").value("account-restricted"));
        assertThat(jdbc.queryForObject("select status from app_user", String.class)).isEqualTo(accountStatus);
    }

    @Test
    void duplicateRegistrationReturnsConflictWithoutReplacingTheAccount() throws Exception {
        register("pending");
        mvc.perform(authenticated(post(PATH), "verified")
                .contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.key").value("user-already-exists"));
        assertThat(jdbc.queryForObject("select count(*) from app_user", Long.class)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("select status from app_user", String.class)).isEqualTo("PENDING");
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void rejectsInvalidBodiesWithoutPersistingAnAccount(String body) throws Exception {
        mvc.perform(authenticated(post(PATH), "pending")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        assertThat(jdbc.queryForObject("select count(*) from app_user", Long.class)).isZero();
    }

    @Test
    void rejectsStaleTermsThroughTheExistingApplicationRule() throws Exception {
        mvc.perform(authenticated(post(PATH), "verified").contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST.replace("terms-v1", "terms-v0")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.key").value("terms-not-accepted"));
        assertThat(jdbc.queryForObject("select count(*) from app_user", Long.class)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"id", "issuer", "subject", "status", "email_verified", "roles", "unexpected"})
    void rejectsClientSuppliedAuthorityAndUnknownFields(String field) throws Exception {
        String body = VALID_REQUEST.substring(0, VALID_REQUEST.length() - 1)
            + ",\"" + field + "\":\"attacker-value\"}";
        mvc.perform(authenticated(post(PATH), "pending").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select count(*) from app_user", Long.class)).isZero();
    }

    @Test
    void requestContentCannotSubstituteForTheTrustedVerificationClaim() throws Exception {
        register("pending");
        mvc.perform(authenticated(post(PATH + "/activate"), "pending")
                .contentType(MediaType.APPLICATION_JSON).content("{\"email_verified\":true}"))
            .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("select status from app_user", String.class)).isEqualTo("PENDING");
    }

    @Test
    void unauthenticatedCallsCannotReadRegisterOrActivate() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
            .andExpect(status().isUnauthorized());
        mvc.perform(post(PATH + "/activate")).andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("select count(*) from app_user", Long.class)).isZero();
    }

    @Test
    void invalidBearerTokenPreservesTheAuthenticationChallenge() throws Exception {
        mvc.perform(authenticated(get(PATH), "invalid"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.startsWith("Bearer")));
    }

    @Test
    void preservesMethodAndMediaTypeErrors() throws Exception {
        mvc.perform(authenticated(put(PATH), "pending")).andExpect(status().isMethodNotAllowed());
        mvc.perform(authenticated(post(PATH), "pending").contentType(MediaType.TEXT_PLAIN).content("name"))
            .andExpect(status().isUnsupportedMediaType());
    }

    private void register(String bearer) throws Exception {
        mvc.perform(authenticated(post(PATH), bearer)
                .contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
            .andExpect(status().isCreated());
    }

    private static MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request, String bearer) {
        return request.header("Authorization", "Bearer " + bearer);
    }

    private static Jwt token(String subject, boolean verified) {
        return Jwt.withTokenValue("synthetic-test-token").header("alg", "RS256")
            .claims(claims -> claims.putAll(Map.of("iss", ISSUER, "sub", subject, "email_verified", verified)))
            .audience(List.of("ceremo-api")).build();
    }

    static Stream<String> invalidRequests() {
        return Stream.of("", "{", "null", "{}", "[]",
            VALID_REQUEST.replace("Ama Mensah", " "),
            VALID_REQUEST.replace("Ama Mensah", "x".repeat(121)),
            VALID_REQUEST.replace("terms-v1", " "),
            VALID_REQUEST.replace("terms-v1", "x".repeat(41)),
            VALID_REQUEST.replace("true", "false"),
            VALID_REQUEST.replace(",\"termsAccepted\":true", ""),
            VALID_REQUEST.replace("true", "null"));
    }
}
