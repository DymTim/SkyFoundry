package net.skyfoundry.island;

import net.skyfoundry.SkyFoundry;
import net.skyfoundry.storage.Database;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class IslandManager {

    private final SkyFoundry plugin;
    private final Database database;
    private final World islandWorld;
    private final IslandPositioner positioner;

    private final Map<UUID, Island> islands = new HashMap<>();

    public IslandManager(
            SkyFoundry plugin,
            Database database,
            World islandWorld) {
        this.plugin = plugin;
        this.database = database;
        this.islandWorld = islandWorld;

        int spacing = plugin.getConfig().getInt(
                "islands.spacing",
                500);

        this.positioner = new IslandPositioner(spacing);
    }

    public void loadIslands() throws SQLException {
        islands.clear();

        String sql = """
                SELECT
                    island_id,
                    owner_uuid,
                    center_x,
                    center_z,
                    home_x,
                    home_y,
                    home_z,
                    home_yaw,
                    home_pitch,
                    created_at
                FROM islands;
                """;

        try (
                Statement statement = database
                        .getConnection()
                        .createStatement();

                ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                UUID ownerUuid;

                try {
                    ownerUuid = UUID.fromString(
                            resultSet.getString("owner_uuid"));
                } catch (IllegalArgumentException exception) {
                    plugin.getLogger().warning(
                            "Skipping island with invalid owner UUID: "
                                    + resultSet.getString("owner_uuid"));
                    continue;
                }

                Location home = new Location(
                        islandWorld,
                        resultSet.getDouble("home_x"),
                        resultSet.getDouble("home_y"),
                        resultSet.getDouble("home_z"),
                        resultSet.getFloat("home_yaw"),
                        resultSet.getFloat("home_pitch"));

                Island island = new Island(
                        resultSet.getLong("island_id"),
                        ownerUuid,
                        resultSet.getInt("center_x"),
                        resultSet.getInt("center_z"),
                        home,
                        resultSet.getLong("created_at"));

                islands.put(ownerUuid, island);
            }
        }

        plugin.getLogger().info(
                "Loaded " + islands.size() + " island(s).");
    }

    public Island createIsland(UUID ownerUuid) throws SQLException {
        Island existing = islands.get(ownerUuid);

        if (existing != null) {
            return existing;
        }

        long nextIslandId = getNextIslandId();

        IslandPositioner.IslandPosition position = positioner.getPosition(nextIslandId - 1);

        int creationY = plugin.getConfig().getInt(
                "islands.creation-y",
                100);

        Location home = new Location(
                islandWorld,
                position.x() + 0.5,
                creationY + 1.0,
                position.z() + 0.5);

        long createdAt = Instant.now().getEpochSecond();

        Island island = new Island(
                nextIslandId,
                ownerUuid,
                position.x(),
                position.z(),
                home,
                createdAt);

        insertIsland(island);

        islands.put(ownerUuid, island);

        return island;
    }

    public boolean deleteIsland(UUID ownerUuid) throws SQLException {
        if (!islands.containsKey(ownerUuid)) {
            return false;
        }

        String sql = """
                DELETE FROM islands
                WHERE owner_uuid = ?;
                """;

        try (
                PreparedStatement statement = database
                        .getConnection()
                        .prepareStatement(sql)) {
            statement.setString(
                    1,
                    ownerUuid.toString());

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                return false;
            }
        }

        islands.remove(ownerUuid);

        return true;
    }

    private void insertIsland(Island island) throws SQLException {
        String sql = """
                INSERT INTO islands (
                    island_id,
                    owner_uuid,
                    center_x,
                    center_z,
                    home_x,
                    home_y,
                    home_z,
                    home_yaw,
                    home_pitch,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                """;

        Location home = island.getHome();

        try (
                PreparedStatement statement = database
                        .getConnection()
                        .prepareStatement(sql)) {
            statement.setLong(
                    1,
                    island.getIslandId());

            statement.setString(
                    2,
                    island.getOwnerUuid().toString());

            statement.setInt(
                    3,
                    island.getCenterX());

            statement.setInt(
                    4,
                    island.getCenterZ());

            statement.setDouble(
                    5,
                    home.getX());

            statement.setDouble(
                    6,
                    home.getY());

            statement.setDouble(
                    7,
                    home.getZ());

            statement.setFloat(
                    8,
                    home.getYaw());

            statement.setFloat(
                    9,
                    home.getPitch());

            statement.setLong(
                    10,
                    island.getCreatedAt());

            statement.executeUpdate();
        }
    }

    private long getNextIslandId() throws SQLException {
        String sql = """
                SELECT seq
                FROM sqlite_sequence
                WHERE name = 'islands';
                """;

        try (
                Statement statement = database
                        .getConnection()
                        .createStatement();

                ResultSet resultSet = statement.executeQuery(sql)) {
            if (resultSet.next()) {
                return resultSet.getLong("seq") + 1;
            }
        }

        return 1;
    }

    public Optional<Island> getIsland(UUID ownerUuid) {
        return Optional.ofNullable(
                islands.get(ownerUuid));
    }

    public boolean hasIsland(UUID ownerUuid) {
        return islands.containsKey(ownerUuid);
    }

    public Map<UUID, Island> getIslands() {
        return Collections.unmodifiableMap(islands);
    }

    public int getIslandCount() {
        return islands.size();
    }
}