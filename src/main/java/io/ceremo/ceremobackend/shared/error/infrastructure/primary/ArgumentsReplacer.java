package io.ceremo.ceremobackend.shared.error.infrastructure.primary;

import java.util.Map;

final class ArgumentsReplacer {

  private static final String OPEN = "{{ ";
  private static final String CLOSE = " }}";

  private final Map<String, ?> arguments;

  private ArgumentsReplacer(Map<String, ?> arguments) {
    this.arguments = arguments;
  }

  public static String replaceParameters(String message, Map<String, ?> arguments) {
    if (message == null || arguments == null) {
      return message;
    }

    return new ArgumentsReplacer(arguments).format(message);
  }

  private String format(String message) {
    var result = new StringBuilder();
    int cursor = 0;
    int opening = message.indexOf(OPEN, cursor);

    while (opening != -1) {
      int closing = message.indexOf(CLOSE, opening + OPEN.length());
      if (closing == -1) {
        break;
      }

      result.append(message, cursor, opening);
      String key = message.substring(opening + OPEN.length(), closing).trim();
      result.append(getArgumentValue(key));
      cursor = closing + CLOSE.length();
      opening = message.indexOf(OPEN, cursor);
    }

    return result.append(message, cursor, message.length()).toString();
  }

  private String getArgumentValue(String key) {
    Object value = arguments.get(key);
    if (value == null) {
      return "{{ " + key + " }}";
    }

    return value.toString();
  }
}
