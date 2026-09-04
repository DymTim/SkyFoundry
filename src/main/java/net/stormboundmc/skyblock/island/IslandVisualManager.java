package net.stormboundmc.skyblock.island;

import net.stormboundmc.skyblock.StormboundSkyblock;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class IslandVisualManager implements Listener {

    private final StormboundSkyblock plugin;
    private final IslandManager islandManager;
    private final IslandSettingsManager settingsManager;
    private final World islandWorld;
    private final Map<UUID, Long> visibleIslandByPlayer = new HashMap<>();

    public IslandVisualManager(
            StormboundSkyblock plugin,
            IslandManager islandManager,
            IslandSettingsManager settingsManager
    ) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.settingsManager = settingsManager;
        this.islandWorld = plugin.getIslandWorld();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> refresh(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event instanceof PlayerTeleportEvent) {
            return;
        }

        if (event.getTo() == null || sameBlock(event.getFrom(), event.getTo())) {
            return;
        }

        refreshIfIslandChanged(event.getPlayer(), event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getTo() == null) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> refresh(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> refresh(event.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        visibleIslandByPlayer.remove(event.getPlayer().getUniqueId());
    }

    public void refresh(Player player) {
        Optional<Island> islandOptional = getIslandAt(player.getLocation());

        if (islandOptional.isEmpty()) {
            clearVisuals(player);
            visibleIslandByPlayer.remove(player.getUniqueId());
            return;
        }

        Island island = islandOptional.get();
        applyVisuals(player, island);
        visibleIslandByPlayer.put(player.getUniqueId(), island.getIslandId());
    }

    public void refreshIsland(Island island) {
        for (Player player : islandWorld.getPlayers()) {
            Optional<Island> current = islandManager.getIslandAt(player.getLocation());
            if (current.isPresent() && current.get().getIslandId() == island.getIslandId()) {
                applyVisuals(player, island);
                visibleIslandByPlayer.put(player.getUniqueId(), island.getIslandId());
            }
        }
    }

    private void refreshIfIslandChanged(Player player, Location to) {
        Optional<Island> islandOptional = getIslandAt(to);
        Long previous = visibleIslandByPlayer.get(player.getUniqueId());
        Long current = islandOptional.map(Island::getIslandId).orElse(null);

        if (previous == null ? current == null : previous.equals(current)) {
            return;
        }

        if (islandOptional.isPresent()) {
            Island island = islandOptional.get();
            applyVisuals(player, island);
            visibleIslandByPlayer.put(player.getUniqueId(), island.getIslandId());
        } else {
            clearVisuals(player);
            visibleIslandByPlayer.remove(player.getUniqueId());
        }
    }

    private Optional<Island> getIslandAt(Location location) {
        if (location.getWorld() == null || !location.getWorld().equals(islandWorld)) {
            return Optional.empty();
        }

        return islandManager.getIslandAt(location);
    }

    private void applyVisuals(Player player, Island island) {
        IslandSettings settings = settingsManager.getSettings(island);
        applyWeather(player, settings.getWeatherMode());
        applyTime(player, settings.getTimeMode());
        applyBorder(player, island, settings.isBorderEnabled());
    }

    private void applyWeather(Player player, IslandWeatherMode mode) {
        switch (mode) {
            case DEFAULT -> player.resetPlayerWeather();
            case CLEAR -> player.setPlayerWeather(WeatherType.CLEAR);
            case RAIN -> player.setPlayerWeather(WeatherType.DOWNFALL);
        }
    }

    private void applyTime(Player player, IslandTimeMode mode) {
        if (mode == IslandTimeMode.DEFAULT) {
            player.resetPlayerTime();
            return;
        }

        player.setPlayerTime(mode.getTicks(), false);
    }

    private void applyBorder(Player player, Island island, boolean enabled) {
        if (!enabled) {
            player.setWorldBorder(null);
            return;
        }

        WorldBorder border = Bukkit.createWorldBorder();
        border.setCenter(island.getCenterX(), island.getCenterZ());
        border.setSize(islandManager.getIslandSize(island, player.getWorld()));
        border.setWarningDistance(0);
        border.setDamageAmount(0.0);
        player.setWorldBorder(border);
    }

    private void clearVisuals(Player player) {
        player.resetPlayerWeather();
        player.resetPlayerTime();
        player.setWorldBorder(null);
    }

    private boolean sameBlock(Location first, Location second) {
        return first.getWorld() == second.getWorld()
                && first.getBlockX() == second.getBlockX()
                && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();
    }
}
