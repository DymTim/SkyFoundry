package net.stormboundmc.skyblock.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.stormboundmc.skyblock.gui.holder.CreateIslandMenuHolder;
import net.stormboundmc.skyblock.gui.holder.IslandMenuHolder;
import net.stormboundmc.skyblock.island.Island;
import net.stormboundmc.skyblock.island.IslandManager;
import net.stormboundmc.skyblock.island.IslandRole;
import net.stormboundmc.skyblock.StormboundSkyblock;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class IslandMenu {

        private static final int INVENTORY_SIZE = 45;

        private final StormboundSkyblock plugin;
        private final IslandManager islandManager;
        private final MiniMessage miniMessage;

        public IslandMenu(
                        StormboundSkyblock plugin,
                        IslandManager islandManager) {
                this.plugin = plugin;
                this.islandManager = islandManager;
                this.miniMessage = MiniMessage.miniMessage();
        }

        public void open(Player player) {
                if (!islandManager.hasIsland(player.getUniqueId())) {
                        openCreateMenu(player);
                        return;
                }

                openMainMenu(player);
        }

        private void openMainMenu(Player player) {
                Island island = islandManager
                                .getIsland(player.getUniqueId())
                                .orElse(null);

                if (island == null) {
                        openCreateMenu(player);
                        return;
                }

                IslandRole role = islandManager
                                .getRole(player.getUniqueId())
                                .orElse(IslandRole.MEMBER);

                IslandMenuHolder holder = new IslandMenuHolder();
                Component title = miniMessage.deserialize(
                                plugin.getConfig().getString(
                                                "gui.main.title",
                                                "<gold><bold>⚙ SKYFOUNDRY ISLAND</bold></gold>"));

                Inventory inventory = Bukkit.createInventory(
                                holder,
                                INVENTORY_SIZE,
                                title);

                holder.setInventory(inventory);
                fillBackground(inventory, "gui.main.filler");
                placeAccents(inventory, "gui.main.accents");

                setButton(
                                inventory,
                                "gui.main.buttons.home",
                                12,
                                Material.WHITE_BED,
                                "<yellow><bold>Island Home</bold></yellow>",
                                List.of(
                                                "<gray>Teleport to your island home.</gray>",
                                                "",
                                                "<gold>Click to teleport.</gold>"),
                                island,
                                role);

                setButton(
                                inventory,
                                "gui.main.buttons.set-home",
                                14,
                                Material.COMPASS,
                                "<yellow><bold>Set Home</bold></yellow>",
                                List.of(
                                                "<gray>Set your personal island home</gray>",
                                                "<gray>to your current location.</gray>",
                                                "",
                                                "<gold>Click to set home.</gold>"),
                                island,
                                role);

                setButton(
                                inventory,
                                "gui.main.buttons.members",
                                20,
                                Material.PLAYER_HEAD,
                                "<yellow><bold>Island Members</bold></yellow>",
                                List.of(
                                                "<gray>View and manage island members.</gray>",
                                                "",
                                                "<gray>Members: <yellow>{members}</yellow>/<yellow>{limit}</yellow></gray>",
                                                "<gray>Your Role: <yellow>{role}</yellow></gray>",
                                                "",
                                                "<gold>Click to view members.</gold>"),
                                island,
                                role);

                setButton(
                                inventory,
                                "gui.main.buttons.info",
                                22,
                                Material.BOOK,
                                "<yellow><bold>Island Information</bold></yellow>",
                                List.of(
                                                "<gray>Owner: <yellow>{owner}</yellow></gray>",
                                                "<gray>Island ID: <yellow>#{id}</yellow></gray>",
                                                "<gray>Size: <yellow>{size}x{size}</yellow></gray>",
                                                "<gray>Center: <yellow>{x}, {z}</yellow></gray>",
                                                "",
                                                "<gold>Click for full information.</gold>"),
                                island,
                                role);

                if (role.canInvite()) {
                        setButton(
                                        inventory,
                                        "gui.main.buttons.invite",
                                        24,
                                        Material.NAME_TAG,
                                        "<yellow><bold>Invite Player</bold></yellow>",
                                        List.of(
                                                        "<gray>Invite another player to</gray>",
                                                        "<gray>join your island.</gray>",
                                                        "",
                                                        "<gold>Click for invite instructions.</gold>"),
                                        island,
                                        role);
                } else {
                        setLockedInviteButton(inventory);
                }

                setButton(
                                inventory,
                                "gui.main.buttons.upgrades",
                                30,
                                Material.BEACON,
                                "<yellow><bold>Island Upgrades</bold></yellow>",
                                List.of(
                                                "<gray>Expand and improve your island.</gray>",
                                                "",
                                                "<dark_gray>Coming soon.</dark_gray>"),
                                island,
                                role);

                setButton(
                                inventory,
                                "gui.main.buttons.settings",
                                32,
                                Material.COMPARATOR,
                                "<yellow><bold>Island Settings</bold></yellow>",
                                List.of(
                                                "<gray>Configure island options.</gray>",
                                                "",
                                                "<dark_gray>Coming soon.</dark_gray>"),
                                island,
                                role);

                if (role == IslandRole.OWNER) {
                        setButton(
                                        inventory,
                                        "gui.main.buttons.danger-owner",
                                        36,
                                        Material.TNT,
                                        "<red><bold>Delete Island</bold></red>",
                                        List.of(
                                                        "<gray>Permanently delete this island.</gray>",
                                                        "",
                                                        "<red>This action requires confirmation.</red>"),
                                        island,
                                        role);
                } else {
                        setButton(
                                        inventory,
                                        "gui.main.buttons.danger-member",
                                        36,
                                        Material.OAK_DOOR,
                                        "<red><bold>Leave Island</bold></red>",
                                        List.of(
                                                        "<gray>Leave your current island.</gray>",
                                                        "",
                                                        "<red>Click to leave.</red>"),
                                        island,
                                        role);
                }

                setSimpleButton(
                                inventory,
                                "gui.main.buttons.close",
                                44,
                                Material.BARRIER,
                                "<red><bold>Close</bold></red>",
                                List.of("<gray>Close this menu.</gray>"));

                player.openInventory(inventory);
        }

        private void openCreateMenu(Player player) {
                CreateIslandMenuHolder holder = new CreateIslandMenuHolder();
                Component title = miniMessage.deserialize(
                                plugin.getConfig().getString(
                                                "gui.create.title",
                                                "<gold><bold>⚙ CREATE ISLAND</bold></gold>"));

                Inventory inventory = Bukkit.createInventory(
                                holder,
                                INVENTORY_SIZE,
                                title);

                holder.setInventory(inventory);
                fillBackground(inventory, "gui.create.filler");
                placeAccents(inventory, "gui.create.accents");

                setSimpleButton(
                                inventory,
                                "gui.create.buttons.create",
                                22,
                                Material.GRASS_BLOCK,
                                "<gold><bold>Create Island</bold></gold>",
                                List.of(
                                                "<gray>Create your Stormbound island</gray>",
                                                "<gray>and begin building your factory.</gray>",
                                                "",
                                                "<gold>Click to create.</gold>"));

                setSimpleButton(
                                inventory,
                                "gui.create.buttons.close",
                                44,
                                Material.BARRIER,
                                "<red><bold>Close</bold></red>",
                                List.of("<gray>Close this menu.</gray>"));

                player.openInventory(inventory);
        }

        private void setLockedInviteButton(Inventory inventory) {
                String path = "gui.main.buttons.invite-locked";
                int slot = getSlot(path, 24);
                Material material = getMaterial(path, Material.BARRIER);
                String name = plugin.getConfig().getString(
                                path + ".name",
                                "<red><bold>Invite Player</bold></red>");
                List<String> lore = getLore(
                                path,
                                List.of(
                                                "<gray>Your island role cannot</gray>",
                                                "<gray>invite new members.</gray>"));

                inventory.setItem(slot, createItem(material, name, lore));
        }

        private void setButton(
                        Inventory inventory,
                        String path,
                        int fallbackSlot,
                        Material fallbackMaterial,
                        String fallbackName,
                        List<String> fallbackLore,
                        Island island,
                        IslandRole role) {
                int slot = getSlot(path, fallbackSlot);
                Material material = getMaterial(path, fallbackMaterial);
                String name = plugin.getConfig().getString(path + ".name", fallbackName);
                List<String> lore = getLore(path, fallbackLore);

                OfflinePlayer owner = Bukkit.getOfflinePlayer(island.getOwnerUuid());
                String ownerName = owner.getName() == null
                                ? island.getOwnerUuid().toString()
                                : owner.getName();

                int members = islandManager.getMemberCount(island);
                int limit = Math.max(1, plugin.getConfig().getInt("islands.member-limit", 5));
                int size = Math.max(1, plugin.getConfig().getInt("islands.size", 50));

                name = replacePlaceholders(
                                name,
                                island,
                                role,
                                ownerName,
                                members,
                                limit,
                                size);

                List<String> replacedLore = new ArrayList<>();
                for (String line : lore) {
                        replacedLore.add(
                                        replacePlaceholders(
                                                        line,
                                                        island,
                                                        role,
                                                        ownerName,
                                                        members,
                                                        limit,
                                                        size));
                }

                inventory.setItem(slot, createItem(material, name, replacedLore));
        }

        private void setSimpleButton(
                        Inventory inventory,
                        String path,
                        int fallbackSlot,
                        Material fallbackMaterial,
                        String fallbackName,
                        List<String> fallbackLore) {
                int slot = getSlot(path, fallbackSlot);
                Material material = getMaterial(path, fallbackMaterial);
                String name = plugin.getConfig().getString(path + ".name", fallbackName);
                List<String> lore = getLore(path, fallbackLore);

                inventory.setItem(slot, createItem(material, name, lore));
        }

        private void fillBackground(Inventory inventory, String path) {
                Material material = getMaterial(path, Material.GRAY_STAINED_GLASS_PANE);
                String name = plugin.getConfig().getString(path + ".name", " ");
                ItemStack filler = createItem(material, name, List.of());

                for (int slot = 0; slot < inventory.getSize(); slot++) {
                        inventory.setItem(slot, filler);
                }
        }

        private void placeAccents(Inventory inventory, String path) {
                if (!plugin.getConfig().getBoolean(path + ".enabled", true)) {
                        return;
                }

                Material material = getMaterial(path, Material.ORANGE_STAINED_GLASS_PANE);
                String name = plugin.getConfig().getString(path + ".name", " ");
                List<Integer> slots = plugin.getConfig().getIntegerList(path + ".slots");

                if (slots.isEmpty()) {
                        slots = List.of(0, 4, 8, 40);
                }

                ItemStack accent = createItem(material, name, List.of());

                for (int slot : slots) {
                        if (slot >= 0 && slot < inventory.getSize()) {
                                inventory.setItem(slot, accent);
                        }
                }
        }

        private int getSlot(String path, int fallback) {
                int slot = plugin.getConfig().getInt(path + ".slot", fallback);

                if (slot < 0 || slot >= INVENTORY_SIZE) {
                        plugin.getLogger().warning(
                                        "Invalid GUI slot " + slot + " at " + path + ".slot. Using " + fallback + ".");
                        return fallback;
                }

                return slot;
        }

        private Material getMaterial(String path, Material fallback) {
                String configured = plugin.getConfig().getString(
                                path + ".material",
                                fallback.name());

                Material material = Material.matchMaterial(configured);

                if (material == null || material.isAir()) {
                        plugin.getLogger().warning(
                                        "Invalid GUI material '" + configured + "' at " + path + ".material. Using "
                                                        + fallback.name() + ".");
                        return fallback;
                }

                return material;
        }

        private List<String> getLore(String path, List<String> fallback) {
                ConfigurationSection section = plugin.getConfig().getConfigurationSection(path);

                if (section == null || !section.isList("lore")) {
                        return fallback;
                }

                return plugin.getConfig().getStringList(path + ".lore");
        }

        private ItemStack createItem(
                        Material material,
                        String name,
                        List<String> lore) {
                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();

                meta.displayName(miniMessage.deserialize(name));

                if (!lore.isEmpty()) {
                        List<Component> components = lore.stream()
                                        .map(miniMessage::deserialize)
                                        .toList();

                        meta.lore(components);
                }

                item.setItemMeta(meta);
                return item;
        }

        private String replacePlaceholders(
                        String input,
                        Island island,
                        IslandRole role,
                        String ownerName,
                        int members,
                        int limit,
                        int size) {
                return input
                                .replace("{owner}", ownerName)
                                .replace("{role}", formatRole(role))
                                .replace("{members}", Integer.toString(members))
                                .replace("{limit}", Integer.toString(limit))
                                .replace("{id}", Long.toString(island.getIslandId()))
                                .replace("{size}", Integer.toString(size))
                                .replace("{x}", Integer.toString(island.getCenterX()))
                                .replace("{z}", Integer.toString(island.getCenterZ()));
        }

        private String formatRole(IslandRole role) {
                return switch (role) {
                        case OWNER -> "Owner";
                        case CO_OWNER -> "Co-Owner";
                        case MEMBER -> "Member";
                };
        }
}
