package net.skyfoundry.gui.holder;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class IslandMembersMenuHolder implements InventoryHolder {

    private final Map<Integer, UUID> memberSlots = new HashMap<>();
    private Inventory inventory;

    public void setMember(int slot, UUID playerUuid) {
        memberSlots.put(slot, playerUuid);
    }

    public Optional<UUID> getMember(int slot) {
        return Optional.ofNullable(memberSlots.get(slot));
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) {
            throw new IllegalStateException("Island members menu inventory has not been initialized.");
        }

        return inventory;
    }
}
