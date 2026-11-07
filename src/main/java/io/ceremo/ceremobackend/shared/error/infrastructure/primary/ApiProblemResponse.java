package io.ceremo.ceremobackend.shared.error.infrastructure.primary;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiProblemResponse(
  String title,
  String detail,
  String key,
  String instance,
  String message,
  Map<String, String> errors
) {

  public static ApiProblemResponse withKey(String title, String detail, String key, String instance) {
    return new ApiProblemResponse(title, detail, key, instance, null, null);
  }

  public static ApiProblemResponse withMessage(String title, String instance, String message) {
    return new ApiProblemResponse(title, null, null, instance, message, null);
  }

  public static ApiProblemResponse withErrors(String title, String detail, String instance, Map<String, String> errors) {
    return new ApiProblemResponse(title, detail, null, instance, null, errors);
  }
}
