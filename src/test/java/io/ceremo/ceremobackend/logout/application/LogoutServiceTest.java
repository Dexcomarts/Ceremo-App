package io.ceremo.ceremobackend.logout.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.ceremo.ceremobackend.logout.application.port.out.CurrentSession;
import io.ceremo.ceremobackend.logout.domain.model.UserSession;
import io.ceremo.ceremobackend.logout.domain.repository.SessionRevocations;
import io.ceremo.ceremobackend.shared.authentication.application.NotAuthenticatedUserException;
import io.ceremo.ceremobackend.account.domain.model.UserIdentity;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class LogoutServiceTest {
  @Test
  void revokesOnlyTheTrustedCurrentSessionAndPropagatesWriteFailure() {
    CurrentSession current = mock(CurrentSession.class);
    SessionRevocations revocations = mock(SessionRevocations.class);
    Clock clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
    LogoutService service = new LogoutService(current, revocations, clock);
    when(current.requireSession()).thenThrow(new NotAuthenticatedUserException());
    assertThatThrownBy(service::logout).isInstanceOf(NotAuthenticatedUserException.class);
    verifyNoInteractions(revocations);
    UserSession session = new UserSession(new UserIdentity("https://id.example", "person"), "session");
    doReturn(session).when(current).requireSession();
    service.logout();
    verify(revocations).revoke(session, Instant.EPOCH);
    doThrow(new IllegalStateException("Write failed")).when(revocations).revoke(session, Instant.EPOCH);
    assertThatThrownBy(service::logout).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> new LogoutService(null, revocations, clock)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new LogoutService(current, null, clock)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new LogoutService(current, revocations, null)).isInstanceOf(NullPointerException.class);
  }
}
