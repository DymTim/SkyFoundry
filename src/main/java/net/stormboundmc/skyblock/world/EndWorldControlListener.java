package net.stormboundmc.skyblock.world;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.boss.DragonBattle;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.entity.EntityCreatePortalEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Removes vanilla End progression mechanics from Stormbound's private End
 * island world. The Nether/End island system handles progression itself, so
 * the vanilla dragon arena, exit fountain, and outer-island gateways must not
 * interfere with islands placed around 0,0.
 */
public final class EndWorldControlListener implements Listener {

        private static final int EXIT_PORTAL_SCAN_RADIUS = 12;
        private static final int EXIT_PORTAL_BEDROCK_RADIUS = 4;
        private static final int EXIT_PORTAL_BEDROCK_HEIGHT = 5;

        private final JavaPlugin plugin;
        private final World endWorld;
        private final boolean disableDragon;
        private final boolean disableExitPortal;
        private final boolean disableGateways;

        public EndWorldControlListener(JavaPlugin plugin, World endWorld) {
                this.plugin = plugin;
                this.endWorld = endWorld;

                String path = "dimensions.end.vanilla-end.";
                this.disableDragon = plugin.getConfig().getBoolean(path + "disable-dragon", true);
                this.disableExitPortal = plugin.getConfig().getBoolean(path + "disable-exit-portal", true);
                this.disableGateways = plugin.getConfig().getBoolean(path + "disable-gateways", true);

        }

        /**
         * Neutralizes Minecraft's built-in dragon fight state before Stormbound begins
         * loading island data or pasting an End starter schematic.
         */
        public void initializeWorldState() {
                neutralizeDragonBattle();
                cleanupWorld();

                // EndDragonFight can finish its legacy scan a few ticks after the world
                // becomes available. Re-assert the disabled state while that initialization
                // settles, before players normally generate their first End island.
                long[] delays = {1L, 20L, 100L, 200L};
                for (long delay : delays) {
                        plugin.getServer().getScheduler().runTaskLater(
                                        plugin,
                                        () -> {
                                                neutralizeDragonBattle();
                                                cleanupWorld();
                                        },
                                        delay);
                }
        }

        /**
         * Called immediately before an End starter island is pasted at 0,0.
         */
        public void prepareForIslandPaste() {
                neutralizeDragonBattle();
                cleanupWorld();
        }

        private void neutralizeDragonBattle() {
                if (!disableDragon && !disableExitPortal && !disableGateways) {
                        return;
                }

                DragonBattle battle = endWorld.getEnderDragonBattle();
                if (battle == null) {
                        return;
                }

                // Mark the vanilla fight as already completed so Minecraft does not keep
                // initializing a fresh first-dragon encounter in this private SkyBlock End.
                try {
                        battle.setPreviouslyKilled(true);
                } catch (RuntimeException exception) {
                        plugin.getLogger().warning(
                                        "Could not mark the Stormbound End dragon battle as completed: "
                                                        + exception.getMessage());
                }

                try {
                        battle.getBossBar().setVisible(false);
                } catch (RuntimeException ignored) {
                        // Some hybrid servers may not expose the battle boss bar immediately.
                }

                if (disableDragon) {
                        try {
                                EnderDragon dragon = battle.getEnderDragon();
                                if (dragon != null) {
                                        dragon.remove();
                                }
                        } catch (RuntimeException ignored) {
                                // The battle may still be completing its initial scan. The
                                // delayed cleanup passes and spawn listener handle that case.
                        }
                }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onDragonSpawn(EntitySpawnEvent event) {
                if (!disableDragon || event.getEntity().getWorld() != endWorld) {
                        return;
                }

                if (event.getEntity() instanceof EnderDragon) {
                        event.setCancelled(true);
                }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onDragonCreatePortal(EntityCreatePortalEvent event) {
                if (!disableExitPortal || event.getEntity().getWorld() != endWorld) {
                        return;
                }

                if (event.getEntity() instanceof EnderDragon) {
                        event.setCancelled(true);
                }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onVanillaEndBlockForm(BlockFormEvent event) {
                if (event.getBlock().getWorld() != endWorld) {
                        return;
                }

                Material formed = event.getNewState().getType();

                if (disableExitPortal && formed == Material.END_PORTAL) {
                        event.setCancelled(true);
                        return;
                }

                if (disableGateways && formed == Material.END_GATEWAY) {
                        event.setCancelled(true);
                }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEndTeleport(PlayerTeleportEvent event) {
                if (event.getFrom().getWorld() != endWorld) {
                        return;
                }

                PlayerTeleportEvent.TeleportCause cause = event.getCause();

                if (disableGateways && cause == PlayerTeleportEvent.TeleportCause.END_GATEWAY) {
                        event.setCancelled(true);
                        return;
                }

                if (disableExitPortal && cause == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
                        event.setCancelled(true);
                }
        }

        @EventHandler
        public void onWorldLoad(WorldLoadEvent event) {
                if (event.getWorld() != endWorld) {
                        return;
                }

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                        neutralizeDragonBattle();
                        cleanupWorld();
                });
        }

        @EventHandler
        public void onChunkLoad(ChunkLoadEvent event) {
                if (event.getWorld() != endWorld) {
                        return;
                }

                if (disableGateways) {
                        removeGateways(event.getChunk());
                }

                // Vanilla's exit fountain is centered at 0,0. Recheck the origin when
                // nearby chunks load so a late vanilla End initialization cannot restore it.
                if (disableExitPortal
                                && Math.abs(event.getChunk().getX()) <= 1
                                && Math.abs(event.getChunk().getZ()) <= 1) {
                        plugin.getServer().getScheduler().runTask(plugin, this::removeExitPortal);
                }
        }

        private void cleanupWorld() {
                if (disableDragon) {
                        for (Entity entity : new ArrayList<>(endWorld.getEntities())) {
                                if (entity instanceof EnderDragon) {
                                        entity.remove();
                                }
                        }
                }

                if (disableExitPortal) {
                        removeExitPortal();
                }

                if (disableGateways) {
                        for (Chunk chunk : endWorld.getLoadedChunks()) {
                                removeGateways(chunk);
                        }
                }
        }

        private void removeGateways(Chunk chunk) {
                for (BlockState state : chunk.getTileEntities()) {
                        if (state.getType() == Material.END_GATEWAY) {
                                state.getBlock().setType(Material.AIR, false);
                        }
                }
        }

        private void removeExitPortal() {
                List<Block> portalBlocks = new ArrayList<>();

                for (int x = -EXIT_PORTAL_SCAN_RADIUS; x <= EXIT_PORTAL_SCAN_RADIUS; x++) {
                        for (int z = -EXIT_PORTAL_SCAN_RADIUS; z <= EXIT_PORTAL_SCAN_RADIUS; z++) {
                                for (int y = endWorld.getMinHeight(); y < endWorld.getMaxHeight(); y++) {
                                        Block block = endWorld.getBlockAt(x, y, z);
                                        if (block.getType() == Material.END_PORTAL) {
                                                portalBlocks.add(block);
                                        }
                                }
                        }
                }

                if (portalBlocks.isEmpty()) {
                        return;
                }

                // Remove the portal blocks first.
                for (Block portal : portalBlocks) {
                        portal.setType(Material.AIR, false);
                }

                // Only remove bedrock that is immediately around an exit portal we actually
                // found. This avoids globally deleting legitimate bedrock from player builds.
                for (Block portal : portalBlocks) {
                        int minY = Math.max(endWorld.getMinHeight(), portal.getY() - EXIT_PORTAL_BEDROCK_HEIGHT);
                        int maxY = Math.min(endWorld.getMaxHeight() - 1, portal.getY() + EXIT_PORTAL_BEDROCK_HEIGHT);

                        for (int x = portal.getX() - EXIT_PORTAL_BEDROCK_RADIUS;
                                        x <= portal.getX() + EXIT_PORTAL_BEDROCK_RADIUS;
                                        x++) {
                                for (int z = portal.getZ() - EXIT_PORTAL_BEDROCK_RADIUS;
                                                z <= portal.getZ() + EXIT_PORTAL_BEDROCK_RADIUS;
                                                z++) {
                                        for (int y = minY; y <= maxY; y++) {
                                                Block block = endWorld.getBlockAt(x, y, z);
                                                if (block.getType() == Material.BEDROCK) {
                                                        block.setType(Material.AIR, false);
                                                }
                                        }
                                }
                        }
                }
        }
}
