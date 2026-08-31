package net.skyfoundry.protection;

import net.skyfoundry.SkyFoundry;
import net.skyfoundry.island.Island;
import net.skyfoundry.island.IslandManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Optional;

public final class IslandProtectionListener implements Listener {

    private final SkyFoundry plugin;
    private final IslandManager islandManager;
    private final World islandWorld;

    public IslandProtectionListener(
            SkyFoundry plugin,
            IslandManager islandManager) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.islandWorld = plugin.getIslandWorld();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(
            BlockBreakEvent event) {
        if (!isProtectionEnabled("block-break")) {
            return;
        }

        Player player = event.getPlayer();

        if (canBypass(player)) {
            return;
        }

        if (!canModify(
                player,
                event.getBlock().getLocation())) {
            event.setCancelled(true);
            sendProtectedMessage(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(
            BlockPlaceEvent event) {
        if (!isProtectionEnabled("block-place")) {
            return;
        }

        Player player = event.getPlayer();

        if (canBypass(player)) {
            return;
        }

        if (!canModify(
                player,
                event.getBlockPlaced().getLocation())) {
            event.setCancelled(true);
            sendProtectedMessage(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(
            PlayerInteractEvent event) {
        if (!isProtectionEnabled("interaction")) {
            return;
        }

        if (event.getClickedBlock() == null) {
            return;
        }

        Player player = event.getPlayer();

        if (canBypass(player)) {
            return;
        }

        if (!canModify(
                player,
                event.getClickedBlock().getLocation())) {
            event.setCancelled(true);
            sendProtectedMessage(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityInteract(
            PlayerInteractEntityEvent event) {
        if (!isProtectionEnabled("entities")) {
            return;
        }

        Player player = event.getPlayer();

        if (canBypass(player)) {
            return;
        }

        if (!canModify(
                player,
                event.getRightClicked().getLocation())) {
            event.setCancelled(true);
            sendProtectedMessage(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(
            EntityDamageByEntityEvent event) {
        if (!isProtectionEnabled("entities")) {
            return;
        }

        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        if (event.getEntity() instanceof Player) {
            return;
        }

        if (canBypass(player)) {
            return;
        }

        if (!canModify(
                player,
                event.getEntity().getLocation())) {
            event.setCancelled(true);
            sendProtectedMessage(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreak(
            HangingBreakByEntityEvent event) {
        if (!isProtectionEnabled("entities")) {
            return;
        }

        Entity remover = event.getRemover();

        if (!(remover instanceof Player player)) {
            return;
        }

        if (canBypass(player)) {
            return;
        }

        if (!canModify(
                player,
                event.getEntity().getLocation())) {
            event.setCancelled(true);
            sendProtectedMessage(player);
        }
    }

    private boolean canModify(
            Player player,
            Location location) {
        if (!location.getWorld().equals(islandWorld)) {
            return true;
        }

        int islandSize = Math.max(
                1,
                plugin.getConfig().getInt(
                        "islands.size",
                        50));

        Optional<Island> islandAtLocation = getIslandAt(
                location,
                islandSize);

        if (islandAtLocation.isEmpty()) {
            return !plugin.getConfig().getBoolean(
                    "protection.prevent-building-outside-islands",
                    true);
        }

        return islandAtLocation
                .get()
                .getOwnerUuid()
                .equals(
                        player.getUniqueId());
    }

    private Optional<Island> getIslandAt(
            Location location,
            int islandSize) {
        for (Island island : islandManager.getIslands().values()) {

            if (island.contains(
                    location,
                    islandSize)) {
                return Optional.of(
                        island);
            }
        }

        return Optional.empty();
    }

    private boolean isProtectionEnabled(
            String setting) {
        if (!plugin.getConfig().getBoolean(
                "protection.enabled",
                true)) {
            return false;
        }

        return plugin.getConfig().getBoolean(
                "protection." + setting,
                true);
    }

    private boolean canBypass(
            Player player) {
        String permission = plugin.getConfig().getString(
                "protection.bypass-permission",
                "skyfoundry.admin.bypass");

        if (permission == null
                || permission.isBlank()) {
            return false;
        }

        return player.hasPermission(
                permission);
    }

    private void sendProtectedMessage(
            Player player) {
        String prefix = plugin.getConfig().getString(
                "messages.prefix",
                "<gold><bold>⚙ SKYFOUNDRY</bold></gold> <dark_gray>┃</dark_gray> ");

        String message = plugin.getConfig().getString(
                "messages.island-protected",
                "<red>You cannot do that here.</red>");

        player.sendRichMessage(
                prefix + message);
    }
}