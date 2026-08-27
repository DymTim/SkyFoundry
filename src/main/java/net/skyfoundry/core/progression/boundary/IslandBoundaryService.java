package net.skyfoundry.core.progression.boundary;

import net.skyfoundry.core.SkyFoundry;
import net.skyfoundry.core.config.ConfigManager;
import net.skyfoundry.core.island.Island;
import net.skyfoundry.core.island.IslandManager;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class IslandBoundaryService {

    private final SkyFoundry plugin;

    private final ConfigManager configManager;

    private final IslandManager islandManager;

    private final Map<UUID, BukkitTask> activeDisplays = new HashMap<>();

    public IslandBoundaryService(
            SkyFoundry plugin,
            ConfigManager configManager,
            IslandManager islandManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.islandManager = islandManager;
    }

    public void showBoundary(
            Player player) {
        Island island = islandManager.getIsland(
                player.getUniqueId()).orElseThrow(
                        () -> new IllegalStateException(
                                "You do not belong to an island."));

        if (!player.getWorld()
                .getName()
                .equals(
                        island.getWorldName())) {

            throw new IllegalStateException(
                    "You must be on your island to view its boundary.");
        }

        stopBoundary(
                player.getUniqueId());

        int updateTicks = configManager.getBoundaryUpdateTicks();

        int durationTicks = configManager.getBoundaryDurationSeconds()
                * 20;

        BukkitRunnable runnable = new BukkitRunnable() {

            private int elapsedTicks = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    stop();
                    return;
                }

                if (!player.getWorld()
                        .getName()
                        .equals(
                                island.getWorldName())) {

                    stop();
                    return;
                }

                showFrame(
                        player,
                        island);

                elapsedTicks += updateTicks;

                if (elapsedTicks >= durationTicks) {
                    stop();
                }
            }

            private void stop() {
                activeDisplays.remove(
                        player.getUniqueId());

                cancel();
            }
        };

        BukkitTask task = runnable.runTaskTimer(
                plugin,
                0L,
                updateTicks);

        activeDisplays.put(
                player.getUniqueId(),
                task);
    }

    public void stopBoundary(
            UUID playerUuid) {
        BukkitTask existing = activeDisplays.remove(
                playerUuid);

        if (existing != null) {
            existing.cancel();
        }
    }

    private void showFrame(
            Player player,
            Island island) {
        World world = player.getWorld();

        int spacing = configManager.getBoundaryParticleSpacing();

        double y = Math.max(
                world.getMinHeight() + 1,
                player.getLocation().getY() + 0.25);

        int minimumX = island.getMinimumX();

        int maximumX = island.getMaximumX();

        int minimumZ = island.getMinimumZ();

        int maximumZ = island.getMaximumZ();

        for (int x = minimumX; x <= maximumX; x += spacing) {

            spawnParticle(
                    player,
                    x + 0.5,
                    y,
                    minimumZ + 0.5);

            spawnParticle(
                    player,
                    x + 0.5,
                    y,
                    maximumZ + 0.5);
        }

        for (int z = minimumZ; z <= maximumZ; z += spacing) {

            spawnParticle(
                    player,
                    minimumX + 0.5,
                    y,
                    z + 0.5);

            spawnParticle(
                    player,
                    maximumX + 0.5,
                    y,
                    z + 0.5);
        }

        /*
         * Ensure all four corners are shown even
         * if the configured spacing doesn't land
         * exactly on them.
         */

        spawnParticle(
                player,
                minimumX + 0.5,
                y,
                minimumZ + 0.5);

        spawnParticle(
                player,
                maximumX + 0.5,
                y,
                minimumZ + 0.5);

        spawnParticle(
                player,
                minimumX + 0.5,
                y,
                maximumZ + 0.5);

        spawnParticle(
                player,
                maximumX + 0.5,
                y,
                maximumZ + 0.5);
    }

    private void spawnParticle(
            Player player,
            double x,
            double y,
            double z) {
        player.spawnParticle(
                Particle.END_ROD,
                new Location(
                        player.getWorld(),
                        x,
                        y,
                        z),
                1,
                0,
                0,
                0,
                0);
    }
}