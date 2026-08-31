package net.skyfoundry.schematic;

import java.util.List;

public record LoadedSchematic(
        int width,
        int height,
        int length,
        List<SchematicBlock> blocks,
        List<SchematicBlockEntity> blockEntities) {

    public LoadedSchematic {
        blocks = List.copyOf(blocks);
        blockEntities = List.copyOf(blockEntities);
    }
}