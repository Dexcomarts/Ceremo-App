package io.ceremo.ceremobackend.providerbusiness.domain.model;

import io.ceremo.ceremobackend.shared.error.domain.Assert;
import java.util.UUID;

/** Identity of one provider business: the private team workspace, not its public listing. */
public record ProviderBusinessId(UUID value) {
  public ProviderBusinessId {
    Assert.notNull("providerBusinessId", value);
  }

  public static ProviderBusinessId of(UUID value) {
    return new ProviderBusinessId(value);
  }
}
