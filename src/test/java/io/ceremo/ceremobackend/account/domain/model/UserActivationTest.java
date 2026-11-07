package io.ceremo.ceremobackend.account.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.ceremo.ceremobackend.shared.error.domain.CeremoException;
import io.ceremo.ceremobackend.shared.error.domain.ErrorStatus;
import io.ceremo.ceremobackend.account.domain.exception.UserErrorKey;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class UserActivationTest {

    private static final UUID ID = UUID.fromString("c091cab0-c5c6-4dbe-9b2e-218fd3c4c969");
    private static final Instant ACCEPTED_AT = Instant.parse("2026-10-20T12:00:00Z");

    @Test
    void activatesPendingAccountWithoutChangingItsIdentityOrTerms() {
        User pending = User.register(ID, "Ama Mensah", "terms-v1", ACCEPTED_AT);

        User active = pending.activate();

        assertThat(active.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(active.id()).isEqualTo(ID);
        assertThat(active.displayName()).isEqualTo("Ama Mensah");
        assertThat(active.termsVersion()).isEqualTo("terms-v1");
        assertThat(active.termsAcceptedAt()).isEqualTo(ACCEPTED_AT);
        assertThat(pending.status()).isEqualTo(UserStatus.PENDING);
    }

    @Test
    void activationOfAnActiveAccountIsIdempotent() {
        User active = User.restore(ID, "Ama Mensah", "terms-v1", ACCEPTED_AT, UserStatus.ACTIVE);

        assertThat(active.activate()).isSameAs(active);
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = {"SUSPENDED", "ANONYMISED"})
    void cannotReactivateRestrictedAccounts(UserStatus status) {
        User user = User.restore(ID, "Ama Mensah", "terms-v1", ACCEPTED_AT, status);

        CeremoException error = assertThrows(CeremoException.class, user::activate);

        assertThat(error.key()).isEqualTo(UserErrorKey.ACCOUNT_RESTRICTED);
        assertThat(error.status()).isEqualTo(ErrorStatus.FORBIDDEN);
        assertThat(user.status()).isEqualTo(status);
    }
}
