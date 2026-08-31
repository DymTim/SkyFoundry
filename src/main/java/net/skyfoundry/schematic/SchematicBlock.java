package net.skyfoundry.schematic;

public record SchematicBlock(
        int x,
        int y,
        int z,
        String blockData) {
}