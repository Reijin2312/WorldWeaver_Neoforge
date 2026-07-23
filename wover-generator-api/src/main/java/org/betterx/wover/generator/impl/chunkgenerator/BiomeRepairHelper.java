package org.betterx.wover.generator.impl.chunkgenerator;

import org.betterx.wover.biome.impl.modification.BiomeTagModificationWorker;
import org.betterx.wover.common.generator.api.biomesource.BiomeSourceWithConfig;
import org.betterx.wover.common.generator.api.biomesource.ReloadableBiomeSource;
import org.betterx.wover.common.generator.api.biomesource.MergeableBiomeSource;
import org.betterx.wover.common.generator.api.chunkgenerator.EnforceableChunkGenerator;
import org.betterx.wover.common.generator.api.chunkgenerator.RebuildableFeaturesPerStep;
import org.betterx.wover.core.api.IntegrationCore;
import org.betterx.wover.entrypoint.LibWoverWorldGenerator;
import org.betterx.wover.generator.api.biomesource.WoverBiomeSource;
import org.betterx.wover.generator.impl.biomesource.end.TheEndBiomesHelper;
import org.betterx.wover.generator.impl.biomesource.nether.NetherBiomesHelper;
import org.betterx.wover.generator.impl.compat.BlueprintBiomeSourceCompat;
import org.betterx.wover.generator.impl.compat.CopiedEndBiomeRegistryCompat;
import org.betterx.wover.generator.impl.compat.TerraBlenderEndBiomeCompat;
import org.betterx.wover.generator.impl.compat.VanillaNetherBiomeCompat;
import org.betterx.wover.tag.api.predefined.CommonBiomeTags;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

import com.google.common.base.Stopwatch;

import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.lang.reflect.Method;

class BiomeRepairHelper {
    private Map<ResourceKey<LevelStem>, ChunkGenerator> vanillaDimensions = null;

    public static TagKey<Biome> getBiomeTagForDimension(ResourceKey<LevelStem> key) {
        if (key.equals(LevelStem.END)) return CommonBiomeTags.IS_END_HIGHLAND;
        else if (key.equals(LevelStem.NETHER)) return BiomeTags.IS_NETHER;
        else if (key.equals(LevelStem.OVERWORLD)) return BiomeTags.IS_OVERWORLD;
        return null;
    }

    public Registry<LevelStem> repairBiomeSourceInAllDimensions(
            RegistryAccess registryAccess,
            Registry<LevelStem> dimensionRegistry
    ) {
        Map<ResourceKey<LevelStem>, ChunkGenerator> configuredDimensions = WorldGeneratorConfigImpl.loadWorldDimensions(
                registryAccess,
                WorldGeneratorConfigImpl.getPresetsNbt()
        );
        final Registry<Biome> biomes = registryAccess.lookupOrThrow(Registries.BIOME);

        // ensure that biomes registered through the loader helpers have the proper tags
        registerAllBiomesFromRegistry(biomes);
        registerVanillaNetherBiomes(biomes);
        registerAllBiomesFromTerraBlender(biomes);
        registerCopiedEndBiomeRegistries(biomes);
        BlueprintBiomeSourceCompat.importActiveEndOverlays(registryAccess, biomes);
        var originalSet =  dimensionRegistry.entrySet();
        for (Map.Entry<ResourceKey<LevelStem>, LevelStem> entry :originalSet) {
            boolean didRepair = false;
            ResourceKey<LevelStem> key = entry.getKey();
            LevelStem loadedStem = entry.getValue();
            final ChunkGenerator loadedChunkGenerator = loadedStem.generator();
            final ChunkGenerator externalChunkGenerator = getExternalBaseGenerator(
                    registryAccess,
                    key,
                    loadedChunkGenerator
            );

            final ChunkGenerator referenceGenerator = configuredDimensions.get(key);

            if (referenceGenerator instanceof EnforceableChunkGenerator<?> enforcer) {
                // if the loaded ChunkGenerator is not the one we expect from vanilla, we will load the vanilla
                // ones and mark all modded biomes with the respective dimension
                registerAllBiomesFromVanillaDimension(registryAccess, biomes, key);

                // now compare the reference world settings (the ones that were created when the world was
                // started) with the settings that were loaded by the game.
                // If those do not match, we will create a new ChunkGenerator / BiomeSources with appropriate
                // settings
                if (enforcer.togetherShouldRepair(loadedChunkGenerator)) {
                    dimensionRegistry = enforcer.enforceGeneratorInWorldGenSettings(
                            registryAccess,
                            key,
                            loadedStem.type().unwrapKey().orElseThrow(),
                            loadedChunkGenerator,
                            dimensionRegistry
                    );
                    didRepair = true;
                } else if (loadedChunkGenerator.getBiomeSource() instanceof BiomeSourceWithConfig lodedSource) {
                    if (referenceGenerator.getBiomeSource() instanceof BiomeSourceWithConfig refSource) {
                        if (!refSource.getBiomeSourceConfig().sameConfig(lodedSource.getBiomeSourceConfig())) {
                            lodedSource.setBiomeSourceConfig(refSource.getBiomeSourceConfig());
                        }
                    }
                }
            }

            LevelStem activeStem = dimensionRegistry.getValue(key);
            if (activeStem != null) {
                if (LevelStem.END.equals(key) && activeStem.generator() instanceof WoverChunkGenerator generator) {
                    generator.wover_removeBlueprintEndWrapper();
                }
                attachExternalBiomeSource(key, activeStem.generator(), externalChunkGenerator);
            }

            if (!didRepair) {
                if (loadedStem.generator().getBiomeSource() instanceof ReloadableBiomeSource reload) {
                    reload.reloadBiomes();
                }
            }
        }

        // we ensure that all dimensions get the correct reference to the originally configured WorldPreset
        copyWorldPresetReference(dimensionRegistry, configuredDimensions);

        return dimensionRegistry;
    }

    private ChunkGenerator getExternalBaseGenerator(
            RegistryAccess registryAccess,
            ResourceKey<LevelStem> dimensionKey,
            ChunkGenerator loadedGenerator
    ) {
        if (!(loadedGenerator.getBiomeSource() instanceof WoverBiomeSource)) {
            LibWoverWorldGenerator.C.log.info(
                    "Using loaded biome source as external source for {}: {}",
                    dimensionKey.identifier(),
                    loadedGenerator.getBiomeSource().getClass().getName()
            );
            return loadedGenerator;
        }

        if (!LevelStem.NETHER.equals(dimensionKey)) {
            return null;
        }

        Holder<NoiseGeneratorSettings> settings = registryAccess
                .lookupOrThrow(Registries.NOISE_SETTINGS)
                .getOrThrow(NoiseGeneratorSettings.NETHER);
        Holder<MultiNoiseBiomeSourceParameterList> parameters = registryAccess
                .lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                .getOrThrow(MultiNoiseBiomeSourceParameterLists.NETHER);
        ChunkGenerator externalGenerator = new NoiseBasedChunkGenerator(
                MultiNoiseBiomeSource.createFromPreset(parameters),
                settings
        );
        LibWoverWorldGenerator.C.log.info(
                "Created vanilla Nether biome source for {}: source={}",
                dimensionKey.identifier(),
                externalGenerator.getBiomeSource().getClass().getName()
        );
        return externalGenerator;
    }

    private void attachExternalBiomeSource(
            ResourceKey<LevelStem> dimensionKey,
            ChunkGenerator targetGenerator,
            ChunkGenerator externalGenerator
    ) {
        if (externalGenerator == null || targetGenerator == externalGenerator) {
            return;
        }
        if (!(targetGenerator.getBiomeSource() instanceof MergeableBiomeSource<?> mergeableSource)) {
            return;
        }

        try {
            mergeableSource.mergeWithBiomeSource(externalGenerator.getBiomeSource());
            if (targetGenerator instanceof RebuildableFeaturesPerStep<?> rebuildable) {
                rebuildable.wover_rebuildFeaturesPerStep();
            }
            LibWoverWorldGenerator.C.log.info(
                    "Attached external biome source for {}: target={}, external={}, possibleBiomes={}",
                    dimensionKey.identifier(),
                    targetGenerator.getBiomeSource().getClass().getName(),
                    externalGenerator.getBiomeSource().getClass().getName(),
                    externalGenerator.getBiomeSource().possibleBiomes().size()
            );
        } catch (RuntimeException e) {
            LibWoverWorldGenerator.C.log.warn(
                    "Unable to attach external biome source for {}",
                    dimensionKey.identifier(),
                    e
            );
        }
    }

    private static void copyWorldPresetReference(
            Registry<LevelStem> dimensionRegistry,
            Map<ResourceKey<LevelStem>, ChunkGenerator> configuredDimensions
    ) {
        for (Map.Entry<ResourceKey<LevelStem>, LevelStem> loadedDimension : dimensionRegistry.entrySet()) {
            final ChunkGenerator referenceGenerator = configuredDimensions.get(loadedDimension.getKey());

            if (referenceGenerator instanceof ConfiguredChunkGenerator refCfg
                    && loadedDimension.getValue().generator() instanceof ConfiguredChunkGenerator loadedCfg) {
                if (loadedCfg.wover_getConfiguredWorldPreset() == null) {
                    loadedCfg.wover_setConfiguredWorldPreset(refCfg.wover_getConfiguredWorldPreset());
                }

            }
        }
    }

    private void registerAllBiomesFromRegistry(
            Registry<Biome> biomes
    ) {
        final Stopwatch sw = Stopwatch.createStarted();
        int biomesAdded = 0;

        final BiomeTagModificationWorker biomeTagWorker = new BiomeTagModificationWorker();
        for (Map.Entry<ResourceKey<Biome>, Biome> e : biomes.entrySet()) {
            TagKey<Biome> tag = null;
            if (NetherBiomesHelper.canGenerateInNether(e.getKey())) {
                tag = BiomeTags.IS_NETHER;
            } else if (TheEndBiomesHelper.canGenerateAsMainIslandBiome(e.getKey())) {
                tag = CommonBiomeTags.IS_END_CENTER;
            } else if (TheEndBiomesHelper.canGenerateAsHighlandsBiome(e.getKey())) {
                tag = CommonBiomeTags.IS_END_HIGHLAND;
            } else if (TheEndBiomesHelper.canGenerateAsEndBarrens(e.getKey())) {
                tag = CommonBiomeTags.IS_END_BARRENS;
            } else if (TheEndBiomesHelper.canGenerateAsSmallIslandsBiome(e.getKey())) {
                tag = CommonBiomeTags.IS_SMALL_END_ISLAND;
            } else if (TheEndBiomesHelper.canGenerateAsEndMidlands(e.getKey())) {
                tag = CommonBiomeTags.IS_END_MIDLAND;
            }

            if (tag != null) {
                final Holder.Reference<Biome> holder = biomes.getOrThrow(e.getKey());
                if (!holder.is(tag)) {
                    biomeTagWorker.addBiomeToTag(tag, biomes, e.getKey(), holder);
                    biomesAdded++;
                }
            }
        }

        biomeTagWorker.finished();

        if (biomesAdded > 0) {
            LibWoverWorldGenerator.C.log.info("Added tags for {} registered biomes in {}", biomesAdded, sw);
        }

    }

    private void registerVanillaNetherBiomes(Registry<Biome> biomes) {
        final Stopwatch sw = Stopwatch.createStarted();
        final int biomesAdded = VanillaNetherBiomeCompat.importBiomes(biomes);
        if (biomesAdded > 0) {
            LibWoverWorldGenerator.C.log.info("Registered {} vanilla Nether biome candidates in {}", biomesAdded, sw);
        }
    }

    private void registerAllBiomesFromTerraBlender(Registry<Biome> biomes) {
        if (!IntegrationCore.RUNS_TERRABLENDER) {
            return;
        }

        final Stopwatch sw = Stopwatch.createStarted();
        int biomesAdded = 0;
        final BiomeTagModificationWorker biomeTagWorker = new BiomeTagModificationWorker();
        try {
            biomesAdded += addTerraBlenderRegionBiomes(biomes, biomeTagWorker, "NETHER", BiomeTags.IS_NETHER);
            biomesAdded += TerraBlenderEndBiomeCompat.importRegisteredBiomes(biomes);
        } catch (Throwable e) {
            LibWoverWorldGenerator.C.log.warn("Failed reading TerraBlender regions for compatibility", e);
        }

        biomeTagWorker.finished();
        if (biomesAdded > 0) {
            LibWoverWorldGenerator.C.log.info("Added {} TerraBlender biomes in {}", biomesAdded, sw);
        }
    }

    private void registerCopiedEndBiomeRegistries(Registry<Biome> biomes) {
        final Stopwatch sw = Stopwatch.createStarted();
        final int biomesAdded = CopiedEndBiomeRegistryCompat.importRegisteredBiomes(biomes);
        if (biomesAdded > 0) {
            LibWoverWorldGenerator.C.log.info("Imported {} copied End Biomes API biome(s) in {}", biomesAdded, sw);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private int addTerraBlenderRegionBiomes(
            Registry<Biome> biomes,
            BiomeTagModificationWorker biomeTagWorker,
            String regionTypeName,
            TagKey<Biome> tag
    ) throws ReflectiveOperationException {
        final Set<ResourceKey<Biome>> regionBiomes = new HashSet<>();
        final Class<?> regionTypeClass = Class.forName("terrablender.api.RegionType");
        final Class<? extends Enum> enumClass = regionTypeClass.asSubclass(Enum.class);
        final Enum<?> regionType = Enum.valueOf(enumClass, regionTypeName);
        final Class<?> regionsClass = Class.forName("terrablender.api.Regions");
        final Method getRegions = regionsClass.getMethod("get", regionTypeClass);
        final Object value = getRegions.invoke(null, regionType);
        if (!(value instanceof Iterable<?> regions)) {
            return 0;
        }

        for (Object region : regions) {
            if (region == null) continue;
            final Method addBiomes = region.getClass().getMethod("addBiomes", Registry.class, Consumer.class);
            addBiomes.invoke(region, biomes, (Consumer<Object>) pairObject -> {
                if (pairObject == null) return;
                try {
                    final Object keyObject = pairObject.getClass().getMethod("getSecond").invoke(pairObject);
                    if (keyObject instanceof ResourceKey<?> key) {
                        @SuppressWarnings("unchecked")
                        final ResourceKey<Biome> biomeKey = (ResourceKey<Biome>) key;
                        if (biomes.containsKey(biomeKey)) {
                            regionBiomes.add(biomeKey);
                        }
                    }
                } catch (ReflectiveOperationException ignored) {
                }
            });
        }

        int added = 0;
        for (ResourceKey<Biome> biomeKey : regionBiomes) {
            final Holder.Reference<Biome> holder = biomes.getOrThrow(biomeKey);
            if (!holder.is(tag)) {
                biomeTagWorker.addBiomeToTag(tag, biomes, biomeKey, holder);
                added++;
            }
        }
        return added;
    }

    private void registerAllBiomesFromVanillaDimension(
            RegistryAccess access,
            Registry<Biome> biomes,
            ResourceKey<LevelStem> key
    ) {
        final Stopwatch sw = Stopwatch.createStarted();
        int biomesAdded = 0;

        final BiomeTagModificationWorker biomeTagWorker = new BiomeTagModificationWorker();
        final TagKey<Biome> tag = getBiomeTagForDimension(key);

        if (tag != null) {
            if (vanillaDimensions == null) {
                vanillaDimensions = DimensionsWrapper.getDimensionsMap(
                        access,
                        net.minecraft.world.level.levelgen.presets.WorldPresets.NORMAL
                );
            }

            final ChunkGenerator vanillaDim = vanillaDimensions.getOrDefault(key, null);
            if (vanillaDim != null && vanillaDim.getBiomeSource() != null) {
                for (Holder<Biome> biomeHolder : vanillaDim.getBiomeSource().possibleBiomes()) {
                    if (biomeHolder.unwrapKey().isPresent() && !biomeHolder.is(tag)) {
                        biomeTagWorker.addBiomeToTag(tag, biomes, biomeHolder.unwrapKey().orElseThrow(), biomeHolder);
                        biomesAdded++;
                    }
                }
            }

            biomeTagWorker.finished();

            if (biomesAdded > 0) {
                LibWoverWorldGenerator.C.log.info("Added {} biomes to {} in {}", biomesAdded, tag.location(), sw);
            }
        }
    }
}
