package net.skyfoundry.core.protection.listener;

import net.skyfoundry.core.protection.IslandProtectionService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public final class BlockProtectionListener
        implements Listener {

    private final IslandProtectionService protectionService;

    public BlockProtectionListener(
            IslandProtectionService protectionService) {
        this.protectionService = protectionService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(
            BlockBreakEvent event) {
        if (protectionService.canModify(
                event.getPlayer(),
                event.getBlock().getLocation())) {
            return;
        }

        event.setCancelled(true);

        event.getPlayer().sendMessage(
                "§cYou cannot break blocks here.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(
            BlockPlaceEvent event) {
        if (protectionService.canModify(
                event.getPlayer(),
                event.getBlockPlaced().getLocation())) {
            return;
        }

        event.setCancelled(true);

        event.getPlayer().sendMessage(
                "§cYou cannot place blocks here.");
    }
}