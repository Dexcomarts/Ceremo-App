package io.ceremo.ceremobackend.logout.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.ceremo.ceremobackend.logout.domain.repository.SessionRevocations;
import io.ceremo.ceremobackend.shared.error.domain.AssertionException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RevocationRetentionServiceTest {
  private final SessionRevocations revocations = mock(SessionRevocations.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-11-14T13:00:00Z"), ZoneOffset.UTC);

  @Test
  void purgesOnlyRevocationsRecordedBeforeTheRetentionWindow() {
    Instant cutoff = Instant.parse("2026-08-14T13:00:00Z");
    when(revocations.purgeRevokedBefore(cutoff)).thenReturn(3);
    RevocationRetentionService service = new RevocationRetentionService(revocations, clock, Duration.ofDays(92));
    assertThat(service.purgeExpired()).isEqualTo(3);
    verify(revocations).purgeRevokedBefore(cutoff);
  }

  @Test
  void rejectsARetentionThatWouldPutTheCutoffInTheFuture() {
    assertThatThrownBy(() -> new RevocationRetentionService(revocations, clock, Duration.ZERO))
        .isInstanceOf(AssertionException.class);
    assertThatThrownBy(() -> new RevocationRetentionService(revocations, clock, Duration.ofDays(-1)))
        .isInstanceOf(AssertionException.class);
  }

  @Test
  void rejectsMissingCollaborators() {
    Duration retention = Duration.ofDays(92);
    assertThatThrownBy(() -> new RevocationRetentionService(null, clock, retention)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new RevocationRetentionService(revocations, null, retention)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new RevocationRetentionService(revocations, clock, null)).isInstanceOf(NullPointerException.class);
  }
}
