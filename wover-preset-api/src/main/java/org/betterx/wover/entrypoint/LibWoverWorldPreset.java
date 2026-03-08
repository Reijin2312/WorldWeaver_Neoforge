package org.betterx.wover.entrypoint;

import org.betterx.wover.core.api.ModCore;
import org.betterx.wover.preset.api.WorldPresetInfo;
import org.betterx.wover.preset.api.WorldPresetInfoBuilder;
import org.betterx.wover.preset.api.WorldPresetInfoRegistry;
import org.betterx.wover.preset.impl.WorldPresetInfoRegistryImpl;
import org.betterx.wover.preset.impl.WorldPresetsManagerImpl;
import org.betterx.wover.preset.impl.flat.FlatLevelPresetManagerImpl;
import org.betterx.wover.preset.datagen.WoverWorldPresetDatagen;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.data.event.GatherDataEvent;
public class LibWoverWorldPreset {
    public static final ModCore C = ModCore.create("wover-preset", "wover");

    public LibWoverWorldPreset(IEventBus modEventBus) {
        C.registerDatapackListener(modEventBus);
        WoverWorldPresetDatagen datagen = new WoverWorldPresetDatagen();
        modEventBus.addListener(GatherDataEvent.Client.class, datagen::onGatherData);
        modEventBus.addListener(GatherDataEvent.Server.class, datagen::onGatherData);
        WorldPresetInfoRegistry.BOOTSTRAP_WORLD_PRESET_INFO_REGISTRY.subscribe(LibWoverWorldPreset::bootstrapVanillaPresetInfo);
        WorldPresetInfoRegistryImpl.initialize();
        WorldPresetsManagerImpl.initialize();
        FlatLevelPresetManagerImpl.initialize();
    }

    private static void bootstrapVanillaPresetInfo(BootstrapContext<WorldPresetInfo> context) {
        WorldPresetInfoBuilder.start(context)
                              .order(1000)
                              .register(WorldPresets.NORMAL);

        WorldPresetInfoBuilder.start(context)
                              .order(2000)
                              .endOverride(WorldPresets.NORMAL)
                              .netherOverride(WorldPresets.NORMAL)
                              .register(WorldPresets.AMPLIFIED);

        WorldPresetInfoBuilder.start(context)
                              .order(3000)
                              .register(WorldPresets.LARGE_BIOMES);

        WorldPresetInfoBuilder.start(context)
                              .order(11000)
                              .overworldOverride(WorldPresets.NORMAL)
                              .endOverride(WorldPresets.NORMAL)
                              .netherOverride(WorldPresets.NORMAL)
                              .register(WorldPresets.FLAT);

        WorldPresetInfoBuilder.start(context)
                              .order(12000)
                              .overworldOverride(WorldPresets.NORMAL)
                              .endOverride(WorldPresets.NORMAL)
                              .netherOverride(WorldPresets.NORMAL)
                              .register(WorldPresets.SINGLE_BIOME_SURFACE);
    }
}
