package io.ceremo.ceremobackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import io.ceremo.ceremobackend.wire.TestOidcConfiguration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = {CeremoBackendApplication.class, TestOidcConfiguration.class}, properties = {
    "spring.datasource.url=jdbc:h2:mem:ceremo-smoke;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
    "application.registration.terms-version=terms-v1",
    "application.logging.logstash.tcp.enabled=false", "application.multitenancy.enabled=false"
})
class CeremoBackendApplicationTests {

    @MockitoBean(name = "jwtDecoder")
    private JwtDecoder jwtDecoder;

	@Test
	void contextLoads() {
	}

}
