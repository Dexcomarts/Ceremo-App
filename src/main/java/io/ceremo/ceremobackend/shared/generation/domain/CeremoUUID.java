package io.ceremo.ceremobackend.shared.generation.domain;

import java.util.UUID;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;

/**
 * UUID generation utility for Ceremo.
 *
 * <p>Uses UUID version 7 (time-based epoch) which provides time-ordered UUIDs. This ensures better
 * database indexing performance compared to random UUIDs.
 */
public final class CeremoUUID {

  private static final TimeBasedEpochGenerator GENERATOR = Generators.timeBasedEpochGenerator();

  private CeremoUUID() {}

  /**
   * Generates a new UUID version 7 (time-based epoch UUID).
   *
   * @return a new time-ordered UUID
   */
  public static UUID generate() {
    return GENERATOR.generate();
  }
}
