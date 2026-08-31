package net.skyfoundry.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.skyfoundry.SkyFoundry;
import net.skyfoundry.gui.holder.IslandMemberConfirmHolder;
import net.skyfoundry.gui.holder.IslandMemberMenuHolder;
import net.skyfoundry.gui.holder.IslandMembersMenuHolder;
import net.skyfoundry.island.Island;
import net.skyfoundry.island.IslandManager;
import net.skyfoundry.island.IslandMember;
import net.skyfoundry.island.IslandRole;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class IslandMembersMenu {

    private static final int INVENTORY_SIZE = 45;

    private static final List<Integer> DEFAULT_MEMBER_SLOTS = List.of(
            11, 12, 13, 14, 15,
            20, 21, 22, 23, 24,
            29, 30, 31, 32, 33
    );

    private final SkyFoundry plugin;
    private final IslandManager islandManager;
    private final IslandMenu islandMenu;
    private final MiniMessage miniMessage;

    public IslandMembersMenu(
            SkyFoundry plugin,
            IslandManager islandManager,
            IslandMenu islandMenu
    ) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.islandMenu = islandMenu;
        this.miniMessage = MiniMessage.miniMessage();
    }

    public void openMembers(Player player) {
        Island island = islandManager
                .getIsland(player.getUniqueId())
                .orElse(null);

        if (island == null) {
            islandMenu.open(player);
            return;
        }

        IslandRole viewerRole = islandManager
                .getRole(player.getUniqueId())
                .orElse(IslandRole.MEMBER);

        IslandMembersMenuHolder holder = new IslandMembersMenuHolder();
        Inventory inventory = Bukkit.createInventory(
                holder,
                INVENTORY_SIZE,
                title("gui.members.title", "<gold><bold>⚙ ISLAND MEMBERS</bold></gold>")
        );
        holder.setInventory(inventory);

        fillBackground(inventory, "gui.members.filler");
        placeAccents(inventory, "gui.members.accents");

        List<IslandMember> members = new ArrayList<>(islandManager.getMembers(island));
        members.sort(
                Comparator
                        .comparingInt((IslandMember member) -> roleOrder(member.getRole()))
                        .thenComparing(member -> getPlayerName(member.getPlayerUuid()), String.CASE_INSENSITIVE_ORDER)
        );

        List<Integer> memberSlots = plugin.getConfig().getIntegerList("gui.members.member-slots");
        if (memberSlots.isEmpty()) {
            memberSlots = DEFAULT_MEMBER_SLOTS;
        }

        int shown = Math.min(members.size(), memberSlots.size());
        for (int index = 0; index < shown; index++) {
            int slot = memberSlots.get(index);
            if (slot < 0 || slot >= INVENTORY_SIZE) {
                continue;
            }

            IslandMember member = members.get(index);
            holder.setMember(slot, member.getPlayerUuid());
            inventory.setItem(
                    slot,
                    createMemberHead(
                            member,
                            member.getPlayerUuid().equals(player.getUniqueId())
                    )
            );
        }

        if (viewerRole.canInvite()) {
            setButton(
                    inventory,
                    "gui.members.buttons.invite",
                    36,
                    Material.NAME_TAG,
                    "<yellow><bold>Invite Player</bold></yellow>",
                    List.of(
                            "<gray>Invite another player to</gray>",
                            "<gray>join this island.</gray>",
                            "",
                            "<gold>Click for invite instructions.</gold>"
                    )
            );
        }

        setButton(
                inventory,
                "gui.members.buttons.back",
                40,
                Material.ARROW,
                "<yellow><bold>Back</bold></yellow>",
                List.of("<gray>Return to the island menu.</gray>")
        );

        setButton(
                inventory,
                "gui.members.buttons.close",
                44,
                Material.BARRIER,
                "<red><bold>Close</bold></red>",
                List.of("<gray>Close this menu.</gray>")
        );

        player.openInventory(inventory);
    }

    public void openMember(Player player, UUID targetUuid) {
        Island viewerIsland = islandManager
                .getIsland(player.getUniqueId())
                .orElse(null);

        Island targetIsland = islandManager
                .getIsland(targetUuid)
                .orElse(null);

        IslandMember target = islandManager
                .getMember(targetUuid)
                .orElse(null);

        IslandRole viewerRole = islandManager
                .getRole(player.getUniqueId())
                .orElse(null);

        if (viewerIsland == null
                || targetIsland == null
                || target == null
                || viewerRole == null
                || viewerIsland.getIslandId() != targetIsland.getIslandId()) {

            openMembers(player);
            return;
        }

        IslandMemberMenuHolder holder = new IslandMemberMenuHolder(targetUuid);
        Inventory inventory = Bukkit.createInventory(
                holder,
                INVENTORY_SIZE,
                title("gui.member.title", "<gold><bold>⚙ ISLAND MEMBER</bold></gold>")
        );
        holder.setInventory(inventory);

        fillBackground(inventory, "gui.member.filler");
        placeAccents(inventory, "gui.member.accents");

        inventory.setItem(
                getSlot("gui.member.buttons.profile", 13),
                createMemberProfile(target)
        );

        boolean targetIsSelf = targetUuid.equals(player.getUniqueId());
        boolean targetIsOwner = target.getRole() == IslandRole.OWNER;

        if (viewerRole.canChangeRoles() && !targetIsSelf && !targetIsOwner) {
            if (target.getRole() == IslandRole.MEMBER) {
                setButton(
                        inventory,
                        "gui.member.buttons.promote",
                        20,
                        Material.GOLD_INGOT,
                        "<gold><bold>Promote to Co-Owner</bold></gold>",
                        List.of(
                                "<gray>Give this member additional</gray>",
                                "<gray>island management permissions.</gray>",
                                "",
                                "<gold>Click to promote.</gold>"
                        )
                );
            } else if (target.getRole() == IslandRole.CO_OWNER) {
                setButton(
                        inventory,
                        "gui.member.buttons.demote",
                        20,
                        Material.IRON_INGOT,
                        "<yellow><bold>Demote to Member</bold></yellow>",
                        List.of(
                                "<gray>Remove this player's</gray>",
                                "<gray>Co-Owner permissions.</gray>",
                                "",
                                "<gold>Click to demote.</gold>"
                        )
                );
            }
        }

        if (viewerRole.canTransferOwnership() && !targetIsSelf && !targetIsOwner) {
            setButton(
                    inventory,
                    "gui.member.buttons.transfer",
                    22,
                    Material.NETHER_STAR,
                    "<gold><bold>Transfer Ownership</bold></gold>",
                    List.of(
                            "<gray>Make this player the new</gray>",
                            "<gray>owner of the island.</gray>",
                            "",
                            "<red>You will become a Co-Owner.</red>",
                            "<gold>Click to continue.</gold>"
                    )
            );
        }

        if (canKick(viewerRole, target.getRole(), targetIsSelf)) {
            setButton(
                    inventory,
                    "gui.member.buttons.kick",
                    24,
                    Material.OAK_DOOR,
                    "<red><bold>Kick Member</bold></red>",
                    List.of(
                            "<gray>Remove this player from</gray>",
                            "<gray>the island.</gray>",
                            "",
                            "<red>Click to continue.</red>"
                    )
            );
        }

        setButton(
                inventory,
                "gui.member.buttons.back",
                40,
                Material.ARROW,
                "<yellow><bold>Back</bold></yellow>",
                List.of("<gray>Return to island members.</gray>")
        );

        setButton(
                inventory,
                "gui.member.buttons.close",
                44,
                Material.BARRIER,
                "<red><bold>Close</bold></red>",
                List.of("<gray>Close this menu.</gray>")
        );

        player.openInventory(inventory);
    }

    public void openConfirmation(
            Player player,
            UUID targetUuid,
            IslandMemberConfirmHolder.Action action
    ) {
        Island viewerIsland = islandManager
                .getIsland(player.getUniqueId())
                .orElse(null);

        Island targetIsland = islandManager
                .getIsland(targetUuid)
                .orElse(null);

        IslandMember target = islandManager
                .getMember(targetUuid)
                .orElse(null);

        if (viewerIsland == null
                || targetIsland == null
                || target == null
                || viewerIsland.getIslandId() != targetIsland.getIslandId()) {

            openMembers(player);
            return;
        }

        IslandMemberConfirmHolder holder = new IslandMemberConfirmHolder(targetUuid, action);
        Inventory inventory = Bukkit.createInventory(
                holder,
                INVENTORY_SIZE,
                title("gui.member-confirm.title", "<red><bold>⚙ CONFIRM ACTION</bold></red>")
        );
        holder.setInventory(inventory);

        fillBackground(inventory, "gui.member-confirm.filler");
        placeAccents(inventory, "gui.member-confirm.accents");

        inventory.setItem(
                getSlot("gui.member-confirm.buttons.profile", 13),
                createMemberProfile(target)
        );

        String actionText = action == IslandMemberConfirmHolder.Action.KICK
                ? "kick this member"
                : "transfer island ownership";

        setButton(
                inventory,
                "gui.member-confirm.buttons.confirm",
                20,
                Material.LIME_CONCRETE,
                "<green><bold>Confirm</bold></green>",
                List.of(
                        "<gray>Confirm that you want to</gray>",
                        "<gray>" + actionText + ".</gray>",
                        "",
                        "<green>Click to confirm.</green>"
                )
        );

        setButton(
                inventory,
                "gui.member-confirm.buttons.cancel",
                24,
                Material.RED_CONCRETE,
                "<red><bold>Cancel</bold></red>",
                List.of(
                        "<gray>Return without making</gray>",
                        "<gray>any changes.</gray>"
                )
        );

        setButton(
                inventory,
                "gui.member-confirm.buttons.back",
                40,
                Material.ARROW,
                "<yellow><bold>Back</bold></yellow>",
                List.of("<gray>Return to this member.</gray>")
        );

        player.openInventory(inventory);
    }

    public IslandMenu getIslandMenu() {
        return islandMenu;
    }

    private ItemStack createMemberHead(IslandMember member, boolean self) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(member.getPlayerUuid());
        String playerName = getPlayerName(member.getPlayerUuid());
        boolean online = offlinePlayer.isOnline();

        String name = plugin.getConfig().getString(
                "gui.members.member.name",
                "<yellow><bold>{player}</bold></yellow>"
        )
                .replace("{player}", playerName)
                .replace("{role}", formatRole(member.getRole()))
                .replace("{status}", online ? "Online" : "Offline");

        List<String> lore = getLore(
                "gui.members.member",
                List.of(
                        "<gray>Role: <yellow>{role}</yellow></gray>",
                        "<gray>Status: {status_color}{status}</gray>",
                        "",
                        self
                                ? "<dark_gray>This is you.</dark_gray>"
                                : "<gold>Click to view member.</gold>"
                )
        );

        List<Component> loreComponents = new ArrayList<>();
        for (String line : lore) {
            String replaced = line
                    .replace("{player}", playerName)
                    .replace("{role}", formatRole(member.getRole()))
                    .replace("{status}", online ? "Online" : "Offline")
                    .replace("{status_color}", online ? "<green>" : "<gray>");
            loreComponents.add(miniMessage.deserialize(replaced));
        }

        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(offlinePlayer);
        meta.displayName(miniMessage.deserialize(name));
        meta.lore(loreComponents);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createMemberProfile(IslandMember member) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(member.getPlayerUuid());
        String playerName = getPlayerName(member.getPlayerUuid());
        boolean online = offlinePlayer.isOnline();

        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(offlinePlayer);

        String name = plugin.getConfig().getString(
                "gui.member.profile.name",
                "<yellow><bold>{player}</bold></yellow>"
        )
                .replace("{player}", playerName)
                .replace("{role}", formatRole(member.getRole()));

        List<String> lore = getLore(
                "gui.member.profile",
                List.of(
                        "<gray>Role: <yellow>{role}</yellow></gray>",
                        "<gray>Status: {status_color}{status}</gray>"
                )
        );

        List<Component> loreComponents = new ArrayList<>();
        for (String line : lore) {
            String replaced = line
                    .replace("{player}", playerName)
                    .replace("{role}", formatRole(member.getRole()))
                    .replace("{status}", online ? "Online" : "Offline")
                    .replace("{status_color}", online ? "<green>" : "<gray>");
            loreComponents.add(miniMessage.deserialize(replaced));
        }

        meta.displayName(miniMessage.deserialize(name));
        meta.lore(loreComponents);
        item.setItemMeta(meta);
        return item;
    }

    private boolean canKick(IslandRole viewerRole, IslandRole targetRole, boolean targetIsSelf) {
        if (targetIsSelf || targetRole == IslandRole.OWNER || !viewerRole.canKick()) {
            return false;
        }

        return viewerRole != IslandRole.CO_OWNER || targetRole == IslandRole.MEMBER;
    }

    private int roleOrder(IslandRole role) {
        return switch (role) {
            case OWNER -> 0;
            case CO_OWNER -> 1;
            case MEMBER -> 2;
        };
    }

    public String getPlayerName(UUID uuid) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        String name = offlinePlayer.getName();

        if (name == null || name.isBlank()) {
            return uuid.toString().substring(0, 8);
        }

        return name;
    }

    private Component title(String path, String fallback) {
        return miniMessage.deserialize(
                plugin.getConfig().getString(path, fallback)
        );
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
            slots = List.of(0, 4, 8);
        }

        ItemStack accent = createItem(material, name, List.of());

        for (int slot : slots) {
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, accent);
            }
        }
    }

    private void setButton(
            Inventory inventory,
            String path,
            int fallbackSlot,
            Material fallbackMaterial,
            String fallbackName,
            List<String> fallbackLore
    ) {
        int slot = getSlot(path, fallbackSlot);
        Material material = getMaterial(path, fallbackMaterial);
        String name = plugin.getConfig().getString(path + ".name", fallbackName);
        List<String> lore = getLore(path, fallbackLore);

        inventory.setItem(slot, createItem(material, name, lore));
    }

    private int getSlot(String path, int fallback) {
        int slot = plugin.getConfig().getInt(path + ".slot", fallback);

        if (slot < 0 || slot >= INVENTORY_SIZE) {
            plugin.getLogger().warning(
                    "Invalid GUI slot " + slot + " at " + path + ".slot. Using " + fallback + "."
            );
            return fallback;
        }

        return slot;
    }

    private Material getMaterial(String path, Material fallback) {
        String configured = plugin.getConfig().getString(
                path + ".material",
                fallback.name()
        );

        Material material = Material.matchMaterial(configured);

        if (material == null || material.isAir()) {
            plugin.getLogger().warning(
                    "Invalid GUI material '" + configured + "' at " + path + ".material. Using " + fallback.name() + "."
            );
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
            List<String> lore
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(miniMessage.deserialize(name));

        if (!lore.isEmpty()) {
            meta.lore(
                    lore.stream()
                            .map(miniMessage::deserialize)
                            .toList()
            );
        }

        item.setItemMeta(meta);
        return item;
    }

    private String formatRole(IslandRole role) {
        return switch (role) {
            case OWNER -> "Owner";
            case CO_OWNER -> "Co-Owner";
            case MEMBER -> "Member";
        };
    }
}
