package io.ceremo.ceremobackend.account.infrastructure.secondary.security;

import io.ceremo.ceremobackend.shared.authentication.application.AuthenticatedUser;
import io.ceremo.ceremobackend.shared.authentication.application.NotAuthenticatedUserException;
import io.ceremo.ceremobackend.shared.error.domain.Assert;
import io.ceremo.ceremobackend.shared.error.domain.AssertionException;
import io.ceremo.ceremobackend.account.application.port.out.CurrentUser;
import io.ceremo.ceremobackend.account.domain.model.UserIdentity;
import java.util.Map;
import java.net.URL;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Maps already validated authentication to the feature's identity port. */
@Component
public final class SpringCurrentUser implements CurrentUser {

  private final String expectedIssuer;

  public SpringCurrentUser(
      @Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}") String expectedIssuer) {
    Assert.field("issuer", expectedIssuer).notBlank().maxLength(512);
    this.expectedIssuer = expectedIssuer;
  }

  @Override
  public UserIdentity requireIdentity() {
    Map<String, Object> claims = AuthenticatedUser.attributes();
    String issuer;
    if (claims.get("iss") instanceof String text) {
      issuer = text;
    } else if (claims.get("iss") instanceof URL url) {
      // Spring's OIDC ID-token decoder converts iss to URL; JWT API claims remain strings.
      issuer = url.toExternalForm();
    } else {
      throw new NotAuthenticatedUserException();
    }
    if (!(claims.get("sub") instanceof String subject)
        || !expectedIssuer.equals(issuer)) {
      throw new NotAuthenticatedUserException();
    }

    try {
      return new UserIdentity(issuer, subject);
    } catch (AssertionException exception) {
      // Invalid authentication claims must not become public domain-validation errors.
      throw new NotAuthenticatedUserException();
    }
  }

  @Override
  public boolean isEmailVerified() {
    requireIdentity();
    return Boolean.TRUE.equals(AuthenticatedUser.attributes().get("email_verified"));
  }
}
