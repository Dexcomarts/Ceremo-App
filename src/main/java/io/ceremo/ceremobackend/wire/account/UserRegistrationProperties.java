package io.ceremo.ceremobackend.wire.account;

import io.ceremo.ceremobackend.shared.error.domain.Assert;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The current terms version. Unset or blank keeps the application running with registration closed (founder decision,
 * 15 Nov 2026), as GitLab treats terms as an optional setting and Mastodon refuses only account creation (403) while
 * registrations are closed. A configured value that is too long still fails startup: that is a typo, not "not yet set".
 */
@ConfigurationProperties(prefix = "application.registration", ignoreUnknownFields = false)
public record UserRegistrationProperties(String termsVersion) {

    public UserRegistrationProperties {
        termsVersion = termsVersion == null || termsVersion.isBlank() ? null : termsVersion;
        if (termsVersion != null) {
            Assert.field("termsVersion", termsVersion).maxLength(40);
        }
    }
}
