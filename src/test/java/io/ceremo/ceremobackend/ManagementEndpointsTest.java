package io.ceremo.ceremobackend;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.ceremo.ceremobackend.wire.TestOidcConfiguration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

class ManagementEndpointsTest {

  private static final String H2_URL = "spring.datasource.url=jdbc:h2:mem:ceremo-management;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

  @Nested
  @SpringBootTest(classes = {CeremoBackendApplication.class, TestOidcConfiguration.class}, properties = {
      H2_URL, "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa", "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
      "application.registration.terms-version=terms-v1",
      "application.logging.logstash.tcp.enabled=false", "application.multitenancy.enabled=false"})
  @AutoConfigureMockMvc
  class DefaultProfile {

    @Autowired MockMvc mvc;
    @MockitoBean(name = "jwtDecoder") JwtDecoder decoder;

    @Test
    void showsOnlyOverallHealthToVisitors() throws Exception {
      mvc.perform(get("/management/health"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("UP"))
          .andExpect(jsonPath("$.components").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"liveness", "readiness"})
    void keepsProbesPublicForThePlatform(String probe) throws Exception {
      mvc.perform(get("/management/health/" + probe))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void hidesHealthDetailsFromSignedInCustomers() throws Exception {
      mvc.perform(get("/management/health").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void showsHealthDetailsToAdministrators() throws Exception {
      mvc.perform(get("/management/health").with(admin()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.components.db.status").value("UP"));
    }

    @Test
    void listsOnlyHealthEndpointsToAdministrators() throws Exception {
      mvc.perform(get("/management").with(admin()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$._links.health").exists())
          .andExpect(jsonPath("$._links.env").doesNotExist())
          .andExpect(jsonPath("$._links.configprops").doesNotExist())
          .andExpect(jsonPath("$._links.heapdump").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"env", "configprops", "logfile", "loggers", "threaddump", "heapdump", "info"})
    void doesNotServeSensitiveEndpointsEvenToAdministrators(String endpoint) throws Exception {
      mvc.perform(get("/management/" + endpoint).with(admin()))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  @SpringBootTest(classes = {CeremoBackendApplication.class, TestOidcConfiguration.class}, properties = {
      "spring.datasource.url=jdbc:h2:mem:ceremo-management-dev;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa", "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
      "application.registration.terms-version=terms-v1",
      "application.logging.logstash.tcp.enabled=false", "application.multitenancy.enabled=false"})
  @ActiveProfiles("dev")
  @AutoConfigureMockMvc
  class DevProfile {

    @Autowired MockMvc mvc;
    @MockitoBean(name = "jwtDecoder") JwtDecoder decoder;

    @ParameterizedTest
    @ValueSource(strings = {"env", "configprops", "loggers", "threaddump", "heapdump", "shutdown"})
    void inheritsHardenedEndpointsInDevelopment(String endpoint) throws Exception {
      mvc.perform(get("/management/" + endpoint).with(admin()))
          .andExpect(status().isNotFound());
    }

    @Test
    void hidesHealthDetailsFromVisitorsInDevelopment() throws Exception {
      mvc.perform(get("/management/health"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.components").doesNotExist());
    }
  }

  private static org.springframework.test.web.servlet.request.RequestPostProcessor admin() {
    return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
  }
}
