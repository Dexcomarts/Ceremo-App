package io.ceremo.ceremobackend.account.infrastructure.secondary.persistence;

import io.ceremo.ceremobackend.shared.error.domain.Assert;
import io.ceremo.ceremobackend.shared.error.domain.CeremoException;
import io.ceremo.ceremobackend.shared.error.domain.ErrorStatus;
import io.ceremo.ceremobackend.account.domain.exception.UserAlreadyExistsException;
import io.ceremo.ceremobackend.account.domain.exception.UserErrorKey;
import io.ceremo.ceremobackend.account.domain.model.User;
import io.ceremo.ceremobackend.account.domain.model.UserIdentity;
import io.ceremo.ceremobackend.account.domain.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.MANDATORY)
public class JpaUserRepository implements UserRepository {

    private static final String IDENTITY_UNIQUE_CONSTRAINT =
        "uq_user_identity_issuer_subject";

    private final EntityManager entityManager;
    private final Supplier<UUID> identityIds;

    public JpaUserRepository(
        EntityManager entityManager,
        Supplier<UUID> identityIds) {

        Assert.notNull("entityManager", entityManager);
        Assert.notNull("identityIds", identityIds);

        this.entityManager = entityManager;
        this.identityIds = identityIds;
    }

    @Override
    public Optional<User> findByIdentity(UserIdentity identity) {
        Assert.notNull("identity", identity);

        try {
            UserJpaEntity account = entityManager.createQuery("""
                    select link.user
                    from UserIdentityJpaEntity link
                    where link.issuer = :issuer
                      and link.subject = :subject
                    """, UserJpaEntity.class)
                .setParameter("issuer", identity.issuer())
                .setParameter("subject", identity.subject())
                .getSingleResult();

            return Optional.of(UserMapper.toDomain(account));
        } catch (NoResultException exception) {
            return Optional.empty();
        }
    }

    @Override
    public void create(User user, UserIdentity identity) {
        Assert.notNull("user", user);
        Assert.notNull("identity", identity);

        try {
            UserJpaEntity account = UserMapper.toNewEntity(user);
            entityManager.persist(account);

            UserIdentityJpaEntity link = new UserIdentityJpaEntity(
                identityIds.get(),
                account,
                identity,
                user.termsAcceptedAt());

            entityManager.persist(link);
            entityManager.flush();
        } catch (RuntimeException exception) {
            if (isDuplicateIdentity(exception)) {
                throw new UserAlreadyExistsException();
            }

            throw exception;
        }
    }

    @Override
    public void updateStatus(User user) {
        Assert.notNull("user", user);
        UserJpaEntity entity = entityManager.find(UserJpaEntity.class, user.id());
        if (entity == null) {
            throw CeremoException.builder(UserErrorKey.USER_NOT_FOUND)
                .status(ErrorStatus.NOT_FOUND)
                .message("The account no longer exists.")
                .build();
        }
        entity.updateStatus(user.status());
        entityManager.flush();
    }

    private static boolean isDuplicateIdentity(Throwable error) {
        for (Throwable cause = error;
             cause != null;
             cause = cause.getCause()) {

            if (cause instanceof ConstraintViolationException violation
                && "23505".equals(violation.getSQLException().getSQLState())
                && IDENTITY_UNIQUE_CONSTRAINT.equals(
                violation.getConstraintName())) {

                return true;
            }
        }

        return false;
    }
}
