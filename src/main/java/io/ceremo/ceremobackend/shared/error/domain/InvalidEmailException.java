package io.ceremo.ceremobackend.shared.error.domain;

import java.util.Map;

public final class InvalidEmailException extends AssertionException {

  private final String value;

  private InvalidEmailException(InvalidEmailExceptionBuilder builder) {
    super(builder.field, builder.message());
    value = builder.value;
  }

  public static InvalidEmailExceptionBuilder builder() {
    return new InvalidEmailExceptionBuilder();
  }

  static final class InvalidEmailExceptionBuilder {

    private String value;
    private String field;

    private InvalidEmailExceptionBuilder() {}

    InvalidEmailExceptionBuilder field(String field) {
      this.field = field;

      return this;
    }

    InvalidEmailExceptionBuilder value(String value) {
      this.value = value;

      return this;
    }

    private String message() {
      return "The value \"%s\" in field \"%s\" is not a valid email address".formatted(value, field);
    }

    public InvalidEmailException build() {
      return new InvalidEmailException(this);
    }
  }

  @Override
  public AssertionErrorType type() {
    return AssertionErrorType.INVALID_EMAIL;
  }

  @Override
  public Map<String, String> parameters() {
    return Map.of("value", value);
  }
}
