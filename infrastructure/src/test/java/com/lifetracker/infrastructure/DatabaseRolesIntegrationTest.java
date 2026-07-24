package com.lifetracker.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two-role split, asserted rather than assumed (ADR-0016).
 *
 * <p>Everything else in this suite would still pass if the application connected as a superuser —
 * that is what makes this worth its own test. A superuser can never fail a permission check, so
 * without these assertions a missing grant is invisible until production. The most likely way to
 * lose the split is someone restoring {@code @ServiceConnection} on the container, which silently
 * hands the tests the superuser again; that would fail here and nowhere else.
 */
class DatabaseRolesIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    DataSource dataSource;

    @Test
    void the_application_connects_as_the_unprivileged_app_role() throws SQLException {
        assertEquals("lifetracker_app", scalar("SELECT current_user"));
    }

    @Test
    void the_application_role_cannot_create_a_table() {
        SQLException thrown = assertThrows(SQLException.class, () -> execute("CREATE TABLE nope (id int)"));
        assertTrue(thrown.getMessage().contains("permission denied"),
                "expected a permission failure, got: " + thrown.getMessage());
    }

    @Test
    void the_application_role_cannot_alter_a_table() {
        SQLException thrown = assertThrows(SQLException.class,
                () -> execute("ALTER TABLE accounts ADD COLUMN nope int"));
        assertTrue(thrown.getMessage().contains("must be owner"),
                "expected an ownership failure, got: " + thrown.getMessage());
    }

    @Test
    void migrations_ran_as_the_migrator_so_default_privileges_applied() throws SQLException {
        // Ownership is the load-bearing part: ALTER DEFAULT PRIVILEGES is scoped to the role that
        // creates the table, so a table owned by anyone else grants the app nothing.
        assertEquals("lifetracker_migrator", scalar("SELECT tableowner FROM pg_tables WHERE tablename = 'accounts'"));
    }

    @Test
    void every_table_grants_full_dml_to_the_application_role() throws SQLException {
        assertEquals("0", scalar("""
                SELECT count(*)::text FROM information_schema.tables t
                 WHERE t.table_schema = 'public' AND t.table_type = 'BASE TABLE'
                   AND NOT EXISTS (
                       SELECT 1 FROM information_schema.role_table_grants g
                        WHERE g.table_schema = 'public' AND g.table_name = t.table_name
                          AND g.grantee = 'lifetracker_app'
                        GROUP BY g.table_name
                       HAVING count(DISTINCT g.privilege_type::text)
                              FILTER (WHERE g.privilege_type::text
                                      IN ('SELECT', 'INSERT', 'UPDATE', 'DELETE')) = 4)
                """));
    }

    private String scalar(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            assertTrue(rows.next(), "query returned no row: " + sql);
            return rows.getString(1);
        }
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
