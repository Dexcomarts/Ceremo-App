package io.ceremo.ceremobackend.account.infrastructure.secondary.persistence;

import io.ceremo.ceremobackend.shared.domain.BaseEntity;
import io.ceremo.ceremobackend.account.domain.model.UserStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_user")
class UserJpaEntity extends BaseEntity {

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "terms_version", nullable = false, length = 40)
    private String termsVersion;

    @Column(name = "terms_accepted_at", nullable = false)
    private Instant termsAcceptedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    protected UserJpaEntity() {}

    UserJpaEntity(
        UUID id,
        String displayName,
        String termsVersion,
        Instant termsAcceptedAt,
        UserStatus status) {

        super(id);
        this.displayName = displayName;
        this.termsVersion = termsVersion;
        this.termsAcceptedAt = termsAcceptedAt;
        this.status = status;
    }

    String getDisplayName() {
        return displayName;
    }

    String getTermsVersion() {
        return termsVersion;
    }

    Instant getTermsAcceptedAt() {
        return termsAcceptedAt;
    }

    UserStatus getStatus() {
        return status;
    }

    void updateStatus(UserStatus status) {
        this.status = status;
    }
}
