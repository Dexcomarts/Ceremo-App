package io.ceremo.ceremobackend.account.application;

import io.ceremo.ceremobackend.account.application.port.out.CurrentUser;
import io.ceremo.ceremobackend.shared.authentication.application.NotAuthenticatedUserException;
import io.ceremo.ceremobackend.shared.generation.domain.CeremoUUID;
import io.ceremo.ceremobackend.account.domain.model.User;
import io.ceremo.ceremobackend.account.domain.model.UserIdentity;
import io.ceremo.ceremobackend.account.domain.repository.UserRepository;
import io.ceremo.ceremobackend.shared.error.domain.CeremoException;
import io.ceremo.ceremobackend.shared.error.domain.ErrorStatus;
import io.ceremo.ceremobackend.account.domain.exception.UserAlreadyExistsException;
import io.ceremo.ceremobackend.account.domain.exception.UserErrorKey;
import io.ceremo.ceremobackend.account.domain.model.UserStatus;
import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final CurrentUser currentUser = mock(CurrentUser.class);
    private static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");

    private final UserService service = new UserService(
        users, currentUser, Clock.fixed(NOW, ZoneOffset.UTC), "terms-v1");

    private final UserIdentity identity = new UserIdentity(
        "https://identity.example/realms/ceremo",
        "person-123");

    @Test
    void returnsAccountLinkedToAuthenticatedIdentity() {
        User user = User.register(
            CeremoUUID.generate(),
            "Ama Mensah",
            "terms-v1",
            Instant.parse("2026-10-20T12:00:00Z"));

        when(currentUser.requireIdentity()).thenReturn(identity);
        when(users.findByIdentity(identity)).thenReturn(Optional.of(user));

        assertEquals(Optional.of(user), service.currentUser());
    }

    @Test
    void missingAccountDoesNotCreateOneDuringLookup() {
        when(currentUser.requireIdentity()).thenReturn(identity);
        when(users.findByIdentity(identity)).thenReturn(Optional.empty());

        assertTrue(service.currentUser().isEmpty());

        verify(users, never()).create(any(), any());
    }

    @Test
    void unauthenticatedCallerCannotReachRepository() {
        when(currentUser.requireIdentity())
            .thenThrow(new NotAuthenticatedUserException());

        assertThrows(
            NotAuthenticatedUserException.class,
            service::currentUser);

        verifyNoInteractions(users);
    }

    @Test
    void registersAccountForAuthenticatedIdentity() {
        when(currentUser.requireIdentity()).thenReturn(identity);
        when(users.findByIdentity(identity)).thenReturn(Optional.empty());

        User user = service.register("Ama Mensah", "terms-v1", true);

        assertAll(
            () -> assertEquals("Ama Mensah", user.displayName()),
            () -> assertEquals("terms-v1", user.termsVersion()),
            () -> assertEquals(NOW, user.termsAcceptedAt()),
            () -> assertEquals(UserStatus.PENDING, user.status()),
            () -> assertEquals(7, user.id().version()));

        verify(currentUser).requireIdentity();
        verify(users).create(user, identity);
    }

    @Test
    void rejectsRegistrationWithoutTermsAcceptance() {
        when(currentUser.requireIdentity()).thenReturn(identity);

        CeremoException error = assertThrows(
            CeremoException.class,
            () -> service.register("Ama Mensah", "terms-v1", false));

        assertEquals(UserErrorKey.TERMS_NOT_ACCEPTED, error.key());
        assertEquals(ErrorStatus.BAD_REQUEST, error.status());
        verifyNoInteractions(users);
    }

    @Test
    void rejectsAcceptanceOfAnOldTermsVersion() {
        when(currentUser.requireIdentity()).thenReturn(identity);

        CeremoException error = assertThrows(
            CeremoException.class,
            () -> service.register("Ama Mensah", "terms-v0", true));

        assertEquals(UserErrorKey.TERMS_NOT_ACCEPTED, error.key());
        assertEquals(ErrorStatus.BAD_REQUEST, error.status());
        verifyNoInteractions(users);
    }

    @Test
    void rejectsAnIdentityAlreadyLinkedToAnAccount() {
        User existing = User.register(CeremoUUID.generate(), "Ama Mensah", "terms-v1", NOW);
        when(currentUser.requireIdentity()).thenReturn(identity);
        when(users.findByIdentity(identity)).thenReturn(Optional.of(existing));

        assertThrows(
            UserAlreadyExistsException.class,
            () -> service.register("Ama Mensah", "terms-v1", true));

        verify(users, never()).create(any(), any());
    }

    @Test
    void unauthenticatedCallerCannotRegister() {
        when(currentUser.requireIdentity()).thenThrow(new NotAuthenticatedUserException());

        assertThrows(
            NotAuthenticatedUserException.class,
            () -> service.register("Ama Mensah", "terms-v1", true));

        verifyNoInteractions(users);
    }
}
