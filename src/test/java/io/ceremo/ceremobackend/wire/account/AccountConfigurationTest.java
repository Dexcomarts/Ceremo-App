package io.ceremo.ceremobackend.wire.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.ceremo.ceremobackend.shared.error.domain.AssertionException;
import io.ceremo.ceremobackend.account.application.UserService;
import io.ceremo.ceremobackend.account.application.port.in.UserAccounts;
import io.ceremo.ceremobackend.account.application.port.out.CurrentUser;
import io.ceremo.ceremobackend.account.infrastructure.primary.transaction.TransactionalUserAccounts;
import jakarta.persistence.EntityManagerFactory;
import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.PlatformTransactionManager;

class AccountConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(AccountConfiguration.class)
        .withBean(EntityManagerFactory.class, () -> mock(EntityManagerFactory.class))
        .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
        .withBean(CurrentUser.class, () -> mock(CurrentUser.class));

    @Test
    void exposesOneTransactionalInboundBeanAndTheConfiguredTerms() {
        runner.withPropertyValues("application.registration.terms-version=terms-v1")
            .run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(UserAccounts.class);
                assertThat(context.getBean(UserAccounts.class))
                    .isInstanceOf(TransactionalUserAccounts.class);
                assertThat(context).doesNotHaveBean(UserService.class);
                assertThat(context).hasSingleBean(
                    io.ceremo.ceremobackend.account.application.port.in.Accounts.class);
                assertThat(context).hasSingleBean(
                    io.ceremo.ceremobackend.account.application.port.out.StaffAccess.class);
                assertThat(context.getBean(UserRegistrationProperties.class).termsVersion())
                    .isEqualTo("terms-v1");
                assertThat(context.getBean(Clock.class).getZone()).isEqualTo(ZoneOffset.UTC);
            });
    }

    @Test
    void rejectsMissingTermsConfiguration() {
        runner.run(context -> assertThat(context).hasFailed()
            .getFailure().hasRootCauseInstanceOf(AssertionException.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "this-terms-version-is-longer-than-forty-characters"})
    void rejectsBlankOrOversizedTermsConfiguration(String version) {
        runner.withPropertyValues("application.registration.terms-version=" + version)
            .run(context -> assertThat(context).hasFailed()
                .getFailure().hasRootCauseInstanceOf(AssertionException.class));
    }
}
