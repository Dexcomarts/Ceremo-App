package io.ceremo.ceremobackend.account.infrastructure.primary.rest.dto.response;

import io.ceremo.ceremobackend.account.domain.model.User;
import io.ceremo.ceremobackend.account.domain.model.UserStatus;
import java.time.Instant;
import java.util.UUID;

public record RestUserResponse(
    UUID id,
    String displayName,
    UserStatus status,
    String termsVersion,
    Instant termsAcceptedAt
) {

    public static RestUserResponse from(User user) {
        return new RestUserResponse(
            user.id(), user.displayName(), user.status(), user.termsVersion(), user.termsAcceptedAt());
    }
}
