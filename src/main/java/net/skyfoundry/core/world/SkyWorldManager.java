package net.skyfoundry.core.world;

import net.skyfoundry.core.SkyFoundry;
import net.skyfoundry.core.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

public final class SkyWorldManager {

    private final SkyFoundry plugin;
    private final ConfigManager configManager;

    private World world;

    public SkyWorldManager(
            SkyFoundry plugin,
            ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void loadOrCreateWorld() {
        String worldName = configManager.getIslandWorldName();

        World existingWorld = Bukkit.getWorld(worldName);

        if (existingWorld != null) {
            world = existingWorld;
            configureWorld(world);
            return;
        }

        plugin.getLogger().info(
                "Creating SkyFoundry void world...");

        WorldCreator creator = new WorldCreator(worldName);

        creator.environment(World.Environment.NORMAL);
        creator.type(WorldType.NORMAL);
        creator.generator(new VoidChunkGenerator());
        creator.generateStructures(false);

        world = creator.createWorld();

        if (world == null) {
            throw new IllegalStateException(
                    "Bukkit failed to create island world.");
        }

        configureWorld(world);
    }

    private void configureWorld(World world) {
        world.setSpawnFlags(false, false);

        world.setGameRule(
                org.bukkit.GameRule.DO_MOB_SPAWNING,
                false);

        world.setGameRule(
                org.bukkit.GameRule.DO_PATROL_SPAWNING,
                false);

        world.setGameRule(
                org.bukkit.GameRule.DO_TRADER_SPAWNING,
                false);

        world.setGameRule(
                org.bukkit.GameRule.DO_INSOMNIA,
                false);
    }

    public World getWorld() {
        if (world == null) {
            throw new IllegalStateException(
                    "SkyFoundry world has not been loaded.");
        }

        return world;
    }
}