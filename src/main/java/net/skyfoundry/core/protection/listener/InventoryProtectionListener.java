package net.skyfoundry.core.protection.listener;

import net.skyfoundry.core.protection.IslandProtectionService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;

public final class InventoryProtectionListener
        implements Listener {

    private final IslandProtectionService protectionService;

    public InventoryProtectionListener(
            IslandProtectionService protectionService) {
        this.protectionService = protectionService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryOpen(
            InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {

            return;
        }

        Location location = event.getInventory()
                .getLocation();

        if (location == null) {
            return;
        }

        if (protectionService.canInteract(
                player,
                location)) {
            return;
        }

        event.setCancelled(
                true);

        player.sendMessage(
                "§cYou cannot open containers here.");
    }
}