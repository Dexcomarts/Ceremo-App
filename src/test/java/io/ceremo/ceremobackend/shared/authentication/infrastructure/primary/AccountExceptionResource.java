package io.ceremo.ceremobackend.shared.authentication.infrastructure.primary;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.ceremo.ceremobackend.shared.authentication.application.NotAuthenticatedUserException;
import io.ceremo.ceremobackend.shared.authentication.application.UnknownAuthenticationException;

@RestController
@org.springframework.boot.test.context.TestComponent
@RequestMapping("/api/account-exceptions")
class AccountExceptionResource {

  @GetMapping("/not-authenticated")
  public void notAuthenticatedUser() {
    throw new NotAuthenticatedUserException();
  }

  @GetMapping("/unknown-authentication")
  public void unknownAuthentication() {
    throw new UnknownAuthenticationException();
  }
}
