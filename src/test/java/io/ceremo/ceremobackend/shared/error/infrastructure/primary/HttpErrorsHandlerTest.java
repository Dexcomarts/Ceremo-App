package io.ceremo.ceremobackend.shared.error.infrastructure.primary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.persistence.OptimisticLockException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

class HttpErrorsHandlerTest {

    private MockMvc mvc;

    @BeforeEach
    void prepare() {
        mvc = MockMvcBuilders.standaloneSetup(new TestResource())
            .setControllerAdvice(new HttpErrorsHandler()).build();
    }

    @Test
    void malformedJsonHasASafeProblemBody() throws Exception {
        mvc.perform(post("/http-errors/body").contentType(MediaType.APPLICATION_JSON).content("{"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.key").value("bad-request"))
            .andExpect(jsonPath("$.instance").value("/http-errors/body"));
    }

    @Test
    void unexpectedFailureDoesNotExposeExceptionOrDatabaseDetails() throws Exception {
        var response = mvc.perform(get("/http-errors/unexpected"))
            .andExpect(status().isInternalServerError())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.key").value("internal-server-error"))
            .andExpect(jsonPath("$.detail").value("An error occurred. Please try again later."))
            .andReturn().getResponse();
        assertThat(response.getContentAsString()).doesNotContain("private-value", "SQLException", "stackTrace");
    }

    @Test
    void optimisticConflictsAreSafeAndRetryable() throws Exception {
        for (String path : new String[]{"/http-errors/jpa-conflict", "/http-errors/spring-conflict"}) {
            mvc.perform(get(path)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.key").value("conflict"))
                .andExpect(jsonPath("$.detail").value("The request could not be processed."));
        }
    }

    @Test
    void preservesFrameworkStatusAndAllowHeader() throws Exception {
        mvc.perform(delete("/http-errors/unexpected"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(header().string("Allow", org.hamcrest.Matchers.containsString("GET")));
    }

    @Test
    void leavesAuthenticationAndAccessDenialsToTheSecurityChain() {
        for (Exception failure : new Exception[]{
            new BadCredentialsException("private-value"), new AccessDeniedException("private-value")}) {
            assertThatThrownBy(() -> new HttpErrorsHandler().preserveSecurityHandling(failure))
                .isSameAs(failure);
        }
    }

    @Test
    void doesNotReplaceAnAlreadyCommittedResponse() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.getWriter().write("already sent");
        response.flushBuffer();

        var result = new HttpErrorsHandler().handleUnexpectedFailure(
            new IllegalStateException("private-value"),
            new ServletWebRequest(new MockHttpServletRequest(), response));

        assertThat(result).isNull();
        assertThat(response.getContentAsString()).isEqualTo("already sent");
    }

    @RestController
    @TestComponent
    static class TestResource {
        @PostMapping("/http-errors/body")
        void body(@RequestBody Map<String, Object> body) {}

        @GetMapping("/http-errors/unexpected")
        void unexpected() { throw new IllegalStateException("SQLException private-value"); }

        @GetMapping("/http-errors/jpa-conflict")
        void jpaConflict() { throw new OptimisticLockException("private-value"); }

        @GetMapping("/http-errors/spring-conflict")
        void springConflict() { throw new OptimisticLockingFailureException("private-value"); }
    }
}
