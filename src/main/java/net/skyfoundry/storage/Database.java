package net.skyfoundry.storage;

import net.skyfoundry.SkyFoundry;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {

    private final SkyFoundry plugin;

    private Connection connection;

    public Database(SkyFoundry plugin) {
        this.plugin = plugin;
    }

    public void initialize() throws SQLException {
        String fileName = plugin.getConfig()
                .getString(
                        "storage.database-file",
                        "skyfoundry.db");

        File databaseFile = new File(
                plugin.getDataFolder(),
                fileName);

        connection = DriverManager.getConnection(
                "jdbc:sqlite:" + databaseFile.getAbsolutePath());

        configureConnection();
        createTables();
    }

    private void configureConnection() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "PRAGMA journal_mode=WAL;");

            statement.execute(
                    "PRAGMA synchronous=NORMAL;");

            statement.execute(
                    "PRAGMA foreign_keys=ON;");

            statement.execute(
                    "PRAGMA busy_timeout=5000;");
        }
    }

    private void createTables() throws SQLException {
        String islandsTable = """
                CREATE TABLE IF NOT EXISTS islands (
                    island_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    owner_uuid TEXT NOT NULL UNIQUE,
                    center_x INTEGER NOT NULL,
                    center_z INTEGER NOT NULL,
                    home_x REAL NOT NULL,
                    home_y REAL NOT NULL,
                    home_z REAL NOT NULL,
                    home_yaw REAL NOT NULL DEFAULT 0,
                    home_pitch REAL NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL
                );
                """;

        String centerIndex = """
                CREATE UNIQUE INDEX IF NOT EXISTS idx_islands_center
                ON islands (center_x, center_z);
                """;

        try (Statement statement = connection.createStatement()) {
            statement.execute(islandsTable);
            statement.execute(centerIndex);
        }
    }

    public Connection getConnection() {
        return connection;
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
                    "Failed to close SQLite database: "
                            + exception.getMessage());
        } finally {
            connection = null;
        }
    }
}