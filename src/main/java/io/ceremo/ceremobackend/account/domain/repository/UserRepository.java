package io.ceremo.ceremobackend.account.domain.repository;

import io.ceremo.ceremobackend.account.domain.exception.UserAlreadyExistsException;
import io.ceremo.ceremobackend.account.domain.model.User;
import io.ceremo.ceremobackend.account.domain.model.UserIdentity;

import java.util.Optional;

public interface UserRepository {

    /**
     * Finds the account linked to the exact issuer and subject.
     *
     * Returns empty only when no account is linked.
     */
    Optional<User> findByIdentity(UserIdentity identity);

    /**
     * Creates an account and its initial identity relationship atomically.
     *
     * An existing identity relationship must never be overwritten.
     *
     * @throws UserAlreadyExistsException if the identity is already linked
     */
    void create(User user, UserIdentity identity);

    /** Updates only account status within the caller's transaction. */
    void updateStatus(User user);
}

