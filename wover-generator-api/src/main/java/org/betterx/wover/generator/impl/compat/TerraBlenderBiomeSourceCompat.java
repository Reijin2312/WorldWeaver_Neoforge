package org.betterx.wover.generator.impl.compat;

import org.betterx.wover.entrypoint.LibWoverWorldGenerator;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class TerraBlenderBiomeSourceCompat {
    private static final String LEVEL_UTILS = "terrablender.util.LevelUtils";

    private TerraBlenderBiomeSourceCompat() {
    }

    public static boolean initialize(
            BiomeSource source,
            RegistryAccess registryAccess,
            Holder<DimensionType> dimensionType,
            ResourceKey<LevelStem> dimensionKey,
            ChunkGenerator settingsOwner,
            long seed
    ) {
        if (!(source instanceof MultiNoiseBiomeSource) || !(settingsOwner instanceof NoiseBasedChunkGenerator noise)) {
            return false;
        }

        try {
            Class<?> levelUtils = Class.forName(LEVEL_UTILS);
            ChunkGenerator externalGenerator = new NoiseBasedChunkGenerator(source, noise.generatorSettings());
            Method shouldApply = levelUtils.getMethod("shouldApplyToChunkGenerator", ChunkGenerator.class);
            if (!Boolean.TRUE.equals(shouldApply.invoke(null, externalGenerator))) {
                return false;
            }

            Method initialize = levelUtils.getMethod(
                    "initializeBiomes",
                    RegistryAccess.class,
                    Holder.class,
                    ResourceKey.class,
                    ChunkGenerator.class,
                    long.class
            );
            initialize.invoke(null, registryAccess, dimensionType, dimensionKey, externalGenerator, seed);
            LibWoverWorldGenerator.C.log.info(
                    "Initialized TerraBlender fallback for {} with {} possible biomes",
                    dimensionKey.location(),
                    source.possibleBiomes().size()
            );
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        } catch (InvocationTargetException e) {
            LibWoverWorldGenerator.C.log.warn(
                    "TerraBlender fallback failed to initialize for {}",
                    dimensionKey.location(),
                    e.getCause()
            );
        } catch (ReflectiveOperationException | RuntimeException e) {
            LibWoverWorldGenerator.C.log.warn(
                    "Unable to initialize TerraBlender fallback for {}",
                    dimensionKey.location(),
                    e
            );
        }
        return false;
    }
}
