package io.ceremo.ceremobackend.shared.error.infrastructure.primary;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

@Configuration
class CeremoErrorsConfiguration {

  @Bean("applicationErrorMessageSource")
  MessageSource applicationErrorMessageSource() {
    var source = new ReloadableResourceBundleMessageSource();

    source.setBasename("classpath:/messages/errors/ceremo-errors-messages");
    source.setDefaultEncoding("UTF-8");
    source.setFallbackToSystemLocale(false);
    source.setDefaultLocale(Locale.ENGLISH);

    return source;
  }
}
