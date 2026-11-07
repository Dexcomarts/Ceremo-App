package io.ceremo.ceremobackend.shared.generation.domain;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;


class CeremoUUIDTest {

  @Test
  void shouldGenerateUUID() {
    UUID uuid = CeremoUUID.generate();

    assertThat(uuid).isNotNull();
  }

  @Test
  void shouldGenerateUniqueUUIDs() {
    UUID uuid1 = CeremoUUID.generate();
    UUID uuid2 = CeremoUUID.generate();

    assertThat(uuid1).isNotEqualTo(uuid2);
  }

  @Test
  void shouldGenerateTimeOrderedUUIDs() {
    UUID uuid1 = CeremoUUID.generate();
    UUID uuid2 = CeremoUUID.generate();

    // UUID v7 are time-ordered, so uuid2 should be greater than uuid1
    assertThat(uuid2.compareTo(uuid1)).isGreaterThanOrEqualTo(0);
  }
}
