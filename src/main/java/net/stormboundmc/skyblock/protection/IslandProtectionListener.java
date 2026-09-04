package net.stormboundmc.skyblock.protection;

import net.stormboundmc.skyblock.StormboundSkyblock;
import net.stormboundmc.skyblock.island.Island;
import net.stormboundmc.skyblock.island.IslandManager;
import net.stormboundmc.skyblock.island.IslandRole;
import net.stormboundmc.skyblock.island.IslandSettingsManager;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Optional;

public final class IslandProtectionListener
        implements Listener {

    private final StormboundSkyblock plugin;
    private final IslandManager islandManager;
    private final IslandSettingsManager settingsManager;

    public IslandProtectionListener(
            StormboundSkyblock plugin,
            IslandManager islandManager,
            IslandSettingsManager settingsManager) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.settingsManager = settingsManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(
            BlockBreakEvent event) {
        if (!isProtectionEnabled(
                "block-break")) {
            return;
        }

        Player player = event.getPlayer();

        if (canBypass(player)) {
            return;
        }

        if (!canBuild(
                player,
                event.getBlock().getLocation())) {
            event.setCancelled(true);
            sendProtectedMessage(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(
            BlockPlaceEvent event) {
        if (!isProtectionEnabled(
                "block-place")) {
            return;
        }

        Player player = event.getPlayer();

        if (canBypass(player)) {
            return;
        }

        if (!canBuild(
                player,
                event.getBlockPlaced().getLocation())) {
            event.setCancelled(true);
            sendProtectedMessage(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(
            PlayerBucketEmptyEvent event) {
        if (!isProtectionEnabled(
                "block-place")) {
            return;
        }

        Player player = event.getPlayer();

        if (canBypass(player)) {
            return;
        }

        Location target = event.getBlockClicked()
                .getRelative(event.getBlockFace())
                .getLocation();

        if (!canBuild(
                player,
                target)) {
            event.setCancelled(true);
            sendProtectedMessage(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(
            PlayerBucketFillEvent event) {
        if (!isProtectionEnabled(
                "block-break")) {
            return;
        }

        Player player = event.getPlayer();

        if (canBypass(player)) {
            return;
        }

        if (!canBuild(
                player,
                event.getBlockClicked().getLocation())) {
            event.setCancelled(true);
            sendProtectedMessage(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(
            PlayerInteractEvent event) {
        if (!isProtectionEnabled(
                "interaction")) {
            return;
        }

        if (event.getClickedBlock() == null) {
            return;
        }

        Player player = event.getPlayer();

        if (canBypass(player)) {
            return;
        }

        if (!canInteract(
                player,
                event.getClickedBlock().getLocation())) {
            event.setCancelled(true);
            sendProtectedMessage(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityInteract(
            PlayerInteractEntityEvent event) {
        if (!isProtectionEnabled(
                "entities")) {
            return;
        }

        Player player = event.getPlayer();

        if (canBypass(player)) {
            return;
        }

        if (!canInteract(
                player,
                event.getRightClicked().getLocation())) {
            event.setCancelled(true);
            sendProtectedMessage(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArmorStandManipulate(
            PlayerArmorStandManipulateEvent event) {
        if (!isProtectionEnabled(
                "entities")) {
            return;
        }

        Player player = event.getPlayer();

        if (canBypass(player)) {
            return;
        }

        if (!canInteract(
                player,
                event.getRightClicked().getLocation())) {
            event.setCancelled(true);
            sendProtectedMessage(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(
            EntityDamageByEntityEvent event) {
        if (!isProtectionEnabled(
                "entities")) {
            return;
        }

        if (event.getEntity() instanceof Player) {
            return;
        }

        Player player = getResponsiblePlayer(
                event.getDamager());

        if (player == null
                || canBypass(player)) {
            return;
        }

        if (!canInteract(
                player,
                event.getEntity().getLocation())) {
            event.setCancelled(true);
            sendProtectedMessage(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreak(
            HangingBreakByEntityEvent event) {
        if (!isProtectionEnabled(
                "entities")) {
            return;
        }

        Player player = getResponsiblePlayer(
                event.getRemover());

        if (player == null
                || canBypass(player)) {
            return;
        }

        if (!canInteract(
                player,
                event.getEntity().getLocation())) {
            event.setCancelled(true);
            sendProtectedMessage(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingPlace(
            HangingPlaceEvent event) {
        if (!isProtectionEnabled(
                "entities")) {
            return;
        }

        Player player = event.getPlayer();

        if (player == null
                || canBypass(player)) {
            return;
        }

        if (!canInteract(
                player,
                event.getEntity().getLocation())) {
            event.setCancelled(true);
            sendProtectedMessage(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFluidFlow(
            BlockFromToEvent event) {
        if (!isProtectionEnabled(
                "environment")) {
            return;
        }

        if (!isSameIslandMovement(
                event.getBlock().getLocation(),
                event.getToBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(
            EntityExplodeEvent event) {
        if (!isProtectionEnabled(
                "environment")) {
            return;
        }

        Location origin = event.getLocation();

        event.blockList().removeIf(
                block -> !isSameIslandMovement(
                        origin,
                        block.getLocation()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(
            BlockExplodeEvent event) {
        if (!isProtectionEnabled(
                "environment")) {
            return;
        }

        Location origin = event.getBlock().getLocation();

        event.blockList().removeIf(
                block -> !isSameIslandMovement(
                        origin,
                        block.getLocation()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(
            BlockPistonExtendEvent event) {
        if (!isProtectionEnabled(
                "pistons")) {
            return;
        }

        for (Block block : event.getBlocks()) {
            Block destination = block.getRelative(
                    event.getDirection());

            if (!isSameIslandMovement(
                    block.getLocation(),
                    destination.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(
            BlockPistonRetractEvent event) {
        if (!isProtectionEnabled(
                "pistons")) {
            return;
        }

        for (Block block : event.getBlocks()) {
            Block destination = block.getRelative(
                    event.getDirection().getOppositeFace());

            if (!isSameIslandMovement(
                    block.getLocation(),
                    destination.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private Player getResponsiblePlayer(
            Entity entity) {
        if (entity instanceof Player player) {
            return player;
        }

        if (!(entity instanceof Projectile projectile)) {
            return null;
        }

        ProjectileSource shooter = projectile.getShooter();

        if (shooter instanceof Player player) {
            return player;
        }

        return null;
    }

    private boolean isSameIslandMovement(
            Location source,
            Location destination) {
        if (source.getWorld() == null
                || destination.getWorld() == null
                || islandManager.getDimensionManager() == null
                || !islandManager.getDimensionManager().isIslandWorld(source.getWorld())
                || !islandManager.getDimensionManager().isIslandWorld(destination.getWorld())) {
            return true;
        }

        Optional<Island> sourceIsland = islandManager.getIslandAt(
                source);

        Optional<Island> destinationIsland = islandManager.getIslandAt(
                destination);

        if (sourceIsland.isEmpty()
                && destinationIsland.isEmpty()) {
            return true;
        }

        if (sourceIsland.isEmpty()
                || destinationIsland.isEmpty()) {
            return false;
        }

        return sourceIsland.get().getIslandId() == destinationIsland.get().getIslandId();
    }

    private boolean canBuild(
            Player player,
            Location location) {
        if (location.getWorld() == null
                || islandManager.getDimensionManager() == null
                || !islandManager.getDimensionManager().isIslandWorld(location.getWorld())) {
            return true;
        }

        Optional<Island> islandOptional = islandManager.getIslandAt(
                location);

        if (islandOptional.isEmpty()) {
            return !plugin.getConfig()
                    .getBoolean(
                            "protection.prevent-building-outside-islands",
                            true);
        }

        Island island = islandOptional.get();

        if (!islandManager.isMemberOf(
                player.getUniqueId(),
                island)) {
            return false;
        }

        IslandRole role = islandManager
                .getRole(player.getUniqueId())
                .orElse(null);

        if (role == IslandRole.MEMBER) {
            return settingsManager.getSettings(island).isMemberBuilding();
        }

        return role != null
                && role.canBuild();
    }

    private boolean canInteract(
            Player player,
            Location location) {
        if (location.getWorld() == null
                || islandManager.getDimensionManager() == null
                || !islandManager.getDimensionManager().isIslandWorld(location.getWorld())) {
            return true;
        }

        Optional<Island> islandOptional = islandManager.getIslandAt(
                location);

        if (islandOptional.isEmpty()) {
            return !plugin.getConfig()
                    .getBoolean(
                            "protection.prevent-building-outside-islands",
                            true);
        }

        Island island = islandOptional.get();

        if (!islandManager.isMemberOf(
                player.getUniqueId(),
                island)) {
            return false;
        }

        IslandRole role = islandManager
                .getRole(player.getUniqueId())
                .orElse(null);

        if (role == IslandRole.MEMBER) {
            return settingsManager.getSettings(island).isMemberInteractions();
        }

        return role != null
                && role.canInteract();
    }

    private boolean isProtectionEnabled(
            String setting) {
        if (!plugin.getConfig()
                .getBoolean(
                        "protection.enabled",
                        true)) {
            return false;
        }

        return plugin.getConfig()
                .getBoolean(
                        "protection."
                                + setting,
                        true);
    }

    private boolean canBypass(
            Player player) {
        String permission = plugin.getConfig()
                .getString(
                        "protection.bypass-permission",
                        "stormbound.skyblock.admin");

        return permission != null
                && !permission.isBlank()
                && player.hasPermission(permission);
    }

    private void sendProtectedMessage(
            Player player) {
        String prefix = plugin.getConfig()
                .getString(
                        "messages.prefix",
                        "<gold><bold>⚙ STORMBOUND</bold></gold> <dark_gray>┃</dark_gray> ");

        String message = plugin.getConfig()
                .getString(
                        "messages.island-protected",
                        "<red>You cannot do that here.</red>");

        player.sendRichMessage(
                prefix + message);
    }
}
