package io.ceremo.ceremobackend.shared.useridentity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import io.ceremo.ceremobackend.shared.error.domain.MissingMandatoryValueException;
import io.ceremo.ceremobackend.shared.error.domain.StringTooLongException;

class PhoneNumberTest {

  @Test
  void shouldNotBuildWithoutPhoneNumber() {
    assertThatThrownBy(() -> new PhoneNumber(null))
        .isExactlyInstanceOf(MissingMandatoryValueException.class)
        .hasMessageContaining("phoneNumber");
  }

  @Test
  void shouldNotBuildWithBlankPhoneNumber() {
    assertThatThrownBy(() -> new PhoneNumber(" "))
        .isExactlyInstanceOf(MissingMandatoryValueException.class)
        .hasMessageContaining("phoneNumber");
  }

  @Test
  void shouldNotBuildWithTooLongPhoneNumber() {
    assertThatThrownBy(() -> new PhoneNumber("1".repeat(51)))
        .isExactlyInstanceOf(StringTooLongException.class)
        .hasMessageContaining("phoneNumber");
  }

  @Test
  void shouldNormalizePhoneNumberToDigits() {
    PhoneNumber phoneNumber = new PhoneNumber("+233 20-123-4567");

    assertThat(phoneNumber.get()).isEqualTo("233201234567");
  }

  @Test
  void shouldBeEmptyWithoutPhoneNumber() {
    assertThat(PhoneNumber.of(null)).isEmpty();
  }

  @Test
  void shouldBeEmptyWhenNoDigitsExist() {
    assertThat(PhoneNumber.of(" ext ")).isEmpty();
  }

  @Test
  void shouldBuildOptionalPhoneNumber() {
    assertThat(PhoneNumber.of("+233 20-123-4567")).contains(new PhoneNumber("233201234567"));
  }
}
