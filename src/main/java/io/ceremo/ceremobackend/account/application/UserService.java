package io.ceremo.ceremobackend.account.application;

import io.ceremo.ceremobackend.shared.error.domain.Assert;
import io.ceremo.ceremobackend.shared.error.domain.CeremoException;
import io.ceremo.ceremobackend.shared.error.domain.ErrorStatus;
import io.ceremo.ceremobackend.shared.generation.domain.CeremoUUID;
import io.ceremo.ceremobackend.account.application.port.in.UserAccounts;
import io.ceremo.ceremobackend.account.application.port.out.CurrentUser;
import io.ceremo.ceremobackend.account.domain.exception.UserAlreadyExistsException;
import io.ceremo.ceremobackend.account.domain.exception.UserErrorKey;
import io.ceremo.ceremobackend.account.domain.model.User;
import io.ceremo.ceremobackend.account.domain.model.UserIdentity;
import io.ceremo.ceremobackend.account.domain.repository.UserRepository;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

public final class UserService implements UserAccounts {

    private final UserRepository users;
    private final CurrentUser currentUser;
    private final Clock clock;
    private final String registrationTermsVersion;

    public UserService(
        UserRepository users,
        CurrentUser currentUser,
        Clock clock,
        String registrationTermsVersion) {

        this.users = Objects.requireNonNull(users, "users");
        this.currentUser = Objects.requireNonNull(currentUser, "currentUser");
        this.clock = Objects.requireNonNull(clock, "clock");

        Assert.field("registrationTermsVersion", registrationTermsVersion)
            .notBlank()
            .maxLength(40);

        this.registrationTermsVersion = registrationTermsVersion;
    }

    @Override
    public Optional<User> currentUser() {
        return users.findByIdentity(requireIdentity());
    }

    @Override
    public User register(
        String displayName,
        String termsVersion,
        boolean termsAccepted) {

        UserIdentity identity = requireIdentity();

        if (!termsAccepted
            || !registrationTermsVersion.equals(termsVersion)) {

            throw CeremoException
                .badRequest(UserErrorKey.TERMS_NOT_ACCEPTED)
                .message("The current CEREMO terms must be accepted.")
                .build();
        }

        if (users.findByIdentity(identity).isPresent()) {
            throw new UserAlreadyExistsException();
        }

        User user = User.register(
            CeremoUUID.generate(),
            displayName,
            registrationTermsVersion,
            clock.instant());

        if (currentUser.isEmailVerified()) {
            user = user.activate();
        }

        users.create(user, identity);

        return user;
    }

    @Override
    public User activate() {
        UserIdentity identity = requireIdentity();
        if (!currentUser.isEmailVerified()) {
            throw CeremoException.builder(UserErrorKey.EMAIL_NOT_VERIFIED)
                .status(ErrorStatus.FORBIDDEN)
                .message("Verify your email before activating your account.")
                .build();
        }
        User user = users.findByIdentity(identity).orElseThrow(() ->
            CeremoException.builder(UserErrorKey.USER_NOT_FOUND)
                .status(ErrorStatus.NOT_FOUND)
                .message("No account is registered for this identity.")
                .build());
        User active = user.activate();
        if (active != user) {
            users.updateStatus(active);
        }
        return active;
    }

    private UserIdentity requireIdentity() {
        return Objects.requireNonNull(
            currentUser.requireIdentity(),
            "CurrentUser must return an authenticated identity");
    }
}
