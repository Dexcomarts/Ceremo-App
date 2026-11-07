package io.ceremo.ceremobackend.shared.error.infrastructure.primary;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ArgumentsReplacerTest {

  @Test
  void shouldReplaceRepeatedAndAdjacentParametersIncludingEmptyValues() {
    assertThat(ArgumentsReplacer.replaceParameters("{{ a }}{{ b }} {{ a }}", Map.of("a", "", "b", "B")))
      .isEqualTo("B ");
  }

  @Test
  void shouldPreserveUnknownAndUnclosedParameters() {
    assertThat(ArgumentsReplacer.replaceParameters("{{ known }} {{ unknown }} {{ incomplete", Map.of("known", "value")))
      .isEqualTo("value {{ unknown }} {{ incomplete");
  }

  @Test
  void shouldTreatReplacementValuesAsLiteralText() {
    assertThat(ArgumentsReplacer.replaceParameters("{{ a }}", Map.of("a", "{{ b }}", "b", "other")))
      .isEqualTo("{{ b }}");
  }

  @Test
  void shouldLeaveMissingInputsUnchanged() {
    assertThat(ArgumentsReplacer.replaceParameters(null, Map.of())).isNull();
    assertThat(ArgumentsReplacer.replaceParameters("message", null)).isEqualTo("message");
  }
}
