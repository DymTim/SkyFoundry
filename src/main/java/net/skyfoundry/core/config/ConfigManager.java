package net.skyfoundry.core.config;

import net.skyfoundry.core.SkyFoundry;
import net.skyfoundry.core.progression.mission.DailyMissionDefinition;
import net.skyfoundry.core.progression.mission.MissionActionType;
import net.skyfoundry.core.progression.mission.MissionCategory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ConfigManager {

    private final SkyFoundry plugin;

    private FileConfiguration config;

    public ConfigManager(
            SkyFoundry plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        config = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();

        config = plugin.getConfig();
    }

    public String getIslandWorldName() {
        return config.getString(
                "world.name",
                "skyfoundry_islands");
    }

    public int getStartingIslandSize() {
        return config.getInt(
                "islands.starting-size",
                50);
    }

    public int getMaximumIslandSize() {
        return config.getInt(
                "islands.maximum-size",
                300);
    }

    public int getIslandUpgradeAmount() {
        return config.getInt(
                "islands.size-upgrade-amount",
                50);
    }

    public int getIslandSpacing() {
        return config.getInt(
                "islands.spacing",
                500);
    }

    public boolean isStarterPlatformEnabled() {
        return config.getBoolean(
                "islands.starter-platform.enabled",
                true);
    }

    public int getStarterPlatformY() {
        return config.getInt(
                "islands.starter-platform.y",
                100);
    }

    public int getStarterPlatformRadius() {
        return config.getInt(
                "islands.starter-platform.radius",
                2);
    }

    public int getDefaultMemberLimit() {
        return config.getInt(
                "members.default-limit",
                5);
    }

    public int getInviteExpirationSeconds() {
        return config.getInt(
                "invites.expiration-seconds",
                60);
    }

    public int getConfirmationExpirationSeconds() {
        return config.getInt(
                "confirmations.expiration-seconds",
                30);
    }

    public int getDefaultLifetimeResets() {
        return config.getInt(
                "reset.default-lifetime-resets",
                1);
    }

    public int getRegionClearBlocksPerTick() {
        return Math.max(
                1,
                config.getInt(
                        "region-clear.blocks-per-tick",
                        15000));
    }

    public boolean isUpgradeManagementRoleRequired() {
        return config.getBoolean(
                "progression.upgrades.require-management-role",
                true);
    }

    public long getIslandLevelBaseXp() {
        return Math.max(
                1L,
                config.getLong(
                        "progression.level.base-xp",
                        100L));
    }

    public double getIslandLevelExponent() {
        return Math.max(
                1.0,
                config.getDouble(
                        "progression.level.exponent",
                        2.0));
    }

    public double getBlockValueContributionMultiplier() {
        return Math.max(
                0.0,
                config.getDouble(
                        "progression.block-value.contribution-multiplier",
                        5.0));
    }

    public Map<String, Long> getBlockValues() {
        Map<String, Long> values = new HashMap<>();

        ConfigurationSection section = config.getConfigurationSection(
                "progression.block-value.values");

        if (section == null) {
            return values;
        }

        for (String key : section.getKeys(false)) {

            long value = Math.max(
                    0L,
                    section.getLong(
                            key,
                            0L));

            values.put(
                    key.toLowerCase(),
                    value);
        }

        return values;
    }

    public int getDailyMissionCount() {
        return Math.max(
                1,
                config.getInt(
                        "progression.daily-missions.count",
                        5));
    }

    public List<DailyMissionDefinition> getDailyMissionDefinitions() {

        List<DailyMissionDefinition> definitions = new ArrayList<>();

        ConfigurationSection templates = config.getConfigurationSection(
                "progression.daily-missions.templates");

        if (templates == null) {
            return definitions;
        }

        for (String missionId : templates.getKeys(false)) {

            ConfigurationSection section = templates.getConfigurationSection(
                    missionId);

            if (section == null) {
                continue;
            }

            String name = section.getString(
                    "name",
                    missionId);

            String rawCategory = section.getString(
                    "category",
                    "");

            String rawType = section.getString(
                    "type",
                    "");

            MissionCategory category;

            try {
                category = MissionCategory.valueOf(
                        rawCategory.toUpperCase());

            } catch (IllegalArgumentException exception) {

                plugin.getLogger().warning(
                        "Ignoring daily mission '"
                                + missionId
                                + "' because category '"
                                + rawCategory
                                + "' is invalid.");

                continue;
            }

            MissionActionType actionType;

            try {
                actionType = MissionActionType.valueOf(
                        rawType.toUpperCase());

            } catch (IllegalArgumentException exception) {

                plugin.getLogger().warning(
                        "Ignoring daily mission '"
                                + missionId
                                + "' because type '"
                                + rawType
                                + "' is invalid.");

                continue;
            }

            int minimumIslandLevel = Math.max(
                    0,
                    section.getInt(
                            "minimum-island-level",
                            0));

            List<String> targets = new ArrayList<>();

            for (String target : section.getStringList(
                    "targets")) {

                if (target == null
                        || target.isBlank()) {

                    continue;
                }

                targets.add(
                        target.toLowerCase());
            }

            if (targets.isEmpty()) {

                plugin.getLogger().warning(
                        "Ignoring daily mission '"
                                + missionId
                                + "' because it has no targets.");

                continue;
            }

            ConfigurationSection amountSection = section.getConfigurationSection(
                    "amount");

            if (amountSection == null) {

                plugin.getLogger().warning(
                        "Ignoring daily mission '"
                                + missionId
                                + "' because it has no amount section.");

                continue;
            }

            int minimumAmount = Math.max(
                    1,
                    amountSection.getInt(
                            "minimum",
                            1));

            int maximumAmount = Math.max(
                    minimumAmount,
                    amountSection.getInt(
                            "maximum",
                            minimumAmount));

            int amountPerLevel = Math.max(
                    0,
                    amountSection.getInt(
                            "per-level",
                            0));

            long xpReward = Math.max(
                    0L,
                    section.getLong(
                            "xp",
                            0L));

            definitions.add(
                    new DailyMissionDefinition(
                            missionId,
                            name,
                            category,
                            actionType,
                            minimumIslandLevel,
                            List.copyOf(
                                    targets),
                            minimumAmount,
                            maximumAmount,
                            amountPerLevel,
                            xpReward));
        }

        return definitions;
    }

    public int getBoundaryDurationSeconds() {
        return Math.max(
                1,
                config.getInt(
                        "boundary.duration-seconds",
                        10));
    }

    public int getBoundaryParticleSpacing() {
        return Math.max(
                1,
                config.getInt(
                        "boundary.particle-spacing",
                        2));
    }

    public int getBoundaryUpdateTicks() {
        return Math.max(
                1,
                config.getInt(
                        "boundary.update-ticks",
                        10));
    }

    public boolean isDebugEnabled() {
        return config.getBoolean(
                "debug",
                false);
    }
}