package net.skyfoundry.core.protection.listener;

import net.skyfoundry.core.protection.IslandProtectionService;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

public final class EntityProtectionListener
        implements Listener {

    private final IslandProtectionService protectionService;

    public EntityProtectionListener(
            IslandProtectionService protectionService) {
        this.protectionService = protectionService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityInteract(
            PlayerInteractEntityEvent event) {
        if (protectionService.canInteract(
                event.getPlayer(),
                event.getRightClicked().getLocation())) {
            return;
        }

        event.setCancelled(true);

        event.getPlayer().sendMessage(
                "§cYou cannot interact with entities here.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArmorStandManipulate(
            PlayerArmorStandManipulateEvent event) {
        if (protectionService.canInteract(
                event.getPlayer(),
                event.getRightClicked().getLocation())) {
            return;
        }

        event.setCancelled(true);

        event.getPlayer().sendMessage(
                "§cYou cannot modify armor stands here.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityPlace(
            EntityPlaceEvent event) {
        Player player = event.getPlayer();

        if (player == null) {
            return;
        }

        if (protectionService.canModify(
                player,
                event.getEntity().getLocation())) {
            return;
        }

        event.setCancelled(true);

        player.sendMessage(
                "§cYou cannot place entities here.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingPlace(
            HangingPlaceEvent event) {
        Player player = event.getPlayer();

        if (player == null) {
            return;
        }

        if (protectionService.canModify(
                player,
                event.getEntity().getLocation())) {
            return;
        }

        event.setCancelled(true);

        player.sendMessage(
                "§cYou cannot place hanging entities here.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreak(
            HangingBreakByEntityEvent event) {
        Entity remover = event.getRemover();

        if (remover == null) {
            return;
        }

        Location target = event.getEntity().getLocation();

        Player responsiblePlayer = resolvePlayer(remover);

        if (responsiblePlayer != null) {

            if (protectionService.canModify(
                    responsiblePlayer,
                    target)) {
                return;
            }

            event.setCancelled(true);

            responsiblePlayer.sendMessage(
                    "§cYou cannot break hanging entities here.");

            return;
        }

        if (!protectionService.canAffect(
                remover.getLocation(),
                target)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(
            EntityDamageByEntityEvent event) {
        Location targetLocation = event.getEntity().getLocation();

        if (!protectionService.isManagedWorld(
                targetLocation)) {
            return;
        }

        Player responsiblePlayer = resolvePlayer(
                event.getDamager());

        if (responsiblePlayer != null) {

            if (protectionService.canModify(
                    responsiblePlayer,
                    targetLocation)) {
                return;
            }

            event.setCancelled(true);

            responsiblePlayer.sendMessage(
                    "§cYou cannot damage entities here.");

            return;
        }

        if (!protectionService.canAffect(
                event.getDamager().getLocation(),
                targetLocation)) {
            event.setCancelled(true);
        }
    }

    private Player resolvePlayer(
            Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }

        if (!(damager instanceof Projectile projectile)) {
            return null;
        }

        ProjectileSource shooter = projectile.getShooter();

        if (shooter instanceof Player player) {
            return player;
        }

        return null;
    }
}