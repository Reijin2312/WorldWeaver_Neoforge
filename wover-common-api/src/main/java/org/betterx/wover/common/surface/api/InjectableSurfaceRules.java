package org.betterx.wover.common.surface.api;

import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * If a ChunkGenerator implements this interface, we can use it to inject Surface
 * Rules from our SurfaceRule Registry.
 * <p>
 * This Interface is already implemented by the
 * {@link net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator}
 *
 * @param <G> Underlying ChunkGenerator type
 */
public interface InjectableSurfaceRules<G extends ChunkGenerator> {
    /**
     * Called when the Surface Rules for this BiomeSource need to be updated with
     * the ones from our SurfaceRule Registry.
     *
     * @param dimensionRegistry The Registry holding the Dimension information for this world
     * @param dimensionKey      The Dimension key string (e.g. minecraft:overworld)
     */
    void wover_injectSurfaceRules(Object dimensionRegistry, String dimensionKey);
}
