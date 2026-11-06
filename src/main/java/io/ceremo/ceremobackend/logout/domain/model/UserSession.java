package io.ceremo.ceremobackend.logout.domain.model;

import io.ceremo.ceremobackend.shared.error.domain.Assert;
import io.ceremo.ceremobackend.account.domain.model.UserIdentity;

public record UserSession(UserIdentity identity, String id) {
  public UserSession {
    Assert.notNull("identity", identity);
    Assert.field("sessionId", id).notBlank().maxLength(255);
  }
}
