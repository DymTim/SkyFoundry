package net.skyfoundry.gui;

import net.skyfoundry.SkyFoundry;
import net.skyfoundry.gui.holder.CreateIslandMenuHolder;
import net.skyfoundry.gui.holder.IslandMemberConfirmHolder;
import net.skyfoundry.gui.holder.IslandMemberMenuHolder;
import net.skyfoundry.gui.holder.IslandMembersMenuHolder;
import net.skyfoundry.gui.holder.IslandMenuHolder;
import net.skyfoundry.island.IslandManager;
import net.skyfoundry.island.IslandMember;
import net.skyfoundry.island.IslandRole;
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

    private final SkyFoundry plugin;
    private final IslandManager islandManager;
    private final IslandMenu islandMenu;
    private final IslandMembersMenu islandMembersMenu;

    public IslandMenuListener(
            SkyFoundry plugin,
            IslandManager islandManager
    ) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.islandMenu = new IslandMenu(plugin, islandManager);
        this.islandMembersMenu = new IslandMembersMenu(plugin, islandManager, islandMenu);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        InventoryHolder holder = topInventory.getHolder();

        if (!isSkyFoundryMenu(holder)) {
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

        handleMainMenuClick(player, slot);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();

        if (!isSkyFoundryMenu(topInventory.getHolder())) {
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

    private boolean isSkyFoundryMenu(InventoryHolder holder) {
        return holder instanceof IslandMenuHolder
                || holder instanceof CreateIslandMenuHolder
                || holder instanceof IslandMembersMenuHolder
                || holder instanceof IslandMemberMenuHolder
                || holder instanceof IslandMemberConfirmHolder;
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
            sendGuiMessage(
                    player,
                    "gui.messages.upgrades-coming-soon",
                    "<gray>Island upgrades are coming soon.</gray>"
            );
            return;
        }

        if (slot == getSlot("gui.main.buttons.settings", 32)) {
            sendGuiMessage(
                    player,
                    "gui.messages.settings-coming-soon",
                    "<gray>Island settings are coming soon.</gray>"
            );
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
                "<gold><bold>⚙ SKYFOUNDRY</bold></gold> <dark_gray>┃</dark_gray> "
        );

        String message = plugin.getConfig().getString(path, fallback);

        for (int index = 0; index + 1 < replacements.length; index += 2) {
            message = message.replace(replacements[index], replacements[index + 1]);
        }

        player.sendRichMessage(prefix + message);
    }
}
