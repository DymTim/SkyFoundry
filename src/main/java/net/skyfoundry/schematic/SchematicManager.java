package net.skyfoundry.schematic;

import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.tag.ByteArrayTag;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.IntTag;
import net.querz.nbt.tag.ShortTag;
import net.querz.nbt.tag.Tag;
import net.skyfoundry.SkyFoundry;
import net.skyfoundry.island.Island;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class SchematicManager {

    private final SkyFoundry plugin;

    public SchematicManager(SkyFoundry plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<LoadedSchematic> loadStarterSchematic() {
        String fileName = plugin.getConfig().getString(
                "schematic.file",
                "starter.schem");

        File file = new File(
                plugin.getDataFolder(),
                fileName);

        if (!file.exists()) {
            return CompletableFuture.failedFuture(
                    new IOException(
                            "Schematic file does not exist: "
                                    + file.getAbsolutePath()));
        }

        boolean async = plugin.getConfig().getBoolean(
                "schematic.async-load",
                true);

        if (!async) {
            try {
                return CompletableFuture.completedFuture(
                        loadSchematic(file));
            } catch (Exception exception) {
                return CompletableFuture.failedFuture(exception);
            }
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                return loadSchematic(file);
            } catch (Exception exception) {
                throw new CompletionException(exception);
            }
        });
    }

    private LoadedSchematic loadSchematic(File file)
            throws IOException {

        CompoundTag root = (CompoundTag) NBTUtil
                .read(file)
                .getTag();

        CompoundTag schematic = root;

        if (root.containsKey("Schematic")) {
            Tag<?> schematicTag = root.get("Schematic");

            if (schematicTag instanceof CompoundTag compoundTag) {
                schematic = compoundTag;
            }
        }

        int version = getInt(schematic, "Version");

        if (version != 2 && version != 3) {
            throw new IOException(
                    "Unsupported Sponge schematic version: "
                            + version);
        }

        int width = getUnsignedShort(
                schematic,
                "Width");

        int height = getUnsignedShort(
                schematic,
                "Height");

        int length = getUnsignedShort(
                schematic,
                "Length");

        if (width <= 0 || height <= 0 || length <= 0) {
            throw new IOException(
                    "Invalid schematic dimensions: "
                            + width
                            + "x"
                            + height
                            + "x"
                            + length);
        }

        CompoundTag paletteTag;
        byte[] rawBlockData;

        if (version == 3) {
            Tag<?> blocksRaw = schematic.get("Blocks");

            if (!(blocksRaw instanceof CompoundTag blocksTag)) {
                throw new IOException(
                        "Sponge v3 schematic is missing Blocks.");
            }

            paletteTag = requireCompound(
                    blocksTag,
                    "Palette");

            rawBlockData = requireByteArray(
                    blocksTag,
                    "Data");

        } else {
            paletteTag = requireCompound(
                    schematic,
                    "Palette");

            rawBlockData = requireByteArray(
                    schematic,
                    "BlockData");
        }

        Map<Integer, String> palette = readPalette(paletteTag);

        int expectedBlocks = Math.multiplyExact(
                Math.multiplyExact(width, height),
                length);

        int[] blockIndices = decodeVarInts(
                rawBlockData,
                expectedBlocks);

        boolean pasteAir = plugin.getConfig().getBoolean(
                "schematic.paste-air",
                false);

        List<SchematicBlock> blocks = new ArrayList<>(expectedBlocks);

        for (int index = 0; index < expectedBlocks; index++) {
            int paletteIndex = blockIndices[index];

            String blockData = palette.get(paletteIndex);

            if (blockData == null) {
                throw new IOException(
                        "Schematic references missing palette index "
                                + paletteIndex);
            }

            if (!pasteAir
                    && blockData.equals("minecraft:air")) {
                continue;
            }

            int x = index % width;
            int z = (index / width) % length;
            int y = index / (width * length);

            blocks.add(
                    new SchematicBlock(
                            x,
                            y,
                            z,
                            blockData));
        }

        return new LoadedSchematic(
                width,
                height,
                length,
                blocks);
    }

    public CompletableFuture<Void> paste(
            Island island,
            LoadedSchematic schematic) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        int blocksPerTick = Math.max(
                1,
                plugin.getConfig().getInt(
                        "schematic.blocks-per-tick",
                        750));

        int baseX = island.getCenterX()
                - (schematic.width() / 2);

        int baseZ = island.getCenterZ()
                - (schematic.length() / 2);

        int baseY = plugin.getConfig().getInt(
                "islands.creation-y",
                100);

        List<SchematicBlock> blocks = schematic.blocks();

        if (blocks.isEmpty()) {
            future.complete(null);
            return future;
        }

        final int[] index = { 0 };
        final BukkitTask[] task = new BukkitTask[1];

        task[0] = Bukkit.getScheduler().runTaskTimer(
                plugin,
                () -> {
                    try {
                        World world = plugin.getIslandWorld();

                        int processed = 0;

                        while (index[0] < blocks.size()
                                && processed < blocksPerTick) {
                            SchematicBlock schematicBlock = blocks.get(index[0]++);

                            BlockData blockData;

                            try {
                                blockData = Bukkit.createBlockData(
                                        schematicBlock.blockData());
                            } catch (IllegalArgumentException exception) {
                                plugin.getLogger().warning(
                                        "Skipping unsupported schematic block: "
                                                + schematicBlock.blockData());

                                processed++;
                                continue;
                            }

                            world.getBlockAt(
                                    baseX + schematicBlock.x(),
                                    baseY + schematicBlock.y(),
                                    baseZ + schematicBlock.z()).setBlockData(
                                            blockData,
                                            false);

                            processed++;
                        }

                        if (index[0] >= blocks.size()) {
                            task[0].cancel();
                            future.complete(null);
                        }

                    } catch (Exception exception) {
                        task[0].cancel();
                        future.completeExceptionally(exception);
                    }
                },
                1L,
                1L);

        return future;
    }

    private Map<Integer, String> readPalette(
            CompoundTag paletteTag) throws IOException {

        Map<Integer, String> palette = new HashMap<>();

        for (String key : paletteTag.keySet()) {
            Tag<?> value = paletteTag.get(key);

            if (!(value instanceof IntTag intTag)) {
                throw new IOException(
                        "Invalid palette value for " + key);
            }

            palette.put(
                    intTag.asInt(),
                    key);
        }

        return palette;
    }

    private int[] decodeVarInts(
            byte[] data,
            int expectedCount) throws IOException {

        int[] result = new int[expectedCount];

        int dataIndex = 0;

        for (int i = 0; i < expectedCount; i++) {
            int value = 0;
            int position = 0;

            while (true) {
                if (dataIndex >= data.length) {
                    throw new IOException(
                            "Schematic block data ended unexpectedly.");
                }

                int currentByte = data[dataIndex++] & 0xFF;

                value |= (currentByte & 0x7F) << position;

                if ((currentByte & 0x80) == 0) {
                    break;
                }

                position += 7;

                if (position >= 35) {
                    throw new IOException(
                            "Invalid VarInt in schematic block data.");
                }
            }

            result[i] = value;
        }

        return result;
    }

    private CompoundTag requireCompound(
            CompoundTag parent,
            String name) throws IOException {

        Tag<?> tag = parent.get(name);

        if (!(tag instanceof CompoundTag compoundTag)) {
            throw new IOException(
                    "Schematic is missing compound tag: "
                            + name);
        }

        return compoundTag;
    }

    private byte[] requireByteArray(
            CompoundTag parent,
            String name) throws IOException {

        Tag<?> tag = parent.get(name);

        if (!(tag instanceof ByteArrayTag byteArrayTag)) {
            throw new IOException(
                    "Schematic is missing byte array: "
                            + name);
        }

        return byteArrayTag.getValue();
    }

    private int getInt(
            CompoundTag parent,
            String name) throws IOException {

        Tag<?> tag = parent.get(name);

        if (!(tag instanceof IntTag intTag)) {
            throw new IOException(
                    "Schematic is missing integer tag: "
                            + name);
        }

        return intTag.asInt();
    }

    private int getUnsignedShort(
            CompoundTag parent,
            String name) throws IOException {

        Tag<?> tag = parent.get(name);

        if (!(tag instanceof ShortTag shortTag)) {
            throw new IOException(
                    "Schematic is missing short tag: "
                            + name);
        }

        return Short.toUnsignedInt(
                shortTag.asShort());
    }
}