package net.skyfoundry.schematic;

import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.tag.ByteArrayTag;
import net.querz.nbt.tag.ByteTag;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.IntArrayTag;
import net.querz.nbt.tag.IntTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.ShortTag;
import net.querz.nbt.tag.StringTag;
import net.querz.nbt.tag.Tag;
import net.skyfoundry.SkyFoundry;
import net.skyfoundry.island.Island;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
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
                                "schematics/starter.schem");

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

                int version = getInt(
                                schematic,
                                "Version");

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
                List<SchematicBlockEntity> blockEntities;

                if (version == 3) {
                        CompoundTag blocksTag = requireCompound(
                                        schematic,
                                        "Blocks");

                        paletteTag = requireCompound(
                                        blocksTag,
                                        "Palette");

                        rawBlockData = requireByteArray(
                                        blocksTag,
                                        "Data");

                        blockEntities = readBlockEntities(
                                        blocksTag,
                                        "BlockEntities");

                } else {
                        paletteTag = requireCompound(
                                        schematic,
                                        "Palette");

                        rawBlockData = requireByteArray(
                                        schematic,
                                        "BlockData");

                        blockEntities = readBlockEntities(
                                        schematic,
                                        "BlockEntities");
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

                        String blockData = palette.get(
                                        paletteIndex);

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

                if (plugin.getConfig().getBoolean(
                                "debug.enabled",
                                false)) {
                        plugin.getLogger().info(
                                        "Loaded schematic "
                                                        + width
                                                        + "x"
                                                        + height
                                                        + "x"
                                                        + length
                                                        + " with "
                                                        + blocks.size()
                                                        + " block(s) and "
                                                        + blockEntities.size()
                                                        + " block entity/entities.");
                }

                return new LoadedSchematic(
                                width,
                                height,
                                length,
                                blocks,
                                blockEntities);
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
                        scheduleBlockEntityPaste(
                                        schematic,
                                        baseX,
                                        baseY,
                                        baseZ,
                                        future);

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

                                                        scheduleBlockEntityPaste(
                                                                        schematic,
                                                                        baseX,
                                                                        baseY,
                                                                        baseZ,
                                                                        future);
                                                }

                                        } catch (Exception exception) {
                                                task[0].cancel();

                                                future.completeExceptionally(
                                                                exception);
                                        }
                                },
                                1L,
                                1L);

                return future;
        }

        private void scheduleBlockEntityPaste(
                        LoadedSchematic schematic,
                        int baseX,
                        int baseY,
                        int baseZ,
                        CompletableFuture<Void> future) {
                Bukkit.getScheduler().runTaskLater(
                                plugin,
                                () -> {
                                        try {
                                                pasteBlockEntities(
                                                                schematic,
                                                                baseX,
                                                                baseY,
                                                                baseZ);

                                                future.complete(null);

                                        } catch (Exception exception) {
                                                future.completeExceptionally(
                                                                exception);
                                        }
                                },
                                1L);
        }

        private void pasteBlockEntities(
                        LoadedSchematic schematic,
                        int baseX,
                        int baseY,
                        int baseZ) {
                for (SchematicBlockEntity blockEntity : schematic.blockEntities()) {
                        Block block = plugin
                                        .getIslandWorld()
                                        .getBlockAt(
                                                        baseX + blockEntity.x(),
                                                        baseY + blockEntity.y(),
                                                        baseZ + blockEntity.z());

                        if (!(block.getState() instanceof Container container)) {
                                plugin.getLogger().warning(
                                                "Schematic block entity at "
                                                                + block.getX()
                                                                + ", "
                                                                + block.getY()
                                                                + ", "
                                                                + block.getZ()
                                                                + " is not a Bukkit container.");

                                continue;
                        }

                        pasteContainerItems(
                                        container,
                                        blockEntity.data());
                }
        }

        private void pasteContainerItems(
                        Container container,
                        CompoundTag data) {
                Tag<?> itemsTag = data.get("Items");

                if (!(itemsTag instanceof ListTag<?> items)) {
                        plugin.getLogger().warning(
                                        "Container schematic data contains no Items list.");

                        return;
                }

                /*
                 * Chest#getInventory() can represent the combined inventory of a
                 * double chest. We only want the inventory belonging to the exact
                 * chest block stored in the schematic.
                 */
                Inventory inventory;

                if (container instanceof Chest chest) {
                        inventory = chest.getBlockInventory();
                } else {
                        inventory = container.getInventory();
                }

                inventory.clear();

                int restoredItems = 0;

                for (Tag<?> rawItem : items) {
                        if (!(rawItem instanceof CompoundTag itemTag)) {
                                continue;
                        }

                        int slot = getNumberValue(
                                        itemTag,
                                        "Slot",
                                        -1);

                        if (slot < 0
                                        || slot >= inventory.getSize()) {
                                plugin.getLogger().warning(
                                                "Skipping schematic item with invalid slot "
                                                                + slot);

                                continue;
                        }

                        String itemId = getStringValue(
                                        itemTag,
                                        "id");

                        if (itemId == null) {
                                itemId = getStringValue(
                                                itemTag,
                                                "Id");
                        }

                        if (itemId == null) {
                                plugin.getLogger().warning(
                                                "Skipping schematic item without an id.");

                                continue;
                        }

                        int count = getNumberValue(
                                        itemTag,
                                        "count",
                                        -1);

                        if (count < 0) {
                                count = getNumberValue(
                                                itemTag,
                                                "Count",
                                                1);
                        }

                        if (count <= 0) {
                                continue;
                        }

                        Material material = Material.matchMaterial(
                                        itemId);

                        if (material == null) {
                                material = Material.matchMaterial(
                                                itemId,
                                                true);
                        }

                        if (material == null
                                        || !material.isItem()) {
                                plugin.getLogger().warning(
                                                "Unable to restore schematic item '"
                                                                + itemId
                                                                + "'.");

                                continue;
                        }

                        ItemStack itemStack = new ItemStack(
                                        material,
                                        Math.min(
                                                        count,
                                                        material.getMaxStackSize()));

                        inventory.setItem(
                                        slot,
                                        itemStack);

                        restoredItems++;
                }

                if (plugin.getConfig().getBoolean(
                                "debug.enabled",
                                false)) {
                        plugin.getLogger().info(
                                        "Restored "
                                                        + restoredItems
                                                        + " schematic item stack(s) into "
                                                        + container.getType()
                                                        + ".");
                }
        }

        private List<SchematicBlockEntity> readBlockEntities(
                        CompoundTag parent,
                        String name) throws IOException {

                Tag<?> tag = parent.get(name);

                if (tag == null) {
                        return List.of();
                }

                if (!(tag instanceof ListTag<?> listTag)) {
                        throw new IOException(
                                        "Invalid block entity list: "
                                                        + name);
                }

                List<SchematicBlockEntity> result = new ArrayList<>();

                for (Tag<?> rawEntry : listTag) {
                        if (!(rawEntry instanceof CompoundTag entry)) {
                                continue;
                        }

                        int[] position = readPosition(
                                        entry);

                        if (position == null) {
                                plugin.getLogger().warning(
                                                "Skipping schematic block entity without Pos.");

                                continue;
                        }

                        CompoundTag data = entry;

                        Tag<?> nestedData = entry.get("Data");

                        if (nestedData instanceof CompoundTag compoundTag) {
                                data = compoundTag;
                        }

                        result.add(
                                        new SchematicBlockEntity(
                                                        position[0],
                                                        position[1],
                                                        position[2],
                                                        data));
                }

                return result;
        }

        private int[] readPosition(
                        CompoundTag blockEntity) {
                Tag<?> positionTag = blockEntity.get("Pos");

                if (positionTag instanceof IntArrayTag intArrayTag) {
                        int[] position = intArrayTag.getValue();

                        if (position.length >= 3) {
                                return new int[] {
                                                position[0],
                                                position[1],
                                                position[2]
                                };
                        }
                }

                return null;
        }

        private Map<Integer, String> readPalette(
                        CompoundTag paletteTag) throws IOException {

                Map<Integer, String> palette = new HashMap<>();

                for (String key : paletteTag.keySet()) {
                        Tag<?> value = paletteTag.get(key);

                        if (!(value instanceof IntTag intTag)) {
                                throw new IOException(
                                                "Invalid palette value for "
                                                                + key);
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

        private int getNumberValue(
                        CompoundTag parent,
                        String name,
                        int defaultValue) {
                Tag<?> tag = parent.get(name);

                if (tag instanceof ByteTag byteTag) {
                        return byteTag.asByte();
                }

                if (tag instanceof ShortTag shortTag) {
                        return shortTag.asShort();
                }

                if (tag instanceof IntTag intTag) {
                        return intTag.asInt();
                }

                return defaultValue;
        }

        private String getStringValue(
                        CompoundTag parent,
                        String name) {
                Tag<?> tag = parent.get(name);

                if (tag instanceof StringTag stringTag) {
                        return stringTag.getValue();
                }

                return null;
        }
}