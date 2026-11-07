package io.ceremo.ceremobackend.shared.useridentity.domain;

import java.util.Optional;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import io.ceremo.ceremobackend.shared.error.domain.Assert;

public record Email(String email) {
  public Email(String email) {
    Assert.field("email", email).notBlank().maxLength(255);

    this.email = email.trim().toLowerCase(Locale.ROOT);
  }

  public static Optional<Email> of(String email) {
    return Optional.ofNullable(email).filter(StringUtils::isNotBlank).map(Email::new);
  }

  public String get() {
    return email();
  }
}
