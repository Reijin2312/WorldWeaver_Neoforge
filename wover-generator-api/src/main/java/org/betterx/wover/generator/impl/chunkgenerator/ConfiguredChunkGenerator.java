package org.betterx.wover.generator.impl.chunkgenerator;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

public interface ConfiguredChunkGenerator {
    String wover_getConfiguredWorldPresetString();

    void wover_setConfiguredWorldPresetString(String preset);

    default ResourceKey<WorldPreset> wover_getConfiguredWorldPreset() {
        String preset = wover_getConfiguredWorldPresetString();
        return preset == null ? null : ResourceKey.create(Registries.WORLD_PRESET, Identifier.parse(preset));
    }

    default void wover_setConfiguredWorldPreset(ResourceKey<WorldPreset> preset) {
        wover_setConfiguredWorldPresetString(preset == null ? null : preset.identifier().toString());
    }
}
