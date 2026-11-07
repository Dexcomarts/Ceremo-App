package io.ceremo.ceremobackend.account.application.port.in;

import io.ceremo.ceremobackend.account.domain.model.User;

/** Account checks shared by protected business use cases. */
public interface Accounts {
  User currentAccount();
  User requireActiveAccount();
  User requireAdministrator();
}
