package io.ceremo.ceremobackend.account.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.ceremo.ceremobackend.account.application.port.out.StaffAccess;
import io.ceremo.ceremobackend.shared.error.domain.CeremoException;
import io.ceremo.ceremobackend.shared.error.domain.ErrorStatus;
import io.ceremo.ceremobackend.account.application.port.in.UserAccounts;
import io.ceremo.ceremobackend.account.domain.model.User;
import io.ceremo.ceremobackend.account.domain.model.UserStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountServiceTest {
  @Test
  void checksLiveAccountStatusBeforeStaffPermission() {
    UserAccounts users = mock(UserAccounts.class);
    StaffAccess staff = mock(StaffAccess.class);
    AccountService service = new AccountService(users, staff);
    when(users.currentUser()).thenReturn(Optional.empty());
    assertThatThrownBy(service::currentAccount).isInstanceOfSatisfying(CeremoException.class,
        error -> assertThat(error.status()).isEqualTo(ErrorStatus.NOT_FOUND));
    for (UserStatus status : UserStatus.values()) {
      User user = User.restore(UUID.fromString("01940d11-7390-7000-8000-000000000001"),
          "Ama", "terms-v1", Instant.EPOCH, status);
      when(users.currentUser()).thenReturn(Optional.of(user));
      assertThat(service.currentAccount()).isSameAs(user);
      if (status == UserStatus.ACTIVE) {
        assertThat(service.requireActiveAccount()).isSameAs(user);
        assertThat(service.requireAdministrator()).isSameAs(user);
        verify(staff).requireAdministrator();
      } else {
        assertThatThrownBy(service::requireAdministrator).isInstanceOfSatisfying(CeremoException.class,
            error -> assertThat(error.status()).isEqualTo(ErrorStatus.FORBIDDEN));
      }
    }
    verifyNoMoreInteractions(staff);
    assertThatThrownBy(() -> new AccountService(null, staff)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new AccountService(users, null)).isInstanceOf(NullPointerException.class);
  }
}
