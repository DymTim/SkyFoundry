package net.skyfoundry.core.protection.listener;

import net.skyfoundry.core.protection.IslandProtectionService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class InteractionProtectionListener
        implements Listener {

    private final IslandProtectionService protectionService;

    public InteractionProtectionListener(
            IslandProtectionService protectionService) {
        this.protectionService = protectionService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(
            PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }

        if (protectionService.canInteract(
                event.getPlayer(),
                event.getClickedBlock().getLocation())) {
            return;
        }

        event.setCancelled(
                true);

        if (event.getAction() != Action.PHYSICAL) {

            event.getPlayer().sendMessage(
                    "§cYou cannot interact with blocks here.");
        }
    }
}