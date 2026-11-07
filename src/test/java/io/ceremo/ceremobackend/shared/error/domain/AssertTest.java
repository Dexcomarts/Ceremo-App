package io.ceremo.ceremobackend.shared.error.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AssertTest {

  @ParameterizedTest
  @MethodSource("invalidValues")
  void shouldRetainFieldAndErrorType(Executable assertion, AssertionErrorType expectedType) {
    Throwable thrown = catchThrowable(assertion::execute);

    assertThat(thrown).isInstanceOf(AssertionException.class);
    var exception = (AssertionException) thrown;
    assertThat(exception.field()).isEqualTo("value");
    assertThat(exception.type()).isEqualTo(expectedType);
  }

  static Stream<Arguments> invalidValues() {
    Instant reference = Instant.parse("2026-01-01T00:00:00Z");
    return Stream.of(
      invalid(() -> Assert.notBlank("value", " "), AssertionErrorType.MISSING_MANDATORY_VALUE),
      invalid(() -> Assert.field("value", "invalid").email(), AssertionErrorType.INVALID_EMAIL),
      invalid(() -> Assert.field("value", "a").minLength(2), AssertionErrorType.STRING_TOO_SHORT),
      invalid(() -> Assert.field("value", "abc").maxLength(2), AssertionErrorType.STRING_TOO_LONG),
      invalid(() -> Assert.field("value", -1).positive(), AssertionErrorType.NUMBER_VALUE_TOO_LOW),
      invalid(() -> Assert.field("value", 2L).max(1), AssertionErrorType.NUMBER_VALUE_TOO_HIGH),
      invalid(() -> Assert.field("value", 2.5F).max(2), AssertionErrorType.NUMBER_VALUE_TOO_HIGH),
      invalid(() -> Assert.field("value", -0.5D).min(0), AssertionErrorType.NUMBER_VALUE_TOO_LOW),
      invalid(() -> Assert.field("value", new BigDecimal("1.01")).max(BigDecimal.ONE), AssertionErrorType.NUMBER_VALUE_TOO_HIGH),
      invalid(() -> Assert.field("value", Arrays.asList("a", null)).noNullElement(), AssertionErrorType.NULL_ELEMENT_IN_COLLECTION),
      invalid(() -> Assert.field("value", new String[] {"a", "b"}).maxSize(1), AssertionErrorType.TOO_MANY_ELEMENTS),
      invalid(() -> Assert.field("value", reference).after(reference), AssertionErrorType.NOT_AFTER_TIME),
      invalid(() -> Assert.field("value", reference).before(reference), AssertionErrorType.NOT_BEFORE_TIME)
    );
  }

  private static Arguments invalid(Executable assertion, AssertionErrorType type) {
    return Arguments.of(assertion, type);
  }

  @Test
  void shouldAcceptInclusiveBoundariesAndOptionalValues() {
    Instant reference = Instant.parse("2026-01-01T00:00:00Z");

    assertThatCode(() -> {
      Assert.field("budget", BigDecimal.ONE).min(BigDecimal.ONE).max(BigDecimal.ONE);
      Assert.field("date", reference).afterOrAt(reference).beforeOrAt(reference);
      Assert.field("optional", (String) null).maxLength(10);
      Assert.field("items", List.of("a")).notEmpty().maxSize(1).noNullElement();
      Assert.notEmpty("properties", Map.of("key", "value"));
    }).doesNotThrowAnyException();
  }
}
