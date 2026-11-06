package io.ceremo.ceremobackend.account.infrastructure.secondary.persistence;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.ceremo.ceremobackend.shared.generation.domain.CeremoUUID;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import java.util.stream.Stream;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Original CEREMO checks; evidence and API versions are in the database research memory. */
class UserSchemaIntegrationTest {

    private static final String ISSUER = "https://identity.example/realms/ceremo";
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.0");

    private static Flyway flyway;
    private Connection connection;

    @BeforeAll
    static void startDatabaseAndMigrate() {
        POSTGRES.start();
        flyway = Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .locations("classpath:db/migration")
            .load();
        flyway.migrate();
    }

    @AfterAll
    static void stopDatabase() {
        POSTGRES.stop();
    }

    @BeforeEach
    void openTransaction() throws SQLException {
        connection = DriverManager.getConnection(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        connection.setAutoCommit(false);
    }

    @AfterEach
    void rollbackAndClose() throws SQLException {
        if (connection != null) {
            try {
                connection.rollback();
            } finally {
                connection.close();
            }
        }
    }

    @Test
    void validatesHistoryAndDoesNotReapplyMigrations() {
        assertTrue(flyway.info().applied().length > 0);
        assertTrue(flyway.validateWithResult().validationSuccessful);
        assertEquals(0, flyway.migrate().migrationsExecuted);
    }

    @Test
    void rejectsAnIdentityAlreadyLinkedToAnotherAccount() throws SQLException {
        UUID firstUser = insertUser();
        UUID secondUser = insertUser();
        insertIdentity(firstUser, ISSUER, "person-123");

        SQLException error = assertThrows(
            SQLException.class,
            () -> insertIdentity(secondUser, ISSUER, "person-123"));

        assertEquals("23505", error.getSQLState());
    }

    @ParameterizedTest
    @CsvSource({
        "https://identity.example/realms/ceremo, person-456",
        "https://identity.example/realms/other, person-123",
        "https://identity.example/realms/ceremo, Person-123",
        "https://identity.example/realms/Ceremo, person-123"
    })
    void allowsDistinctExternalIdentities(String secondIssuer, String secondSubject)
        throws SQLException {

        insertIdentity(insertUser(), ISSUER, "person-123");
        insertIdentity(insertUser(), secondIssuer, secondSubject);

        try (var statement = connection.prepareStatement("SELECT count(*) FROM user_identity");
             var result = statement.executeQuery()) {
            assertTrue(result.next());
            assertEquals(2L, result.getLong(1));
        }
    }

    @Test
    void rejectsIdentityWithoutAnExistingAccount() {
        SQLException error = assertThrows(
            SQLException.class,
            () -> insertIdentity(CeremoUUID.generate(), ISSUER, "person-123"));

        assertEquals("23503", error.getSQLState());
    }

    @Test
    void preventsDeletingAnAccountThatStillHasAnIdentity() throws SQLException {
        UUID userId = insertUser();
        insertIdentity(userId, ISSUER, "person-123");

        try (var statement = connection.prepareStatement("DELETE FROM app_user WHERE id = ?")) {
            statement.setObject(1, userId);
            SQLException error = assertThrows(SQLException.class, statement::executeUpdate);
            assertEquals("23001", error.getSQLState());
        }
    }

    @ParameterizedTest
    @MethodSource("invalidIdentityValues")
    void rejectsMissingOrBlankIdentityValues(String issuer, String subject, String sqlState)
        throws SQLException {

        UUID userId = insertUser();
        SQLException error = assertThrows(
            SQLException.class, () -> insertIdentity(userId, issuer, subject));

        assertEquals(sqlState, error.getSQLState());
    }

    static Stream<Arguments> invalidIdentityValues() {
        return Stream.of(
            Arguments.of(null, "person-123", "23502"),
            Arguments.of(ISSUER, null, "23502"),
            Arguments.of(" ", "person-123", "23514"),
            Arguments.of(ISSUER, " ", "23514"));
    }

    @Test
    void rejectsUnknownAccountStatus() throws SQLException {
        try (var statement = connection.prepareStatement("""
            INSERT INTO app_user (id, display_name, terms_version, terms_accepted_at, status)
            VALUES (?, 'Ama Mensah', 'terms-v1', CURRENT_TIMESTAMP, 'UNKNOWN')
            """)) {
            statement.setObject(1, CeremoUUID.generate());
            SQLException error = assertThrows(SQLException.class, statement::executeUpdate);
            assertEquals("23514", error.getSQLState());
        }
    }

    @Test
    void providesInheritedAuditAndVersionDefaults() throws SQLException {
        UUID userId = insertUser();
        insertIdentity(userId, ISSUER, "person-123");

        try (var statement = connection.prepareStatement("""
            SELECT u.created_at, u.updated_at, u.version,
                   i.created_at AS identity_created_at, i.updated_at AS identity_updated_at,
                   i.version AS identity_version
            FROM app_user u JOIN user_identity i ON i.user_id = u.id
            WHERE u.id = ?
            """)) {
            statement.setObject(1, userId);
            try (var result = statement.executeQuery()) {
                assertTrue(result.next());
                // Read JDBC values before assertAll so SQLExceptions are reported directly.
                var createdAt = result.getTimestamp("created_at");
                var updatedAt = result.getTimestamp("updated_at");
                long version = result.getLong("version");
                var identityCreatedAt = result.getTimestamp("identity_created_at");
                var identityUpdatedAt = result.getTimestamp("identity_updated_at");
                long identityVersion = result.getLong("identity_version");
                assertAll(
                    () -> assertNotNull(createdAt),
                    () -> assertNotNull(updatedAt),
                    () -> assertEquals(0L, version),
                    () -> assertNotNull(identityCreatedAt),
                    () -> assertNotNull(identityUpdatedAt),
                    () -> assertEquals(0L, identityVersion));
            }
        }
    }

    private UUID insertUser() throws SQLException {
        UUID id = CeremoUUID.generate();
        try (var statement = connection.prepareStatement("""
            INSERT INTO app_user (id, display_name, terms_version, terms_accepted_at, status)
            VALUES (?, 'Ama Mensah', 'terms-v1', CURRENT_TIMESTAMP, 'PENDING')
            """)) {
            statement.setObject(1, id);
            statement.executeUpdate();
        }
        return id;
    }

    private void insertIdentity(UUID userId, String issuer, String subject) throws SQLException {
        try (var statement = connection.prepareStatement("""
            INSERT INTO user_identity (id, user_id, issuer, subject, last_seen_at)
            VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
            """)) {
            statement.setObject(1, CeremoUUID.generate());
            statement.setObject(2, userId);
            statement.setString(3, issuer);
            statement.setString(4, subject);
            statement.executeUpdate();
        }
    }
}
