package io.ceremo.ceremobackend.account.infrastructure.primary.transaction;

import io.ceremo.ceremobackend.shared.error.domain.Assert;
import io.ceremo.ceremobackend.account.application.port.in.UserAccounts;
import io.ceremo.ceremobackend.account.domain.model.User;
import java.util.Objects;
import java.util.Optional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Keeps Spring transaction handling outside the account application core. */
public final class TransactionalUserAccounts implements UserAccounts {

    private final UserAccounts delegate;
    private final TransactionTemplate reads;
    private final TransactionTemplate writes;

    public TransactionalUserAccounts(
        UserAccounts delegate,
        PlatformTransactionManager transactionManager) {

        Assert.notNull("delegate", delegate);
        Assert.notNull("transactionManager", transactionManager);
        this.delegate = delegate;
        this.reads = new TransactionTemplate(transactionManager);
        this.reads.setReadOnly(true);
        this.writes = new TransactionTemplate(transactionManager);
    }

    @Override
    public Optional<User> currentUser() {
        return reads.execute(status -> Objects.requireNonNull(delegate.currentUser()));
    }

    @Override
    public User register(String displayName, String termsVersion, boolean termsAccepted) {
        return writes.execute(status -> Objects.requireNonNull(
            delegate.register(displayName, termsVersion, termsAccepted)));
    }

    @Override
    public User activate() {
        return writes.execute(status -> Objects.requireNonNull(delegate.activate()));
    }
}
