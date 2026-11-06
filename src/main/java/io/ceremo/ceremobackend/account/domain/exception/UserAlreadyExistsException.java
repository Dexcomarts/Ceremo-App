package io.ceremo.ceremobackend.account.domain.exception;

import io.ceremo.ceremobackend.shared.error.domain.CeremoException;
import io.ceremo.ceremobackend.shared.error.domain.ErrorStatus;

public final class UserAlreadyExistsException extends CeremoException {
    public UserAlreadyExistsException() {
        super(CeremoException.builder(UserErrorKey.USER_ALREADY_EXISTS)
            .status(ErrorStatus.CONFLICT)
            .message("A CEREMO account already exists for this identity."));
    }
}
