package io.ceremo.ceremobackend.shared.useridentity.domain;

import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

import io.ceremo.ceremobackend.shared.error.domain.Assert;

public record PhoneNumber(String phoneNumber) {

  public PhoneNumber(String phoneNumber) {
    String normalized = normalize(phoneNumber);
    Assert.field("phoneNumber", normalized).notBlank().maxLength(50);

    this.phoneNumber = normalized;
  }

  public static Optional<PhoneNumber> of(String phoneNumber) {
    return Optional.ofNullable(phoneNumber)
        .filter(StringUtils::isNotBlank)
        .map(PhoneNumber::normalize)
        .filter(StringUtils::isNotBlank)
        .map(PhoneNumber::new);
  }

  public String get() {
    return phoneNumber();
  }

  private static String normalize(String phoneNumber) {
    if (phoneNumber == null) {
      return null;
    }

    StringBuilder digits = new StringBuilder();
    for (int index = 0; index < phoneNumber.length(); index++) {
      char character = phoneNumber.charAt(index);
      if (Character.isDigit(character)) {
        digits.append(character);
      }
    }
    return digits.toString();
  }
}
