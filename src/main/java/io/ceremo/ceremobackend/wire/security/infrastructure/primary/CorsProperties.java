package io.ceremo.ceremobackend.wire.security.infrastructure.primary;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
class CorsProperties {

  @Bean
  @ConfigurationProperties(prefix = "application.cors", ignoreUnknownFields = false)
  public CorsConfiguration corsConfiguration(Environment environment) {
    CorsConfiguration cors = new CorsConfiguration();
    cors.setAllowedOrigins(Binder.get(environment)
        .bind("application.security.allowed-origins", Bindable.listOf(String.class)).orElse(List.of()));
    cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Accept-Language",
        "Idempotency-Key", "X-Request-Id"));
    cors.setAllowCredentials(false);
    return cors;
  }
}
