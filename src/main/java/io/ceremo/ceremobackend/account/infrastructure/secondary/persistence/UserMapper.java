package io.ceremo.ceremobackend.account.infrastructure.secondary.persistence;

import io.ceremo.ceremobackend.shared.error.domain.Assert;
import io.ceremo.ceremobackend.account.domain.model.User;

final class UserMapper {

    private UserMapper() {}

    static UserJpaEntity toNewEntity(User user) {
        Assert.notNull("user", user);

        return new UserJpaEntity(
            user.id(),
            user.displayName(),
            user.termsVersion(),
            user.termsAcceptedAt(),
            user.status());
    }

    static User toDomain(UserJpaEntity entity) {
        Assert.notNull("entity", entity);

        return User.restore(
            entity.getId(),
            entity.getDisplayName(),
            entity.getTermsVersion(),
            entity.getTermsAcceptedAt(),
            entity.getStatus());
    }
}
