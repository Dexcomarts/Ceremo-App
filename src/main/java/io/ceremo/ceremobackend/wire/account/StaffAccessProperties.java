package io.ceremo.ceremobackend.wire.account;

import io.ceremo.ceremobackend.shared.error.domain.Assert;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Staff (super admin) actions need a sign-in no older than {@code reauthenticationMaxAge}, with no MFA step (founder
 * decision, 16 Nov 2026). The window follows GitHub sudo mode and PostHog sensitive actions (2 h). Only tokens issued to
 * one of {@code clients} (the token's {@code azp}) may perform staff actions, so an Android offline token cannot.
 * The maximum is the realm's 12-hour SSO session maximum: a longer window would mean no re-authentication at all.
 */
@ConfigurationProperties(prefix = "application.staff", ignoreUnknownFields = false)
public record StaffAccessProperties(Duration reauthenticationMaxAge, List<String> clients) {
  static final Duration MINIMUM_MAX_AGE = Duration.ofMinutes(1);
  static final Duration MAXIMUM_MAX_AGE = Duration.ofHours(12);

  public StaffAccessProperties {
    Assert.notNull("reauthenticationMaxAge", reauthenticationMaxAge);
    Assert.field("reauthenticationMaxAgeSeconds", reauthenticationMaxAge.toSeconds())
        .min(MINIMUM_MAX_AGE.toSeconds()).max(MAXIMUM_MAX_AGE.toSeconds());
    Assert.field("clients", clients).notEmpty().noNullElement();
    clients.forEach(client -> Assert.field("clients", client).notBlank());
    clients = List.copyOf(clients);
  }
}
