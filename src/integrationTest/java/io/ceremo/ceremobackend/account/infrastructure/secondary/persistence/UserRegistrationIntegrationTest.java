package io.ceremo.ceremobackend.account.infrastructure.secondary.persistence;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.ceremo.ceremobackend.shared.authentication.application.NotAuthenticatedUserException;
import io.ceremo.ceremobackend.shared.error.domain.CeremoException;
import io.ceremo.ceremobackend.account.application.UserService;
import io.ceremo.ceremobackend.account.application.port.in.UserAccounts;
import io.ceremo.ceremobackend.account.application.port.out.CurrentUser;
import io.ceremo.ceremobackend.account.domain.exception.UserAlreadyExistsException;
import io.ceremo.ceremobackend.account.domain.exception.UserErrorKey;
import io.ceremo.ceremobackend.account.domain.model.User;
import io.ceremo.ceremobackend.account.domain.model.UserIdentity;
import io.ceremo.ceremobackend.account.domain.model.UserStatus;
import io.ceremo.ceremobackend.account.domain.repository.UserRepository;
import io.ceremo.ceremobackend.account.infrastructure.primary.transaction.TransactionalUserAccounts;
import io.ceremo.ceremobackend.account.infrastructure.primary.rest.UserResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.OptimisticLockException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Exercises trusted-principal mapping and production composition; not JWT signature validation. */
@DataJpaTest(showSql = false, properties = {
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true",
    "spring.jpa.properties.hibernate.connection.provider_disables_autocommit=true",
    "spring.jpa.properties.hibernate.cache.use_second_level_cache=false",
    "application.registration.terms-version=terms-v1",
    "spring.security.oauth2.client.provider.oidc.issuer-uri=https://identity.example/realms/ceremo"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import(PostgresUserTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class UserRegistrationIntegrationTest {

    private static final String ISSUER = "https://identity.example/realms/ceremo";
    private static final UserIdentity IDENTITY = new UserIdentity(ISSUER, "Person-123");

    @Autowired private UserAccounts accounts;
    @Autowired private UserRepository users;
    @Autowired private CurrentUser currentUser;
    @Autowired private Clock clock;
    @Autowired private PlatformTransactionManager transactionManager;
    @PersistenceContext private EntityManager entityManager;

    private TransactionTemplate transactions;

    @BeforeEach
    void prepareDatabaseAndIdentity() {
        SecurityContextHolder.clearContext();
        transactions = new TransactionTemplate(transactionManager);
        transactions.executeWithoutResult(status -> {
            entityManager.createQuery("delete from UserIdentityJpaEntity").executeUpdate();
            entityManager.createQuery("delete from UserJpaEntity").executeUpdate();
        });
        authenticate(ISSUER);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void commitsRegistrationBeforeTheTopLevelCallReturns() {
        Instant before = clock.instant();
        User registered = accounts.register("Ama Mensah", "terms-v1", true);
        Instant after = clock.instant();

        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        assertAll(
            () -> assertEquals(UserStatus.PENDING, registered.status()),
            () -> assertEquals(7, registered.id().version()),
            () -> assertEquals("terms-v1", registered.termsVersion()),
            () -> assertFalse(registered.termsAcceptedAt().isBefore(before)),
            () -> assertFalse(registered.termsAcceptedAt().isAfter(after)));
        assertCounts(1, 1);
        assertEquals(registered.id(), accounts.currentUser().orElseThrow().id());
    }

    @Test
    void missingAccountLookupDoesNotCreateRecords() {
        assertTrue(accounts.currentUser().isEmpty());
        assertCounts(0, 0);
    }

    @ParameterizedTest
    @CsvSource({"terms-v1, false", "terms-v0, true"})
    void rejectsUnacceptedOrOutdatedTerms(String version, boolean accepted) {
        CeremoException failure = assertThrows(CeremoException.class,
            () -> accounts.register("Ama Mensah", version, accepted));
        assertEquals(UserErrorKey.TERMS_NOT_ACCEPTED, failure.key());
        assertCounts(0, 0);
    }

    @Test
    void rejectsUnauthenticatedCallsBeforeStorage() {
        SecurityContextHolder.clearContext();
        assertThrows(NotAuthenticatedUserException.class,
            () -> accounts.register("Ama Mensah", "terms-v1", true));
        assertThrows(NotAuthenticatedUserException.class, accounts::currentUser);
        assertCounts(0, 0);
    }

    @Test
    void rejectsForeignIssuerBeforeStorage() {
        authenticate("https://identity.example/realms/other");
        assertThrows(NotAuthenticatedUserException.class,
            () -> accounts.register("Ama Mensah", "terms-v1", true));
        assertCounts(0, 0);
    }

    @Test
    void existingAccountCannotBeReplacedByAnotherRegistration() {
        User original = accounts.register("Ama Mensah", "terms-v1", true);
        assertThrows(UserAlreadyExistsException.class,
            () -> accounts.register("Another name", "terms-v1", true));
        User saved = accounts.currentUser().orElseThrow();
        assertEquals(original.id(), saved.id());
        assertEquals("Ama Mensah", saved.displayName());
        assertCounts(1, 1);
    }

    @Test
    void joinedOperationRollsBackBothFlushedRecords() {
        IllegalStateException failure = new IllegalStateException("Operation failed after registration");
        UserAccounts failingOperation = new UserAccounts() {
            @Override public Optional<User> currentUser() { return accounts.currentUser(); }
            @Override public User activate() { return accounts.activate(); }
            @Override public User register(String name, String version, boolean accepted) {
                accounts.register(name, version, accepted);
                throw failure;
            }
        };
        UserAccounts outer = new TransactionalUserAccounts(failingOperation, transactionManager);
        assertSame(failure, assertThrows(IllegalStateException.class,
            () -> outer.register("Ama Mensah", "terms-v1", true)));
        assertCounts(0, 0);
    }

    @Test
    void suppliesReadOnlyLookupAndWriteRegistrationTransactions() {
        UserRepository observed = new UserRepository() {
            @Override public Optional<User> findByIdentity(UserIdentity identity) {
                assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
                assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
                return users.findByIdentity(identity);
            }
            @Override public void create(User user, UserIdentity identity) {
                throw new AssertionError("A lookup must not create an account");
            }
            @Override public void updateStatus(User user) { users.updateStatus(user); }
        };
        UserAccounts lookup = new TransactionalUserAccounts(
            new UserService(observed, currentUser, clock, "terms-v1"), transactionManager);
        assertTrue(lookup.currentUser().isEmpty());

        UserRepository writeObserved = new UserRepository() {
            @Override public Optional<User> findByIdentity(UserIdentity identity) {
                assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
                return users.findByIdentity(identity);
            }
            @Override public void create(User user, UserIdentity identity) {
                assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
                assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
                users.create(user, identity);
            }
            @Override public void updateStatus(User user) { users.updateStatus(user); }
        };
        new TransactionalUserAccounts(
            new UserService(writeObserved, currentUser, clock, "terms-v1"), transactionManager)
            .register("Ama Mensah", "terms-v1", true);
        assertCounts(1, 1);
    }

    @Test
    @Timeout(45)
    void concurrentRegistrationsCommitOnlyOneAccountAndKeepItsIdentity() throws Exception {
        CyclicBarrier bothLookupsComplete = new CyclicBarrier(2);
        UserRepository coordinated = new UserRepository() {
            @Override public Optional<User> findByIdentity(UserIdentity identity) {
                Optional<User> found = users.findByIdentity(identity);
                assertTrue(found.isEmpty());
                try {
                    bothLookupsComplete.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Registration coordination interrupted", exception);
                } catch (Exception exception) {
                    throw new IllegalStateException("Both registration lookups must finish", exception);
                }
                return found;
            }
            @Override public void create(User user, UserIdentity identity) {
                users.create(user, identity);
            }
            @Override public void updateStatus(User user) { users.updateStatus(user); }
        };
        UserAccounts competing = new TransactionalUserAccounts(
            new UserService(coordinated, currentUser, clock, "terms-v1"), transactionManager);

        ExecutorService workers = Executors.newFixedThreadPool(2);
        Optional<User> first;
        Optional<User> second;
        try {
            var firstRequest = workers.submit(() -> attemptRegistration(competing, "Ama Mensah"));
            var secondRequest = workers.submit(() -> attemptRegistration(competing, "Kojo Mensah"));
            first = firstRequest.get(20, TimeUnit.SECONDS);
            second = secondRequest.get(20, TimeUnit.SECONDS);
        } finally {
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS));
        }

        assertEquals(1, (first.isPresent() ? 1 : 0) + (second.isPresent() ? 1 : 0));
        User winner = first.isPresent() ? first.orElseThrow() : second.orElseThrow();
        assertCounts(1, 1);
        transactions.executeWithoutResult(status -> {
            User saved = users.findByIdentity(IDENTITY).orElseThrow();
            assertEquals(winner.id(), saved.id());
            assertEquals(winner.displayName(), saved.displayName());
        });
    }

    @Test
    void verifiedRegistrationCommitsAnActiveAccount() {
        authenticateVerified();
        User registered = accounts.register("Ama Mensah", "terms-v1", true);
        assertEquals(UserStatus.ACTIVE, registered.status());
        assertEquals(UserStatus.ACTIVE, accounts.currentUser().orElseThrow().status());
        assertCounts(1, 1);
    }

    @Test
    void activationCommitsAndRetriesWithoutAnotherVersionChange() {
        User pending = accounts.register("Ama Mensah", "terms-v1", true);
        authenticateVerified();

        User active = accounts.activate();

        assertEquals(pending.id(), active.id());
        assertEquals(UserStatus.ACTIVE, accounts.currentUser().orElseThrow().status());
        long version = transactions.execute(status -> entityManager.find(
            UserJpaEntity.class, active.id()).getVersion());
        accounts.activate();
        long afterRetry = transactions.execute(status -> entityManager.find(
            UserJpaEntity.class, active.id()).getVersion());
        assertEquals(1L, version);
        assertEquals(version, afterRetry);
    }

    @Test
    @Timeout(45)
    void concurrentRestrictionCannotBeOverwrittenByActivation() throws Exception {
        User pending = accounts.register("Ama Mensah", "terms-v1", true);
        CountDownLatch accountRead = new CountDownLatch(1);
        CountDownLatch restrictionCommitted = new CountDownLatch(1);
        UserRepository coordinated = new UserRepository() {
            @Override public Optional<User> findByIdentity(UserIdentity identity) {
                Optional<User> found = users.findByIdentity(identity);
                accountRead.countDown();
                try {
                    assertTrue(restrictionCommitted.await(10, TimeUnit.SECONDS));
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Activation coordination interrupted", exception);
                }
                return found;
            }
            @Override public void create(User user, UserIdentity identity) {
                throw new AssertionError("Activation must not create an account");
            }
            @Override public void updateStatus(User user) { users.updateStatus(user); }
        };
        UserAccounts competing = new TransactionalUserAccounts(
            new UserService(coordinated, currentUser, clock, "terms-v1"), transactionManager);
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            var activation = worker.submit(() -> {
                authenticateVerified();
                try {
                    return competing.activate();
                } finally {
                    SecurityContextHolder.clearContext();
                }
            });
            assertTrue(accountRead.await(10, TimeUnit.SECONDS));
            transactions.executeWithoutResult(status -> assertEquals(1,
                entityManager.createQuery("update UserJpaEntity account set account.status = :status, "
                        + "account.version = account.version + 1 where account.id = :id")
                    .setParameter("status", UserStatus.SUSPENDED)
                    .setParameter("id", pending.id()).executeUpdate()));
            restrictionCommitted.countDown();

            ExecutionException failure = assertThrows(ExecutionException.class,
                () -> activation.get(20, TimeUnit.SECONDS));
            assertTrue(failure.getCause() instanceof OptimisticLockException
                || failure.getCause() instanceof OptimisticLockingFailureException);
        } finally {
            restrictionCommitted.countDown();
            worker.shutdownNow();
            assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
        }
        assertEquals(UserStatus.SUSPENDED, accounts.currentUser().orElseThrow().status());
        assertCounts(1, 1);
    }

    @Test
    void requestToPostgresCommitsBeforeReturningCreated() throws Exception {
        authenticateVerified();
        var mvc = MockMvcBuilders.standaloneSetup(new UserResource(accounts)).build();

        // Authentication was established above; bearer validation is exercised by the HTTP/security suite.
        mvc.perform(post("/api/v1/users/me").contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"Ama Mensah\",\"termsVersion\":\"terms-v1\",\"termsAccepted\":true}"))
            .andExpect(status().isCreated());

        assertCounts(1, 1);
        assertEquals(UserStatus.ACTIVE, accounts.currentUser().orElseThrow().status());
    }

    private static void authenticateVerified() {
        Jwt jwt = Jwt.withTokenValue("synthetic-verified-token")
            .header("alg", "RS256").claim("iss", ISSUER).claim("sub", IDENTITY.subject())
            .claim("email_verified", true).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    private Optional<User> attemptRegistration(UserAccounts competing, String name) {
        authenticate(ISSUER);
        try {
            return Optional.of(competing.register(name, "terms-v1", true));
        } catch (UserAlreadyExistsException expectedConflict) {
            return Optional.empty();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void assertCounts(long expectedUsers, long expectedIdentities) {
        transactions.executeWithoutResult(status -> assertAll(
            () -> assertEquals(expectedUsers, entityManager.createQuery(
                "select count(account) from UserJpaEntity account", Long.class).getSingleResult().longValue()),
            () -> assertEquals(expectedIdentities, entityManager.createQuery(
                "select count(link) from UserIdentityJpaEntity link", Long.class).getSingleResult().longValue())));
    }

    private static void authenticate(String issuer) {
        Jwt jwt = Jwt.withTokenValue("synthetic-test-token")
            .header("alg", "RS256").claim("iss", issuer).claim("sub", IDENTITY.subject()).build();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        SecurityContextHolder.setContext(context);
    }
}
