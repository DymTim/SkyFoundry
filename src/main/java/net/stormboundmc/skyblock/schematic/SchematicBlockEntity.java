package net.stormboundmc.skyblock.schematic;

import net.querz.nbt.tag.CompoundTag;

public record SchematicBlockEntity(
                int x,
                int y,
                int z,
                CompoundTag data) {
}