package net.skyfoundry.core.protection.listener;

import net.skyfoundry.core.protection.IslandProtectionService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;

public final class FluidProtectionListener
        implements Listener {

    private final IslandProtectionService protectionService;

    public FluidProtectionListener(
            IslandProtectionService protectionService) {
        this.protectionService = protectionService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFluidFlow(
            BlockFromToEvent event) {
        if (!protectionService.isManagedWorld(
                event.getBlock().getLocation())
                && !protectionService.isManagedWorld(
                        event.getToBlock().getLocation())) {

            return;
        }

        if (protectionService.canAffect(
                event.getBlock().getLocation(),
                event.getToBlock().getLocation())) {
            return;
        }

        event.setCancelled(
                true);
    }
}