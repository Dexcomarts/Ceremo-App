package io.ceremo.ceremobackend.shared.authentication.infrastructure.primary;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import io.ceremo.ceremobackend.shared.authentication.application.NotAuthenticatedUserException;
import io.ceremo.ceremobackend.shared.authentication.application.UnknownAuthenticationException;
import io.ceremo.ceremobackend.shared.error.infrastructure.primary.ApiProblemResponse;

import static org.mockito.Mockito.*;

class AuthenticationExceptionAdviceTest {

  private final AuthenticationExceptionAdvice advice = new AuthenticationExceptionAdvice();

  @Test
  void shouldMapNotAuthenticatedException() {
    ResponseEntity<ApiProblemResponse> detail = advice.handleNotAuthenticateUser(new NotAuthenticatedUserException(), request());

    assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(detail.getBody()).isEqualTo(ApiProblemResponse.withMessage("not authenticated", "/api/test", "error.http.401"));
  }

  @Test
  void shouldMapUnknownAuthenticationException() {
    ResponseEntity<ApiProblemResponse> detail = advice.handleUnknownAuthentication(new UnknownAuthenticationException(), request());

    assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(detail.getBody()).isEqualTo(ApiProblemResponse.withMessage("unknown authentication", "/api/test", "error.http.500"));
  }

  private HttpServletRequest request() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/api/test");
    return request;
  }
}
