package io.ceremo.ceremobackend.account.domain.model;

import io.ceremo.ceremobackend.shared.error.domain.Assert;
import io.ceremo.ceremobackend.shared.error.domain.CeremoException;
import io.ceremo.ceremobackend.shared.error.domain.ErrorStatus;
import io.ceremo.ceremobackend.account.domain.exception.UserErrorKey;

import java.time.Instant;
import java.util.UUID;

public final class User {
    private final UUID id;
    private final String displayName;
    private final String termsVersion;
    private final Instant termsAcceptedAt;
    private final UserStatus status;

    private User(
        UUID id,
        String displayName,
        String termsVersion,
        Instant termsAcceptedAt,
        UserStatus status){

        Assert.notNull("id", id);
        Assert.field("displayName",displayName).notBlank().maxLength(120);
        Assert.field("termsVersion", termsVersion).notBlank().maxLength(40);
        Assert.notNull("termsAcceptedAt", termsAcceptedAt);
        Assert.notNull("status", status);

        this.id =id;
        this.displayName = displayName;
        this.termsVersion = termsVersion;
        this.termsAcceptedAt = termsAcceptedAt;
        this.status = status;
    }


    public static User register (
        UUID id,
        String displayName,
        String termsVersion,
        Instant termsAcceptedAt
    ){
        return new User(
            id,
            displayName,
            termsVersion,
            termsAcceptedAt,
            UserStatus.PENDING);
    }



    public static User restore(
        UUID id,
        String displayName,
        String termsVersion,
        Instant termsAcceptedAt,
        UserStatus status) {

        return new User(
            id,
            displayName,
            termsVersion,
            termsAcceptedAt,
            status);
    }





    public User activate() {
        if (status == UserStatus.ACTIVE) {
            return this;
        }
        if (status != UserStatus.PENDING) {
            throw CeremoException.builder(UserErrorKey.ACCOUNT_RESTRICTED)
                .status(ErrorStatus.FORBIDDEN)
                .message("This account cannot be activated.")
                .build();
        }
        return new User(id, displayName, termsVersion, termsAcceptedAt, UserStatus.ACTIVE);
    }

    public UUID id(){
        return  id;
    }

    public String displayName() {
        return displayName;
    }

    public String termsVersion() {
        return termsVersion;
    }

    public Instant termsAcceptedAt() {
        return termsAcceptedAt;
    }

    public UserStatus status() {
        return status;
    }



    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof User user)) {
            return false;
        }

        return id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
