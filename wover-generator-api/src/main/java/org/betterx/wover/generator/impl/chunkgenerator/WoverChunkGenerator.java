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
import org.betterx.wover.generator.impl.biomesource.nether.WoverNetherBiomeSource;
import org.betterx.wover.generator.impl.compat.BlueprintBiomeSourceCompat;
import org.betterx.wover.surface.impl.SurfaceRuleUtil;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.SurfaceRuleData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.*;

import java.lang.reflect.Method;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class WoverChunkGenerator extends NoiseBasedChunkGenerator implements
        RestorableBiomeSource<WoverChunkGenerator>,
        InjectableSurfaceRules<WoverChunkGenerator>,
        EnforceableChunkGenerator<WoverChunkGenerator>,
        RebuildableFeaturesPerStep<WoverChunkGenerator> {
    public static final ResourceLocation ID = LibWoverWorldGenerator.C.id("betterx");

    protected static final NoiseSettings NETHER_NOISE_SETTINGS_AMPLIFIED = NoiseSettings.create(0, 256, 1, 4);
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
            applyTerraBlenderRuleCategory(holder, null, biomeSource);
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
            if (LevelStem.END.equals(dimensionKey) && wover_removeBlueprintEndWrapper()) {
                return;
            }
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
                .location()
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
                dimensionRegistry::get,
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
                NoiseRouterData.noNewCaves(
                        densityGetter,
                        bootstapContext.lookup(Registries.NOISE),
                        NoiseRouterData.slideNetherLike(densityGetter, 0, 256)
                ),
                SurfaceRuleData.nether(),
                List.of(),
                32,
                false,
                false,
                false,
                true
        );
    }

    @Override
    public void wover_injectSurfaceRules(Registry<LevelStem> dimensionRegistry, ResourceKey<LevelStem> dimensionKey) {
        if (LevelStem.END.equals(dimensionKey)) {
            wover_removeBlueprintEndWrapper();
        }
        if (IntegrationCore.RUNS_TERRABLENDER) {
            applyTerraBlenderRuleCategory(generatorSettings(), dimensionKey, this.getBiomeSource());
        }
        SurfaceRuleUtil.injectNoiseBasedSurfaceRules(
                dimensionKey,
                generatorSettings(),
                this.getBiomeSource()
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void applyTerraBlenderRuleCategory(
            Holder<NoiseGeneratorSettings> holder,
            ResourceKey<LevelStem> dimensionKey,
            BiomeSource biomeSource
    ) {
        if (!holder.isBound()) return;

        String categoryName = null;
        if (dimensionKey != null) {
            if (dimensionKey.equals(LevelStem.NETHER)) categoryName = "NETHER";
            else if (dimensionKey.equals(LevelStem.END)) categoryName = "END";
            else if (dimensionKey.equals(LevelStem.OVERWORLD)) categoryName = "OVERWORLD";
        }
        if (categoryName == null) {
            if (biomeSource instanceof WoverNetherBiomeSource) categoryName = "NETHER";
            else if (biomeSource instanceof WoverEndBiomeSource) categoryName = "END";
        }
        if (categoryName == null) return;

        try {
            final Class<?> ruleCategoryClass = Class.forName("terrablender.api.SurfaceRuleManager$RuleCategory");
            final Enum ruleCategory = Enum.valueOf((Class<Enum>) ruleCategoryClass.asSubclass(Enum.class), categoryName);
            final Method setRuleCategory = holder.value().getClass().getMethod("setRuleCategory", ruleCategoryClass);
            setRuleCategory.invoke(holder.value(), ruleCategory);
        } catch (ReflectiveOperationException e) {
            LibWoverWorldGenerator.C.log.warn(
                    "Unable to set TerraBlender surface rule category {} for {}",
                    categoryName,
                    biomeSource.getClass().getName(),
                    e
            );
        }
    }

    /**
     * Blueprint applies its End overlay after the configured generator was created. Once every active overlay has
     * been imported into the Wover picker, retaining that wrapper makes the surface-rule context observe a different
     * source than the one that owns BetterEnd's biome rules.
     */
    boolean wover_removeBlueprintEndWrapper() {
        final BiomeSource currentSource = getBiomeSource();
        final BiomeSource unwrappedSource = LithostitchedBiomeSourceCompat.unwrap(currentSource);
        if (!BlueprintBiomeSourceCompat.canReplaceEndWrapper()
                || currentSource == unwrappedSource
                || !(unwrappedSource instanceof WoverEndBiomeSource)
                || !(this instanceof ChunkGeneratorAccessor acc)) {
            return false;
        }
        acc.wover_setBiomeSource(unwrappedSource);
        if (unwrappedSource instanceof ReloadableBiomeSource reloadable) {
            reloadable.reloadBiomes();
        }
        ChunkGeneratorHelper.rebuildFeaturesPerStep(this, unwrappedSource);
        LibWoverWorldGenerator.C.log.info(
                "Replaced Blueprint End wrapper after importing all active biome overlays"
        );
        return true;
    }
}

