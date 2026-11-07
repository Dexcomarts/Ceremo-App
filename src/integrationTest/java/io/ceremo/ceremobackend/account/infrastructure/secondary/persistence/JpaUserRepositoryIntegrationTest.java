package io.ceremo.ceremobackend.account.infrastructure.secondary.persistence;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.ceremo.ceremobackend.shared.generation.domain.CeremoUUID;
import io.ceremo.ceremobackend.account.domain.exception.UserAlreadyExistsException;
import io.ceremo.ceremobackend.account.domain.model.User;
import io.ceremo.ceremobackend.account.domain.model.UserIdentity;
import io.ceremo.ceremobackend.account.domain.model.UserStatus;
import io.ceremo.ceremobackend.account.domain.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;


@DataJpaTest(showSql = false, properties = {
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true",
    "application.registration.terms-version=terms-v1",
    "spring.security.oauth2.client.provider.oidc.issuer-uri=https://identity.example/realms/ceremo",
    "spring.jpa.properties.hibernate.connection.provider_disables_autocommit=true",
    "spring.jpa.properties.hibernate.cache.use_second_level_cache=false"
})
@AutoConfigureTestDatabase(
    replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import(PostgresUserTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class JpaUserRepositoryIntegrationTest {

    private static final Instant REGISTERED_AT =
        Instant.parse("2026-10-21T12:00:00Z");

    private static final UserIdentity IDENTITY = new UserIdentity(
        "https://identity.example/realms/ceremo",
        "Person-123");

    @Autowired
    private UserRepository users;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    private TransactionTemplate transactions;

    @BeforeEach
    void prepareDatabase() {
        transactions = new TransactionTemplate(transactionManager);

        transactions.executeWithoutResult(status -> {
            entityManager.createQuery(
                "delete from UserIdentityJpaEntity").executeUpdate();

            entityManager.createQuery(
                "delete from UserJpaEntity").executeUpdate();
        });
    }

    @ParameterizedTest
    @EnumSource(UserStatus.class)
    void findsOnlyTheExactIdentityAndPreservesAccountFields(
        UserStatus accountStatus) {

        transactions.executeWithoutResult(status ->
            assertTrue(users.findByIdentity(IDENTITY).isEmpty()));

        assertRowCounts(0, 0);

        User original = newUser(accountStatus);

        transactions.executeWithoutResult(status ->
            users.create(original, IDENTITY));

        transactions.executeWithoutResult(status -> {
            User saved = users.findByIdentity(IDENTITY).orElseThrow();

            assertAll(
                () -> assertEquals(original.id(), saved.id()),
                () -> assertEquals(
                    original.displayName(), saved.displayName()),
                () -> assertEquals(
                    original.termsVersion(), saved.termsVersion()),
                () -> assertEquals(
                    original.termsAcceptedAt(), saved.termsAcceptedAt()),
                () -> assertEquals(accountStatus, saved.status()),
                () -> assertTrue(users.findByIdentity(
                    new UserIdentity(
                        "https://identity.example/realms/other",
                        IDENTITY.subject())).isEmpty()),
                () -> assertTrue(users.findByIdentity(
                    new UserIdentity(
                        "https://identity.example/realms/Ceremo",
                        IDENTITY.subject())).isEmpty()),
                () -> assertTrue(users.findByIdentity(
                    new UserIdentity(
                        IDENTITY.issuer(),
                        "person-123")).isEmpty()));
        });

        assertRowCounts(1, 1);
    }

    @Test
    void rejectsDuplicateIdentityWithoutLeavingAnotherAccount() {
        User original = newUser(UserStatus.PENDING);

        transactions.executeWithoutResult(status ->
            users.create(original, IDENTITY));

        User secondUser = newUser(UserStatus.PENDING);

        assertThrows(
            UserAlreadyExistsException.class,
            () -> transactions.executeWithoutResult(status ->
                users.create(secondUser, IDENTITY)));

        assertRowCounts(1, 1);

        transactions.executeWithoutResult(status ->
            assertEquals(
                original.id(),
                users.findByIdentity(IDENTITY).orElseThrow().id()));
    }

    @Test
    void doesNotTranslateAnAccountPrimaryKeyConflictAsDuplicateIdentity() {
        User original = newUser(UserStatus.PENDING);

        transactions.executeWithoutResult(status ->
            users.create(original, IDENTITY));

        User conflictingUser = User.restore(
            original.id(),
            "Another name",
            "terms-v1",
            REGISTERED_AT,
            UserStatus.PENDING);

        UserIdentity anotherIdentity = new UserIdentity(
            IDENTITY.issuer(),
            "another-person");

        assertThrows(
            PersistenceException.class,
            () -> transactions.executeWithoutResult(status ->
                users.create(conflictingUser, anotherIdentity)));

        assertRowCounts(1, 1);

        transactions.executeWithoutResult(status ->
            assertEquals(
                original.displayName(),
                users.findByIdentity(IDENTITY)
                    .orElseThrow()
                    .displayName()));
    }

    @Test
    void rollsBackBothRecordsWhenTheOuterOperationFails() {
        User user = newUser(UserStatus.PENDING);
        IllegalStateException failure =
            new IllegalStateException("Simulated operation failure");

        IllegalStateException thrown = assertThrows(
            IllegalStateException.class,
            () -> transactions.executeWithoutResult(status -> {
                users.create(user, IDENTITY);
                throw failure;
            }));

        assertSame(failure, thrown);
        assertRowCounts(0, 0);
    }

    @Test
    void rejectsWritesWithoutAnActiveTransaction() {
        User user = newUser(UserStatus.PENDING);

        assertThrows(
            IllegalTransactionStateException.class,
            () -> users.create(user, IDENTITY));

        assertRowCounts(0, 0);
    }

    @Test
    void doesNotTranslateAnUnrelatedCheckConstraintFailureAsDuplicateIdentity() {
        User user = newUser(UserStatus.PENDING);
        UserIdentity blocked = new UserIdentity(IDENTITY.issuer(), "blocked-by-test-policy");
        assertThrows(PersistenceException.class,
            () -> transactions.executeWithoutResult(status -> {
                entityManager.createNativeQuery("""
                    alter table user_identity add constraint ck_test_identity_policy
                    check (subject <> 'blocked-by-test-policy')
                    """).executeUpdate();
                users.create(user, blocked);
            }));
        assertRowCounts(0, 0);
    }

    private void assertRowCounts(
        long expectedUsers,
        long expectedIdentities) {

        transactions.executeWithoutResult(status -> {
            long userCount = entityManager.createQuery(
                    "select count(account) from UserJpaEntity account",
                    Long.class)
                .getSingleResult()
                .longValue();

            long identityCount = entityManager.createQuery(
                    "select count(link) from UserIdentityJpaEntity link",
                    Long.class)
                .getSingleResult()
                .longValue();

            assertAll(
                () -> assertEquals(expectedUsers, userCount),
                () -> assertEquals(expectedIdentities, identityCount));
        });
    }

    private static User newUser(UserStatus status) {
        return User.restore(
            CeremoUUID.generate(),
            "Ama Mensah",
            "terms-v1",
            REGISTERED_AT,
            status);
    }

}


