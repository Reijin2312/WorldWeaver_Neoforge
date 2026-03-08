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
import org.betterx.wover.generator.datagen.WoverWorldGeneratorDatagen;
import org.betterx.wover.preset.api.WorldPresetManager;
import org.betterx.wover.preset.api.WorldPresetTags;
import org.betterx.wover.preset.impl.WorldPresetsManagerImpl;

import net.minecraft.data.worldgen.BootstrapContext;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.data.event.GatherDataEvent;
public class LibWoverWorldGenerator {
    public static final ModCore C = ModCore.create("wover-generator", "wover");

    public LibWoverWorldGenerator(IEventBus modEventBus) {
        C.registerDatapackListener(modEventBus);
        WorldPresetsManagerImpl.initialize();

        WoverWorldGeneratorDatagen datagen = new WoverWorldGeneratorDatagen();
        modEventBus.addListener(GatherDataEvent.Client.class, datagen::onGatherData);
        modEventBus.addListener(GatherDataEvent.Server.class, datagen::onGatherData);
        modEventBus.addListener(net.neoforged.neoforge.registries.RegisterEvent.class, BiomeSourceManagerImpl::register);
        modEventBus.addListener(net.neoforged.neoforge.registries.RegisterEvent.class, ChunkGeneratorManagerImpl::onRegister);
        WorldPresetManager.BOOTSTRAP_WORLD_PRESETS.subscribe(PresetRegistryImpl::bootstrapWorldPresets);
        WorldPresetTags.TAGS.bootstrapEvent().subscribe(PresetRegistryImpl::bootstrapWorldPresetTags);
        WorldPresetInfoRegistry.BOOTSTRAP_WORLD_PRESET_INFO_REGISTRY.subscribe(LibWoverWorldGenerator::bootstrapPresetInfo);
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
