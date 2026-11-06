package io.ceremo.ceremobackend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import io.ceremo.ceremobackend.shared.generation.domain.ExcludeFromGeneratedCodeCoverage;

@SpringBootApplication
@ExcludeFromGeneratedCodeCoverage(reason = "Not testing logs")
public class CeremoBackendApplication {

  private static final Logger log = LoggerFactory.getLogger(CeremoBackendApplication.class);

  public static void main(String[] args) {
    Environment env = SpringApplication.run(CeremoBackendApplication.class, args).getEnvironment();

    if (log.isInfoEnabled()) {
      log.info(ApplicationStartupTraces.of(env));
    }
  }
}
