package io.ceremo.ceremobackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

import io.ceremo.ceremobackend.shared.error.domain.MissingMandatoryValueException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ApplicationStartupTracesTest {
    @Test
    void describesANonWebApplicationWithoutInventingUrlsOrProfiles() {
        String trace = ApplicationStartupTraces.of(new MockEnvironment());
        assertThat(trace).contains("Application is running!")
            .doesNotContain("Local:", "External:", "Profile(s):", "Config Server:");
        assertThatThrownBy(() -> ApplicationStartupTraces.of(null))
            .isInstanceOf(MissingMandatoryValueException.class);
    }

    @Test
    void printsHttpUrlsWithARootContext() throws Exception {
        InetAddress address = InetAddress.getByAddress(new byte[]{127, 0, 0, 1});
        try (var hosts = mockStatic(InetAddress.class)) {
            hosts.when(InetAddress::getLocalHost).thenReturn(address);
            String trace = ApplicationStartupTraces.of(new MockEnvironment()
                .withProperty("spring.application.name", "ceremo").withProperty("server.port", "8080"));
            assertThat(trace).contains("Application 'ceremo' is running!",
                "Local: \thttp://localhost:8080/", "External: \thttp://127.0.0.1:8080/");
        }
    }

    @Test
    void printsHttpsContextProfilesAndConfigServerWhenConfigured() {
        MockEnvironment environment = new MockEnvironment().withProperty("server.port", "8443")
            .withProperty("server.ssl.key-store", "test-keystore")
            .withProperty("server.servlet.context-path", "/ceremo")
            .withProperty("configserver.status", "connected");
        environment.setActiveProfiles("test", "local");
        assertThat(ApplicationStartupTraces.of(environment)).contains(
            "Local: \thttps://localhost:8443/ceremo", "Profile(s): \ttest, local", "Config Server: connected");
    }

    @Test
    void usesLocalhostWhenTheHostCannotBeResolved() {
        try (var hosts = mockStatic(InetAddress.class)) {
            hosts.when(InetAddress::getLocalHost).thenThrow(new UnknownHostException("Synthetic lookup failure"));
            assertThat(ApplicationStartupTraces.of(new MockEnvironment().withProperty("server.port", "8080")))
                .contains("External: \thttp://localhost:8080/");
        }
    }
}
