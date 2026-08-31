package net.skyfoundry.gui;

import net.skyfoundry.SkyFoundry;
import net.skyfoundry.gui.holder.CreateIslandMenuHolder;
import net.skyfoundry.gui.holder.IslandMenuHolder;
import net.skyfoundry.island.IslandManager;
import net.skyfoundry.island.IslandRole;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public final class IslandMenuListener implements Listener {

    private final SkyFoundry plugin;
    private final IslandManager islandManager;

    public IslandMenuListener(
            SkyFoundry plugin,
            IslandManager islandManager) {
        this.plugin = plugin;
        this.islandManager = islandManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();

        if (!(topInventory.getHolder() instanceof IslandMenuHolder)
                && !(topInventory.getHolder() instanceof CreateIslandMenuHolder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getClickedInventory() == null
                || event.getClickedInventory() != topInventory) {
            return;
        }

        int slot = event.getRawSlot();

        if (topInventory.getHolder() instanceof CreateIslandMenuHolder) {
            handleCreateMenuClick(player, slot);
            return;
        }

        handleMainMenuClick(player, slot);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();

        if (!(topInventory.getHolder() instanceof IslandMenuHolder)
                && !(topInventory.getHolder() instanceof CreateIslandMenuHolder)) {
            return;
        }

        int topSize = topInventory.getSize();

        boolean touchesMenu = event.getRawSlots()
                .stream()
                .anyMatch(slot -> slot < topSize);

        if (touchesMenu) {
            event.setCancelled(true);
        }
    }

    private void handleMainMenuClick(Player player, int slot) {
        if (slot == getSlot("gui.main.buttons.home", 12)) {
            runIslandCommand(player, "home");
            return;
        }

        if (slot == getSlot("gui.main.buttons.set-home", 14)) {
            runIslandCommand(player, "sethome");
            return;
        }

        if (slot == getSlot("gui.main.buttons.members", 20)) {
            sendGuiMessage(
                    player,
                    "gui.messages.members-coming-soon",
                    "<gray>The island members menu is coming next.</gray>");
            return;
        }

        if (slot == getSlot("gui.main.buttons.info", 22)) {
            runIslandCommand(player, "info");
            return;
        }

        if (slot == getSlot("gui.main.buttons.invite", 24)
                || slot == getSlot("gui.main.buttons.invite-locked", 24)) {

            IslandRole role = islandManager
                    .getRole(player.getUniqueId())
                    .orElse(null);

            if (role == null || !role.canInvite()) {
                sendGuiMessage(
                        player,
                        "messages.invite-no-permission",
                        "<red>Your island role cannot invite players.</red>");
                return;
            }

            player.closeInventory();
            sendGuiMessage(
                    player,
                    "gui.messages.invite-instructions",
                    "<gray>Use <gold>/island invite <player></gold> to invite someone.</gray>");
            return;
        }

        if (slot == getSlot("gui.main.buttons.upgrades", 30)) {
            sendGuiMessage(
                    player,
                    "gui.messages.upgrades-coming-soon",
                    "<gray>Island upgrades are coming soon.</gray>");
            return;
        }

        if (slot == getSlot("gui.main.buttons.settings", 32)) {
            sendGuiMessage(
                    player,
                    "gui.messages.settings-coming-soon",
                    "<gray>Island settings are coming soon.</gray>");
            return;
        }

        if (slot == getSlot("gui.main.buttons.danger-owner", 36)
                || slot == getSlot("gui.main.buttons.danger-member", 36)) {

            IslandRole role = islandManager
                    .getRole(player.getUniqueId())
                    .orElse(null);

            if (role == null) {
                player.closeInventory();
                return;
            }

            if (role == IslandRole.OWNER) {
                runIslandCommand(player, "delete");
            } else {
                runIslandCommand(player, "leave");
            }
            return;
        }

        if (slot == getSlot("gui.main.buttons.close", 44)) {
            player.closeInventory();
        }
    }

    private void handleCreateMenuClick(Player player, int slot) {
        if (slot == getSlot("gui.create.buttons.create", 22)) {
            runIslandCommand(player, "create");
            return;
        }

        if (slot == getSlot("gui.create.buttons.close", 44)) {
            player.closeInventory();
        }
    }

    private void runIslandCommand(Player player, String arguments) {
        player.closeInventory();

        Bukkit.getScheduler().runTask(
                plugin,
                () -> Bukkit.dispatchCommand(
                        player,
                        "island " + arguments));
    }

    private int getSlot(String path, int fallback) {
        return plugin.getConfig().getInt(path + ".slot", fallback);
    }

    private void sendGuiMessage(
            Player player,
            String path,
            String fallback) {
        String prefix = plugin.getConfig().getString(
                "messages.prefix",
                "<gold><bold>⚙ SKYFOUNDRY</bold></gold> <dark_gray>┃</dark_gray> ");

        String message = plugin.getConfig().getString(path, fallback);
        player.sendRichMessage(prefix + message);
    }
}
