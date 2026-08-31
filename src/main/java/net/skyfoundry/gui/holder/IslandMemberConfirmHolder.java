package net.skyfoundry.gui.holder;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class IslandMemberConfirmHolder implements InventoryHolder {

    public enum Action {
        KICK,
        TRANSFER
    }

    private final UUID targetUuid;
    private final Action action;
    private Inventory inventory;

    public IslandMemberConfirmHolder(UUID targetUuid, Action action) {
        this.targetUuid = targetUuid;
        this.action = action;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public Action getAction() {
        return action;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) {
            throw new IllegalStateException("Island member confirmation inventory has not been initialized.");
        }

        return inventory;
    }
}
