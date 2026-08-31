package net.skyfoundry.schematic;

import java.util.List;

public record LoadedSchematic(
        int width,
        int height,
        int length,
        List<SchematicBlock> blocks) {

    public LoadedSchematic {
        blocks = List.copyOf(blocks);
    }
}