package io.ceremo.ceremobackend.shared.useridentity.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import io.ceremo.ceremobackend.shared.error.domain.MissingMandatoryValueException;
import io.ceremo.ceremobackend.shared.error.domain.StringTooLongException;

class EmailTest {

  @Test
  void shouldNotBuildWithoutEmail() {
    assertThatThrownBy(() -> new Email(null))
        .isExactlyInstanceOf(MissingMandatoryValueException.class)
        .hasMessageContaining("email");
  }

  @Test
  void shouldNotBuildWithBlankEmail() {
    assertThatThrownBy(() -> new Email(" "))
        .isExactlyInstanceOf(MissingMandatoryValueException.class)
        .hasMessageContaining("email");
  }

  @Test
  void shouldNotBuildWithTooLongEmail() {
    assertThatThrownBy(() -> new Email("a".repeat(256)))
        .isExactlyInstanceOf(StringTooLongException.class)
        .hasMessageContaining("email");
  }

  @Test
  void shouldBeEmptyWithoutEmail() {
    assertThat(Email.of(null)).isEmpty();
  }

  @Test
  void shouldBeEmptyWithBlankEmail() {
    assertThat(Email.of(" ")).isEmpty();
  }

  @Test
  void shouldGetEmail() {
    Email email = new Email("mail@company.fr");

    assertThat(email.get()).isEqualTo("mail@company.fr");
  }

  @Test
  void shouldNormalizeEmail() {
    Email email = new Email("  MAIL@COMPANY.FR  ");

    assertThat(email.get()).isEqualTo("mail@company.fr");
  }

  @Test
  void shouldBuildOptionalEmail() {
    assertThat(Email.of("mail@company.fr")).contains(new Email("mail@company.fr"));
  }
}
