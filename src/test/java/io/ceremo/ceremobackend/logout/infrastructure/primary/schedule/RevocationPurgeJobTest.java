package io.ceremo.ceremobackend.logout.infrastructure.primary.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.ceremo.ceremobackend.logout.application.port.in.RevocationRetention;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

class RevocationPurgeJobTest {
  private final RevocationRetention retention = mock(RevocationRetention.class);

  @Test
  void registersThePurgeOnTheValidatedCronAndRunsTheRetentionUseCase() {
    ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();
    new RevocationPurgeJob(retention, "0 */15 * * * *").configureTasks(registrar);
    assertThat(registrar.getCronTaskList()).singleElement()
        .satisfies(task -> assertThat(task.getExpression()).isEqualTo("0 */15 * * * *"));
    when(retention.purgeExpired()).thenReturn(2, 0);
    registrar.getCronTaskList().getFirst().getRunnable().run();
    new RevocationPurgeJob(retention, "-").purge();
    verify(retention, times(2)).purgeExpired();
  }

  @Test
  void registersNothingWhenDisabled() {
    ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();
    new RevocationPurgeJob(retention, "-").configureTasks(registrar);
    assertThat(registrar.getCronTaskList()).isEmpty();
  }

  @Test
  void letsTheSchedulerSeeFailuresSoTheNextRunRetries() {
    when(retention.purgeExpired()).thenThrow(new IllegalStateException("Database unavailable"));
    assertThatThrownBy(() -> new RevocationPurgeJob(retention, "-").purge()).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void rejectsMissingCollaborators() {
    assertThatThrownBy(() -> new RevocationPurgeJob(null, "-")).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new RevocationPurgeJob(retention, null)).isInstanceOf(NullPointerException.class);
  }
}
