package org.betterx.wover.generator.impl.compat;

import org.betterx.wover.biome.api.data.BiomeData;
import org.betterx.wover.biome.api.data.BiomeGenerationDataContainer;
import org.betterx.wover.biome.impl.modification.BiomeTagModificationWorker;
import org.betterx.wover.entrypoint.LibWoverWorldGenerator;
import org.betterx.wover.generator.api.biomesource.WoverBiomeData;
import org.betterx.wover.tag.api.predefined.CommonBiomeTags;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TerraBlenderEndBiomeCompat {
    private static volatile Map<ResourceKey<Biome>, BiomeData> importedBiomeData = Map.of();

    private TerraBlenderEndBiomeCompat() {
    }

    public static BiomeData getImportedBiomeData(ResourceKey<Biome> biome) {
        return importedBiomeData.get(biome);
    }

    public static int importRegisteredBiomes(Registry<Biome> biomes) {
        final Map<ResourceKey<Biome>, BiomeData> imported = new LinkedHashMap<>();
        final BiomeTagModificationWorker tagWorker = new BiomeTagModificationWorker();

        try {
            final Class<?> registryClass = Class.forName("terrablender.api.EndBiomeRegistry");
            importBiomeSet(
                    registryClass,
                    "getIslandBiomes",
                    CommonBiomeTags.IS_SMALL_END_ISLAND,
                    biomes,
                    tagWorker,
                    imported
            );
            importBiomeSet(
                    registryClass,
                    "getHighlandsBiomes",
                    CommonBiomeTags.IS_END_HIGHLAND,
                    biomes,
                    tagWorker,
                    imported
            );
            importBiomeSet(
                    registryClass,
                    "getMidlandsBiomes",
                    CommonBiomeTags.IS_END_MIDLAND,
                    biomes,
                    tagWorker,
                    imported
            );
            importBiomeSet(
                    registryClass,
                    "getEdgeBiomes",
                    CommonBiomeTags.IS_END_BARRENS,
                    biomes,
                    tagWorker,
                    imported
            );
            tagWorker.finished();
        } catch (ClassNotFoundException ignored) {
            importedBiomeData = Map.of();
            return 0;
        } catch (ReflectiveOperationException | RuntimeException e) {
            importedBiomeData = Map.of();
            LibWoverWorldGenerator.C.log.warn("Unable to import TerraBlender End biomes", e);
            return 0;
        }

        importedBiomeData = Map.copyOf(imported);
        return imported.size();
    }

    @SuppressWarnings("unchecked")
    private static void importBiomeSet(
            Class<?> registryClass,
            String getter,
            TagKey<Biome> tag,
            Registry<Biome> biomes,
            BiomeTagModificationWorker tagWorker,
            Map<ResourceKey<Biome>, BiomeData> imported
    ) throws ReflectiveOperationException {
        final Object value = registryClass.getMethod(getter).invoke(null);
        if (!(value instanceof Iterable<?> entries)) {
            return;
        }

        final List<BiomeEntry> importedEntries = new ArrayList<>();
        for (Object entry : entries) {
            if (entry == null) {
                continue;
            }

            final Object data = entry.getClass().getMethod("data").invoke(entry);
            if (!(data instanceof ResourceKey<?> key)) {
                continue;
            }

            final ResourceKey<Biome> biomeKey = (ResourceKey<Biome>) key;
            if (!biomes.containsKey(biomeKey)) {
                continue;
            }

            importedEntries.add(new BiomeEntry(biomeKey, weightOf(entry)));
        }

        final Map<String, Float> normalizationFactors = normalizationFactors(importedEntries);
        for (BiomeEntry entry : importedEntries) {
            final ResourceKey<Biome> biomeKey = entry.biome();
            tagWorker.addBiomeToTag(tag, biomes, biomeKey, biomes.getHolderOrThrow(biomeKey));
            imported.putIfAbsent(biomeKey, new WoverBiomeData(
                    1.0F,
                    biomeKey,
                    BiomeGenerationDataContainer.EMPTY,
                    0.1F,
                    entry.weight() * normalizationFactors.get(biomeKey.location().getNamespace()),
                    0,
                    false,
                    null,
                    null
            ));
        }
    }

    private static Map<String, Float> normalizationFactors(List<BiomeEntry> entries) {
        final Map<String, Float> totalWeights = new HashMap<>();
        final Map<String, Integer> biomeCounts = new HashMap<>();
        for (BiomeEntry entry : entries) {
            final String namespace = entry.biome().location().getNamespace();
            totalWeights.merge(namespace, entry.weight(), Float::sum);
            biomeCounts.merge(namespace, 1, Integer::sum);
        }

        final Map<String, Float> factors = new HashMap<>();
        for (Map.Entry<String, Float> entry : totalWeights.entrySet()) {
            final float totalWeight = entry.getValue();
            factors.put(entry.getKey(), totalWeight > 0.0F
                    ? biomeCounts.get(entry.getKey()) / totalWeight
                    : 1.0F);
        }
        return factors;
    }

    private static float weightOf(Object entry) {
        try {
            final Object weight = entry.getClass().getMethod("getWeight").invoke(entry);
            final Object value = weight.getClass().getMethod("asInt").invoke(weight);
            if (value instanceof Integer integer && integer > 0) {
                return integer;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return 1.0F;
    }

    private record BiomeEntry(ResourceKey<Biome> biome, float weight) {
    }
}
