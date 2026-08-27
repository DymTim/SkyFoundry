package net.skyfoundry.core.protection.listener;

import net.skyfoundry.core.protection.IslandProtectionService;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;

public final class AutomationProtectionListener
        implements Listener {

    private final IslandProtectionService protectionService;

    public AutomationProtectionListener(
            IslandProtectionService protectionService) {
        this.protectionService = protectionService;
    }

    /**
     * Covers vanilla and potentially modded entities
     * that modify blocks through Bukkit's event bridge.
     *
     * Examples may include:
     * falling blocks,
     * Endermen,
     * certain automation entities,
     * modded fake/automation entities.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityChangeBlock(
            EntityChangeBlockEvent event) {
        Location source = event.getEntity().getLocation();

        Location target = event.getBlock().getLocation();

        if (!protectionService.isManagedWorld(
                target)) {
            return;
        }

        if (protectionService.canAffect(
                source,
                target)) {
            return;
        }

        event.setCancelled(
                true);
    }

    /**
     * Prevents fire and similar block spreading
     * across island boundaries.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockSpread(
            BlockSpreadEvent event) {
        if (!protectionService.isManagedWorld(
                event.getBlock().getLocation())
                && !protectionService.isManagedWorld(
                        event.getSource().getLocation())) {

            return;
        }

        if (protectionService.canAffect(
                event.getSource().getLocation(),
                event.getBlock().getLocation())) {
            return;
        }

        event.setCancelled(
                true);
    }

    /**
     * Prevents Bukkit-visible inventory automation
     * from moving items between islands.
     *
     * This covers vanilla hoppers and may cover
     * modded inventories that bridge into Bukkit.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryMove(
            InventoryMoveItemEvent event) {
        Location source = event.getSource().getLocation();

        Location destination = event.getDestination().getLocation();

        if (source == null
                || destination == null) {

            return;
        }

        if (!protectionService.isManagedWorld(
                source)
                && !protectionService.isManagedWorld(
                        destination)) {

            return;
        }

        if (protectionService.canAffect(
                source,
                destination)) {
            return;
        }

        event.setCancelled(
                true);
    }

    /**
     * Prevents inventories from collecting items
     * across an island boundary where Bukkit exposes
     * the inventory location.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryPickup(
            InventoryPickupItemEvent event) {
        Location inventoryLocation = event.getInventory()
                .getLocation();

        if (inventoryLocation == null) {
            return;
        }

        Location itemLocation = event.getItem()
                .getLocation();

        if (!protectionService.isManagedWorld(
                inventoryLocation)
                && !protectionService.isManagedWorld(
                        itemLocation)) {

            return;
        }

        if (protectionService.canAffect(
                itemLocation,
                inventoryLocation)) {
            return;
        }

        event.setCancelled(
                true);
    }
}