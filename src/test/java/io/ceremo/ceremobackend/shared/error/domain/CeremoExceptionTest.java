package io.ceremo.ceremobackend.shared.error.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CeremoExceptionTest {

  @Test
  void derivedExceptionsStillDefaultToAnInternalFailure() {
    assertThat(new DerivedFailure().status()).isEqualTo(ErrorStatus.INTERNAL_SERVER_ERROR);
  }

  @Test
  void bulkParametersAreCopiedAndCannotBeChangedByTheCaller() {
    var parameters = new java.util.HashMap<>(java.util.Map.of("field", "displayName", "limit", "120"));
    var exception = CeremoException.badRequest(StandardErrorKey.BAD_REQUEST).addParameters(parameters).build();
    parameters.put("field", "changed");
    assertThat(exception.parameters()).containsExactlyInAnyOrderEntriesOf(
        java.util.Map.of("field", "displayName", "limit", "120"));
    assertThatThrownBy(() -> CeremoException.badRequest(StandardErrorKey.BAD_REQUEST).addParameters(null))
        .isInstanceOf(MissingMandatoryValueException.class);
  }

  private static final class DerivedFailure extends CeremoException {
    private DerivedFailure() { super(CeremoException.builder(StandardErrorKey.INTERNAL_SERVER_ERROR)); }
  }

  @Test
  void shouldDefaultDomainErrorsToInternalServerError() {
    CeremoException exception = CeremoException.builder(null).build();

    assertThat(exception.status()).isEqualTo(ErrorStatus.INTERNAL_SERVER_ERROR);
    assertThat(exception.key()).isEqualTo(StandardErrorKey.INTERNAL_SERVER_ERROR);
    assertThat(exception.getMessage()).isEqualTo("An error occurred");
  }

  @Test
  void shouldKeepBuiltParametersIndependentOfBuilderChanges() {
    var builder = CeremoException.badRequest(StandardErrorKey.BAD_REQUEST).addParameter("field", "name");
    CeremoException exception = builder.build();

    builder.addParameter("field", "changed");

    assertThat(exception.parameters()).containsEntry("field", "name");
    assertThatThrownBy(() -> exception.parameters().put("field", "changed"))
      .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void shouldPreserveTechnicalCauseAndServerStatus() {
    var cause = new IllegalStateException("dependency failed");
    CeremoException exception = CeremoException.technicalError("Unable to proceed", cause);

    assertThat(exception.status()).isEqualTo(ErrorStatus.INTERNAL_SERVER_ERROR);
    assertThat(exception.getCause()).isSameAs(cause);
  }
}
