package io.ceremo.ceremobackend.wire.logstash.infrastructure.secondary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import net.logstash.logback.encoder.LogstashEncoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

class LogstashEncodingTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void buildsTheConfiguredAppenderAndEncodesItsMetadataWithoutOpeningASocket(boolean optionalSettings) {
        var environment = new MockEnvironment()
            .withProperty("application.logging.logstash.tcp.enabled", "true")
            .withProperty("application.logging.logstash.tcp.host", "127.0.0.1")
            .withProperty("application.logging.logstash.tcp.port", "5000");
        if (optionalSettings) {
            environment.withProperty("application.logging.logstash.tcp.ring-buffer-size", "1024")
                .withProperty("application.logging.logstash.tcp.shutdown-grace-period", "PT2S");
        }
        var properties = Binder.get(environment).bind("application.logging.logstash.tcp",
            Bindable.of(LogstashTcpProperties.class)).get();
        assertThat(properties.isEnabled()).isTrue();
        var lifecycle = new LogstashTcpLifeCycle(Map.of("app_name", "ceremo"), properties);
        var context = new LoggerContext();
        context.setMDCAdapter(new ch.qos.logback.classic.util.LogbackMDCAdapter());
        try {
            var appender = lifecycle.logstashTcpSocketAppender(context);
            assertThat(appender.getDestinations()).containsExactly(new InetSocketAddress("127.0.0.1", 5000));
            assertThat(appender.getName()).isEqualTo(LogstashTcpLifeCycle.ASYNC_LOGSTASH_APPENDER_NAME);
            assertThat(appender.isStarted()).isFalse();
            if (optionalSettings) {
                assertThat(appender.getRingBufferSize()).isEqualTo(1024);
                assertThat(appender.getShutdownGracePeriod().getMilliseconds()).isEqualTo(2000);
            }
            var encoder = (LogstashEncoder) appender.getEncoder();
            encoder.start();
            try {
                var event = new LoggingEvent("ceremo-test", context.getLogger("ceremo-test"),
                    Level.INFO, "Synthetic event", null, null);
                var json = new ObjectMapper().readTree(encoder.encode(event));
                assertThat(json.get("app_name").asString()).isEqualTo("ceremo");
                assertThat(json.get("message").asString()).isEqualTo("Synthetic event");
            } finally {
                encoder.stop();
            }
        } finally {
            context.stop();
        }
    }

    @Test
    void handlesStopBeforeStartAndCopiesMetadataAtConstruction() {
        var fields = new HashMap<>(Map.of("app_name", "ceremo"));
        var lifecycle = new LogstashTcpLifeCycle(fields, new LogstashTcpProperties());
        fields.put("app_name", "changed");
        lifecycle.stop();
        assertThat(lifecycle.isRunning()).isFalse();
        assertThat(new ObjectMapper().readTree(lifecycle.serializedFields()).get("app_name").asString())
            .isEqualTo("ceremo");
        assertThat(new LogstashTcpLifeCycle(null, new LogstashTcpProperties()).serializedFields()).isEqualTo("{}");
    }

    @Test
    void retainsAnExplicitApplicationPortInTheLogMetadata() {
        var lifecycle = new LogstashTcpConfiguration("ceremo", "8080", new LogstashTcpProperties())
            .logstashTcpLifeCycle();
        assertThat(new ObjectMapper().readTree(lifecycle.serializedFields()).get("app_port").asString())
            .isEqualTo("8080");
    }

    @Test
    void reportsSerializationFailureInsteadOfStartingWithInvalidMetadata() {
        var mapper = mock(ObjectMapper.class);
        var failure = mock(JacksonException.class);
        when(mapper.writeValueAsString(any())).thenThrow(failure);
        var lifecycle = new LogstashTcpLifeCycle(Map.of("app_name", "ceremo"), new LogstashTcpProperties(), mapper);
        assertThatThrownBy(lifecycle::serializedFields)
            .isInstanceOf(IllegalStateException.class).hasMessage("Unable to serialize custom fields").hasCause(failure);
    }
}
