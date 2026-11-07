package io.ceremo.ceremobackend.wire.springdoc.infrastructure.primary;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
class SpringdocConfiguration {

  @Value("${application.version:undefined}")
  private String version;

  @Value("${application.documentation-url:}")
  private String documentationUrl;

  @Bean
  public OpenAPI ceremoOpenAPI() {
    return new OpenAPI().info(swaggerInfo()).externalDocs(swaggerExternalDoc());
  }

  private Info swaggerInfo() {
    return new Info()
        .title("CEREMO API")
        .description(
"""
CEREMO helps people find ceremony and event service providers in Ghana.
Customers browse free. Providers can subscribe for continued discovery.
""")
        .version(version);
  }

  private ExternalDocumentation swaggerExternalDoc() {
    if (!StringUtils.hasText(documentationUrl)) {
      return null;
    }
    return new ExternalDocumentation().description("Project Documentation").url(documentationUrl);
  }

  @Bean
  public GroupedOpenApi ceremoAllOpenAPI() {
    return GroupedOpenApi.builder().group("all").pathsToMatch("/api/**").build();
  }
}
