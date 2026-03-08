package org.betterx.wover.generator.mixin.generator;

import org.betterx.wover.generator.impl.chunkgenerator.ConfiguredChunkGenerator;

import net.minecraft.world.level.chunk.ChunkGenerator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorMixin implements ConfiguredChunkGenerator {
    @Unique
    private String wover_configuredWorldPreset;

    @Override
    public String wover_getConfiguredWorldPresetString() {
        return wover_configuredWorldPreset;
    }

    @Override
    public void wover_setConfiguredWorldPresetString(String preset) {
        wover_configuredWorldPreset = preset;
    }
}
