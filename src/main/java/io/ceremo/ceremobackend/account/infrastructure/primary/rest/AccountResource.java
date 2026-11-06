package io.ceremo.ceremobackend.account.infrastructure.primary.rest;

import io.ceremo.ceremobackend.account.application.port.in.Accounts;
import io.ceremo.ceremobackend.account.infrastructure.primary.rest.dto.response.RestUserResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/account")
public class AccountResource {
  private final Accounts accounts;

  public AccountResource(Accounts accounts) {
    this.accounts = accounts;
  }

  @GetMapping
  public ResponseEntity<RestUserResponse> currentAccount() {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
        .body(RestUserResponse.from(accounts.currentAccount()));
  }
}
