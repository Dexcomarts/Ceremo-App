package io.ceremo.ceremobackend.shared.error.infrastructure.primary;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class BeanValidationErrorsHandlerTest {
  private final BeanValidationErrorsHandler handler = new BeanValidationErrorsHandler();
  private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/providers");

  @Test
  @ExtendWith(OutputCaptureExtension.class)
  void logsNoRequestControlledTextWhenBodyValidationFails(CapturedOutput output) throws Exception {
    var forged = new MockHttpServletRequest("POST", "/api/v1/providers\r\nFORGED-ADMIN-LOGIN");
    var binding = bindingResult();
    binding.addError(new FieldError("input", "name", "must not be blank"));

    handler.handleMethodArgumentNotValid(exception(binding), forged);

    assertThat(output).contains("Bean validation failed for 1 field(s)").doesNotContain("FORGED-ADMIN-LOGIN");
  }

  @Test
  @ExtendWith(OutputCaptureExtension.class)
  void logsNoRequestControlledTextWhenParameterValidationFails(CapturedOutput output) {
    var forged = new MockHttpServletRequest("GET", "/api/v1/providers\r\nFORGED-ADMIN-LOGIN");
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      var exception = new ConstraintViolationException(factory.getValidator().validate(new BlankMessageInput()));

      handler.handleConstraintViolationException(exception, forged);
    }

    assertThat(output).contains("Bean validation failed for 1 field(s)").doesNotContain("FORGED-ADMIN-LOGIN");
  }

  @Test
  void keepsObjectLevelValidationErrors() throws Exception {
    var binding = bindingResult();
    binding.addError(new ObjectError("input", "The date range is invalid."));
    var response = handler.handleMethodArgumentNotValid(exception(binding), request);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(response.getBody().instance()).isEqualTo("/api/v1/providers");
    assertThat(response.getBody().errors()).containsEntry("input", "The date range is invalid.");
  }

  @Test
  void suppliesReadableMessagesWhenFieldMessagesAreMissingOrBlank() throws Exception {
    var binding = bindingResult();
    binding.addError(new FieldError("input", "name", null));
    binding.addError(new FieldError("input", "description", " "));
    var response = handler.handleMethodArgumentNotValid(exception(binding), request);
    assertThat(response.getBody().errors())
        .containsEntry("name", "Invalid value")
        .containsEntry("description", "Invalid value");
  }

  @Test
  void keepsFirstMessageForDuplicateFieldErrors() throws Exception {
    var binding = bindingResult();
    binding.addError(new FieldError("input", "name", "Name is required."));
    binding.addError(new FieldError("input", "name", "Name is too short."));
    var response = handler.handleMethodArgumentNotValid(exception(binding), request);
    assertThat(response.getBody().errors()).containsExactlyEntriesOf(
        java.util.Map.of("name", "Name is required."));
  }

  @Test
  void suppliesReadableMessagesForConstraintViolationsWithBlankMessages() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      var exception = new ConstraintViolationException(factory.getValidator().validate(new BlankMessageInput()));
      var response = handler.handleConstraintViolationException(exception, request);
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody().errors()).containsEntry("name", "Invalid value");
    }
  }

  private static BeanPropertyBindingResult bindingResult() {
    return new BeanPropertyBindingResult(new BlankMessageInput(), "input");
  }

  private static MethodArgumentNotValidException exception(BeanPropertyBindingResult binding) throws Exception {
    return new MethodArgumentNotValidException(new MethodParameter(
        BeanValidationErrorsHandlerTest.class.getDeclaredMethod("submit", BlankMessageInput.class), 0), binding);
  }

  private void submit(BlankMessageInput input) {}

  static class BlankMessageInput {
    @NotBlank(message = "")
    private final String name = "";
  }
}
