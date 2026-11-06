package io.ceremo.ceremobackend.wire.logstash.infrastructure.secondary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.LoggerContext;
import java.util.Map;
import net.logstash.logback.appender.LogstashTcpSocketAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

class LogstashTcpLifeCycleTest {
  @Test
  void startsOnceAndDetachesOnlyItsOwnAppenderOnStop() {
    var context = (LoggerContext) LoggerFactory.getILoggerFactory();
    var root = context.getLogger(Logger.ROOT_LOGGER_NAME);
    var appender = mock(LogstashTcpSocketAppender.class);
    when(appender.getName()).thenReturn(LogstashTcpLifeCycle.ASYNC_LOGSTASH_APPENDER_NAME);
    when(appender.isStarted()).thenReturn(true);
    var lifecycle = new LogstashTcpLifeCycle(Map.of("app_name", "ceremo"), new LogstashTcpProperties()) {
      @Override
      protected LogstashTcpSocketAppender logstashTcpSocketAppender(LoggerContext ignored) { return appender; }
    };
    try {
      lifecycle.start();
      lifecycle.start();
      assertThat(lifecycle.isRunning()).isTrue();
      assertThat(root.getAppender(LogstashTcpLifeCycle.ASYNC_LOGSTASH_APPENDER_NAME)).isSameAs(appender);
      verify(appender, times(1)).start();
    } finally {
      lifecycle.stop();
    }
    assertThat(lifecycle.isRunning()).isFalse();
    assertThat(root.getAppender(LogstashTcpLifeCycle.ASYNC_LOGSTASH_APPENDER_NAME)).isNull();
    verify(appender, times(1)).stop();
  }

  @Test
  void writesApplicationMetadataWithoutInventingAPort() {
    var configuration = new LogstashTcpConfiguration("ceremo", null, new LogstashTcpProperties());
    var metadata = new ObjectMapper().readTree(configuration.logstashTcpLifeCycle().serializedFields());
    assertThat(metadata.get("app_name").asString()).isEqualTo("ceremo");
    assertThat(metadata.has("app_port")).isFalse();
  }
}
