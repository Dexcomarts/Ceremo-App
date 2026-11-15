package io.ceremo.ceremobackend.administration.infrastructure.secondary.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.ceremo.ceremobackend.shared.generation.domain.CeremoUUID;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;
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
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

class OccasionTypeSchemaIntegrationTest {

    private static final PostgreSQLContainer POSTGRES =
        new PostgreSQLContainer("postgres:18.0");

    private static Flyway flyway;

    private Connection connection;

    @BeforeAll
    static void startDatabaseAndMigrate() {
        POSTGRES.start();

        flyway = Flyway.configure()
            .dataSource(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
            )
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
            POSTGRES.getJdbcUrl(),
            POSTGRES.getUsername(),
            POSTGRES.getPassword()
        );

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
        assertTrue(flyway.validateWithResult().validationSuccessful);
        assertEquals(0, flyway.migrate().migrationsExecuted);
    }

    @Test
    void storesAnInactiveOccasionWithAuditDefaults() throws SQLException {
        UUID id = CeremoUUID.generate();

        try (var statement = connection.prepareStatement("""
            INSERT INTO occasion_type (id, code, name, active)
            VALUES (?, ?, ?, ?)
            RETURNING id, code, name, description, active,
                      created_at, updated_at, created_by, updated_by, version
            """)) {

            statement.setObject(1, id);
            statement.setString(2, "TEST_OCCASION");
            statement.setString(3, "Test occasion");
            statement.setBoolean(4, false);

            try (var result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals(id, result.getObject("id", UUID.class));
                assertEquals("TEST_OCCASION", result.getString("code"));
                assertEquals("Test occasion", result.getString("name"));
                assertNull(result.getString("description"));
                assertFalse(result.getBoolean("active"));
                assertNotNull(result.getTimestamp("created_at"));
                assertNotNull(result.getTimestamp("updated_at"));
                assertNull(result.getString("created_by"));
                assertNull(result.getString("updated_by"));
                assertEquals(0L, result.getLong("version"));
                assertFalse(result.next());
            }
        }
    }

    @Test
    void acceptsValuesAtTheirLengthLimits() throws SQLException {
        UUID id = CeremoUUID.generate();

        insertOccasion(
            id,
            "C".repeat(60),
            "N".repeat(120),
            "D".repeat(1000),
            true,
            0L
        );

        try (var statement = connection.prepareStatement("""
            SELECT length(code), length(name), length(description)
            FROM occasion_type
            WHERE id = ?
            """)) {

            statement.setObject(1, id);

            try (var result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals(60, result.getInt(1));
                assertEquals(120, result.getInt(2));
                assertEquals(1000, result.getInt(3));
            }
        }
    }

    @Test
    void rejectsADuplicateId() throws SQLException {
        UUID id = CeremoUUID.generate();

        insertOccasion(id, "FIRST", "First occasion", null, true, 0L);

        SQLException error = assertThrows(
            SQLException.class,
            () -> insertOccasion(
                id, "SECOND", "Second occasion", null, true, 0L
            )
        );

        assertEquals("23505", error.getSQLState());
    }

    @Test
    void rejectsACodeAlreadyUsedByAnInactiveOccasion() throws SQLException {
        insertOccasion(
            CeremoUUID.generate(),
            "TEST_OCCASION",
            "Retired occasion",
            null,
            false,
            0L
        );

        SQLException error = assertThrows(
            SQLException.class,
            () -> insertOccasion(
                CeremoUUID.generate(),
                "TEST_OCCASION",
                "Another occasion",
                null,
                true,
                0L
            )
        );

        assertEquals("23505", error.getSQLState());
    }

    @ParameterizedTest
    @MethodSource("invalidOccasions")
    void rejectsInvalidValues(
        UUID id,
        String code,
        String name,
        String description,
        Boolean active,
        Long version,
        String expectedSqlState
    ) {
        SQLException error = assertThrows(
            SQLException.class,
            () -> insertOccasion(
                id, code, name, description, active, version
            )
        );

        assertEquals(expectedSqlState, error.getSQLState());
    }

    static Stream<Arguments> invalidOccasions() {
        UUID id = UUID.fromString(
            "019a67b0-0000-7000-8000-000000000001"
        );

        return Stream.of(
            Arguments.of(null, "TEST", "Test", null, true, 0L, "23502"),
            Arguments.of(id, null, "Test", null, true, 0L, "23502"),
            Arguments.of(id, "TEST", null, null, true, 0L, "23502"),
            Arguments.of(id, "TEST", "Test", null, null, 0L, "23502"),
            Arguments.of(id, "TEST", "Test", null, true, null, "23502"),
            Arguments.of(id, "", "Test", null, true, 0L, "23514"),
            Arguments.of(id, "   ", "Test", null, true, 0L, "23514"),
            Arguments.of(id, "\t", "Test", null, true, 0L, "23514"),
            Arguments.of(id, "TEST", "", null, true, 0L, "23514"),
            Arguments.of(id, "TEST", "\n", null, true, 0L, "23514"),
            Arguments.of(
                id, "C".repeat(61), "Test", null, true, 0L, "22001"
            ),
            Arguments.of(
                id, "TEST", "N".repeat(121), null, true, 0L, "22001"
            ),
            Arguments.of(
                id, "TEST", "Test", "D".repeat(1001), true, 0L, "22001"
            ),
            Arguments.of(id, "TEST", "Test", null, true, -1L, "23514")
        );
    }

    @Test
    void rollsBackAnUncommittedOccasion() throws SQLException {
        UUID id = CeremoUUID.generate();

        insertOccasion(id, "TEST_OCCASION", "Test occasion", null, true, 0L);

        connection.rollback();

        try (var statement = connection.prepareStatement("""
            SELECT count(*)
            FROM occasion_type
            WHERE id = ?
            """)) {

            statement.setObject(1, id);

            try (var result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals(0L, result.getLong(1));
            }
        }
    }

    private void insertOccasion(
        UUID id,
        String code,
        String name,
        String description,
        Boolean active,
        Long version
    ) throws SQLException {

        try (var statement = connection.prepareStatement("""
            INSERT INTO occasion_type (
                id, code, name, description, active, version
            )
            VALUES (?, ?, ?, ?, ?, ?)
            """)) {

            statement.setObject(1, id);
            statement.setString(2, code);
            statement.setString(3, name);
            statement.setString(4, description);
            statement.setObject(5, active, Types.BOOLEAN);
            statement.setObject(6, version, Types.BIGINT);

            assertEquals(1, statement.executeUpdate());
        }
    }
}