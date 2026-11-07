package io.ceremo.ceremobackend.account.infrastructure.secondary.persistence;

import io.ceremo.ceremobackend.shared.domain.BaseEntity;
import io.ceremo.ceremobackend.shared.error.domain.Assert;
import io.ceremo.ceremobackend.account.domain.model.UserIdentity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "user_identity",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_user_identity_issuer_subject",
        columnNames = {"issuer", "subject"}))
class UserIdentityJpaEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        updatable = false,
        foreignKey = @ForeignKey(name = "fk_user_identity_user"))
    private UserJpaEntity user;

    @Column(
        name = "issuer",
        nullable = false,
        updatable = false,
        length = 512)
    private String issuer;

    @Column(
        name = "subject",
        nullable = false,
        updatable = false,
        length = 255)
    private String subject;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    protected UserIdentityJpaEntity() {}

    UserIdentityJpaEntity(
        UUID id,
        UserJpaEntity user,
        UserIdentity identity,
        Instant lastSeenAt) {

        super(id);

        Assert.notNull("id", id);
        Assert.notNull("user", user);
        Assert.notNull("userId", user.getId());
        Assert.notNull("identity", identity);
        Assert.notNull("lastSeenAt", lastSeenAt);

        this.user = user;
        this.issuer = identity.issuer();
        this.subject = identity.subject();
        this.lastSeenAt = lastSeenAt;
    }

    UserJpaEntity getUser() {
        return user;
    }

    String getIssuer() {
        return issuer;
    }

    String getSubject() {
        return subject;
    }

    Instant getLastSeenAt() {
        return lastSeenAt;
    }
}
