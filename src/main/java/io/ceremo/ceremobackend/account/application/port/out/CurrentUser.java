package io.ceremo.ceremobackend.account.application.port.out;

import io.ceremo.ceremobackend.account.domain.model.UserIdentity;

public interface CurrentUser {

    /**
     * Returns the identity from validated authentication.
     *
     * Implementations must reject unauthenticated callers and must
     * never return null or use a caller-supplied identity.
     */
    UserIdentity requireIdentity();

    /** True only when validated authentication contains the boolean email_verified claim. */
    boolean isEmailVerified();
}
