package net.skyfoundry.core.protection.listener;

import net.skyfoundry.core.protection.IslandProtectionService;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public final class ExplosionProtectionListener
        implements Listener {

    private final IslandProtectionService protectionService;

    public ExplosionProtectionListener(
            IslandProtectionService protectionService) {
        this.protectionService = protectionService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(
            EntityExplodeEvent event) {
        Location source = event.getLocation();

        if (!protectionService.isManagedWorld(
                source)) {
            return;
        }

        event.blockList().removeIf(
                block -> !protectionService.canAffect(
                        source,
                        block.getLocation()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(
            BlockExplodeEvent event) {
        Block sourceBlock = event.getBlock();

        Location source = sourceBlock.getLocation();

        if (!protectionService.isManagedWorld(
                source)) {
            return;
        }

        event.blockList().removeIf(
                block -> !protectionService.canAffect(
                        source,
                        block.getLocation()));
    }
}