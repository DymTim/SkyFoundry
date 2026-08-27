package net.skyfoundry.core.config;

import net.skyfoundry.core.SkyFoundry;
import org.bukkit.configuration.file.FileConfiguration;

public final class ConfigManager {

    private final SkyFoundry plugin;

    private FileConfiguration config;

    public ConfigManager(SkyFoundry plugin) {
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