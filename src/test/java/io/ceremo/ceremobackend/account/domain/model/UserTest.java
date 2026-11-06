package io.ceremo.ceremobackend.account.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.ceremo.ceremobackend.shared.error.domain.AssertionException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserTest {

    private static final UUID USER_ID =
        UUID.fromString("c091cab0-c5c6-4dbe-9b2e-218fd3c4c969");

    private static final Instant ACCEPTED_AT =
        Instant.parse("2026-10-20T12:00:00Z");

    @Test
    void equalityUsesTheAccountIdAcrossRestoredSnapshots() {
        User original = User.register(USER_ID, "Ama Mensah", "terms-v1", ACCEPTED_AT);
        User restored = User.restore(USER_ID, "Updated name", "terms-v2", ACCEPTED_AT, UserStatus.SUSPENDED);
        User another = User.register(UUID.fromString("c091cab0-c5c6-4dbe-9b2e-218fd3c4c970"),
            "Ama Mensah", "terms-v1", ACCEPTED_AT);
        assertThat(original).isEqualTo(original).isEqualTo(restored)
            .isNotEqualTo(another).isNotEqualTo(null).isNotEqualTo("not an account");
        assertTrue(original.equals(original));
        assertEquals(original.hashCode(), restored.hashCode());
        assertEquals(2, new HashSet<>(List.of(original, restored, another)).size());
    }

    @Test
    void registrationStartsPendingAndRecordsAcceptedTerms() {
        User user = User.register(
            USER_ID, "Ama Mensah", "terms-v1", ACCEPTED_AT);

        assertEquals(USER_ID, user.id());
        assertEquals("Ama Mensah", user.displayName());
        assertEquals(UserStatus.PENDING, user.status());
        assertEquals("terms-v1", user.termsVersion());
        assertEquals(ACCEPTED_AT, user.termsAcceptedAt());
    }

    @Test
    void registrationRejectsBlankDisplayName() {
        assertThrows(
            AssertionException.class,
            () -> User.register(
                USER_ID, " ", "terms-v1", ACCEPTED_AT));
    }

    @Test
    void registrationRejectsMissingTermsVersion() {
        assertThrows(
            AssertionException.class,
            () -> User.register(
                USER_ID, "Ama Mensah", null, ACCEPTED_AT));
    }

    @Test
    void registrationRejectsMissingAcceptanceTime() {
        assertThrows(
            AssertionException.class,
            () -> User.register(
                USER_ID, "Ama Mensah", "terms-v1", null));
    }

    @Test
    void restorationRejectsMissingStatus() {
        assertThrows(
            AssertionException.class,
            () -> User.restore(
                USER_ID, "Ama Mensah", "terms-v1", ACCEPTED_AT, null));
    }
}


