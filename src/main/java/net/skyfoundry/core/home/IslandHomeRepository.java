package net.skyfoundry.core.home;

import net.skyfoundry.core.database.DatabaseManager;
import org.bukkit.Location;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public final class IslandHomeRepository {

    private final DatabaseManager databaseManager;

    public IslandHomeRepository(
            DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void saveHome(
            long islandId,
            UUID playerUuid,
            Location location) {
        if (location.getWorld() == null) {
            throw new IllegalArgumentException(
                    "Home location must have a world.");
        }

        String sql = """
                INSERT INTO island_member_homes (
                    island_id,
                    player_uuid,
                    world_name,
                    x,
                    y,
                    z,
                    yaw,
                    pitch
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(island_id, player_uuid)
                DO UPDATE SET
                    world_name = excluded.world_name,
                    x = excluded.x,
                    y = excluded.y,
                    z = excluded.z,
                    yaw = excluded.yaw,
                    pitch = excluded.pitch;
                """;

        try (PreparedStatement statement = databaseManager
                .getConnection()
                .prepareStatement(sql)) {

            statement.setLong(
                    1,
                    islandId);

            statement.setString(
                    2,
                    playerUuid.toString());

            statement.setString(
                    3,
                    location.getWorld().getName());

            statement.setDouble(
                    4,
                    location.getX());

            statement.setDouble(
                    5,
                    location.getY());

            statement.setDouble(
                    6,
                    location.getZ());

            statement.setFloat(
                    7,
                    location.getYaw());

            statement.setFloat(
                    8,
                    location.getPitch());

            statement.executeUpdate();

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "Could not save island home.",
                    exception);
        }
    }

    public Optional<IslandHome> findHome(
            long islandId,
            UUID playerUuid) {
        String sql = """
                SELECT *
                FROM island_member_homes
                WHERE island_id = ?
                  AND player_uuid = ?
                LIMIT 1;
                """;

        try (PreparedStatement statement = databaseManager
                .getConnection()
                .prepareStatement(sql)) {

            statement.setLong(
                    1,
                    islandId);

            statement.setString(
                    2,
                    playerUuid.toString());

            try (ResultSet results = statement.executeQuery()) {

                if (!results.next()) {
                    return Optional.empty();
                }

                return Optional.of(
                        new IslandHome(
                                results.getLong(
                                        "island_id"),
                                UUID.fromString(
                                        results.getString(
                                                "player_uuid")),
                                results.getString(
                                        "world_name"),
                                results.getDouble("x"),
                                results.getDouble("y"),
                                results.getDouble("z"),
                                results.getFloat("yaw"),
                                results.getFloat("pitch")));
            }

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "Could not load island home.",
                    exception);
        }
    }

    public void deleteHome(
            long islandId,
            UUID playerUuid) {
        String sql = """
                DELETE FROM island_member_homes
                WHERE island_id = ?
                  AND player_uuid = ?;
                """;

        try (PreparedStatement statement = databaseManager
                .getConnection()
                .prepareStatement(sql)) {

            statement.setLong(
                    1,
                    islandId);

            statement.setString(
                    2,
                    playerUuid.toString());

            statement.executeUpdate();

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "Could not delete island home.",
                    exception);
        }
    }

    public void deleteHomesForIsland(
            long islandId) {
        String sql = """
                DELETE FROM island_member_homes
                WHERE island_id = ?;
                """;

        try (PreparedStatement statement = databaseManager
                .getConnection()
                .prepareStatement(sql)) {

            statement.setLong(
                    1,
                    islandId);

            statement.executeUpdate();

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "Could not clear island homes.",
                    exception);
        }
    }
}