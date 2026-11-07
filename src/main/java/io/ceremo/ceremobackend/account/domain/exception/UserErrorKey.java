package io.ceremo.ceremobackend.account.domain.exception;

import io.ceremo.ceremobackend.shared.error.domain.ErrorKey;

public enum UserErrorKey implements ErrorKey {
    USER_ALREADY_EXISTS("user-already-exists"),
    TERMS_NOT_ACCEPTED("terms-not-accepted"),
    USER_NOT_FOUND("user-not-found"),
    EMAIL_NOT_VERIFIED("email-not-verified"),
    ACCOUNT_RESTRICTED("account-restricted");
    
    private final String key;

    UserErrorKey(String key) {
        this.key = key;
    }

    @Override
    public String get() {
        return key;
    }
}
