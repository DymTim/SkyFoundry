package net.stormboundmc.skyblock.storage;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import net.stormboundmc.skyblock.StormboundSkyblock;

public final class Database {

    private final StormboundSkyblock plugin;

    private Connection connection;

    public Database(
            StormboundSkyblock plugin) {
        this.plugin = plugin;
    }

    public void connect()
            throws SQLException {

        if (!plugin.getDataFolder().exists()
                && !plugin.getDataFolder().mkdirs()) {

            throw new SQLException(
                    "Could not create Stormbound plugin directory.");
        }

        String databaseFileName = plugin.getConfig().getString(
                "storage.database-file",
                "stormbound.db");

        if (databaseFileName == null
                || databaseFileName.isBlank()) {

            databaseFileName = "stormbound.db";
        }

        File databaseFile = new File(
                plugin.getDataFolder(),
                databaseFileName);

        String url = "jdbc:sqlite:"
                + databaseFile.getAbsolutePath();

        connection = DriverManager.getConnection(
                url);

        try (
                Statement statement = connection.createStatement()) {
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

    public void initialize()
            throws SQLException {

        try (
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
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
                    """);

            statement.executeUpdate("""
                    CREATE UNIQUE INDEX IF NOT EXISTS idx_islands_center
                    ON islands (
                        center_x,
                        center_z
                    );
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS island_members (
                        island_id INTEGER NOT NULL,
                        player_uuid TEXT NOT NULL UNIQUE,
                        role TEXT NOT NULL,

                        home_x REAL NOT NULL,
                        home_y REAL NOT NULL,
                        home_z REAL NOT NULL,
                        home_yaw REAL NOT NULL DEFAULT 0,
                        home_pitch REAL NOT NULL DEFAULT 0,

                        joined_at INTEGER NOT NULL,

                        PRIMARY KEY (
                            island_id,
                            player_uuid
                        ),

                        FOREIGN KEY (
                            island_id
                        )
                        REFERENCES islands (
                            island_id
                        )
                        ON DELETE CASCADE
                    );
                    """);

            statement.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_island_members_island
                    ON island_members (
                        island_id
                    );
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS island_invites (
                        island_id INTEGER NOT NULL,
                        player_uuid TEXT NOT NULL,
                        inviter_uuid TEXT NOT NULL,
                        created_at INTEGER NOT NULL,

                        PRIMARY KEY (
                            island_id,
                            player_uuid
                        ),

                        FOREIGN KEY (
                            island_id
                        )
                        REFERENCES islands (
                            island_id
                        )
                        ON DELETE CASCADE
                    );
                    """);

            statement.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_island_invites_player
                    ON island_invites (
                        player_uuid
                    );
                    """);
        }
    }

    public Connection getConnection() {
        if (connection == null) {
            throw new IllegalStateException(
                    "Database has not been connected.");
        }

        return connection;
    }

    public void close() {
        if (connection == null) {
            return;
        }

        try {
            connection.close();

        } catch (SQLException exception) {
            plugin.getLogger().warning(
                    "Failed to close SQLite connection: "
                            + exception.getMessage());
        }

        connection = null;
    }
}