package com.grash.integration;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Exercise the real upgrade against legacy rows, in an isolated PostgreSQL schema. */
@ResourceLock("cmms-test-db")
class UserInvitationMigrationIntegrationTest {
    @BeforeAll
    static void startDatabase() {
        AbstractTestContainer.startContainer();
    }

    @Test
    void upgrade_revokesAllLegacyInvitationsAndPreservesAuditWhenRoleIsRemoved() throws Exception {
        PostgreSQLContainer<?> postgres = AbstractTestContainer.getPostgresqlContainer();
        String schema = "invitation_migration_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = connect(postgres); Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
            statement.execute("SET search_path TO " + schema);
            statement.execute("CREATE TABLE company (id BIGINT PRIMARY KEY, company_settings_id BIGINT)");
            statement.execute("CREATE TABLE role (id BIGINT PRIMARY KEY, company_settings_id BIGINT)");
            statement.execute("CREATE TABLE own_user (id BIGINT PRIMARY KEY, company_id BIGINT)");
            statement.execute("CREATE TABLE user_invitation (id BIGINT PRIMARY KEY, email VARCHAR(255), " +
                    "role_id BIGINT NOT NULL, created_by BIGINT, " +
                    "CONSTRAINT fkf77hi4ixclj9edbkfheps1sv9 FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE CASCADE)");
            statement.execute("INSERT INTO company VALUES (1, 11), (2, 22)");
            statement.execute("INSERT INTO role VALUES (10, 11), (20, NULL)");
            statement.execute("INSERT INTO own_user VALUES (100, 2)");
            statement.execute("INSERT INTO user_invitation VALUES " +
                    "(1, 'custom@example.com', 10, 100), " +
                    "(2, 'global@example.com', 20, 100), " +
                    "(3, 'orphan@example.com', 20, 999)");

            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            database.setDefaultSchemaName(schema);
            database.setLiquibaseSchemaName(schema);
            try (Liquibase liquibase = new Liquibase(
                    "db/changelog/2026_10_18_00000000001_secure_user_invitations.xml",
                    new ClassLoaderResourceAccessor(), database)) {
                liquibase.update(new Contexts(), new LabelExpression());
                connection.setAutoCommit(true);
                try (ResultSet legacy = statement.executeQuery("SELECT * FROM user_invitation ORDER BY id")) {
                    for (long id = 1; id <= 3; id++) {
                        assertTrue(legacy.next());
                        assertEquals(id, legacy.getLong("id"));
                        assertNull(legacy.getString("token_hash"));
                        assertNotNull(legacy.getTimestamp("revoked_at"));
                        assertNotNull(legacy.getTimestamp("expires_at"));
                        assertNull(legacy.getTimestamp("consumed_at"));
                        assertNull(legacy.getObject("accepted_user_id"));
                        assertEquals(id == 1 ? 10L : 20L, legacy.getLong("invited_role_id"));
                        if (id == 3) {
                            assertNull(legacy.getObject("company_id"));
                            assertEquals(999L, legacy.getLong("invited_by_id"));
                        } else {
                            assertEquals(id, legacy.getLong("company_id"));
                            assertEquals(100L, legacy.getLong("invited_by_id"));
                        }
                    }
                    assertFalse(legacy.next());
                }

                statement.execute("INSERT INTO user_invitation " +
                        "(id,email,role_id,company_id,token_hash,expires_at,invited_by_id,invited_role_id,consumed_at,accepted_user_id) " +
                        "VALUES (4,'accepted@example.com',10,1,'" + "c".repeat(64) + "'," +
                        "CURRENT_TIMESTAMP + INTERVAL '7 days',100,10,CURRENT_TIMESTAMP,200)");
                statement.execute("DELETE FROM role WHERE id=10");
                try (ResultSet audit = statement.executeQuery("SELECT * FROM user_invitation WHERE id=4")) {
                    assertTrue(audit.next());
                    assertNull(audit.getObject("role_id"));
                    assertEquals(10L, audit.getLong("invited_role_id"));
                    assertEquals(100L, audit.getLong("invited_by_id"));
                    assertEquals(200L, audit.getLong("accepted_user_id"));
                    assertNotNull(audit.getTimestamp("consumed_at"));
                }
                SQLException unprotected = assertThrows(SQLException.class,
                        () -> statement.execute("INSERT INTO user_invitation (id,email,role_id) " +
                                "VALUES (5,'insecure@example.com',20)"));
                assertEquals("23514", unprotected.getSQLState());
                SQLException missingAudit = assertThrows(SQLException.class,
                        () -> statement.execute("UPDATE user_invitation SET accepted_user_id=NULL WHERE id=4"));
                assertEquals("23514", missingAudit.getSQLState());
            }
        } finally {
            try (Connection connection = connect(postgres); Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            }
        }
    }

    @Test
    void authorizationUpgradeAddsSnapshotsWithoutInferringOrChangingExistingGrants() throws Exception {
        PostgreSQLContainer<?> postgres = AbstractTestContainer.getPostgresqlContainer();
        String schema = "authorization_migration_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = connect(postgres); Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
            statement.execute("SET search_path TO " + schema);
            statement.execute("CREATE TABLE user_invitation (id BIGINT PRIMARY KEY, role_id BIGINT, invited_by_id BIGINT, token_hash VARCHAR(64), revoked_at TIMESTAMP)");
            statement.execute("INSERT INTO user_invitation VALUES (1, 42, 7, 'existing-token-hash', NULL)");
            statement.execute("CREATE TABLE own_user (id BIGINT PRIMARY KEY, role_id BIGINT)");
            statement.execute("INSERT INTO own_user VALUES (7, 42)");
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            database.setDefaultSchemaName(schema); database.setLiquibaseSchemaName(schema);
            try (Liquibase migration = new Liquibase("db/changelog/2026_10_20_00000000001_invitation_authorization.xml", new ClassLoaderResourceAccessor(), database)) {
                migration.update(new Contexts(), new LabelExpression());
                migration.update(new Contexts(), new LabelExpression());
                connection.setAutoCommit(true);
                try (ResultSet result = statement.executeQuery("SELECT * FROM user_invitation")) {
                    assertTrue(result.next()); assertEquals(42L, result.getLong("role_id"));
                    assertEquals(7L, result.getLong("invited_by_id"));
                    assertEquals("existing-token-hash", result.getString("token_hash"));
                    assertNull(result.getString("role_authorization_hash"));
                    assertNull(result.getString("issuer_authorization_hash"));
                    assertNull(result.getTimestamp("revoked_at")); assertFalse(result.next());
                }
                try (ResultSet result = statement.executeQuery("SELECT role_id FROM own_user WHERE id=7")) {
                    assertTrue(result.next()); assertEquals(42L, result.getLong(1));
                }
            }
        } finally {
            try (Connection connection = connect(postgres); Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            }
        }
    }

    private Connection connect(PostgreSQLContainer<?> postgres) throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }
}
