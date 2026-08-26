package net.skyfoundry.core.service;

import net.skyfoundry.core.SkyFoundry;
import net.skyfoundry.core.config.ConfigManager;
import net.skyfoundry.core.island.Island;
import net.skyfoundry.core.world.SkyWorldManager;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public final class IslandRegionService {

    private final SkyFoundry plugin;
    private final ConfigManager configManager;
    private final SkyWorldManager skyWorldManager;

    private final Set<Long> activeIslands = new HashSet<>();

    public IslandRegionService(
            SkyFoundry plugin,
            ConfigManager configManager,
            SkyWorldManager skyWorldManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.skyWorldManager = skyWorldManager;
    }

    public boolean isBusy(long islandId) {
        return activeIslands.contains(
                islandId);
    }

    public void clearIsland(
            Island island,
            Runnable onComplete,
            Consumer<Throwable> onFailure) {
        if (!activeIslands.add(
                island.getId())) {

            throw new IllegalStateException(
                    "That island is already being modified.");
        }

        World world = skyWorldManager.getWorld();

        removeEntities(
                island,
                world);

        int blocksPerTick = configManager
                .getRegionClearBlocksPerTick();

        new BukkitRunnable() {

            private int x = island.getMinimumX();

            private int z = island.getMinimumZ();

            private int y = world.getMinHeight();

            private final int maxY = world.getMaxHeight();

            @Override
            public void run() {
                try {
                    int processed = 0;

                    while (processed < blocksPerTick) {

                        if (x > island.getMaximumX()) {
                            finish();
                            return;
                        }

                        if (!world
                                .getBlockAt(x, y, z)
                                .getType()
                                .isAir()) {

                            world.getBlockAt(
                                    x,
                                    y,
                                    z).setType(
                                            Material.AIR,
                                            false);
                        }

                        processed++;

                        advanceCursor();
                    }

                } catch (Throwable throwable) {

                    activeIslands.remove(
                            island.getId());

                    cancel();

                    onFailure.accept(
                            throwable);
                }
            }

            private void advanceCursor() {
                y++;

                if (y >= maxY) {
                    y = world.getMinHeight();
                    z++;
                }

                if (z > island.getMaximumZ()) {
                    z = island.getMinimumZ();
                    x++;
                }
            }

            private void finish() {
                removeEntities(
                        island,
                        world);

                activeIslands.remove(
                        island.getId());

                cancel();

                onComplete.run();
            }

        }.runTaskTimer(
                plugin,
                1L,
                1L);
    }

    private void removeEntities(
            Island island,
            World world) {
        for (Entity entity : world.getEntities()) {

            if (entity instanceof Player) {
                continue;
            }

            if (!island.contains(
                    entity.getLocation().getBlockX(),
                    entity.getLocation().getBlockZ())) {
                continue;
            }

            entity.remove();
        }
    }
}