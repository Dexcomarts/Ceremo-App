package io.ceremo.ceremobackend.account.infrastructure.primary.security;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.ceremo.ceremobackend.account.application.port.in.Accounts;
import io.ceremo.ceremobackend.account.domain.exception.UserErrorKey;
import io.ceremo.ceremobackend.account.domain.model.User;
import io.ceremo.ceremobackend.account.domain.model.UserStatus;
import io.ceremo.ceremobackend.shared.error.domain.CeremoException;
import io.ceremo.ceremobackend.shared.error.domain.ErrorStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

class ActiveAccountAuthorizationManagerTest {
  private final Accounts accounts = mock(Accounts.class);
  private final ActiveAccountAuthorizationManager manager = new ActiveAccountAuthorizationManager(accounts);
  private final RequestAuthorizationContext context =
      new RequestAuthorizationContext(new MockHttpServletRequest());
  private final Authentication caller = UsernamePasswordAuthenticationToken.authenticated(
      "customer", "unused", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

  @Test
  void anonymousAndUnauthenticatedRequestsNeverReachAccountStorage() {
    Authentication anonymous = new AnonymousAuthenticationToken("test", "anonymousUser",
        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    Authentication unauthenticated = UsernamePasswordAuthenticationToken.unauthenticated("customer", "unused");
    assertThat(manager.authorize(() -> anonymous, context).isGranted()).isFalse();
    assertThat(manager.authorize(() -> unauthenticated, context).isGranted()).isFalse();
    assertThat(manager.authorize(() -> null, context).isGranted()).isFalse();
    verifyNoInteractions(accounts);
  }

  @Test
  void grantsAnActiveAccountAfterTheExistingUseCaseCheck() {
    User account = User.restore(UUID.fromString("01940d11-7390-7000-8000-000000000001"),
        "Ama", "terms-v1", Instant.EPOCH, UserStatus.ACTIVE);
    when(accounts.requireActiveAccount()).thenReturn(account);
    assertThat(manager.authorize(() -> caller, context).isGranted()).isTrue();
    verify(accounts).requireActiveAccount();
  }

  @ParameterizedTest
  @EnumSource(value = UserErrorKey.class, names = {"USER_NOT_FOUND", "ACCOUNT_RESTRICTED"})
  void deniesMissingAndRestrictedAccounts(UserErrorKey key) {
    when(accounts.requireActiveAccount()).thenThrow(
        CeremoException.builder(key).status(ErrorStatus.FORBIDDEN).build());
    assertThat(manager.authorize(() -> caller, context).isGranted()).isFalse();
  }

  @Test
  void propagatesUnexpectedFailuresWithoutGrantingAccess() {
    CeremoException failure = CeremoException.technicalError("Account store unavailable");
    when(accounts.requireActiveAccount()).thenThrow(failure);
    assertThatThrownBy(() -> manager.authorize(() -> caller, context)).isSameAs(failure);
    assertThatThrownBy(() -> new ActiveAccountAuthorizationManager(null)).isInstanceOf(NullPointerException.class);
  }
}
