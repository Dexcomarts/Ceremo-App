package io.ceremo.ceremobackend.shared.error.infrastructure.primary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.ceremo.ceremobackend.shared.error.domain.Assert;
import io.ceremo.ceremobackend.shared.error.domain.AssertionErrorType;
import io.ceremo.ceremobackend.shared.error.domain.CeremoException;
import io.ceremo.ceremobackend.shared.error.domain.ErrorStatus;
import io.ceremo.ceremobackend.shared.error.domain.MissingMandatoryValueException;
import io.ceremo.ceremobackend.shared.error.domain.StandardErrorKey;
import io.ceremo.ceremobackend.account.domain.exception.UserAlreadyExistsException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ErrorHandlingTest {

  private final Locale originalLocale = Locale.getDefault();
  private AnnotationConfigApplicationContext context;
  private LocalValidatorFactoryBean validator;
  private MockMvc mvc;

  @BeforeAll
  void configure() {
    context = new AnnotationConfigApplicationContext(
      AssertionErrorsConfiguration.class,
      CeremoErrorsConfiguration.class,
      AssertionErrorsHandler.class,
      CeremoErrorsHandler.class,
      BeanValidationErrorsHandler.class
    );
    validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();
    mvc = MockMvcBuilders.standaloneSetup(new ErrorResource(validator))
      .setControllerAdvice(
        context.getBean(AssertionErrorsHandler.class),
        context.getBean(CeremoErrorsHandler.class),
        context.getBean(BeanValidationErrorsHandler.class)
      )
      .setValidator(validator)
      .build();
  }

  @AfterAll
  void close() {
    validator.close();
    context.close();
  }

  @Test
  void shouldRenderPrimaryAssertionAsProblemJson() throws Exception {
    mvc.perform(get("/test/errors/assertion"))
      .andExpect(status().isBadRequest())
      .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
      .andExpect(jsonPath("$.title").value("Missing mandatory value"))
      .andExpect(jsonPath("$.detail").value("Content of name must be set"))
      .andExpect(jsonPath("$.key").value("MISSING_MANDATORY_VALUE"))
      .andExpect(jsonPath("$.instance").value("/test/errors/assertion"))
      .andExpect(jsonPath("$.message").doesNotExist())
      .andExpect(jsonPath("$.errors").doesNotExist());
  }

  @Test
  void shouldUseRequestedLocaleAndFallBackToEnglish() throws Exception {
    mvc.perform(get("/test/errors/assertion").header(HttpHeaders.ACCEPT_LANGUAGE, "fr"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.title").value("Valeur obligatoire manquante"));

    mvc.perform(get("/test/errors/assertion").header(HttpHeaders.ACCEPT_LANGUAGE, "de"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.title").value("Missing mandatory value"));
  }

  @Test
  void shouldTreatApplicationAssertionsAsServerErrors() throws Exception {
    mvc.perform(get("/test/errors/domain-assertion"))
      .andExpect(status().isInternalServerError());
  }

  @ParameterizedTest
  @CsvSource({
    "BAD_REQUEST,400", "UNAUTHORIZED,401", "PAYMENT_REQUIRED,402", "FORBIDDEN,403",
    "NOT_FOUND,404", "METHOD_NOT_ALLOWED,405", "NOT_ACCEPTABLE,406",
    "UNPROCESSABLE_ENTITY,422", "CONFLICT,409", "GONE,410", "I_AM_A_TEAPOT,418",
    "INTERNAL_SERVER_ERROR,500", "NOT_IMPLEMENTED,501"
  })
  void shouldMapEveryDomainStatusWithoutSalesyncUtilities(ErrorStatus errorStatus, int expected) throws Exception {
    mvc.perform(get("/test/errors/application/" + errorStatus))
      .andExpect(status().is(expected))
      .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
      .andExpect(jsonPath("$.key").value("bad-request"));
  }

  @Test
  void shouldKeepTechnicalErrorsAt500EvenFromPrimaryCode() throws Exception {
    String body = mvc.perform(get("/test/errors/technical"))
      .andExpect(status().isInternalServerError())
      .andExpect(jsonPath("$.key").value("internal-server-error"))
      .andExpect(jsonPath("$.detail").value("An error occurred. Please try again later."))
      .andReturn().getResponse().getContentAsString();

    assertThat(body).doesNotContain("sensitive internal information");
  }

  @ParameterizedTest
  @CsvSource({
    "en,Account already exists",
    "fr,Compte existant",
    "de,Account already exists"
  })
  void shouldRenderDuplicateUserThroughSharedErrorHandling(String language, String title) throws Exception {
    mvc.perform(get("/test/errors/duplicate-user").header(HttpHeaders.ACCEPT_LANGUAGE, language))
      .andExpect(status().isConflict())
      .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
      .andExpect(jsonPath("$.title").value(title))
      .andExpect(jsonPath("$.key").value("user-already-exists"))
      .andExpect(jsonPath("$.instance").value("/test/errors/duplicate-user"))
      .andExpect(jsonPath("$.detail").isNotEmpty());
  }

  @Test
  void shouldCombineMultipleBodyValidationErrorsForOneField() throws Exception {
    mvc.perform(post("/test/errors/validation").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors.name").isString())
      .andExpect(jsonPath("$.errors.length()").value(1))
      .andExpect(jsonPath("$.key").doesNotExist());
  }

  @Test
  void shouldCombineMultipleConstraintViolationsForOneField() throws Exception {
    mvc.perform(get("/test/errors/constraint"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors.name").isString())
      .andExpect(jsonPath("$.errors.length()").value(1));
  }

  @ParameterizedTest
  @MethodSource("assertionTypes")
  void shouldLoadEveryAssertionMessageInBothLocales(AssertionErrorType type) {
    var messages = context.getBean("assertionErrorMessageSource", org.springframework.context.MessageSource.class);
    for (Locale locale : new Locale[] {Locale.ENGLISH, Locale.FRENCH}) {
      assertThat(messages.getMessage("assertion-error." + type + ".title", null, locale)).isNotBlank();
      assertThat(messages.getMessage("assertion-error." + type + ".detail", null, locale)).isNotBlank();
    }
  }

  static Stream<AssertionErrorType> assertionTypes() {
    return Stream.of(AssertionErrorType.values());
  }

  @Test
  void shouldNotChangeProcessLocaleWhenHandlersAreCreated() {
    assertThat(Locale.getDefault()).isEqualTo(originalLocale);
  }

  record Input(@NotBlank @Size(min = 3) String name) {}

  @RestController
  @org.springframework.boot.test.context.TestComponent
  @RequestMapping("/test/errors")
  static class ErrorResource {

    private final Validator validator;

    ErrorResource(Validator validator) {
      this.validator = validator;
    }

    @GetMapping("/assertion")
    void assertion() {
      Assert.notBlank("name", " ");
    }

    @GetMapping("/domain-assertion")
    void domainAssertion() {
      var exception = MissingMandatoryValueException.forNullValue("name");
      exception.setStackTrace(new StackTraceElement[] {
        new StackTraceElement("io.ceremo.ceremobackend.providermanagement.application.ProviderService", "create", "ProviderService.java", 20)
      });
      throw exception;
    }

    @GetMapping("/application/{errorStatus}")
    void application(@PathVariable("errorStatus") ErrorStatus errorStatus) {
      throw CeremoException.builder(StandardErrorKey.BAD_REQUEST).status(errorStatus).build();
    }

    @GetMapping("/technical")
    void technical() {
      throw CeremoException.technicalError("sensitive internal information");
    }

    @GetMapping("/duplicate-user")
    void duplicateUser() {
      throw new UserAlreadyExistsException();
    }

    @PostMapping("/validation")
    void validation(@Valid @RequestBody Input input) {}

    @GetMapping("/constraint")
    void constraint() {
      throw new ConstraintViolationException(validator.validate(new Input("")));
    }
  }
}
