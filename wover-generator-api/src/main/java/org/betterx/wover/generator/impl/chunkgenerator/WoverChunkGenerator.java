package org.betterx.wover.generator.impl.chunkgenerator;

import org.betterx.wover.biome.impl.modification.ChunkGeneratorHelper;
import org.betterx.wover.biome.mixin.ChunkGeneratorAccessor;
import org.betterx.wover.common.generator.api.biomesource.BiomeSourceWithNoiseRelatedSettings;
import org.betterx.wover.common.generator.api.biomesource.MergeableBiomeSource;
import org.betterx.wover.common.generator.api.biomesource.NoiseGeneratorSettingsProvider;
import org.betterx.wover.common.generator.api.biomesource.ReloadableBiomeSource;
import org.betterx.wover.common.generator.api.chunkgenerator.EnforceableChunkGenerator;
import org.betterx.wover.common.generator.api.chunkgenerator.RebuildableFeaturesPerStep;
import org.betterx.wover.common.generator.api.chunkgenerator.RestorableBiomeSource;
import org.betterx.wover.common.generator.impl.compat.LithostitchedBiomeSourceCompat;
import org.betterx.wover.common.surface.api.InjectableSurfaceRules;
import org.betterx.wover.core.api.IntegrationCore;
import org.betterx.wover.entrypoint.LibWoverWorldGenerator;
import org.betterx.wover.generator.impl.biomesource.end.WoverEndBiomeSource;
import org.betterx.wover.generator.impl.compat.BlueprintBiomeSourceCompat;
import org.betterx.wover.surface.impl.SurfaceRuleUtil;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.SurfaceRuleData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.List;
import org.jetbrains.annotations.NotNull;

public class WoverChunkGenerator extends NoiseBasedChunkGenerator implements
        RestorableBiomeSource<WoverChunkGenerator>,
        InjectableSurfaceRules<WoverChunkGenerator>,
        EnforceableChunkGenerator<WoverChunkGenerator>,
        RebuildableFeaturesPerStep<WoverChunkGenerator> {
    public static final Identifier ID = LibWoverWorldGenerator.C.id("betterx");

    protected static final NoiseSettings NETHER_NOISE_SETTINGS_AMPLIFIED = NoiseSettings.create(0, 256, 1, 4);
    private static final ResourceKey<DensityFunction> BASE_3D_NOISE_NETHER = ResourceKey.create(
            Registries.DENSITY_FUNCTION,
            Identifier.withDefaultNamespace("nether/base_3d_noise")
    );
    public static final ResourceKey<NoiseGeneratorSettings> AMPLIFIED_NETHER = ResourceKey.create(
            Registries.NOISE_SETTINGS,
            LibWoverWorldGenerator.C.id("amplified_nether")
    );

    public static final MapCodec<WoverChunkGenerator> CODEC = RecordCodecBuilder
            .mapCodec((RecordCodecBuilder.Instance<WoverChunkGenerator> builderInstance) -> {

                RecordCodecBuilder<WoverChunkGenerator, BiomeSource> biomeSourceCodec = BiomeSource.CODEC
                        .fieldOf("biome_source")
                        .forGetter((WoverChunkGenerator generator) -> generator.biomeSource);

                RecordCodecBuilder<WoverChunkGenerator, Holder<NoiseGeneratorSettings>> settingsCodec = NoiseGeneratorSettings.CODEC
                        .fieldOf("settings")
                        .forGetter((WoverChunkGenerator generator) -> generator.generatorSettings());


                return builderInstance.group(biomeSourceCodec, settingsCodec)
                                      .apply(builderInstance, builderInstance.stable(WoverChunkGenerator::new));
            });

    public final BiomeSource initialBiomeSource;

    public WoverChunkGenerator(
            BiomeSource biomeSource,
            Holder<NoiseGeneratorSettings> holder
    ) {
        super(biomeSource, holder);
        initialBiomeSource = biomeSource;
        if (biomeSource instanceof BiomeSourceWithNoiseRelatedSettings bcl && holder.isBound()) {
            bcl.onLoadGeneratorSettings(holder.value());
        }

        if (IntegrationCore.RUNS_TERRABLENDER) {
            LibWoverWorldGenerator.C.log.info("Make sure features are loaded from terrablender:"
                    + biomeSource.getClass().getName());
            //terrablender is invalidating the feature initialization
            //we redo it at this point, otherwise we will get blank biomes
            ChunkGeneratorHelper.rebuildFeaturesPerStep(this, biomeSource);
        }

        LibWoverWorldGenerator.C.log.info("Created WoverChunkGenerator with " + biomeSource.getClass().getName());
    }

    @Override
    protected @NotNull MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    public void wover_rebuildFeaturesPerStep() {
        ChunkGeneratorHelper.rebuildFeaturesPerStep(this, getBiomeSource());
    }

    /**
     * Other Mods like TerraBlender might inject new BiomeSources. We undo that change after the world setup did run.
     *
     * @param dimensionKey The Dimension where this ChunkGenerator is used from
     */
    @Override
    public void restoreInitialBiomeSource(ResourceKey<LevelStem> dimensionKey) {
        if (BlueprintBiomeSourceCompat.wraps(getBiomeSource(), initialBiomeSource)) {
            if (LevelStem.END.equals(dimensionKey) && wover_removeBlueprintEndWrapper()) return;

            ChunkGeneratorHelper.rebuildFeaturesPerStep(this, getBiomeSource());
            return;
        }

        if (initialBiomeSource != getBiomeSource()) {
            if (this instanceof ChunkGeneratorAccessor acc) {
                if (initialBiomeSource instanceof MergeableBiomeSource<?> bs) {
                    acc.wover_setBiomeSource(bs.mergeWithBiomeSource(getBiomeSource()));
                } else if (initialBiomeSource instanceof ReloadableBiomeSource bs) {
                    bs.reloadBiomes();
                }

                ChunkGeneratorHelper.rebuildFeaturesPerStep(this, getBiomeSource());
            }
        }
    }

    @Override
    public String toString() {
        return ChunkGeneratorManagerImpl.printGeneratorInfo("WoVer - Chunk Generator", this);
    }

    // This method is injected by Terrablender.
    // We make sure terrablender does not rewrite the feature-set for our ChunkGenerator by overwriting the
    // Mixin-Method with an empty implementation
    public void appendFeaturesPerStep() {
    }

    @Override
    public Registry<LevelStem> enforceGeneratorInWorldGenSettings(
            RegistryAccess access,
            ResourceKey<LevelStem> dimensionKey,
            ResourceKey<DimensionType> dimensionTypeKey,
            ChunkGenerator loadedChunkGenerator,
            Registry<LevelStem> dimensionRegistry
    ) {
        LibWoverWorldGenerator.C.log.info("Enforcing Correct Generator for " + dimensionKey
                .identifier()
                .toString() + ".");

        ChunkGenerator referenceGenerator = this;
        if (loadedChunkGenerator instanceof ChunkGeneratorAccessor generator) {
            if (loadedChunkGenerator instanceof NoiseGeneratorSettingsProvider noiseProvider) {
                if (referenceGenerator instanceof NoiseGeneratorSettingsProvider referenceProvider) {
                    final BiomeSource bs;
                    if (referenceGenerator.getBiomeSource() instanceof MergeableBiomeSource<?> mbs) {
                        bs = mbs.mergeWithBiomeSource(loadedChunkGenerator.getBiomeSource());
                    } else {
                        bs = referenceGenerator.getBiomeSource();
                    }

                    referenceProvider.wover_getNoiseGeneratorSettingHolders();
                    referenceGenerator = new WoverChunkGenerator(
                            bs,
                            noiseProvider.wover_getNoiseGeneratorSettingHolders()
                    );
                }
            }
        }

        return WoverChunkGeneratorImpl.replaceGenerator(
                dimensionKey,
                dimensionTypeKey,
                access,
                dimensionRegistry.entrySet(),
                referenceGenerator,
                key -> dimensionRegistry.get(key).map(Holder.Reference::value).orElse(null),
                (registry, key, stem) -> registry.register(
                        key, stem,
                        dimensionRegistry.registrationInfo(key).orElse(RegistrationInfo.BUILT_IN)
                )
        );
    }


    public static NoiseGeneratorSettings amplifiedNether(BootstrapContext<NoiseGeneratorSettings> bootstapContext) {
        HolderGetter<DensityFunction> densityGetter = bootstapContext.lookup(Registries.DENSITY_FUNCTION);
        return new NoiseGeneratorSettings(
                NETHER_NOISE_SETTINGS_AMPLIFIED,
                Blocks.NETHERRACK.defaultBlockState(),
                Blocks.LAVA.defaultBlockState(),
                amplifiedNetherRouter(densityGetter, bootstapContext.lookup(Registries.NOISE)),
                SurfaceRuleData.nether(bootstapContext.lookup(Registries.BIOME)),
                List.of(),
                32,
                false,
                false,
                false,
                true
        );
    }

    private static NoiseRouter amplifiedNetherRouter(
            HolderGetter<DensityFunction> functions,
            HolderGetter<NormalNoise.NoiseParameters> noises
    ) {
        DensityFunction temperature = DensityFunctions.shiftedNoise2d(
                DensityFunctions.zero(), DensityFunctions.zero(), 0.25, noises.getOrThrow(Noises.TEMPERATURE_NETHER)
        );
        DensityFunction vegetation = DensityFunctions.shiftedNoise2d(
                DensityFunctions.zero(), DensityFunctions.zero(), 0.25, noises.getOrThrow(Noises.VEGETATION_NETHER)
        );
        DensityFunction slide = slideNetherLike(functions, 0, 256);
        DensityFunction fullNoise = postProcess(slide);
        return new NoiseRouter(
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                temperature,
                vegetation,
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                fullNoise,
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero()
        );
    }

    private static DensityFunction slideNetherLike(
            HolderGetter<DensityFunction> functions,
            int minY,
            int height
    ) {
        return slide(getFunction(functions, BASE_3D_NOISE_NETHER), minY, height, 24, 0, 0.9375, -8, 24, 2.5);
    }

    private static DensityFunction getFunction(
            HolderGetter<DensityFunction> functions,
            ResourceKey<DensityFunction> name
    ) {
        return new DensityFunctions.HolderHolder(functions.getOrThrow(name));
    }

    private static DensityFunction postProcess(DensityFunction slide) {
        DensityFunction blended = DensityFunctions.blendDensity(slide);
        return DensityFunctions.mul(DensityFunctions.interpolated(blended), DensityFunctions.constant(0.64)).squeeze();
    }

    private static DensityFunction slide(
            DensityFunction caves,
            int minY,
            int height,
            int topStartY,
            int topEndY,
            double topTarget,
            int bottomStartY,
            int bottomEndY,
            double bottomTarget
    ) {
        DensityFunction topFactor = DensityFunctions.yClampedGradient(minY + height - topStartY, minY + height - topEndY, 1.0, 0.0);
        DensityFunction noiseValue = DensityFunctions.lerp(topFactor, topTarget, caves);
        DensityFunction bottomFactor = DensityFunctions.yClampedGradient(minY + bottomStartY, minY + bottomEndY, 0.0, 1.0);
        return DensityFunctions.lerp(bottomFactor, bottomTarget, noiseValue);
    }

    @Override
    public void wover_injectSurfaceRules(Object dimensionRegistry, String dimensionKey) {
        if (dimensionKey == null) return;
        ResourceKey<LevelStem> key = ResourceKey.create(Registries.LEVEL_STEM, Identifier.parse(dimensionKey));
        if (LevelStem.END.equals(key)) wover_removeBlueprintEndWrapper();

        SurfaceRuleUtil.injectNoiseBasedSurfaceRules(
                key,
                generatorSettings(),
                this.getBiomeSource()
        );
    }

    boolean wover_removeBlueprintEndWrapper() {
        final BiomeSource currentSource = getBiomeSource();
        final BiomeSource unwrappedSource = LithostitchedBiomeSourceCompat.unwrap(currentSource);
        if (!BlueprintBiomeSourceCompat.canReplaceEndWrapper()
                || currentSource == unwrappedSource
                || !(unwrappedSource instanceof WoverEndBiomeSource)
                || !(this instanceof ChunkGeneratorAccessor accessor)) {
            return false;
        }

        accessor.wover_setBiomeSource(unwrappedSource);
        if (unwrappedSource instanceof ReloadableBiomeSource reloadable) reloadable.reloadBiomes();
        ChunkGeneratorHelper.rebuildFeaturesPerStep(this, unwrappedSource);
        LibWoverWorldGenerator.C.log.info("Removed Blueprint End biome-source wrapper after importing its active overlays.");
        return true;
    }
}
