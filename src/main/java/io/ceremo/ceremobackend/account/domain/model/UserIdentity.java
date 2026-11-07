package io.ceremo.ceremobackend.account.domain.model;

import io.ceremo.ceremobackend.shared.error.domain.Assert;

public record UserIdentity(String issuer, String subject) {
    public  UserIdentity {
        Assert.field("issuer", issuer).notBlank().maxLength(512);
        Assert.field("subject", subject).notBlank().maxLength(255);
    }
}
