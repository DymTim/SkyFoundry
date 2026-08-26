package net.skyfoundry.core.database;

import net.skyfoundry.core.SkyFoundry;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseManager {

    private final SkyFoundry plugin;

    private Connection connection;

    public DatabaseManager(SkyFoundry plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        try {
            if (!plugin.getDataFolder().exists()
                    && !plugin.getDataFolder().mkdirs()) {

                throw new IllegalStateException(
                        "Could not create SkyFoundry data folder.");
            }

            File databaseFile = new File(
                    plugin.getDataFolder(),
                    "skyfoundry.db");

            connection = DriverManager.getConnection(
                    "jdbc:sqlite:" + databaseFile.getAbsolutePath());

            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON;");
                statement.execute("PRAGMA journal_mode = WAL;");
            }

            createTables();

        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Could not initialize SQLite database.",
                    exception);
        }
    }

    private void createTables() throws SQLException {
        String islandsTable = """
                CREATE TABLE IF NOT EXISTS islands (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    owner_uuid TEXT NOT NULL UNIQUE,
                    world_name TEXT NOT NULL,
                    center_x INTEGER NOT NULL,
                    center_y INTEGER NOT NULL,
                    center_z INTEGER NOT NULL,
                    size INTEGER NOT NULL,
                    slot_index INTEGER NOT NULL UNIQUE,
                    created_at INTEGER NOT NULL
                );
                """;

        String membersTable = """
                CREATE TABLE IF NOT EXISTS island_members (
                    island_id INTEGER NOT NULL,
                    player_uuid TEXT NOT NULL,
                    role TEXT NOT NULL,
                    joined_at INTEGER NOT NULL,

                    PRIMARY KEY (island_id, player_uuid),

                    FOREIGN KEY (island_id)
                        REFERENCES islands(id)
                        ON DELETE CASCADE
                );
                """;

        try (Statement statement = connection.createStatement()) {
            statement.execute(islandsTable);
            statement.execute(membersTable);
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                throw new IllegalStateException(
                        "Database connection is not available.");
            }

            return connection;

        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Could not check database connection.",
                    exception);
        }
    }

    public void close() {
        if (connection == null) {
            return;
        }

        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning(
                    "Could not cleanly close SQLite connection.");

            exception.printStackTrace();
        }
    }
}