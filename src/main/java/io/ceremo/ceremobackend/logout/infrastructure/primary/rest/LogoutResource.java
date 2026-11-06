package io.ceremo.ceremobackend.logout.infrastructure.primary.rest;

import io.ceremo.ceremobackend.logout.application.port.in.Logout;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LogoutResource {
  private final Logout logout;

  public LogoutResource(Logout logout) {
    this.logout = logout;
  }

  @PostMapping("/api/v1/logout")
  public ResponseEntity<Void> logout() {
    logout.logout();
    return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
  }
}
