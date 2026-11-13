package io.ceremo.ceremobackend.shared.error.infrastructure.primary;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 1000)
class BeanValidationErrorsHandler {

  private static final Logger log = LoggerFactory.getLogger(BeanValidationErrorsHandler.class);
  private static final String DETAIL = "One or more fields were invalid. See 'errors' for details.";
  private static final String TITLE = "Bean validation error";
  private static final String DEFAULT_MESSAGE = "Invalid value";

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiProblemResponse> handleMethodArgumentNotValid(
    MethodArgumentNotValidException exception,
    HttpServletRequest request
  ) {
    var errors = buildErrors(exception);
    var problem = ApiProblemResponse.withErrors(TITLE, DETAIL, request.getRequestURI(), errors);

    // Log server-controlled values only; the request URI is attacker-supplied.
    log.info("Bean validation failed for {} field(s)", errors.size());

    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .contentType(MediaType.APPLICATION_PROBLEM_JSON)
      .body(problem);
  }

  private Map<String, String> buildErrors(MethodArgumentNotValidException exception) {
    return exception
      .getBindingResult()
      .getAllErrors()
      .stream()
      .collect(Collectors.toUnmodifiableMap(
        this::fieldName,
        error -> errorMessage(error.getDefaultMessage()),
        (first, second) -> first
      ));
  }

  private String fieldName(ObjectError error) {
    return error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName();
  }

  private String errorMessage(String message) {
    return message == null || message.isBlank() ? DEFAULT_MESSAGE : message;
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<ApiProblemResponse> handleConstraintViolationException(
    ConstraintViolationException exception,
    HttpServletRequest request
  ) {
    var errors = buildErrors(exception);
    var problem = ApiProblemResponse.withErrors(TITLE, DETAIL, request.getRequestURI(), errors);

    // Log server-controlled values only; the request URI is attacker-supplied.
    log.info("Bean validation failed for {} field(s)", errors.size());

    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .contentType(MediaType.APPLICATION_PROBLEM_JSON)
      .body(problem);
  }

  private Map<String, String> buildErrors(ConstraintViolationException exception) {
    return exception
      .getConstraintViolations()
      .stream()
      .collect(Collectors.toUnmodifiableMap(
        toFieldName(),
        error -> errorMessage(error.getMessage()),
        (first, second) -> first
      ));
  }

  private Function<ConstraintViolation<?>, String> toFieldName() {
    return error -> {
      var propertyPath = error.getPropertyPath().toString();

      return propertyPath.substring(propertyPath.lastIndexOf(".") + 1);
    };
  }
}
