package io.ceremo.ceremobackend.account.infrastructure.primary.rest;

import io.ceremo.ceremobackend.shared.error.domain.CeremoException;
import io.ceremo.ceremobackend.shared.error.domain.ErrorStatus;
import io.ceremo.ceremobackend.account.application.port.in.UserAccounts;
import io.ceremo.ceremobackend.account.domain.exception.UserErrorKey;
import io.ceremo.ceremobackend.account.domain.model.User;
import io.ceremo.ceremobackend.account.infrastructure.primary.rest.dto.request.RestRegisterUserRequest;
import io.ceremo.ceremobackend.account.infrastructure.primary.rest.dto.response.RestUserResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Objects;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/users/me", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserResource {

    private final UserAccounts accounts;

    public UserResource(UserAccounts accounts) {
        this.accounts = Objects.requireNonNull(accounts, "accounts");
    }

    @GetMapping
    public ResponseEntity<RestUserResponse> currentUser() {
        User user = accounts.currentUser().orElseThrow(() ->
            CeremoException.builder(UserErrorKey.USER_NOT_FOUND)
                .status(ErrorStatus.NOT_FOUND)
                .message("No account is registered for this identity.")
                .build());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(RestUserResponse.from(user));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RestUserResponse> register(@Valid @RequestBody RestRegisterUserRequest request) {
        User user = accounts.register(request.displayName(), request.termsVersion(), request.termsAccepted());
        return ResponseEntity.created(URI.create("/api/v1/users/me"))
            .cacheControl(CacheControl.noStore())
            .body(RestUserResponse.from(user));
    }

    @PostMapping("/activate")
    public ResponseEntity<RestUserResponse> activate() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(RestUserResponse.from(accounts.activate()));
    }
}
