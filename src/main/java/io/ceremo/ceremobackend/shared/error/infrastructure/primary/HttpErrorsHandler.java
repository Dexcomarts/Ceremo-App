package io.ceremo.ceremobackend.shared.error.infrastructure.primary;

import jakarta.persistence.OptimisticLockException;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Fallback after the existing domain and Bean Validation advice. */
@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
class HttpErrorsHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(HttpErrorsHandler.class);

    @Override
    protected ResponseEntity<Object> createResponseEntity(
        @Nullable Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        HttpStatus responseStatus = HttpStatus.valueOf(status.value());
        String detail = status.is5xxServerError()
            ? "An error occurred. Please try again later."
            : "The request could not be processed.";
        ApiProblemResponse problem = ApiProblemResponse.withKey(
            responseStatus.getReasonPhrase(), detail,
            responseStatus.name().toLowerCase(Locale.ROOT).replace('_', '-'),
            request.getDescription(false).replaceFirst("^uri=", ""));
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.putAll(headers);
        responseHeaders.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        responseHeaders.setCacheControl("no-store");
        return new ResponseEntity<>(problem, responseHeaders, status);
    }

    @ExceptionHandler({OptimisticLockException.class, OptimisticLockingFailureException.class})
    @Nullable ResponseEntity<Object> handleConcurrentUpdate(Exception exception, WebRequest request) {
        return handleExceptionInternal(exception, null, new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler({AuthenticationException.class, AccessDeniedException.class})
    void preserveSecurityHandling(Exception exception) throws Exception {
        throw exception;
    }

    @ExceptionHandler(Exception.class)
    @Nullable ResponseEntity<Object> handleUnexpectedFailure(Exception exception, WebRequest request) {
        log.error("Unhandled request failure of type {}", exception.getClass().getName());
        return handleExceptionInternal(exception, null, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
}
