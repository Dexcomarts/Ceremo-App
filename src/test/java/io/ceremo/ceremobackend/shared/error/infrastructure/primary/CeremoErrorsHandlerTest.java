package io.ceremo.ceremobackend.shared.error.infrastructure.primary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.ceremo.ceremobackend.shared.error.domain.CeremoException;
import io.ceremo.ceremobackend.shared.error.domain.ErrorStatus;
import io.ceremo.ceremobackend.shared.error.domain.StandardErrorKey;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;

class CeremoErrorsHandlerTest {
    private final StaticMessageSource messages = new StaticMessageSource();

    @ParameterizedTest
    @CsvSource({"BAD_REQUEST,400", "UNAUTHORIZED,401", "PAYMENT_REQUIRED,402", "FORBIDDEN,403",
        "NOT_FOUND,404", "METHOD_NOT_ALLOWED,405", "NOT_ACCEPTABLE,406", "UNPROCESSABLE_ENTITY,422",
        "CONFLICT,409", "GONE,410", "I_AM_A_TEAPOT,418", "INTERNAL_SERVER_ERROR,500", "NOT_IMPLEMENTED,501"})
    void mapsEveryDomainStatusToItsHttpCode(ErrorStatus status, int code) {
        var error = CeremoException.builder(StandardErrorKey.BAD_REQUEST).status(status)
            .addParameter("field", "name").build();
        var response = handler().handleCeremoException(error, new MockHttpServletRequest("POST", "/api/v1/users"));
        assertThat(response.getStatusCode().value()).isEqualTo(code);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody().instance()).isEqualTo("/api/v1/users");
    }

    @Test
    void treatsAnErrorWithAMissingStatusAsAnInternalFailure() {
        var malformed = mock(CeremoException.class);
        when(malformed.key()).thenReturn(StandardErrorKey.BAD_REQUEST);
        when(malformed.parameters()).thenReturn(Map.of());
        var response = handler().handleCeremoException(malformed, new MockHttpServletRequest());
        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }

    @Test
    void errorsCreatedAtThePrimaryBoundaryDefaultToBadRequest() {
        assertThat(CeremoException.builder(StandardErrorKey.BAD_REQUEST).build().status())
            .isEqualTo(ErrorStatus.BAD_REQUEST);
    }

    private CeremoErrorsHandler handler() {
        messages.addMessage("error.bad-request.title", Locale.getDefault(), "Invalid request");
        messages.addMessage("error.bad-request.detail", Locale.getDefault(), "Check the request");
        return new CeremoErrorsHandler(messages);
    }
}
