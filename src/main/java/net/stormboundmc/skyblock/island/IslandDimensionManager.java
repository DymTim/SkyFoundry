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
             ResultSet rs = statement.executeQuery("SELECT island_id, dimension, generated, size, home_x, home_y, home_z, home_yaw, home_pitch FROM island_dimensions;")) {
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
                        .put(dimension, new IslandDimensionData(rs.getLong("island_id"), dimension, rs.getInt("generated") != 0, rs.getInt("size"), home));
            }
        }
        for (Island island : islands) ensureRecords(island);
    }

    public void ensureRecords(Island island) throws SQLException {
        EnumMap<IslandDimension, IslandDimensionData> map = data.computeIfAbsent(island.getIslandId(), id -> new EnumMap<>(IslandDimension.class));
        for (IslandDimension dimension : IslandDimension.values()) {
            if (map.containsKey(dimension)) continue;
            int size = dimension == IslandDimension.OVERWORLD ? island.getSize() : getDefaultSize(dimension);
            boolean generated = dimension == IslandDimension.OVERWORLD;
            Location home = dimension == IslandDimension.OVERWORLD ? island.getHome() : defaultHome(island, dimension);
            insert(island.getIslandId(), dimension, generated, size, home);
            map.put(dimension, new IslandDimensionData(island.getIslandId(), dimension, generated, size, home));
        }
    }

    private void insert(long islandId, IslandDimension dimension, boolean generated, int size, Location home) throws SQLException {
        String sql = "INSERT OR IGNORE INTO island_dimensions (island_id, dimension, generated, size, home_x, home_y, home_z, home_yaw, home_pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setLong(1, islandId); ps.setString(2, dimension.name()); ps.setInt(3, generated ? 1 : 0); ps.setInt(4, size);
            if (home == null) { for (int i = 5; i <= 9; i++) ps.setObject(i, null); }
            else { ps.setDouble(5, home.getX()); ps.setDouble(6, home.getY()); ps.setDouble(7, home.getZ()); ps.setFloat(8, home.getYaw()); ps.setFloat(9, home.getPitch()); }
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
        int y = plugin.getConfig().getInt("dimensions." + dimension.getConfigKey() + ".creation-y", plugin.getConfig().getInt("islands.creation-y", 100));
        return new Location(world, island.getCenterX() + 0.5, y + 3.0, island.getCenterZ() + 0.5);
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
