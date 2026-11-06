package io.ceremo.ceremobackend.wire.security.infrastructure.primary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
class CorsFilterConfiguration {

  private static final Logger log = LoggerFactory.getLogger(CorsFilterConfiguration.class);

  private final CorsConfiguration corsConfiguration;

  public CorsFilterConfiguration(CorsConfiguration corsConfiguration) {
    this.corsConfiguration = corsConfiguration;
  }

  @Bean
  public CorsFilter corsFilter() {
    var source = new UrlBasedCorsConfigurationSource();
    log.debug("Registering CORS filter");
    source.registerCorsConfiguration("/api/**", corsConfiguration);
    source.registerCorsConfiguration("/management/**", corsConfiguration);
    source.registerCorsConfiguration("/v2/api-docs", corsConfiguration);
    source.registerCorsConfiguration("/v3/api-docs", corsConfiguration);
    source.registerCorsConfiguration("/v3/api-docs/**", corsConfiguration);
    source.registerCorsConfiguration("/swagger-resources", corsConfiguration);
    source.registerCorsConfiguration("/swagger-ui/**", corsConfiguration);
    return new CorsFilter(source);
  }

  @Bean
  public FilterRegistrationBean<CorsFilter> corsFilterRegistration(CorsFilter filter) {
    FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }
}
