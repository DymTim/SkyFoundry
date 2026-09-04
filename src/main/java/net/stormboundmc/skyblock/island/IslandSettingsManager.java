package net.stormboundmc.skyblock.island;

import net.stormboundmc.skyblock.StormboundSkyblock;
import net.stormboundmc.skyblock.storage.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class IslandSettingsManager {

    private final StormboundSkyblock plugin;
    private final Database database;
    private final IslandManager islandManager;
    private final Map<Long, IslandSettings> settingsByIsland = new HashMap<>();

    public IslandSettingsManager(
            StormboundSkyblock plugin,
            Database database,
            IslandManager islandManager
    ) {
        this.plugin = plugin;
        this.database = database;
        this.islandManager = islandManager;
    }

    public void loadSettings() throws SQLException {
        settingsByIsland.clear();

        String sql = """
                SELECT
                    island_id,
                    member_building,
                    member_interactions,
                    visiting_enabled,
                    weather_mode,
                    time_mode,
                    border_enabled
                FROM island_settings;
                """;

        try (
                Statement statement = database.getConnection().createStatement();
                ResultSet resultSet = statement.executeQuery(sql)
        ) {
            while (resultSet.next()) {
                long islandId = resultSet.getLong("island_id");

                IslandSettings settings = new IslandSettings(
                        islandId,
                        resultSet.getInt("member_building") != 0,
                        resultSet.getInt("member_interactions") != 0,
                        resultSet.getInt("visiting_enabled") != 0,
                        parseWeather(resultSet.getString("weather_mode")),
                        parseTime(resultSet.getString("time_mode")),
                        resultSet.getInt("border_enabled") != 0
                );

                settingsByIsland.put(islandId, settings);
            }
        }

        plugin.getLogger().info(
                "Loaded " + settingsByIsland.size() + " island setting record(s)."
        );
    }

    public IslandSettings getSettings(Island island) {
        return getSettings(island.getIslandId());
    }

    public IslandSettings getSettings(long islandId) {
        return settingsByIsland.computeIfAbsent(
                islandId,
                IslandSettings::defaults
        );
    }

    public Optional<IslandSettings> getSettings(UUID playerUuid) {
        return islandManager.getIsland(playerUuid).map(this::getSettings);
    }

    public boolean canManage(UUID playerUuid) {
        IslandRole role = islandManager.getRole(playerUuid).orElse(null);
        return role == IslandRole.OWNER || role == IslandRole.CO_OWNER;
    }

    public boolean setMemberBuilding(UUID actorUuid, boolean enabled) throws SQLException {
        Island island = getManagedIsland(actorUuid);
        if (island == null) {
            return false;
        }

        IslandSettings settings = getSettings(island);
        settings.setMemberBuilding(enabled);
        save(settings);
        return true;
    }

    public boolean setMemberInteractions(UUID actorUuid, boolean enabled) throws SQLException {
        Island island = getManagedIsland(actorUuid);
        if (island == null) {
            return false;
        }

        IslandSettings settings = getSettings(island);
        settings.setMemberInteractions(enabled);
        save(settings);
        return true;
    }

    public boolean setVisitingEnabled(UUID actorUuid, boolean enabled) throws SQLException {
        Island island = getManagedIsland(actorUuid);
        if (island == null) {
            return false;
        }

        IslandSettings settings = getSettings(island);
        settings.setVisitingEnabled(enabled);
        save(settings);
        return true;
    }

    public boolean setWeatherMode(UUID actorUuid, IslandWeatherMode mode) throws SQLException {
        Island island = getManagedIsland(actorUuid);
        if (island == null || mode == null) {
            return false;
        }

        IslandSettings settings = getSettings(island);
        settings.setWeatherMode(mode);
        save(settings);
        return true;
    }

    public boolean setTimeMode(UUID actorUuid, IslandTimeMode mode) throws SQLException {
        Island island = getManagedIsland(actorUuid);
        if (island == null || mode == null) {
            return false;
        }

        IslandSettings settings = getSettings(island);
        settings.setTimeMode(mode);
        save(settings);
        return true;
    }

    public boolean setBorderEnabled(UUID actorUuid, boolean enabled) throws SQLException {
        Island island = getManagedIsland(actorUuid);
        if (island == null) {
            return false;
        }

        IslandSettings settings = getSettings(island);
        settings.setBorderEnabled(enabled);
        save(settings);
        return true;
    }

    private Island getManagedIsland(UUID actorUuid) {
        if (!canManage(actorUuid)) {
            return null;
        }

        return islandManager.getIsland(actorUuid).orElse(null);
    }

    private void save(IslandSettings settings) throws SQLException {
        String sql = """
                INSERT INTO island_settings (
                    island_id,
                    member_building,
                    member_interactions,
                    visiting_enabled,
                    weather_mode,
                    time_mode,
                    border_enabled
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(island_id) DO UPDATE SET
                    member_building = excluded.member_building,
                    member_interactions = excluded.member_interactions,
                    visiting_enabled = excluded.visiting_enabled,
                    weather_mode = excluded.weather_mode,
                    time_mode = excluded.time_mode,
                    border_enabled = excluded.border_enabled;
                """;

        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setLong(1, settings.getIslandId());
            statement.setInt(2, settings.isMemberBuilding() ? 1 : 0);
            statement.setInt(3, settings.isMemberInteractions() ? 1 : 0);
            statement.setInt(4, settings.isVisitingEnabled() ? 1 : 0);
            statement.setString(5, settings.getWeatherMode().name());
            statement.setString(6, settings.getTimeMode().name());
            statement.setInt(7, settings.isBorderEnabled() ? 1 : 0);
            statement.executeUpdate();
        }
    }

    private IslandWeatherMode parseWeather(String value) {
        try {
            return IslandWeatherMode.valueOf(value == null ? "DEFAULT" : value);
        } catch (IllegalArgumentException exception) {
            return IslandWeatherMode.DEFAULT;
        }
    }

    private IslandTimeMode parseTime(String value) {
        try {
            return IslandTimeMode.valueOf(value == null ? "DEFAULT" : value);
        } catch (IllegalArgumentException exception) {
            return IslandTimeMode.DEFAULT;
        }
    }
}
