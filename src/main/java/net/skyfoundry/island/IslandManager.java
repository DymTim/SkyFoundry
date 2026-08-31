package net.skyfoundry.island;

import net.skyfoundry.SkyFoundry;
import net.skyfoundry.storage.Database;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

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
import java.util.concurrent.CompletableFuture;

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
                                                        resultSet.getString(
                                                                        "owner_uuid"));

                                } catch (IllegalArgumentException exception) {
                                        plugin.getLogger().warning(
                                                        "Skipping island with invalid owner UUID: "
                                                                        + resultSet.getString(
                                                                                        "owner_uuid"));

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

                                islands.put(
                                                ownerUuid,
                                                island);
                        }
                }

                plugin.getLogger().info(
                                "Loaded "
                                                + islands.size()
                                                + " island(s).");
        }

        public Island createIsland(
                        UUID ownerUuid) throws SQLException {

                Island existing = islands.get(ownerUuid);

                if (existing != null) {
                        return existing;
                }

                long nextIslandId = getNextIslandId();

                IslandPositioner.IslandPosition position = positioner.getPosition(
                                nextIslandId - 1);

                int creationY = plugin.getConfig().getInt(
                                "islands.creation-y",
                                100);

                double spawnOffsetX = plugin.getConfig().getDouble(
                                "islands.spawn-offset.x",
                                0.5);

                double spawnOffsetY = plugin.getConfig().getDouble(
                                "islands.spawn-offset.y",
                                8.0);

                double spawnOffsetZ = plugin.getConfig().getDouble(
                                "islands.spawn-offset.z",
                                0.5);

                float spawnYaw = (float) plugin.getConfig().getDouble(
                                "islands.spawn-offset.yaw",
                                0.0);

                float spawnPitch = (float) plugin.getConfig().getDouble(
                                "islands.spawn-offset.pitch",
                                0.0);

                Location home = new Location(
                                islandWorld,
                                position.x() + spawnOffsetX,
                                creationY + spawnOffsetY,
                                position.z() + spawnOffsetZ,
                                spawnYaw,
                                spawnPitch);

                long createdAt = Instant.now().getEpochSecond();

                Island island = new Island(
                                nextIslandId,
                                ownerUuid,
                                position.x(),
                                position.z(),
                                home,
                                createdAt);

                insertIsland(island);

                islands.put(
                                ownerUuid,
                                island);

                return island;
        }

        public CompletableFuture<Boolean> deleteIsland(
                        UUID ownerUuid) {
                Island island = islands.get(ownerUuid);

                if (island == null) {
                        return CompletableFuture.completedFuture(
                                        false);
                }

                teleportPlayersOffIsland(
                                island);

                return clearIsland(
                                island).thenApply(unused -> {
                                        try {
                                                deleteIslandRecord(
                                                                ownerUuid);

                                                islands.remove(
                                                                ownerUuid);

                                                return true;

                                        } catch (SQLException exception) {
                                                throw new RuntimeException(
                                                                "Failed to delete island database record.",
                                                                exception);
                                        }
                                });
        }

        private CompletableFuture<Void> clearIsland(
                        Island island) {
                CompletableFuture<Void> future = new CompletableFuture<>();

                int islandSize = Math.max(
                                1,
                                plugin.getConfig().getInt(
                                                "islands.size",
                                                50));

                int positionsPerTick = Math.max(
                                1,
                                plugin.getConfig().getInt(
                                                "deletion.positions-per-tick",
                                                25000));

                int minX = island.getCenterX()
                                - (islandSize / 2);

                int minZ = island.getCenterZ()
                                - (islandSize / 2);

                int maxX = minX + islandSize - 1;

                int maxZ = minZ + islandSize - 1;

                int minY = islandWorld.getMinHeight();

                int maxY = islandWorld.getMaxHeight() - 1;

                removeIslandEntities(
                                minX,
                                maxX,
                                minY,
                                maxY,
                                minZ,
                                maxZ);

                final int[] x = { minX };
                final int[] y = { minY };
                final int[] z = { minZ };

                final BukkitTask[] task = new BukkitTask[1];

                task[0] = plugin
                                .getServer()
                                .getScheduler()
                                .runTaskTimer(
                                                plugin,
                                                () -> {
                                                        try {
                                                                int processed = 0;

                                                                while (processed < positionsPerTick) {
                                                                        if (x[0] > maxX) {
                                                                                task[0].cancel();

                                                                                if (plugin.getConfig()
                                                                                                .getBoolean(
                                                                                                                "debug.enabled",
                                                                                                                false)) {
                                                                                        plugin.getLogger().info(
                                                                                                        "Finished clearing island "
                                                                                                                        + island.getIslandId());
                                                                                }

                                                                                future.complete(
                                                                                                null);

                                                                                return;
                                                                        }

                                                                        if (islandWorld
                                                                                        .getBlockAt(
                                                                                                        x[0],
                                                                                                        y[0],
                                                                                                        z[0])
                                                                                        .getType() != Material.AIR) {
                                                                                islandWorld
                                                                                                .getBlockAt(
                                                                                                                x[0],
                                                                                                                y[0],
                                                                                                                z[0])
                                                                                                .setType(
                                                                                                                Material.AIR,
                                                                                                                false);
                                                                        }

                                                                        processed++;
                                                                        y[0]++;

                                                                        if (y[0] > maxY) {
                                                                                y[0] = minY;
                                                                                z[0]++;

                                                                                if (z[0] > maxZ) {
                                                                                        z[0] = minZ;
                                                                                        x[0]++;
                                                                                }
                                                                        }
                                                                }

                                                        } catch (Exception exception) {
                                                                task[0].cancel();

                                                                future.completeExceptionally(
                                                                                exception);
                                                        }
                                                },
                                                1L,
                                                1L);

                return future;
        }

        private void teleportPlayersOffIsland(
                        Island island) {
                int islandSize = Math.max(
                                1,
                                plugin.getConfig().getInt(
                                                "islands.size",
                                                50));

                int minX = island.getCenterX()
                                - (islandSize / 2);

                int minZ = island.getCenterZ()
                                - (islandSize / 2);

                int maxX = minX + islandSize - 1;

                int maxZ = minZ + islandSize - 1;

                Location destination = getDeletionTeleportLocation();

                for (Player player : islandWorld.getPlayers()) {

                        Location location = player.getLocation();

                        if (location.getX() >= minX
                                        && location.getX() < maxX + 1.0
                                        && location.getZ() >= minZ
                                        && location.getZ() < maxZ + 1.0) {
                                player.teleport(
                                                destination);
                        }
                }
        }

        private Location getDeletionTeleportLocation() {
                String configuredWorld = plugin.getConfig().getString(
                                "deletion.teleport-world",
                                "world");

                if (configuredWorld != null
                                && !configuredWorld.isBlank()) {
                        World world = plugin.getServer().getWorld(
                                        configuredWorld);

                        if (world != null
                                        && !world.equals(
                                                        islandWorld)) {
                                return world
                                                .getSpawnLocation()
                                                .clone()
                                                .add(
                                                                0.5,
                                                                0.0,
                                                                0.5);
                        }
                }

                for (World world : plugin.getServer().getWorlds()) {

                        if (!world.equals(islandWorld)) {
                                return world
                                                .getSpawnLocation()
                                                .clone()
                                                .add(
                                                                0.5,
                                                                0.0,
                                                                0.5);
                        }
                }

                plugin.getLogger().warning(
                                "No non-island world was found for deletion teleport. "
                                                + "Using the island world's spawn.");

                return islandWorld
                                .getSpawnLocation()
                                .clone()
                                .add(
                                                0.5,
                                                0.0,
                                                0.5);
        }

        private void removeIslandEntities(
                        int minX,
                        int maxX,
                        int minY,
                        int maxY,
                        int minZ,
                        int maxZ) {
                for (Entity entity : islandWorld.getEntities()) {

                        if (entity instanceof Player) {
                                continue;
                        }

                        Location location = entity.getLocation();

                        double x = location.getX();

                        double y = location.getY();

                        double z = location.getZ();

                        if (x >= minX
                                        && x < maxX + 1.0
                                        && y >= minY
                                        && y < maxY + 1.0
                                        && z >= minZ
                                        && z < maxZ + 1.0) {
                                entity.remove();
                        }
                }
        }

        private void deleteIslandRecord(
                        UUID ownerUuid) throws SQLException {

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

                        statement.executeUpdate();
                }
        }

        private void insertIsland(
                        Island island) throws SQLException {

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
                                        island.getOwnerUuid()
                                                        .toString());

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

        private long getNextIslandId()
                        throws SQLException {

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
                                return resultSet
                                                .getLong("seq")
                                                + 1;
                        }
                }

                return 1;
        }

        public Optional<Island> getIsland(
                        UUID ownerUuid) {
                return Optional.ofNullable(
                                islands.get(ownerUuid));
        }

        public boolean hasIsland(
                        UUID ownerUuid) {
                return islands.containsKey(
                                ownerUuid);
        }

        public Map<UUID, Island> getIslands() {
                return Collections.unmodifiableMap(
                                islands);
        }

        public int getIslandCount() {
                return islands.size();
        }
}