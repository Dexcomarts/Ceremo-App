package io.ceremo.ceremobackend.account.application;

import io.ceremo.ceremobackend.account.application.port.in.Accounts;
import io.ceremo.ceremobackend.account.application.port.out.StaffAccess;
import io.ceremo.ceremobackend.shared.error.domain.CeremoException;
import io.ceremo.ceremobackend.shared.error.domain.ErrorStatus;
import io.ceremo.ceremobackend.account.application.port.in.UserAccounts;
import io.ceremo.ceremobackend.account.domain.exception.UserErrorKey;
import io.ceremo.ceremobackend.account.domain.model.User;
import io.ceremo.ceremobackend.account.domain.model.UserStatus;
import java.util.Objects;

public final class AccountService implements Accounts {
  private final UserAccounts users;
  private final StaffAccess staff;

  public AccountService(UserAccounts users, StaffAccess staff) {
    this.users = Objects.requireNonNull(users, "users");
    this.staff = Objects.requireNonNull(staff, "staff");
  }

  @Override
  public User currentAccount() {
    return users.currentUser().orElseThrow(() ->
        CeremoException.builder(UserErrorKey.USER_NOT_FOUND).status(ErrorStatus.NOT_FOUND)
            .message("No account is registered for this identity.").build());
  }

  @Override
  public User requireActiveAccount() {
    User account = currentAccount();
    if (account.status() != UserStatus.ACTIVE) {
      throw CeremoException.builder(UserErrorKey.ACCOUNT_RESTRICTED).status(ErrorStatus.FORBIDDEN)
          .message("An active account is required.").build();
    }
    return account;
  }

  @Override
  public User requireAdministrator() {
    User account = requireActiveAccount();
    staff.requireAdministrator();
    return account;
  }
}
