package net.stormboundmc.skyblock.gui.holder;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class IslandMemberMenuHolder implements InventoryHolder {

    private final UUID targetUuid;
    private Inventory inventory;

    public IslandMemberMenuHolder(UUID targetUuid) {
        this.targetUuid = targetUuid;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) {
            throw new IllegalStateException("Island member menu inventory has not been initialized.");
        }

        return inventory;
    }
}
