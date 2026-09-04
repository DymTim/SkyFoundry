package net.stormboundmc.skyblock.island;

import net.stormboundmc.skyblock.StormboundSkyblock;
import net.stormboundmc.skyblock.storage.Database;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class IslandDimensionManager {
    private final StormboundSkyblock plugin;
    private final Database database;
    private final Map<IslandDimension, World> worlds;
    private final Map<Long, EnumMap<IslandDimension, IslandDimensionData>> data = new HashMap<>();

    public IslandDimensionManager(StormboundSkyblock plugin, Database database, Map<IslandDimension, World> worlds) {
        this.plugin = plugin;
        this.database = database;
        this.worlds = new EnumMap<>(worlds);
    }

    public void load(Iterable<Island> islands) throws SQLException {
        data.clear();
        try (Statement statement = database.getConnection().createStatement();
             ResultSet rs = statement.executeQuery("SELECT island_id, dimension, generated, unlocked, size, home_x, home_y, home_z, home_yaw, home_pitch FROM island_dimensions;")) {
            while (rs.next()) {
                IslandDimension dimension;
                try { dimension = IslandDimension.valueOf(rs.getString("dimension")); }
                catch (IllegalArgumentException ignored) { continue; }
                World world = worlds.get(dimension);
                Location home = null;
                if (world != null && rs.getObject("home_x") != null) {
                    home = new Location(world, rs.getDouble("home_x"), rs.getDouble("home_y"), rs.getDouble("home_z"), rs.getFloat("home_yaw"), rs.getFloat("home_pitch"));
                }
                data.computeIfAbsent(rs.getLong("island_id"), id -> new EnumMap<>(IslandDimension.class))
                        .put(dimension, new IslandDimensionData(rs.getLong("island_id"), dimension, rs.getInt("generated") != 0, rs.getInt("unlocked") != 0, rs.getInt("size"), home));
            }
        }
        for (Island island : islands) {
            ensureRecords(island);
            for (IslandDimension dimension : IslandDimension.values()) {
                IslandDimensionData value = get(island, dimension).orElse(null);
                if (value == null) continue;

                // Migration safety: the Overworld, already-generated dimensions, and
                // dimensions with unlock requirements disabled must remain accessible.
                if (!value.isUnlocked()
                        && (dimension == IslandDimension.OVERWORLD
                        || value.isGenerated()
                        || !requiresUnlock(dimension))) {
                    setUnlocked(island, dimension, true);
                }
            }
        }
    }

    public void ensureRecords(Island island) throws SQLException {
        EnumMap<IslandDimension, IslandDimensionData> map = data.computeIfAbsent(island.getIslandId(), id -> new EnumMap<>(IslandDimension.class));
        for (IslandDimension dimension : IslandDimension.values()) {
            if (map.containsKey(dimension)) continue;
            int size = dimension == IslandDimension.OVERWORLD ? island.getSize() : getDefaultSize(dimension);
            boolean generated = dimension == IslandDimension.OVERWORLD;
            boolean unlocked = dimension == IslandDimension.OVERWORLD || !requiresUnlock(dimension);
            Location home = dimension == IslandDimension.OVERWORLD ? island.getHome() : defaultHome(island, dimension);
            insert(island.getIslandId(), dimension, generated, unlocked, size, home);
            map.put(dimension, new IslandDimensionData(island.getIslandId(), dimension, generated, unlocked, size, home));
        }
    }

    private void insert(long islandId, IslandDimension dimension, boolean generated, boolean unlocked, int size, Location home) throws SQLException {
        String sql = "INSERT OR IGNORE INTO island_dimensions (island_id, dimension, generated, unlocked, size, home_x, home_y, home_z, home_yaw, home_pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setLong(1, islandId);
            ps.setString(2, dimension.name());
            ps.setInt(3, generated ? 1 : 0);
            ps.setInt(4, unlocked ? 1 : 0);
            ps.setInt(5, size);
            if (home == null) { for (int i = 6; i <= 10; i++) ps.setObject(i, null); }
            else { ps.setDouble(6, home.getX()); ps.setDouble(7, home.getY()); ps.setDouble(8, home.getZ()); ps.setFloat(9, home.getYaw()); ps.setFloat(10, home.getPitch()); }
            ps.executeUpdate();
        }
    }

    public IslandDimension getDimension(World world) {
        if (world == null) return null;
        for (Map.Entry<IslandDimension, World> entry : worlds.entrySet()) if (world.equals(entry.getValue())) return entry.getKey();
        return null;
    }

    public World getWorld(IslandDimension dimension) { return worlds.get(dimension); }
    public boolean isIslandWorld(World world) { return getDimension(world) != null; }

    public Optional<IslandDimensionData> get(Island island, IslandDimension dimension) {
        EnumMap<IslandDimension, IslandDimensionData> map = data.get(island.getIslandId());
        return map == null ? Optional.empty() : Optional.ofNullable(map.get(dimension));
    }


    public boolean isGenerated(Island island, IslandDimension dimension) {
        return get(island, dimension).map(IslandDimensionData::isGenerated).orElse(dimension == IslandDimension.OVERWORLD);
    }

    public boolean isUnlocked(Island island, IslandDimension dimension) {
        if (dimension == IslandDimension.OVERWORLD) return true;
        return get(island, dimension).map(IslandDimensionData::isUnlocked).orElse(!requiresUnlock(dimension));
    }

    public void setUnlocked(Island island, IslandDimension dimension, boolean unlocked) throws SQLException {
        if (dimension == IslandDimension.OVERWORLD) unlocked = true;
        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "UPDATE island_dimensions SET unlocked = ? WHERE island_id = ? AND dimension = ?;")) {
            ps.setInt(1, unlocked ? 1 : 0);
            ps.setLong(2, island.getIslandId());
            ps.setString(3, dimension.name());
            ps.executeUpdate();
        }
        boolean finalUnlocked = unlocked;
        get(island, dimension).ifPresent(value -> value.setUnlocked(finalUnlocked));
    }

    public boolean requiresUnlock(IslandDimension dimension) {
        if (dimension == IslandDimension.OVERWORLD) return false;
        return plugin.getConfig().getBoolean(
                "dimensions." + dimension.getConfigKey() + ".unlock.required",
                true);
    }

    public double getUnlockCost(IslandDimension dimension) {
        if (dimension == IslandDimension.OVERWORLD) return 0.0D;
        return Math.max(0.0D, plugin.getConfig().getDouble(
                "dimensions." + dimension.getConfigKey() + ".unlock.cost",
                dimension == IslandDimension.NETHER ? 100000.0D : 500000.0D));
    }

    public boolean requiresNether(IslandDimension dimension) {
        return dimension == IslandDimension.END
                && plugin.getConfig().getBoolean("dimensions.end.unlock.requires-nether", true);
    }

    public void setGenerated(Island island, IslandDimension dimension, boolean generated) throws SQLException {
        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "UPDATE island_dimensions SET generated = ? WHERE island_id = ? AND dimension = ?;")) {
            ps.setInt(1, generated ? 1 : 0);
            ps.setLong(2, island.getIslandId());
            ps.setString(3, dimension.name());
            ps.executeUpdate();
        }
        get(island, dimension).ifPresent(value -> value.setGenerated(generated));
    }

    public Location getHome(Island island, IslandDimension dimension) {
        return get(island, dimension)
                .map(IslandDimensionData::getHome)
                .orElseGet(() -> defaultHome(island, dimension));
    }

    public void setHome(Island island, IslandDimension dimension, Location home) throws SQLException {
        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "UPDATE island_dimensions SET home_x = ?, home_y = ?, home_z = ?, home_yaw = ?, home_pitch = ? WHERE island_id = ? AND dimension = ?;")) {
            ps.setDouble(1, home.getX());
            ps.setDouble(2, home.getY());
            ps.setDouble(3, home.getZ());
            ps.setFloat(4, home.getYaw());
            ps.setFloat(5, home.getPitch());
            ps.setLong(6, island.getIslandId());
            ps.setString(7, dimension.name());
            ps.executeUpdate();
        }
        get(island, dimension).ifPresent(value -> value.setHome(home));
        if (dimension == IslandDimension.OVERWORLD) {
            island.setHome(home);
        }
    }

    public int getSize(Island island, IslandDimension dimension) {
        return get(island, dimension).map(IslandDimensionData::getSize).orElse(dimension == IslandDimension.OVERWORLD ? island.getSize() : getDefaultSize(dimension));
    }

    public void setSize(Island island, IslandDimension dimension, int size) throws SQLException {
        try (PreparedStatement ps = database.getConnection().prepareStatement("UPDATE island_dimensions SET size = ? WHERE island_id = ? AND dimension = ?;")) {
            ps.setInt(1, size); ps.setLong(2, island.getIslandId()); ps.setString(3, dimension.name()); ps.executeUpdate();
        }
        get(island, dimension).ifPresent(value -> value.setSize(size));
        if (dimension == IslandDimension.OVERWORLD) island.setSize(size);
    }

    public Location defaultHome(Island island, IslandDimension dimension) {
        World world = worlds.get(dimension);
        if (world == null) return null;

        int baseY = dimension == IslandDimension.OVERWORLD
                ? plugin.getConfig().getInt("islands.creation-y", 100)
                : plugin.getConfig().getInt(
                        "dimensions." + dimension.getConfigKey() + ".creation-y",
                        plugin.getConfig().getInt("islands.creation-y", 100));

        double offsetX = plugin.getConfig().getDouble("islands.spawn-offset.x", 0.5);
        double offsetY = plugin.getConfig().getDouble("islands.spawn-offset.y", 3.0);
        double offsetZ = plugin.getConfig().getDouble("islands.spawn-offset.z", 0.5);
        float yaw = (float) plugin.getConfig().getDouble("islands.spawn-offset.yaw", 0.0);
        float pitch = (float) plugin.getConfig().getDouble("islands.spawn-offset.pitch", 0.0);

        return new Location(
                world,
                island.getCenterX() + offsetX,
                baseY + offsetY,
                island.getCenterZ() + offsetZ,
                yaw,
                pitch);
    }

    public int getDefaultSize(IslandDimension dimension) {
        var tiers = plugin.getConfig().getMapList("island_upgrades.size." + dimension.getConfigKey() + ".tiers");
        if (!tiers.isEmpty()) {
            Object value = tiers.get(0).get("value");
            if (value instanceof Number number && number.intValue() > 0) return number.intValue();
        }
        return 50;
    }
}
