package io.ceremo.ceremobackend.shared.error.infrastructure.primary;

import java.util.Locale;
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
import io.ceremo.ceremobackend.shared.error.domain.ErrorStatus;
import io.ceremo.ceremobackend.shared.error.domain.ErrorKey;
import io.ceremo.ceremobackend.shared.error.domain.CeremoException;

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 1000)
class CeremoErrorsHandler {

  private static final Logger log = LoggerFactory.getLogger(CeremoErrorsHandler.class);
  private static final String MESSAGES_PREFIX = "error.";

  private final MessageSource messages;

  public CeremoErrorsHandler(@Qualifier("applicationErrorMessageSource") MessageSource messages) {
    this.messages = messages;
  }

  @ExceptionHandler(CeremoException.class)
  ResponseEntity<ApiProblemResponse> handleCeremoException(CeremoException exception, HttpServletRequest request) {
    HttpStatus status = mapStatus(exception.status());
    var problem = ApiProblemResponse.withKey(
      getMessage(exception.key(), "title"),
      buildDetail(exception),
      exception.key().get(),
      request.getRequestURI()
    );

    logException(exception, status);

    return ResponseEntity
      .status(status)
      .contentType(MediaType.APPLICATION_PROBLEM_JSON)
      .body(problem);
  }

  private HttpStatus mapStatus(ErrorStatus status) {
    if (status == null) {
      return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    return switch (status) {
      case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
      case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
      case PAYMENT_REQUIRED -> HttpStatus.PAYMENT_REQUIRED;
      case FORBIDDEN -> HttpStatus.FORBIDDEN;
      case NOT_FOUND -> HttpStatus.NOT_FOUND;
      case METHOD_NOT_ALLOWED -> HttpStatus.METHOD_NOT_ALLOWED;
      case NOT_ACCEPTABLE -> HttpStatus.NOT_ACCEPTABLE;
      case UNPROCESSABLE_ENTITY -> HttpStatus.valueOf(422);
      case CONFLICT -> HttpStatus.CONFLICT;
      case GONE -> HttpStatus.GONE;
      case I_AM_A_TEAPOT -> HttpStatus.valueOf(418);
      case INTERNAL_SERVER_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
      case NOT_IMPLEMENTED -> HttpStatus.NOT_IMPLEMENTED;
    };
  }

  private String buildDetail(CeremoException exception) {
    String messageTemplate = getMessage(exception.key(), "detail");

    return ArgumentsReplacer.replaceParameters(messageTemplate, exception.parameters());
  }

  private String getMessage(ErrorKey key, String suffix) {
    return messages.getMessage(MESSAGES_PREFIX + key.get() + "." + suffix, null, locale());
  }

  private Locale locale() {
    return LocaleContextHolder.getLocale();
  }

  private void logException(CeremoException exception, HttpStatus status) {
    if (status.is4xxClientError()) {
      log.info("CeremoException key={} status={}", exception.key().get(), status.value());
    } else {
      log.error("CeremoException key={} status={}", exception.key().get(), status.value());
    }
  }
}
