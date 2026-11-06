package io.ceremo.ceremobackend.shared.error.infrastructure.primary;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import io.ceremo.ceremobackend.shared.error.domain.AssertionErrorType;
import io.ceremo.ceremobackend.shared.error.domain.AssertionException;

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 1000)
class AssertionErrorsHandler {

  private static final Logger log = LoggerFactory.getLogger(AssertionErrorsHandler.class);
  private static final String MESSAGES_PREFIX = "assertion-error.";

  private final MessageSource messages;

  public AssertionErrorsHandler(@Qualifier("assertionErrorMessageSource") MessageSource messages) {
    this.messages = messages;
  }

  @ExceptionHandler(AssertionException.class)
  ResponseEntity<ApiProblemResponse> handleAssertionError(AssertionException exception, HttpServletRequest request) {
    HttpStatus status = buildStatus(exception);
    var problem = ApiProblemResponse.withKey(
      getMessage(exception.type(), "title"),
      buildDetail(exception),
      exception.type().name(),
      request.getRequestURI()
    );

    logException(exception, status);

    return ResponseEntity
      .status(status)
      .contentType(MediaType.APPLICATION_PROBLEM_JSON)
      .body(problem);
  }

  private HttpStatus buildStatus(AssertionException exception) {
    return Stream.of(exception.getStackTrace())
      .map(StackTraceElement::getClassName)
      .filter(inApplication())
      .filter(notInErrorDomain())
      .findFirst()
      .filter(primaryClass())
      .map(className -> HttpStatus.BAD_REQUEST)
      .orElse(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private Predicate<String> inApplication() {
    return className -> className.startsWith("io.ceremo.ceremobackend.");
  }

  private Predicate<String> notInErrorDomain() {
    return className -> !className.startsWith("io.ceremo.ceremobackend.shared.error.domain");
  }

  private Predicate<String> primaryClass() {
    return className -> className.contains(".primary.");
  }

  private String buildDetail(AssertionException exception) {
    String messageTemplate = getMessage(exception.type(), "detail");

    return ArgumentsReplacer.replaceParameters(messageTemplate, parameters(exception));
  }

  private Map<String, String> parameters(AssertionException exception) {
    HashMap<String, String> parameters = new HashMap<>(exception.parameters());
    parameters.put("field", exception.field());

    return parameters;
  }

  private String getMessage(AssertionErrorType type, String suffix) {
    return messages.getMessage(MESSAGES_PREFIX + type.name() + "." + suffix, null, locale());
  }

  private Locale locale() {
    return LocaleContextHolder.getLocale();
  }

  private void logException(AssertionException exception, HttpStatus status) {
    if (status.is4xxClientError()) {
      log.info("Assertion error type={} field={}", exception.type(), exception.field());
    } else {
      log.error("Assertion error type={} field={}", exception.type(), exception.field());
    }
  }
}
