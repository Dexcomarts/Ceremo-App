package io.ceremo.ceremobackend.shared.error.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AssertBoundariesTest {
    @Test
    void integerAndLongBoundsAreInclusiveAndReportTheRejectedValue() {
        assertDoesNotThrow(() -> Assert.field("value", 0).positive().min(0).max(0));
        assertDoesNotThrow(() -> Assert.field("value", 0L).positive().min(0).max(0));
        assertThat(failure(() -> Assert.field("value", -1L).min(0)).parameters())
            .containsExactlyInAnyOrderEntriesOf(Map.of("min", "0", "value", "-1"));
        assertThat(failure(() -> Assert.field("value", 1).max(0)).parameters())
            .containsExactlyInAnyOrderEntriesOf(Map.of("max", "0", "value", "1"));
    }

    @Test
    void floatBoundsDistinguishInclusiveAndStrictComparisons() {
        assertDoesNotThrow(() -> Assert.field("value", 0F).positive().min(0).max(0));
        assertDoesNotThrow(() -> Assert.field("value", 1F).strictlyPositive().over(0).under(2));
        assertThat(failure(() -> Assert.field("value", -1F).min(0)).parameters())
            .containsExactlyInAnyOrderEntriesOf(Map.of("min", "0.0", "value", "-1.0"));
        assertThat(failure(() -> Assert.field("value", 0F).over(0)).type())
            .isEqualTo(AssertionErrorType.NUMBER_VALUE_TOO_LOW);
        assertThat(failure(() -> Assert.field("value", 2F).under(2)).type())
            .isEqualTo(AssertionErrorType.NUMBER_VALUE_TOO_HIGH);
        assertThat(failure(() -> Assert.field("value", 3F).max(2)).parameters())
            .containsExactlyInAnyOrderEntriesOf(Map.of("max", "2.0", "value", "3.0"));
    }

    @Test
    void doubleBoundsDistinguishInclusiveAndStrictComparisons() {
        assertDoesNotThrow(() -> Assert.field("value", 0D).positive().min(0).max(0));
        assertDoesNotThrow(() -> Assert.field("value", 1D).strictlyPositive().over(0).under(2));
        assertThat(failure(() -> Assert.field("value", -1D).min(0)).type())
            .isEqualTo(AssertionErrorType.NUMBER_VALUE_TOO_LOW);
        assertThat(failure(() -> Assert.field("value", 0D).over(0)).type())
            .isEqualTo(AssertionErrorType.NUMBER_VALUE_TOO_LOW);
        assertThat(failure(() -> Assert.field("value", 2D).under(2)).type())
            .isEqualTo(AssertionErrorType.NUMBER_VALUE_TOO_HIGH);
        assertThat(failure(() -> Assert.field("value", 3D).max(2)).parameters())
            .containsExactlyInAnyOrderEntriesOf(Map.of("max", "2.0", "value", "3.0"));
    }

    @Test
    void decimalBoundsPreservePrecisionAndTreatScaleAsNumericEquality() {
        assertDoesNotThrow(() -> Assert.field("value", new BigDecimal("0.00")).positive().min(0).max(0));
        assertDoesNotThrow(() -> Assert.field("value", BigDecimal.ONE).strictlyPositive()
            .over(BigDecimal.ZERO).under(new BigDecimal("2.00")).under(2));
        assertThat(failure(() -> Assert.field("value", new BigDecimal("0.01"))
            .min(new BigDecimal("0.02"))).parameters())
            .containsExactlyInAnyOrderEntriesOf(Map.of("min", "0.02", "value", "0.01"));
        assertThat(failure(() -> Assert.field("value", BigDecimal.ZERO).over(0)).type())
            .isEqualTo(AssertionErrorType.NUMBER_VALUE_TOO_LOW);
        assertThat(failure(() -> Assert.field("value", new BigDecimal("2.00"))
            .under(BigDecimal.valueOf(2))).type()).isEqualTo(AssertionErrorType.NUMBER_VALUE_TOO_HIGH);
        assertThat(failure(() -> Assert.field("value", BigDecimal.TEN).max(2)).type())
            .isEqualTo(AssertionErrorType.NUMBER_VALUE_TOO_HIGH);
    }

    @Test
    void optionalLengthAllowsNullOnlyWhenTheMinimumAllowsIt() {
        assertDoesNotThrow(() -> Assert.field("value", (String) null).minLength(0));
        assertDoesNotThrow(() -> Assert.field("value", "").minLength(0));
        assertDoesNotThrow(() -> Assert.field("value", "ab").minLength(2));
        assertThat(failure(() -> Assert.field("value", (String) null).minLength(1)).type())
            .isEqualTo(AssertionErrorType.MISSING_MANDATORY_VALUE);
        assertThat(failure(() -> Assert.field("value", "a").minLength(2)).parameters())
            .containsExactlyInAnyOrderEntriesOf(Map.of("minLength", "2", "currentLength", "1"));
        assertThat(failure(() -> Assert.field("value", "abc").maxLength(2)).parameters())
            .containsExactlyInAnyOrderEntriesOf(Map.of("maxLength", "2", "currentLength", "3"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "missing", "@example.com", "a@", "a@nodot", "a@.example", "a@example."})
    void rejectsMalformedEmailAndPreservesItForTheValidationResponse(String email) {
        assertThat(failure(() -> Assert.field("value", email).email()).parameters())
            .containsExactlyEntriesOf(Map.of("value", email));
    }

    @Test
    void acceptsACompleteEmail() {
        assertDoesNotThrow(() -> Assert.field("value", "ama@example.com").email());
    }

    @Test
    void collectionBoundsHandleEmptyNullAndOversizedInputs() {
        assertDoesNotThrow(() -> Assert.notEmpty("value", List.of("a")));
        assertThat(failure(() -> Assert.notEmpty("value", List.of())).type())
            .isEqualTo(AssertionErrorType.MISSING_MANDATORY_VALUE);
        assertThat(failure(() -> Assert.notEmpty("value", Map.of())).type())
            .isEqualTo(AssertionErrorType.MISSING_MANDATORY_VALUE);
        assertDoesNotThrow(() -> Assert.field("value", (Collection<String>) null).maxSize(0).noNullElement());
        assertDoesNotThrow(() -> Assert.field("value", List.of()).maxSize(0));
        assertThat(failure(() -> Assert.field("value", (Collection<String>) null).maxSize(1)).type())
            .isEqualTo(AssertionErrorType.MISSING_MANDATORY_VALUE);
        assertThat(failure(() -> Assert.field("value", List.of("a", "b")).maxSize(1)).parameters())
            .containsExactlyInAnyOrderEntriesOf(Map.of("maxSize", "1", "currentSize", "2"));
    }

    @Test
    void arrayBoundsAndNullElementsMatchCollectionValidation() {
        assertDoesNotThrow(() -> Assert.field("value", new String[]{"a"}).notEmpty().maxSize(1).noNullElement());
        assertDoesNotThrow(() -> Assert.field("value", (String[]) null).maxSize(0).noNullElement());
        assertDoesNotThrow(() -> Assert.field("value", new String[0]).maxSize(0));
        assertThat(failure(() -> Assert.field("value", new String[0]).notEmpty()).type())
            .isEqualTo(AssertionErrorType.MISSING_MANDATORY_VALUE);
        assertThat(failure(() -> Assert.field("value", (String[]) null).maxSize(1)).type())
            .isEqualTo(AssertionErrorType.MISSING_MANDATORY_VALUE);
        assertThat(failure(() -> Assert.field("value", new String[]{null}).noNullElement()).type())
            .isEqualTo(AssertionErrorType.NULL_ELEMENT_IN_COLLECTION);
    }

    @Test
    void instantComparisonsHandleStrictEqualityAndBothSidesOfTheBoundary() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        assertDoesNotThrow(() -> Assert.field("value", now.plusSeconds(1)).after(now).afterOrAt(now));
        assertDoesNotThrow(() -> Assert.field("value", now.minusSeconds(1)).before(now).beforeOrAt(now));
        assertThat(failure(() -> Assert.field("value", now.minusSeconds(1)).afterOrAt(now)).type())
            .isEqualTo(AssertionErrorType.NOT_AFTER_TIME);
        assertThat(failure(() -> Assert.field("value", now.plusSeconds(1)).beforeOrAt(now)).type())
            .isEqualTo(AssertionErrorType.NOT_BEFORE_TIME);
        assertDoesNotThrow(() -> Assert.field("value", Instant.MAX).inFuture());
        assertDoesNotThrow(() -> Assert.field("value", Instant.MIN).inPast());
    }

    private static AssertionException failure(Executable operation) {
        AssertionException failure = assertThrows(AssertionException.class, operation);
        assertThat(failure.field()).isEqualTo("value");
        return failure;
    }
}
