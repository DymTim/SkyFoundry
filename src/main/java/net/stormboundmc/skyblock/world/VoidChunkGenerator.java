package net.stormboundmc.skyblock.world;

import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public final class VoidChunkGenerator extends ChunkGenerator {

    @Override
    public boolean shouldGenerateNoise(
            @NotNull WorldInfo worldInfo,
            @NotNull Random random,
            int chunkX,
            int chunkZ) {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface(
            @NotNull WorldInfo worldInfo,
            @NotNull Random random,
            int chunkX,
            int chunkZ) {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves(
            @NotNull WorldInfo worldInfo,
            @NotNull Random random,
            int chunkX,
            int chunkZ) {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations(
            @NotNull WorldInfo worldInfo,
            @NotNull Random random,
            int chunkX,
            int chunkZ) {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs(
            @NotNull WorldInfo worldInfo,
            @NotNull Random random,
            int chunkX,
            int chunkZ) {
        return false;
    }

    @Override
    public boolean shouldGenerateStructures(
            @NotNull WorldInfo worldInfo,
            @NotNull Random random,
            int chunkX,
            int chunkZ) {
        return false;
    }
}