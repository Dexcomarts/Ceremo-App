package io.ceremo.ceremobackend.account.infrastructure.primary.security;

import io.ceremo.ceremobackend.account.application.port.in.Accounts;
import io.ceremo.ceremobackend.account.domain.exception.UserErrorKey;
import io.ceremo.ceremobackend.shared.error.domain.CeremoException;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.security.authorization.AuthenticatedAuthorizationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/** Requires authentication and an active local account before profile access. */
public final class ActiveAccountAuthorizationManager
    implements AuthorizationManager<RequestAuthorizationContext> {
  private final Accounts accounts;
  private final AuthenticatedAuthorizationManager<RequestAuthorizationContext> authenticated =
      AuthenticatedAuthorizationManager.authenticated();

  public ActiveAccountAuthorizationManager(Accounts accounts) {
    this.accounts = Objects.requireNonNull(accounts, "accounts");
  }

  @Override
  public AuthorizationResult authorize(
      Supplier<? extends Authentication> authentication, RequestAuthorizationContext context) {
    AuthorizationResult result = authenticated.authorize(authentication, context);
    if (!result.isGranted()) {
      return result;
    }
    try {
      accounts.requireActiveAccount();
      return new AuthorizationDecision(true);
    } catch (CeremoException error) {
      if (error.key() == UserErrorKey.USER_NOT_FOUND || error.key() == UserErrorKey.ACCOUNT_RESTRICTED) {
        return new AuthorizationDecision(false);
      }
      throw error;
    }
  }
}
