package org.betterx.wover.generator.impl.compat;

import org.betterx.wover.entrypoint.LibWoverWorldGenerator;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public final class TerraBlenderBiomeSourceCompat {
    private static final String LEVEL_UTILS = "terrablender.util.LevelUtils";

    private TerraBlenderBiomeSourceCompat() {
    }

    public static boolean initialize(BiomeSource source, RegistryAccess registryAccess, Holder<DimensionType> dimensionType, ResourceKey<LevelStem> dimensionKey, ChunkGenerator settingsOwner, long seed) {
        if (!(source instanceof MultiNoiseBiomeSource) || !(settingsOwner instanceof NoiseBasedChunkGenerator noise)) {
            return false;
        }

        try {
            Class<?> levelUtils = Class.forName(LEVEL_UTILS);
            ChunkGenerator externalGenerator = new NoiseBasedChunkGenerator(source, noise.generatorSettings());
            Method shouldApply = levelUtils.getMethod("shouldApplyToChunkGenerator", ChunkGenerator.class);
            if (!Boolean.TRUE.equals(shouldApply.invoke(null, externalGenerator))) {
                return false;
            }
            Method initialize = levelUtils.getMethod("initializeBiomes", RegistryAccess.class, Holder.class, ResourceKey.class, ChunkGenerator.class, long.class);
            initialize.invoke(null, registryAccess, dimensionType, dimensionKey, externalGenerator, seed);
            LibWoverWorldGenerator.C.log.info("Initialized TerraBlender fallback for {} with {} possible biomes", dimensionKey.identifier(), source.possibleBiomes().size());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        } catch (InvocationTargetException e) {
            LibWoverWorldGenerator.C.log.warn("TerraBlender fallback failed to initialize for {}", dimensionKey.identifier(), e.getCause());
        } catch (ReflectiveOperationException | RuntimeException e) {
            LibWoverWorldGenerator.C.log.warn("Unable to initialize TerraBlender fallback for {}", dimensionKey.identifier(), e);
        }
        return false;
    }

    /**
     * TerraBlender can return region biomes that are absent from the biome source's declared possible-biome set.
     * ChunkGenerator feature sorting only considers that declared set, so import the registered region output too.
     */
    public static Set<Holder<Biome>> registeredBiomes(
            RegistryAccess registryAccess,
            ResourceKey<LevelStem> dimensionKey
    ) {
        final String regionTypeName;
        if (LevelStem.NETHER.equals(dimensionKey)) {
            regionTypeName = "NETHER";
        } else if (LevelStem.OVERWORLD.equals(dimensionKey)) {
            regionTypeName = "OVERWORLD";
        } else {
            return Set.of();
        }

        final Registry<Biome> biomes = registryAccess.lookupOrThrow(Registries.BIOME);
        final Set<Holder<Biome>> result = new HashSet<>();
        try {
            final Class<?> regionTypeClass = Class.forName("terrablender.api.RegionType");
            @SuppressWarnings("rawtypes")
            final Class<? extends Enum> enumClass = regionTypeClass.asSubclass(Enum.class);
            @SuppressWarnings("unchecked")
            final Enum<?> regionType = Enum.valueOf(enumClass, regionTypeName);
            final Class<?> regionsClass = Class.forName("terrablender.api.Regions");
            final Object value = regionsClass.getMethod("get", regionTypeClass).invoke(null, regionType);
            if (!(value instanceof Iterable<?> regions)) {
                return Set.of();
            }

            for (Object region : regions) {
                if (region == null) continue;
                final Method addBiomes = region.getClass().getMethod("addBiomes", Registry.class, Consumer.class);
                addBiomes.invoke(region, biomes, (Consumer<Object>) pair -> {
                    if (pair == null) return;
                    try {
                        final Object keyObject = pair.getClass().getMethod("getSecond").invoke(pair);
                        if (keyObject instanceof ResourceKey<?> key) {
                            @SuppressWarnings("unchecked")
                            final ResourceKey<Biome> biomeKey = (ResourceKey<Biome>) key;
                            biomes.get(biomeKey).ifPresent(result::add);
                        }
                    } catch (ReflectiveOperationException ignored) {
                    }
                });
            }
        } catch (ClassNotFoundException ignored) {
            return Set.of();
        } catch (ReflectiveOperationException | RuntimeException e) {
            LibWoverWorldGenerator.C.log.warn(
                    "Unable to collect TerraBlender region biomes for {}",
                    dimensionKey.identifier(),
                    e
            );
        }
        return Set.copyOf(result);
    }
}
