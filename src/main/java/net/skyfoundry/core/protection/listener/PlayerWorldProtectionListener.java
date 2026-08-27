package net.skyfoundry.core.protection.listener;

import net.skyfoundry.core.protection.IslandProtectionService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

public final class PlayerWorldProtectionListener
        implements Listener {

    private final IslandProtectionService protectionService;

    public PlayerWorldProtectionListener(
            IslandProtectionService protectionService) {
        this.protectionService = protectionService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(
            PlayerBucketEmptyEvent event) {
        if (protectionService.canModify(
                event.getPlayer(),
                event.getBlock().getLocation())) {
            return;
        }

        event.setCancelled(true);

        event.getPlayer().sendMessage(
                "§cYou cannot empty buckets here.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(
            PlayerBucketFillEvent event) {
        if (protectionService.canModify(
                event.getPlayer(),
                event.getBlock().getLocation())) {
            return;
        }

        event.setCancelled(true);

        event.getPlayer().sendMessage(
                "§cYou cannot fill buckets here.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onIgnite(
            BlockIgniteEvent event) {
        if (event.getPlayer() == null) {
            return;
        }

        if (protectionService.canModify(
                event.getPlayer(),
                event.getBlock().getLocation())) {
            return;
        }

        event.setCancelled(true);

        event.getPlayer().sendMessage(
                "§cYou cannot ignite blocks here.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFertilize(
            BlockFertilizeEvent event) {
        if (event.getPlayer() == null) {
            return;
        }

        /*
         * Check the source block first.
         */
        if (!protectionService.canModify(
                event.getPlayer(),
                event.getBlock().getLocation())) {
            event.setCancelled(true);

            event.getPlayer().sendMessage(
                    "§cYou cannot modify blocks here.");

            return;
        }

        /*
         * Fertilization can change multiple blocks.
         * Every resulting block must remain inside
         * the player's island.
         */
        boolean invalid = event.getBlocks()
                .stream()
                .anyMatch(
                        blockState -> !protectionService.canModify(
                                event.getPlayer(),
                                blockState
                                        .getLocation()));

        if (invalid) {
            event.setCancelled(true);

            event.getPlayer().sendMessage(
                    "§cThat action would affect blocks outside your island.");
        }
    }
}