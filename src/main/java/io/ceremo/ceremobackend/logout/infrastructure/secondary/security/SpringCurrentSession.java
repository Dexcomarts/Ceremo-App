package io.ceremo.ceremobackend.logout.infrastructure.secondary.security;

import io.ceremo.ceremobackend.logout.application.port.out.CurrentSession;
import io.ceremo.ceremobackend.logout.domain.model.UserSession;
import io.ceremo.ceremobackend.shared.authentication.application.AuthenticatedUser;
import io.ceremo.ceremobackend.shared.authentication.application.NotAuthenticatedUserException;
import io.ceremo.ceremobackend.shared.error.domain.AssertionException;
import io.ceremo.ceremobackend.account.application.port.out.CurrentUser;
import io.ceremo.ceremobackend.account.domain.model.UserIdentity;
import java.util.Objects;

public final class SpringCurrentSession implements CurrentSession {
  private final CurrentUser currentUser;

  public SpringCurrentSession(CurrentUser currentUser) {
    this.currentUser = Objects.requireNonNull(currentUser, "currentUser");
  }

  @Override
  public UserSession requireSession() {
    UserIdentity identity = currentUser.requireIdentity();
    if (!(AuthenticatedUser.attributes().get("sid") instanceof String sessionId)) {
      throw new NotAuthenticatedUserException();
    }
    try {
      return new UserSession(identity, sessionId);
    } catch (AssertionException exception) {
      throw new NotAuthenticatedUserException();
    }
  }
}
