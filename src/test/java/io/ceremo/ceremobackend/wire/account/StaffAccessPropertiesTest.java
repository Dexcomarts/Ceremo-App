package io.ceremo.ceremobackend.wire.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.ceremo.ceremobackend.shared.error.domain.AssertionException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StaffAccessPropertiesTest {
  private static final List<String> CLIENTS = List.of("web_app");

  @ParameterizedTest
  @ValueSource(longs = {60, 7_200, 43_200})
  void acceptsAWindowFromOneMinuteToTheTwelveHourSessionMaximum(long seconds) {
    assertThat(new StaffAccessProperties(Duration.ofSeconds(seconds), CLIENTS).reauthenticationMaxAge())
        .hasSeconds(seconds);
  }

  @ParameterizedTest
  @ValueSource(longs = {59, 43_201})
  void rejectsAWindowOutsideTheBounds(long seconds) {
    Duration window = Duration.ofSeconds(seconds);

    assertThatThrownBy(() -> new StaffAccessProperties(window, CLIENTS)).isInstanceOf(AssertionException.class);
  }

  @Test
  void requiresAWindow() {
    assertThatThrownBy(() -> new StaffAccessProperties(null, CLIENTS)).isInstanceOf(AssertionException.class);
  }

  @Test
  void requiresAtLeastOneNamedStaffClientAndKeepsAnImmutableCopy() {
    Duration window = Duration.ofHours(2);
    List<String> nullEntry = Arrays.asList("web_app", null);
    List<String> blankEntry = List.of("web_app", " ");
    List<String> empty = List.of();

    assertThatThrownBy(() -> new StaffAccessProperties(window, null)).isInstanceOf(AssertionException.class);
    assertThatThrownBy(() -> new StaffAccessProperties(window, empty)).isInstanceOf(AssertionException.class);
    assertThatThrownBy(() -> new StaffAccessProperties(window, nullEntry)).isInstanceOf(AssertionException.class);
    assertThatThrownBy(() -> new StaffAccessProperties(window, blankEntry)).isInstanceOf(AssertionException.class);

    List<String> source = new ArrayList<>(List.of("web_app", "ceremo-admin"));
    StaffAccessProperties properties = new StaffAccessProperties(window, source);
    source.clear();
    assertThat(properties.clients()).containsExactly("web_app", "ceremo-admin");
  }
}
