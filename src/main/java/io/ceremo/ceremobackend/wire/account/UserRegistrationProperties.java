package io.ceremo.ceremobackend.wire.account;

import io.ceremo.ceremobackend.shared.error.domain.Assert;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application.registration", ignoreUnknownFields = false)
public record UserRegistrationProperties(String termsVersion) {

    public UserRegistrationProperties {
        Assert.field("termsVersion", termsVersion).notBlank().maxLength(40);
    }
}
