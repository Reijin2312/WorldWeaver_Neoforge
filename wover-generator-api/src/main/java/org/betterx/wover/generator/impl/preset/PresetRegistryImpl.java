package org.betterx.wover.generator.impl.preset;

import org.betterx.wover.entrypoint.LibWoverWorldGenerator;
import org.betterx.wover.generator.api.biomesource.end.WoverEndConfig;
import org.betterx.wover.generator.api.biomesource.nether.WoverNetherConfig;
import org.betterx.wover.generator.api.preset.WorldPresets;
import org.betterx.wover.generator.impl.biomesource.end.WoverEndBiomeSource;
import org.betterx.wover.generator.impl.biomesource.nether.WoverNetherBiomeSource;
import org.betterx.wover.generator.impl.chunkgenerator.WoverChunkGenerator;
import org.betterx.wover.legacy.api.LegacyHelper;
import org.betterx.wover.preset.api.WorldPresetManager;
import org.betterx.wover.preset.api.WorldPresetTags;
import org.betterx.wover.preset.api.context.WorldPresetBootstrapContext;
import org.betterx.wover.tag.api.event.context.TagBootstrapContext;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

import java.util.Map;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public class PresetRegistryImpl {
    public final static ResourceKey<WorldPreset> WOVER_WORLD = WorldPresetManager.createKey(LibWoverWorldGenerator.C.id(
            "normal"));
    public final static ResourceKey<WorldPreset> WOVER_WORLD_LARGE = WorldPresetManager.createKey(LibWoverWorldGenerator.C.id(
            "large"));
    public final static ResourceKey<WorldPreset> WOVER_WORLD_AMPLIFIED = WorldPresetManager.createKey(
            LibWoverWorldGenerator.C.id(
                    "amplified"));
    public final static ResourceKey<WorldPreset> WOVER_WORLD_SUPERFLAT = WorldPresetManager.createKey(
            LibWoverWorldGenerator.C.id(
                    "superflat"));
    public final static ResourceKey<WorldPreset> BCL_WORLD_17
            = WorldPresetManager.createKey(LegacyHelper.BCLIB_CORE.id("legacy_17"));

    public static void bootstrapWorldPresets(WorldPresetBootstrapContext ctx) {
        ctx.register(WorldPresets.WOVER_WORLD, createNormal(ctx));
        ctx.register(WorldPresets.WOVER_WORLD_LARGE, createLarge(ctx));
        ctx.register(WorldPresets.WOVER_WORLD_AMPLIFIED, createAmplified(ctx));
        ctx.register(WorldPresets.WOVER_WORLD_SUPERFLAT, createSuperflat(ctx));

        if (LegacyHelper.isLegacyEnabled()) {
            final ResourceKey<WorldPreset> BCL_WORLD
                    = WorldPresetManager.createKey(LegacyHelper.BCLIB_CORE.convertNamespace(WorldPresets.WOVER_WORLD));
            final ResourceKey<WorldPreset> BCL_WORLD_LARGE
                    = WorldPresetManager.createKey(LegacyHelper.BCLIB_CORE.convertNamespace(WorldPresets.WOVER_WORLD_LARGE));
            final ResourceKey<WorldPreset> BCL_WORLD_AMPLIFIED
                    = WorldPresetManager.createKey(LegacyHelper.BCLIB_CORE.convertNamespace(WorldPresets.WOVER_WORLD_AMPLIFIED));

            ctx.register(PresetRegistryImpl.BCL_WORLD_17, createLegacy(ctx));
            ctx.register(BCL_WORLD, createNormal(ctx));
            ctx.register(BCL_WORLD_LARGE, createLarge(ctx));
            ctx.register(BCL_WORLD_AMPLIFIED, createAmplified(ctx));
        }
    }

    public static void bootstrapWorldPresetTags(TagBootstrapContext<WorldPreset> context) {
        // Optional tag entries avoid hard registry failures if a preset was not
        // materialized by another bootstrap path in the current runtime.
        context.addOptional(
                WorldPresetTags.NORMAL,
                WorldPresets.WOVER_WORLD,
                WorldPresets.WOVER_WORLD_AMPLIFIED,
                WorldPresets.WOVER_WORLD_LARGE,
                WorldPresets.WOVER_WORLD_SUPERFLAT
        );
    }

    @NotNull
    public static LevelStem makeWoverNetherStem(
            WorldPresetBootstrapContext.StemContext context,
            WoverNetherConfig config
    ) {
        WoverNetherBiomeSource netherSource = new WoverNetherBiomeSource(config);

        return new LevelStem(
                context.dimension,
                new WoverChunkGenerator(netherSource, context.generatorSettings)
        );
    }

    public static LevelStem makeWoverEndStem(WorldPresetBootstrapContext.StemContext context, WoverEndConfig config) {
        WoverEndBiomeSource endSource = new WoverEndBiomeSource(config);
        return new LevelStem(
                context.dimension,
                new WoverChunkGenerator(endSource, context.generatorSettings)
        );
    }

    private static WorldPreset createLegacy(WorldPresetBootstrapContext ctx) {
        return buildPreset(
                ctx.overworldStem,
                ctx.netherContext, WoverNetherConfig.MINECRAFT_17,
                ctx.endContext, WoverEndConfig.MINECRAFT_17
        );
    }

    private static WorldPreset createAmplified(WorldPresetBootstrapContext ctx) {
        Holder<NoiseGeneratorSettings> amplifiedBiomeGenerator = ctx.noiseSettings
                .getOrThrow(NoiseGeneratorSettings.AMPLIFIED);

        WorldPresetBootstrapContext.StemContext amplifiedNetherContext = WorldPresetBootstrapContext.StemContext.of(
                ctx.netherContext.dimension,
                ctx.netherContext.structureSets,
                ctx.noiseSettings.getOrThrow(WoverChunkGenerator.AMPLIFIED_NETHER)
        );

        return buildPreset(
                ctx.makeNoiseBasedOverworld(
                        ctx.overworldStem.generator().getBiomeSource(),
                        amplifiedBiomeGenerator
                ),
                amplifiedNetherContext, WoverNetherConfig.MINECRAFT_18_AMPLIFIED,
                ctx.endContext, WoverEndConfig.MINECRAFT_20_AMPLIFIED
        );
    }

    private static WorldPreset createLarge(WorldPresetBootstrapContext ctx) {
        Holder<NoiseGeneratorSettings> largeBiomeGenerator = ctx.noiseSettings
                .getOrThrow(NoiseGeneratorSettings.LARGE_BIOMES);
        return buildPreset(
                ctx.makeNoiseBasedOverworld(
                        ctx.overworldStem.generator().getBiomeSource(),
                        largeBiomeGenerator
                ),
                ctx.netherContext, WoverNetherConfig.MINECRAFT_18_LARGE,
                ctx.endContext, WoverEndConfig.MINECRAFT_20_LARGE
        );
    }

    private static WorldPreset createNormal(WorldPresetBootstrapContext ctx) {
        return buildPreset(
                ctx.overworldStem,
                ctx.netherContext, WoverNetherConfig.DEFAULT,
                ctx.endContext, WoverEndConfig.DEFAULT
        );
    }

    private static WorldPreset createSuperflat(WorldPresetBootstrapContext ctx) {
        return buildPreset(
                resolveFlatOverworld(ctx),
                ctx.netherContext, WoverNetherConfig.DEFAULT,
                ctx.endContext, WoverEndConfig.DEFAULT
        );
    }

    private static LevelStem resolveFlatOverworld(WorldPresetBootstrapContext ctx) {
        final HolderGetter<WorldPreset> presets = ctx.lookup(Registries.WORLD_PRESET);
        if (presets != null) {
            final Holder<WorldPreset> flatPreset = presets.getOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT);
            final LevelStem stem = WorldPresetManager.getDimension(flatPreset, LevelStem.OVERWORLD);
            if (stem != null) {
                return stem;
            }
        }
        return ctx.overworldStem;
    }

    private static WorldPreset buildPreset(
            LevelStem overworldStem,
            WorldPresetBootstrapContext.StemContext netherContext,
            WoverNetherConfig netherConfig,
            WorldPresetBootstrapContext.StemContext endContext,
            WoverEndConfig endConfig
    ) {
        return WorldPresetManager.of(buildDimensionMap(
                overworldStem, netherContext, netherConfig, endContext, endConfig
        ));
    }

    private static Map<ResourceKey<LevelStem>, LevelStem> buildDimensionMap(
            LevelStem overworldStem,
            WorldPresetBootstrapContext.StemContext netherContext,
            WoverNetherConfig netherConfig,
            WorldPresetBootstrapContext.StemContext endContext,
            WoverEndConfig endConfig
    ) {
        return Map.of(
                LevelStem.OVERWORLD,
                overworldStem,
                LevelStem.NETHER,
                WorldPresets.makeWoverNetherStem(netherContext, netherConfig),
                LevelStem.END,
                WorldPresets.makeWoverEndStem(endContext, endConfig)
        );
    }

    @ApiStatus.Internal
    public static void ensureStaticallyLoaded() {
        // no-op
    }
}
