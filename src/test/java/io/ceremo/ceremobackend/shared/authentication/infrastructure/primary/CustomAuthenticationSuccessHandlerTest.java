package io.ceremo.ceremobackend.shared.authentication.infrastructure.primary;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;


class CustomAuthenticationSuccessHandlerTest {

  @Test
  void shouldRedirectToSwaggerUiAfterAuthenticationSuccess() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    Authentication authentication = mock(Authentication.class);

    new CustomAuthenticationSuccessHandler()
        .onAuthenticationSuccess(request, response, authentication);

    verify(response).sendRedirect("/swagger-ui/index.html");
  }
}
