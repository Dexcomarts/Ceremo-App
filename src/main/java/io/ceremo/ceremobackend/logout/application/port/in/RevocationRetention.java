package io.ceremo.ceremobackend.logout.application.port.in;

/** Removes session revocations that can no longer match a usable Keycloak session. */
public interface RevocationRetention {
  int purgeExpired();
}
