package io.ceremo.ceremobackend.wire.security.infrastructure.primary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.CorsFilter;

class CorsConfigurationTest {
  private final ApplicationContextRunner runner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
      .withUserConfiguration(CorsProperties.class, CorsFilterConfiguration.class);

  @Test
  void blocksCrossOriginRequestsByDefault() {
    runner.run(context -> {
      assertThat(context).hasNotFailed();
      var mvc = MockMvcBuilders.standaloneSetup(new TestResource())
          .addFilters(context.getBean(CorsFilter.class)).build();
      mvc.perform(options("/api/v1/providers").header("Origin", "https://app.example")
          .header("Access-Control-Request-Method", "GET")).andExpect(status().isForbidden());
      assertThat(context.getBean("corsFilterRegistration",
          org.springframework.boot.web.servlet.FilterRegistrationBean.class).isEnabled()).isFalse();
    });
  }

  @Test
  void usesWireOriginsBeforeLegacyOrigins() {
    runner.withPropertyValues("application.cors.allowed-origins[0]=https://new.example",
        "application.security.allowed-origins[0]=https://old.example").run(context -> {
      var mvc = MockMvcBuilders.standaloneSetup(new TestResource())
          .addFilters(context.getBean(CorsFilter.class)).build();
      mvc.perform(options("/api/v1/providers").header("Origin", "https://new.example")
          .header("Access-Control-Request-Method", "GET"))
          .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "https://new.example"));
      mvc.perform(options("/api/v1/providers").header("Origin", "https://old.example")
          .header("Access-Control-Request-Method", "GET")).andExpect(status().isForbidden());
    });
  }

  @RestController
  @org.springframework.boot.test.context.TestComponent
  static class TestResource {
    @GetMapping("/api/v1/providers") String providers() { return "ok"; }
  }
}
