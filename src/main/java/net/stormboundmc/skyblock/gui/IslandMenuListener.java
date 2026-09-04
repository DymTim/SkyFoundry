package net.stormboundmc.skyblock.gui;

import net.stormboundmc.skyblock.StormboundSkyblock;
import net.stormboundmc.skyblock.gui.holder.CreateIslandMenuHolder;
import net.stormboundmc.skyblock.gui.holder.IslandMemberConfirmHolder;
import net.stormboundmc.skyblock.gui.holder.IslandMemberMenuHolder;
import net.stormboundmc.skyblock.gui.holder.IslandMembersMenuHolder;
import net.stormboundmc.skyblock.gui.holder.IslandMenuHolder;
import net.stormboundmc.skyblock.gui.holder.IslandSettingsMenuHolder;
import net.stormboundmc.skyblock.gui.holder.IslandTimeMenuHolder;
import net.stormboundmc.skyblock.gui.holder.IslandWeatherMenuHolder;
import net.stormboundmc.skyblock.gui.holder.IslandUpgradesMenuHolder;
import net.stormboundmc.skyblock.island.Island;
import net.stormboundmc.skyblock.island.IslandManager;
import net.stormboundmc.skyblock.island.IslandMember;
import net.stormboundmc.skyblock.island.IslandRole;
import net.stormboundmc.skyblock.island.IslandSettings;
import net.stormboundmc.skyblock.island.IslandSettingsManager;
import net.stormboundmc.skyblock.island.IslandTimeMode;
import net.stormboundmc.skyblock.island.IslandVisualManager;
import net.stormboundmc.skyblock.island.IslandWeatherMode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.sql.SQLException;
import java.util.UUID;

public final class IslandMenuListener implements Listener {

    private final StormboundSkyblock plugin;
    private final IslandManager islandManager;
    private final IslandMenu islandMenu;
    private final IslandMembersMenu islandMembersMenu;
    private final IslandSettingsManager settingsManager;
    private final IslandSettingsMenu islandSettingsMenu;
    private final IslandVisualManager visualManager;
    private final IslandUpgradesMenu islandUpgradesMenu;

    public IslandMenuListener(
            StormboundSkyblock plugin,
            IslandManager islandManager,
            IslandSettingsManager settingsManager,
            IslandVisualManager visualManager
    ) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.settingsManager = settingsManager;
        this.visualManager = visualManager;
        this.islandMenu = new IslandMenu(plugin, islandManager);
        this.islandMembersMenu = new IslandMembersMenu(plugin, islandManager, islandMenu);
        this.islandUpgradesMenu = new IslandUpgradesMenu(plugin, islandManager);
        this.islandSettingsMenu = new IslandSettingsMenu(
                plugin,
                islandManager,
                settingsManager,
                islandMenu
        );
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        InventoryHolder holder = topInventory.getHolder();

        if (!isStormboundMenu(holder)) {
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

        if (holder instanceof CreateIslandMenuHolder) {
            handleCreateMenuClick(player, slot);
            return;
        }

        if (holder instanceof IslandMembersMenuHolder membersHolder) {
            handleMembersMenuClick(player, membersHolder, slot);
            return;
        }

        if (holder instanceof IslandMemberMenuHolder memberHolder) {
            handleMemberMenuClick(player, memberHolder, slot);
            return;
        }

        if (holder instanceof IslandMemberConfirmHolder confirmHolder) {
            handleConfirmationClick(player, confirmHolder, slot);
            return;
        }

        if (holder instanceof IslandUpgradesMenuHolder) {
            handleUpgradesMenuClick(player, slot);
            return;
        }

        if (holder instanceof IslandSettingsMenuHolder) {
            handleSettingsMenuClick(player, slot);
            return;
        }

        if (holder instanceof IslandWeatherMenuHolder) {
            handleWeatherMenuClick(player, slot);
            return;
        }

        if (holder instanceof IslandTimeMenuHolder) {
            handleTimeMenuClick(player, slot);
            return;
        }

        handleMainMenuClick(player, slot);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();

        if (!isStormboundMenu(topInventory.getHolder())) {
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

    private boolean isStormboundMenu(InventoryHolder holder) {
        return holder instanceof IslandMenuHolder
                || holder instanceof CreateIslandMenuHolder
                || holder instanceof IslandMembersMenuHolder
                || holder instanceof IslandMemberMenuHolder
                || holder instanceof IslandMemberConfirmHolder
                || holder instanceof IslandSettingsMenuHolder
                || holder instanceof IslandUpgradesMenuHolder
                || holder instanceof IslandWeatherMenuHolder
                || holder instanceof IslandTimeMenuHolder;
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
            islandMembersMenu.openMembers(player);
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
                        "<red>Your island role cannot invite players.</red>"
                );
                return;
            }

            player.closeInventory();
            sendGuiMessage(
                    player,
                    "gui.messages.invite-instructions",
                    "<gray>Use <gold>/island invite <player></gold> to invite someone.</gray>"
            );
            return;
        }

        if (slot == getSlot("gui.main.buttons.upgrades", 30)) {
            islandUpgradesMenu.open(player);
            return;
        }

        if (slot == getSlot("gui.main.buttons.settings", 32)) {
            islandSettingsMenu.open(player);
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

    private void handleSettingsMenuClick(Player player, int slot) {
        IslandSettings settings = settingsManager
                .getSettings(player.getUniqueId())
                .orElse(null);

        if (settings == null) {
            islandMenu.open(player);
            return;
        }

        boolean canManage = settingsManager.canManage(player.getUniqueId());

        if (slot == getSlot("gui.settings.buttons.visiting", 11)) {
            if (!canManage) {
                sendViewOnly(player);
                return;
            }
            updateFunctionalSetting(player, "visiting", !settings.isVisitingEnabled());
            return;
        }

        if (slot == getSlot("gui.settings.buttons.member-building", 13)) {
            if (!canManage) {
                sendViewOnly(player);
                return;
            }
            updateFunctionalSetting(player, "building", !settings.isMemberBuilding());
            return;
        }

        if (slot == getSlot("gui.settings.buttons.member-interactions", 15)) {
            if (!canManage) {
                sendViewOnly(player);
                return;
            }
            updateFunctionalSetting(player, "interactions", !settings.isMemberInteractions());
            return;
        }

        if (slot == getSlot("gui.settings.buttons.weather", 20)) {
            if (!canEditCosmetic(player, "settings.cosmetics.permissions.weather", "stormbound.skyblock.cosmetic.weather")) {
                sendCosmeticLocked(player);
                return;
            }
            islandSettingsMenu.openWeather(player);
            return;
        }

        if (slot == getSlot("gui.settings.buttons.time", 22)) {
            if (!canEditCosmetic(player, "settings.cosmetics.permissions.time", "stormbound.skyblock.cosmetic.time")) {
                sendCosmeticLocked(player);
                return;
            }
            islandSettingsMenu.openTime(player);
            return;
        }

        if (slot == getSlot("gui.settings.buttons.border", 24)) {
            if (!canEditCosmetic(player, "settings.cosmetics.permissions.border", "stormbound.skyblock.cosmetic.border")) {
                sendCosmeticLocked(player);
                return;
            }

            try {
                if (settingsManager.setBorderEnabled(player.getUniqueId(), !settings.isBorderEnabled())) {
                    refreshIslandVisuals(player);
                }
                islandSettingsMenu.open(player);
            } catch (SQLException exception) {
                logSettingsError(exception);
                sendSettingsFailed(player);
            }
            return;
        }

        if (slot == getSlot("gui.settings.buttons.back", 40)) {
            islandMenu.open(player);
            return;
        }

        if (slot == getSlot("gui.settings.buttons.close", 44)) {
            player.closeInventory();
        }
    }

    private void handleWeatherMenuClick(Player player, int slot) {
        if (!canEditCosmetic(player, "settings.cosmetics.permissions.weather", "stormbound.skyblock.cosmetic.weather")) {
            sendCosmeticLocked(player);
            islandSettingsMenu.open(player);
            return;
        }

        IslandWeatherMode mode = null;
        if (slot == getSlot("gui.settings-weather.buttons.default", 20)) {
            mode = IslandWeatherMode.DEFAULT;
        } else if (slot == getSlot("gui.settings-weather.buttons.clear", 22)) {
            mode = IslandWeatherMode.CLEAR;
        } else if (slot == getSlot("gui.settings-weather.buttons.rain", 24)) {
            mode = IslandWeatherMode.RAIN;
        } else if (slot == getSlot("gui.settings-weather.buttons.back", 40)) {
            islandSettingsMenu.open(player);
            return;
        } else if (slot == getSlot("gui.settings-weather.buttons.close", 44)) {
            player.closeInventory();
            return;
        }

        if (mode == null) {
            return;
        }

        try {
            if (settingsManager.setWeatherMode(player.getUniqueId(), mode)) {
                refreshIslandVisuals(player);
            }
            islandSettingsMenu.openWeather(player);
        } catch (SQLException exception) {
            logSettingsError(exception);
            sendSettingsFailed(player);
        }
    }

    private void handleTimeMenuClick(Player player, int slot) {
        if (!canEditCosmetic(player, "settings.cosmetics.permissions.time", "stormbound.skyblock.cosmetic.time")) {
            sendCosmeticLocked(player);
            islandSettingsMenu.open(player);
            return;
        }

        IslandTimeMode mode = null;
        if (slot == getSlot("gui.settings-time.buttons.default", 11)) {
            mode = IslandTimeMode.DEFAULT;
        } else if (slot == getSlot("gui.settings-time.buttons.sunrise", 13)) {
            mode = IslandTimeMode.SUNRISE;
        } else if (slot == getSlot("gui.settings-time.buttons.noon", 15)) {
            mode = IslandTimeMode.NOON;
        } else if (slot == getSlot("gui.settings-time.buttons.sunset", 21)) {
            mode = IslandTimeMode.SUNSET;
        } else if (slot == getSlot("gui.settings-time.buttons.midnight", 23)) {
            mode = IslandTimeMode.MIDNIGHT;
        } else if (slot == getSlot("gui.settings-time.buttons.back", 40)) {
            islandSettingsMenu.open(player);
            return;
        } else if (slot == getSlot("gui.settings-time.buttons.close", 44)) {
            player.closeInventory();
            return;
        }

        if (mode == null) {
            return;
        }

        try {
            if (settingsManager.setTimeMode(player.getUniqueId(), mode)) {
                refreshIslandVisuals(player);
            }
            islandSettingsMenu.openTime(player);
        } catch (SQLException exception) {
            logSettingsError(exception);
            sendSettingsFailed(player);
        }
    }

    private void updateFunctionalSetting(Player player, String setting, boolean enabled) {
        try {
            boolean changed = switch (setting) {
                case "visiting" -> settingsManager.setVisitingEnabled(player.getUniqueId(), enabled);
                case "building" -> settingsManager.setMemberBuilding(player.getUniqueId(), enabled);
                case "interactions" -> settingsManager.setMemberInteractions(player.getUniqueId(), enabled);
                default -> false;
            };

            if (!changed) {
                sendSettingsFailed(player);
                return;
            }

            islandSettingsMenu.open(player);
        } catch (SQLException exception) {
            logSettingsError(exception);
            sendSettingsFailed(player);
        }
    }

    private boolean canEditCosmetic(Player player, String path, String fallback) {
        if (!settingsManager.canManage(player.getUniqueId())) {
            return false;
        }

        String permission = plugin.getConfig().getString(path, fallback);
        return permission != null && !permission.isBlank() && player.hasPermission(permission);
    }

    private void refreshIslandVisuals(Player player) {
        islandManager.getIsland(player.getUniqueId()).ifPresent(visualManager::refreshIsland);
    }

    private void sendViewOnly(Player player) {
        sendGuiMessage(
                player,
                "messages.settings-view-only",
                "<red>Only the island Owner or a Co-Owner can change this setting.</red>"
        );
    }

    private void sendCosmeticLocked(Player player) {
        sendGuiMessage(
                player,
                "messages.cosmetic-locked",
                "<red>You do not have access to change this island cosmetic.</red>"
        );
    }

    private void sendSettingsFailed(Player player) {
        sendGuiMessage(
                player,
                "messages.settings-save-failed",
                "<red>Could not save that island setting.</red>"
        );
    }

    private void logSettingsError(SQLException exception) {
        plugin.getLogger().severe("Failed to update island settings: " + exception.getMessage());
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

    private void handleMembersMenuClick(
            Player player,
            IslandMembersMenuHolder holder,
            int slot
    ) {
        UUID memberUuid = holder.getMember(slot).orElse(null);

        if (memberUuid != null) {
            islandMembersMenu.openMember(player, memberUuid);
            return;
        }

        if (slot == getSlot("gui.members.buttons.invite", 36)) {
            IslandRole role = islandManager
                    .getRole(player.getUniqueId())
                    .orElse(null);

            if (role == null || !role.canInvite()) {
                return;
            }

            player.closeInventory();
            sendGuiMessage(
                    player,
                    "gui.messages.invite-instructions",
                    "<gray>Use <gold>/island invite <player></gold> to invite someone.</gray>"
            );
            return;
        }

        if (slot == getSlot("gui.members.buttons.back", 40)) {
            islandMenu.open(player);
            return;
        }

        if (slot == getSlot("gui.members.buttons.close", 44)) {
            player.closeInventory();
        }
    }

    private void handleMemberMenuClick(
            Player player,
            IslandMemberMenuHolder holder,
            int slot
    ) {
        UUID targetUuid = holder.getTargetUuid();
        IslandMember target = islandManager
                .getMember(targetUuid)
                .orElse(null);

        IslandRole viewerRole = islandManager
                .getRole(player.getUniqueId())
                .orElse(null);

        if (target == null || viewerRole == null) {
            islandMembersMenu.openMembers(player);
            return;
        }

        int roleSlot = target.getRole() == IslandRole.MEMBER
                ? getSlot("gui.member.buttons.promote", 20)
                : getSlot("gui.member.buttons.demote", 20);

        if (slot == roleSlot
                && viewerRole.canChangeRoles()
                && target.getRole() != IslandRole.OWNER
                && !targetUuid.equals(player.getUniqueId())) {

            IslandRole newRole = target.getRole() == IslandRole.MEMBER
                    ? IslandRole.CO_OWNER
                    : IslandRole.MEMBER;

            changeRole(player, targetUuid, newRole);
            return;
        }

        if (slot == getSlot("gui.member.buttons.transfer", 22)
                && viewerRole.canTransferOwnership()
                && target.getRole() != IslandRole.OWNER
                && !targetUuid.equals(player.getUniqueId())) {

            islandMembersMenu.openConfirmation(
                    player,
                    targetUuid,
                    IslandMemberConfirmHolder.Action.TRANSFER
            );
            return;
        }

        if (slot == getSlot("gui.member.buttons.kick", 24)
                && canKick(viewerRole, target.getRole(), targetUuid.equals(player.getUniqueId()))) {

            islandMembersMenu.openConfirmation(
                    player,
                    targetUuid,
                    IslandMemberConfirmHolder.Action.KICK
            );
            return;
        }

        if (slot == getSlot("gui.member.buttons.back", 40)) {
            islandMembersMenu.openMembers(player);
            return;
        }

        if (slot == getSlot("gui.member.buttons.close", 44)) {
            player.closeInventory();
        }
    }

    private void handleConfirmationClick(
            Player player,
            IslandMemberConfirmHolder holder,
            int slot
    ) {
        if (slot == getSlot("gui.member-confirm.buttons.cancel", 24)
                || slot == getSlot("gui.member-confirm.buttons.back", 40)) {
            islandMembersMenu.openMember(player, holder.getTargetUuid());
            return;
        }

        if (slot != getSlot("gui.member-confirm.buttons.confirm", 20)) {
            return;
        }

        if (holder.getAction() == IslandMemberConfirmHolder.Action.KICK) {
            confirmKick(player, holder.getTargetUuid());
        } else {
            confirmTransfer(player, holder.getTargetUuid());
        }
    }

    private void changeRole(
            Player player,
            UUID targetUuid,
            IslandRole newRole
    ) {
        try {
            if (!islandManager.setRole(player.getUniqueId(), targetUuid, newRole)) {
                sendGuiMessage(
                        player,
                        "messages.role-change-failed",
                        "<red>Could not change that player's island role.</red>"
                );
                islandMembersMenu.openMember(player, targetUuid);
                return;
            }

            String targetName = islandMembersMenu.getPlayerName(targetUuid);
            String roleName = formatRole(newRole);

            sendGuiMessage(
                    player,
                    "messages.role-changed",
                    "<green>{player}'s role is now <yellow>{role}</yellow>.</green>",
                    "{player}", targetName,
                    "{role}", roleName
            );

            Player targetPlayer = Bukkit.getPlayer(targetUuid);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                sendGuiMessage(
                        targetPlayer,
                        "messages.your-role-changed",
                        "<green>Your island role is now <yellow>{role}</yellow>.</green>",
                        "{role}", roleName
                );
            }

            islandMembersMenu.openMember(player, targetUuid);

        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "Failed to change island member role: " + exception.getMessage()
            );

            sendGuiMessage(
                    player,
                    "messages.role-change-failed",
                    "<red>Could not change that player's island role.</red>"
            );
        }
    }

    private void confirmKick(Player player, UUID targetUuid) {
        String targetName = islandMembersMenu.getPlayerName(targetUuid);

        try {
            if (!islandManager.kickMember(player.getUniqueId(), targetUuid)) {
                sendGuiMessage(
                        player,
                        "messages.kick-failed",
                        "<red>Could not remove that island member.</red>"
                );
                islandMembersMenu.openMembers(player);
                return;
            }

            sendGuiMessage(
                    player,
                    "messages.kick-success",
                    "<green>{player} has been removed from the island.</green>",
                    "{player}", targetName
            );

            Player targetPlayer = Bukkit.getPlayer(targetUuid);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                sendGuiMessage(
                        targetPlayer,
                        "messages.kicked",
                        "<red>You have been removed from the island.</red>"
                );
            }

            islandMembersMenu.openMembers(player);

        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "Failed to kick island member: " + exception.getMessage()
            );

            sendGuiMessage(
                    player,
                    "messages.kick-failed",
                    "<red>Could not remove that island member.</red>"
            );
        }
    }

    private void confirmTransfer(Player player, UUID targetUuid) {
        String targetName = islandMembersMenu.getPlayerName(targetUuid);

        try {
            if (!islandManager.transferOwnership(player.getUniqueId(), targetUuid)) {
                sendGuiMessage(
                        player,
                        "messages.transfer-failed",
                        "<red>Could not transfer island ownership.</red>"
                );
                islandMembersMenu.openMembers(player);
                return;
            }

            sendGuiMessage(
                    player,
                    "messages.transfer-success",
                    "<green>Island ownership has been transferred to <yellow>{player}</yellow>.</green>",
                    "{player}", targetName
            );

            Player targetPlayer = Bukkit.getPlayer(targetUuid);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                sendGuiMessage(
                        targetPlayer,
                        "messages.transfer-received",
                        "<green>You are now the owner of this island.</green>"
                );
            }

            islandMenu.open(player);

        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "Failed to transfer island ownership: " + exception.getMessage()
            );

            sendGuiMessage(
                    player,
                    "messages.transfer-failed",
                    "<red>Could not transfer island ownership.</red>"
            );
        }
    }

    private boolean canKick(
            IslandRole viewerRole,
            IslandRole targetRole,
            boolean targetIsSelf
    ) {
        if (targetIsSelf || targetRole == IslandRole.OWNER || !viewerRole.canKick()) {
            return false;
        }

        return viewerRole != IslandRole.CO_OWNER || targetRole == IslandRole.MEMBER;
    }

    private void handleUpgradesMenuClick(Player player, int slot) {
        if (slot == getSlot("gui.upgrades.buttons.back", 40)) { islandMenu.open(player); return; }
        if (slot == getSlot("gui.upgrades.buttons.close", 44)) { player.closeInventory(); return; }
        Island island = islandManager.getIsland(player.getUniqueId()).orElse(null);
        IslandRole role = islandManager.getRole(player.getUniqueId()).orElse(null);
        if (island == null || role == null) return;
        boolean editable = role == IslandRole.OWNER || role == IslandRole.CO_OWNER;
        if (!editable) { sendGuiMessage(player,"gui.messages.upgrade-no-permission","<red>Only the Owner or Co-Owner can upgrade the island.</red>"); return; }
        try {
            if (slot == getSlot("gui.upgrades.buttons.size", 20)) {
                if (islandManager.upgradeSize(player.getUniqueId())) {
                    visualManager.refreshIsland(island);
                    sendGuiMessage(player,"gui.messages.upgrade-size-success","<green>Island size upgraded to <yellow>{value}x{value}</yellow>.</green>","{value}",String.valueOf(island.getSize()));
                }
                islandUpgradesMenu.open(player); return;
            }
            if (slot == getSlot("gui.upgrades.buttons.member-limit", 24)) {
                if (islandManager.upgradeMemberLimit(player.getUniqueId())) sendGuiMessage(player,"gui.messages.upgrade-members-success","<green>Member limit upgraded to <yellow>{value}</yellow>.</green>","{value}",String.valueOf(island.getMemberLimit()));
                islandUpgradesMenu.open(player);
            }
        } catch (SQLException exception) {
            plugin.getLogger().severe("Failed to upgrade island: " + exception.getMessage());
            sendGuiMessage(player,"gui.messages.upgrade-failed","<red>Could not save that island upgrade.</red>");
        }
    }

    private void runIslandCommand(Player player, String arguments) {
        player.closeInventory();

        Bukkit.getScheduler().runTask(
                plugin,
                () -> Bukkit.dispatchCommand(
                        player,
                        "island " + arguments
                )
        );
    }

    private int getSlot(String path, int fallback) {
        return plugin.getConfig().getInt(path + ".slot", fallback);
    }

    private String formatRole(IslandRole role) {
        return switch (role) {
            case OWNER -> "Owner";
            case CO_OWNER -> "Co-Owner";
            case MEMBER -> "Member";
        };
    }

    private void sendGuiMessage(
            Player player,
            String path,
            String fallback,
            String... replacements
    ) {
        String prefix = plugin.getConfig().getString(
                "messages.prefix",
                "<gold><bold>⚙ STORMBOUND</bold></gold> <dark_gray>┃</dark_gray> "
        );

        String message = plugin.getConfig().getString(path, fallback);

        for (int index = 0; index + 1 < replacements.length; index += 2) {
            message = message.replace(replacements[index], replacements[index + 1]);
        }

        player.sendRichMessage(prefix + message);
    }
}
