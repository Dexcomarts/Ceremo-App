package io.ceremo.ceremobackend.account.application.port.in;

import java.util.UUID;

/** Internal account existence check; this is not a public customer directory. */
public interface UserDirectory {
  boolean isRegistered(UUID userId);
}
