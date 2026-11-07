package io.ceremo.ceremobackend.account.infrastructure.secondary.persistence;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.ceremo.ceremobackend.shared.error.domain.AssertionException;
import io.ceremo.ceremobackend.shared.generation.domain.CeremoUUID;
import io.ceremo.ceremobackend.account.domain.model.User;
import io.ceremo.ceremobackend.account.domain.model.UserStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class UserMapperTest {

    private static final Instant ACCEPTED_AT = Instant.parse("2026-10-20T12:00:00Z");

    @ParameterizedTest
    @EnumSource(UserStatus.class)
    void preservesSavedAccountState(UserStatus status) {
        User original = User.restore(
            CeremoUUID.generate(), "Ama Mensah", "terms-v1", ACCEPTED_AT, status);

        UserJpaEntity entity = UserMapper.toNewEntity(original);
        User restored = UserMapper.toDomain(entity);

        assertAll(
            () -> assertEquals(original.id(), restored.id()),
            () -> assertEquals(original.displayName(), restored.displayName()),
            () -> assertEquals(original.termsVersion(), restored.termsVersion()),
            () -> assertEquals(original.termsAcceptedAt(), restored.termsAcceptedAt()),
            () -> assertEquals(status, restored.status()),
            () -> assertNull(entity.getVersion()));
    }

    @Test
    void rejectsMissingUser() {
        assertThrows(AssertionException.class, () -> UserMapper.toNewEntity(null));
    }

    @Test
    void rejectsMissingEntity() {
        assertThrows(AssertionException.class, () -> UserMapper.toDomain(null));
    }
}
