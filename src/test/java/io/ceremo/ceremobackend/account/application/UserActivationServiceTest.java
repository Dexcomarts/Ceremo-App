package io.ceremo.ceremobackend.account.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import io.ceremo.ceremobackend.shared.authentication.application.NotAuthenticatedUserException;
import io.ceremo.ceremobackend.shared.error.domain.CeremoException;
import io.ceremo.ceremobackend.shared.error.domain.ErrorStatus;
import io.ceremo.ceremobackend.account.application.port.out.CurrentUser;
import io.ceremo.ceremobackend.account.domain.exception.UserErrorKey;
import io.ceremo.ceremobackend.account.domain.model.User;
import io.ceremo.ceremobackend.account.domain.model.UserIdentity;
import io.ceremo.ceremobackend.account.domain.model.UserStatus;
import io.ceremo.ceremobackend.account.domain.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class UserActivationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");
    private static final UUID ID = UUID.fromString("c091cab0-c5c6-4dbe-9b2e-218fd3c4c969");
    private final UserRepository users = mock(UserRepository.class);
    private final CurrentUser currentUser = mock(CurrentUser.class);
    private final UserIdentity identity = new UserIdentity("https://identity.example/realms/ceremo", "person-123");
    private final UserService service = new UserService(users, currentUser, Clock.fixed(NOW, ZoneOffset.UTC), "terms-v1");

    @Test
    void registersAnActiveAccountOnlyWhenTheTrustedEmailClaimIsVerified() {
        when(currentUser.requireIdentity()).thenReturn(identity);
        when(currentUser.isEmailVerified()).thenReturn(true);
        when(users.findByIdentity(identity)).thenReturn(Optional.empty());

        User user = service.register("Ama Mensah", "terms-v1", true);

        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.id().version()).isEqualTo(7);
        verify(users).create(user, identity);
    }

    @Test
    void activatesOnlyTheCurrentPendingAccount() {
        User pending = User.register(ID, "Ama Mensah", "terms-v1", NOW);
        when(currentUser.requireIdentity()).thenReturn(identity);
        when(currentUser.isEmailVerified()).thenReturn(true);
        when(users.findByIdentity(identity)).thenReturn(Optional.of(pending));

        User active = service.activate();

        assertThat(active.id()).isEqualTo(ID);
        assertThat(active.status()).isEqualTo(UserStatus.ACTIVE);
        verify(users).updateStatus(active);
        verify(users, never()).create(any(), any());
    }

    @Test
    void rejectsActivationUntilTheTrustedEmailClaimIsVerified() {
        when(currentUser.requireIdentity()).thenReturn(identity);

        CeremoException error = assertThrows(CeremoException.class, service::activate);

        assertThat(error.key()).isEqualTo(UserErrorKey.EMAIL_NOT_VERIFIED);
        assertThat(error.status()).isEqualTo(ErrorStatus.FORBIDDEN);
        verifyNoInteractions(users);
    }

    @Test
    void rejectsUnauthenticatedActivationBeforeRepositoryAccess() {
        when(currentUser.requireIdentity()).thenThrow(new NotAuthenticatedUserException());

        assertThrows(NotAuthenticatedUserException.class, service::activate);

        verifyNoInteractions(users);
    }

    @Test
    void activationDoesNotCreateAMissingAccount() {
        when(currentUser.requireIdentity()).thenReturn(identity);
        when(currentUser.isEmailVerified()).thenReturn(true);
        when(users.findByIdentity(identity)).thenReturn(Optional.empty());

        CeremoException error = assertThrows(CeremoException.class, service::activate);

        assertThat(error.key()).isEqualTo(UserErrorKey.USER_NOT_FOUND);
        assertThat(error.status()).isEqualTo(ErrorStatus.NOT_FOUND);
        verify(users, never()).create(any(), any());
        verify(users, never()).updateStatus(any());
    }

    @Test
    void repeatedActivationDoesNotWriteAgain() {
        User active = User.restore(ID, "Ama Mensah", "terms-v1", NOW, UserStatus.ACTIVE);
        when(currentUser.requireIdentity()).thenReturn(identity);
        when(currentUser.isEmailVerified()).thenReturn(true);
        when(users.findByIdentity(identity)).thenReturn(Optional.of(active));

        assertThat(service.activate()).isSameAs(active);

        verify(users, never()).updateStatus(any());
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = {"SUSPENDED", "ANONYMISED"})
    void verificationCannotReactivateARestrictedAccount(UserStatus status) {
        User restricted = User.restore(ID, "Ama Mensah", "terms-v1", NOW, status);
        when(currentUser.requireIdentity()).thenReturn(identity);
        when(currentUser.isEmailVerified()).thenReturn(true);
        when(users.findByIdentity(identity)).thenReturn(Optional.of(restricted));

        CeremoException error = assertThrows(CeremoException.class, service::activate);

        assertThat(error.key()).isEqualTo(UserErrorKey.ACCOUNT_RESTRICTED);
        verify(users, never()).updateStatus(any());
    }

    @Test
    void readingAnAccountDoesNotActivateItEvenAfterEmailVerification() {
        User pending = User.register(ID, "Ama Mensah", "terms-v1", NOW);
        when(currentUser.requireIdentity()).thenReturn(identity);
        when(users.findByIdentity(identity)).thenReturn(Optional.of(pending));

        assertThat(service.currentUser()).containsSame(pending);

        verify(currentUser, never()).isEmailVerified();
        verify(users, never()).updateStatus(any());
    }
}
