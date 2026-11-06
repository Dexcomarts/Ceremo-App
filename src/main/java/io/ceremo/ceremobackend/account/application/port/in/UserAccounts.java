package io.ceremo.ceremobackend.account.application.port.in;

import io.ceremo.ceremobackend.account.domain.model.User;
import java.util.Optional;

public interface UserAccounts {

    Optional<User> currentUser();

    User activate();

    User register(
        String displayName,
        String termsVersion,
        boolean termsAccepted);
}
