package io.ceremo.ceremobackend.shared.authentication.infrastructure.primary;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import io.ceremo.ceremobackend.shared.error.infrastructure.primary.ApiProblemResponse;
import io.ceremo.ceremobackend.shared.authentication.application.NotAuthenticatedUserException;
import io.ceremo.ceremobackend.shared.authentication.application.UnknownAuthenticationException;

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 20_000)
class AuthenticationExceptionAdvice {

  @ExceptionHandler(NotAuthenticatedUserException.class)
  public ResponseEntity<ApiProblemResponse> handleNotAuthenticateUser(
    NotAuthenticatedUserException e,
    HttpServletRequest request
  ) {
    return ResponseEntity
      .status(HttpStatus.UNAUTHORIZED)
      .contentType(MediaType.APPLICATION_PROBLEM_JSON)
      .body(ApiProblemResponse.withMessage("not authenticated", request.getRequestURI(), "error.http.401"));
  }

  @ExceptionHandler(UnknownAuthenticationException.class)
  public ResponseEntity<ApiProblemResponse> handleUnknownAuthentication(
    UnknownAuthenticationException e,
    HttpServletRequest request
  ) {
    return ResponseEntity
      .status(HttpStatus.INTERNAL_SERVER_ERROR)
      .contentType(MediaType.APPLICATION_PROBLEM_JSON)
      .body(ApiProblemResponse.withMessage("unknown authentication", request.getRequestURI(), "error.http.500"));
  }
}
