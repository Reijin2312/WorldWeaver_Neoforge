package org.betterx.wover.generator.impl.chunkgenerator;

import org.betterx.wover.common.generator.api.chunkgenerator.RestorableBiomeSource;
import org.betterx.wover.common.generator.api.chunkgenerator.RebuildableFeaturesPerStep;
import org.betterx.wover.common.generator.impl.compat.LithostitchedBiomeSourceCompat;
import org.betterx.wover.entrypoint.LibWoverWorldGenerator;
import org.betterx.wover.events.api.WorldLifecycle;
import org.betterx.wover.legacy.api.LegacyHelper;
import org.betterx.wover.state.api.WorldState;
import org.betterx.wover.generator.api.biomesource.WoverBiomeSource;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;

import java.util.*;
import org.jetbrains.annotations.ApiStatus;

public class WoverChunkGeneratorImpl {
    public static final ResourceKey<NoiseGeneratorSettings> LEGACY_AMPLIFIED_NETHER = ResourceKey.create(
            Registries.NOISE_SETTINGS,
            LegacyHelper.BCLIB_CORE.convertNamespace(WoverChunkGenerator.AMPLIFIED_NETHER.identifier())
    );

    @ApiStatus.Internal
    public static void initialize() {
        WorldLifecycle.MINECRAFT_SERVER_READY.subscribe(WoverChunkGeneratorImpl::restoreInitialBiomeSourceInAllDimensions);
        WorldLifecycle.ON_DIMENSION_LOAD.subscribe(WoverChunkGeneratorImpl::repairBiomeSourceInAllDimensions);
        WorldLifecycle.BEFORE_CREATING_LEVELS.subscribe(WoverChunkGeneratorImpl::initializeExternalBiomeSources);
        WorldLifecycle.BEFORE_CREATING_LEVELS.subscribe(WoverChunkGeneratorImpl::printInfo, -1000);
    }

    private static void initializeExternalBiomeSources(
            LevelStorageSource.LevelStorageAccess ignoredStorageAccess,
            PackRepository ignoredPackRepository,
            LayeredRegistryAccess<RegistryLayer> registries,
            WorldData ignoredWorldData,
            WorldGenSettings worldGenSettings
    ) {
        final long seed = worldGenSettings.options().seed();
        final RegistryAccess registryAccess = registries.compositeAccess();
        final Registry<LevelStem> dimensions = registryAccess.lookupOrThrow(Registries.LEVEL_STEM);
        LibWoverWorldGenerator.C.log.info(
                "Initializing external biome sources from active dimensions registry ({} entries)",
                dimensions.size()
        );
        for (var entry : dimensions.entrySet()) {
            final ChunkGenerator generator = entry.getValue().generator();
            final var loadedSource = generator.getBiomeSource();
            final var sourceForCompatibility = LithostitchedBiomeSourceCompat.unwrap(loadedSource);
            if (sourceForCompatibility instanceof WoverBiomeSource source
                    && source.initializeExternalBiomeSource(
                    seed,
                    registryAccess,
                    entry.getValue().type(),
                    entry.getKey(),
                    generator
            )) {
                if (LithostitchedBiomeSourceCompat.refreshPossibleBiomes(loadedSource, source.possibleBiomes())) {
                    LibWoverWorldGenerator.C.log.info(
                            "Refreshed Lithostitched biome cache for {} with {} possible biomes",
                            entry.getKey().identifier(),
                            loadedSource.possibleBiomes().size()
                    );
                }
                if (generator instanceof RebuildableFeaturesPerStep<?> rebuildable) {
                    rebuildable.wover_rebuildFeaturesPerStep();
                }
            }
        }
    }

    private static void printInfo(
            LevelStorageSource.LevelStorageAccess levelStorageAccess,
            PackRepository packRepository,
            LayeredRegistryAccess<RegistryLayer> registryLayerLayeredRegistryAccess,
            WorldData worldData,
            WorldGenSettings ignoredWorldGenSettings
    ) {
        if (WorldState.registryAccess() != null) {
            final Registry<LevelStem> dimensionsRegistry = WorldState.registryAccess()
                                                                     .lookupOrThrow(Registries.LEVEL_STEM);
            ChunkGeneratorManagerImpl.printDimensionInfo(dimensionsRegistry);
        }
    }


    /**
     * Some mods forcefully swap the biomeSource that is attached to a ChunkGenerator. This method checks if the
     * Generator implements {@link RestorableBiomeSource}, and if so, it will restore the original biomeSource, usually
     * the one that was created in the constructor of the generator.
     *
     * @param levelStorageAccess The levelStorageAccess
     * @param packRepository     The packRepository
     * @param worldStem          The worldStem
     */
    private static void restoreInitialBiomeSourceInAllDimensions(
            LevelStorageSource.LevelStorageAccess levelStorageAccess,
            PackRepository packRepository,
            WorldStem worldStem
    ) {
        for (var entry : WorldState.registryAccess().lookupOrThrow(Registries.LEVEL_STEM).entrySet()) {
            ResourceKey<LevelStem> key = entry.getKey();
            LevelStem stem = entry.getValue();

            if (stem.generator() instanceof RestorableBiomeSource<?> generator) {
                generator.restoreInitialBiomeSource(key);
            }
        }
    }

    private static LayeredRegistryAccess<RegistryLayer> repairBiomeSourceInAllDimensions(LayeredRegistryAccess<RegistryLayer> registries) {
        final RegistryAccess.Frozen access = registries.compositeAccess();
        final Registry<LevelStem> dimensions = access.lookupOrThrow(Registries.LEVEL_STEM);

        WorldGeneratorConfigImpl.migrateGeneratorSettings(access, dimensions);

        final BiomeRepairHelper biomeHelper = new BiomeRepairHelper();
        final Registry<LevelStem> changedDimensions = biomeHelper.repairBiomeSourceInAllDimensions(access, dimensions);

        if (dimensions != changedDimensions) {
            LibWoverWorldGenerator.C.log.verbose("Loading World with initially configured Dimensions.");
            registries = registries.replaceFrom(
                    RegistryLayer.DIMENSIONS,
                    new RegistryAccess.ImmutableRegistryAccess(List.of(changedDimensions)).freeze()
            );
        }

        return registries;
    }

    public interface RegisterHelper{
        Holder.Reference<LevelStem> register(MappedRegistry<LevelStem> writableRegistry, ResourceKey<LevelStem> key, LevelStem stem);
    }

    public interface StemGetter{
        LevelStem get(ResourceKey<LevelStem> key);
    }

    public static Registry<LevelStem> replaceGenerator(
            ResourceKey<LevelStem> dimensionKey,
            ResourceKey<DimensionType> dimensionTypeKey,
            RegistryAccess registryAccess,
            Set<Map.Entry<ResourceKey<LevelStem>, LevelStem>> dimensionRegistry,
            ChunkGenerator generator,
            StemGetter getter,
            RegisterHelper registerHelper
    ) {
        final Registry<DimensionType> dimensionTypeRegistry = registryAccess.lookupOrThrow(Registries.DIMENSION_TYPE);
        final LevelStem levelStem = getter.get(dimensionKey);

        Holder<DimensionType> dimensionType = levelStem == null
                ? dimensionTypeRegistry.getOrThrow(dimensionTypeKey)
                : levelStem.type();

        MappedRegistry<LevelStem> writableRegistry = new MappedRegistry<>(
                Registries.LEVEL_STEM,
                Lifecycle.experimental()
        );

        writableRegistry.register(
                dimensionKey,
                new LevelStem(dimensionType, generator),
                RegistrationInfo.BUILT_IN
        );

        //copy all other dimensions
        for (Map.Entry<ResourceKey<LevelStem>, LevelStem> entry : dimensionRegistry) {
            final ResourceKey<LevelStem> resourceKey = entry.getKey();
            if (dimensionKey.identifier().equals(resourceKey.identifier())) continue;

            registerHelper.register(writableRegistry, resourceKey, entry.getValue());
        }

        return writableRegistry;
    }

}
