package net.skyfoundry.core.protection.listener;

import net.skyfoundry.core.protection.IslandProtectionService;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

public final class PistonProtectionListener
        implements Listener {

    private final IslandProtectionService protectionService;

    public PistonProtectionListener(
            IslandProtectionService protectionService) {
        this.protectionService = protectionService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(
            BlockPistonExtendEvent event) {
        if (!isMovementAllowed(
                event.getBlock(),
                event.getDirection(),
                event.getBlocks())) {

            event.setCancelled(
                    true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(
            BlockPistonRetractEvent event) {
        if (!isMovementAllowed(
                event.getBlock(),
                event.getDirection(),
                event.getBlocks())) {

            event.setCancelled(
                    true);
        }
    }

    private boolean isMovementAllowed(
            Block piston,
            BlockFace direction,
            Iterable<Block> movedBlocks) {
        Location pistonLocation = piston.getLocation();

        if (!protectionService.isManagedWorld(
                pistonLocation)) {
            return true;
        }

        /*
         * Pistons cannot operate in unallocated
         * island-world space.
         */
        if (!protectionService
                .isInsideAllocatedIsland(
                        pistonLocation)) {

            return false;
        }

        for (Block block : movedBlocks) {

            Location source = block.getLocation();

            Location destination = block.getRelative(
                    direction).getLocation();

            if (!protectionService.canAffect(
                    pistonLocation,
                    source)) {
                return false;
            }

            if (!protectionService.canAffect(
                    source,
                    destination)) {
                return false;
            }
        }

        return true;
    }
}