package org.betterx.wover.entrypoint;

import org.betterx.wover.config.api.Configs;
import org.betterx.wover.core.api.ModCore;
import org.betterx.wover.generator.api.preset.WorldPresets;
import org.betterx.wover.generator.impl.biomesource.BiomeSourceManagerImpl;
import org.betterx.wover.generator.impl.biomesource.WoverBiomeDataImpl;
import org.betterx.wover.generator.impl.chunkgenerator.ChunkGeneratorManagerImpl;
import org.betterx.wover.generator.impl.chunkgenerator.WoverChunkGeneratorImpl;
import org.betterx.wover.generator.impl.preset.PresetRegistryImpl;
import org.betterx.wover.preset.api.WorldPresetInfo;
import org.betterx.wover.preset.api.WorldPresetInfoBuilder;
import org.betterx.wover.preset.api.WorldPresetInfoRegistry;
import org.betterx.wover.preset.api.WorldPresetManager;
import org.betterx.wover.preset.api.WorldPresetTags;

import net.minecraft.data.worldgen.BootstrapContext;
import net.fabricmc.api.ModInitializer;

public class LibWoverWorldGenerator implements ModInitializer {
    public static final ModCore C = ModCore.create("wover-generator", "wover");

    @Override
    public void onInitialize() {
        WorldPresetManager.BOOTSTRAP_WORLD_PRESETS.subscribe(PresetRegistryImpl::bootstrapWorldPresets);
        WorldPresetTags.TAGS.bootstrapEvent().subscribe(PresetRegistryImpl::bootstrapWorldPresetTags);
        WorldPresetInfoRegistry.BOOTSTRAP_WORLD_PRESET_INFO_REGISTRY.subscribe(
                LibWoverWorldGenerator::bootstrapPresetInfo
        );
        if (!ModCore.isClient() && Configs.MAIN.forceDefaultWorldPresetOnServer.get()) {
            WorldPresetManager.suggestDefault(WorldPresets.WOVER_WORLD, 2000);
        }
        
        PresetRegistryImpl.ensureStaticallyLoaded();
        WoverBiomeDataImpl.initialize();
        BiomeSourceManagerImpl.initialize();
        ChunkGeneratorManagerImpl.initialize();
        WoverChunkGeneratorImpl.initialize();
    }

    private static void bootstrapPresetInfo(BootstrapContext<WorldPresetInfo> context) {
        WorldPresetInfoBuilder.start(context)
                              .order(1500)
                              .overworldOverride(net.minecraft.world.level.levelgen.presets.WorldPresets.NORMAL)
                              .register(WorldPresets.WOVER_WORLD);

        WorldPresetInfoBuilder.start(context)
                              .order(1600)
                              .overworldOverride(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT)
                              .netherOverride(WorldPresets.WOVER_WORLD)
                              .endOverride(WorldPresets.WOVER_WORLD)
                              .register(WorldPresets.WOVER_WORLD_SUPERFLAT);

        WorldPresetInfoBuilder.start(context)
                              .order(2500)
                              .overworldOverride(net.minecraft.world.level.levelgen.presets.WorldPresets.AMPLIFIED)
                              .endOverride(WorldPresets.WOVER_WORLD)
                              .register(WorldPresets.WOVER_WORLD_AMPLIFIED);

        WorldPresetInfoBuilder.start(context)
                              .order(3500)
                              .overworldOverride(net.minecraft.world.level.levelgen.presets.WorldPresets.LARGE_BIOMES)
                              .register(WorldPresets.WOVER_WORLD_LARGE);
    }
}
