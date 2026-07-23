package org.betterx.wover.generator.impl.compat;

import org.betterx.wover.entrypoint.LibWoverWorldGenerator;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.dimension.LevelStem;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class ElysiumBiomeSourceCompat {
    private static final String MOSAIC_BIOME_SOURCE = "net.jadenxgamer.elysium_api.impl.core.biome.MosaicBiomeSource";

    private ElysiumBiomeSourceCompat() {
    }

    public static boolean initialize(BiomeSource source, long seed, ResourceKey<LevelStem> dimensionKey) {
        if (source == null || !MOSAIC_BIOME_SOURCE.equals(source.getClass().getName())) {
            return false;
        }

        try {
            Method initialize = source.getClass().getMethod("initialize", long.class, ResourceKey.class);
            initialize.invoke(source, seed, dimensionKey);
            LibWoverWorldGenerator.C.log.info("Initialized Elysium mosaic fallback for {} with {} possible biomes", dimensionKey.identifier(), source.possibleBiomes().size());
            return true;
        } catch (InvocationTargetException e) {
            LibWoverWorldGenerator.C.log.warn("Elysium mosaic fallback failed to initialize for {}", dimensionKey.identifier(), e.getCause());
        } catch (ReflectiveOperationException | RuntimeException e) {
            LibWoverWorldGenerator.C.log.warn("Unable to initialize Elysium mosaic fallback for {}", dimensionKey.identifier(), e);
        }
        return false;
    }
}
