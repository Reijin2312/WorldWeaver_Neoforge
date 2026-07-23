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
import net.minecraft.util.random.Weighted;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TerraBlenderEndBiomeCompat {
    private static volatile Map<ResourceKey<Biome>, BiomeData> importedBiomeData = Map.of();

    private TerraBlenderEndBiomeCompat() {
    }

    public static BiomeData getImportedBiomeData(ResourceKey<Biome> biome) {
        return importedBiomeData.get(biome);
    }

    public static int importRegisteredBiomes(Registry<Biome> biomes) {
        Map<ResourceKey<Biome>, BiomeData> imported = new LinkedHashMap<>();
        BiomeTagModificationWorker tagWorker = new BiomeTagModificationWorker();
        try {
            Class<?> registryClass = Class.forName("terrablender.api.EndBiomeRegistry");
            importBiomeSet(registryClass, "getIslandBiomes", CommonBiomeTags.IS_SMALL_END_ISLAND, biomes, tagWorker, imported);
            importBiomeSet(registryClass, "getHighlandsBiomes", CommonBiomeTags.IS_END_HIGHLAND, biomes, tagWorker, imported);
            importBiomeSet(registryClass, "getMidlandsBiomes", CommonBiomeTags.IS_END_MIDLAND, biomes, tagWorker, imported);
            importBiomeSet(registryClass, "getEdgeBiomes", CommonBiomeTags.IS_END_BARRENS, biomes, tagWorker, imported);
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
    private static void importBiomeSet(Class<?> registryClass, String getter, TagKey<Biome> tag, Registry<Biome> biomes, BiomeTagModificationWorker tagWorker, Map<ResourceKey<Biome>, BiomeData> imported) throws ReflectiveOperationException {
        Object value = registryClass.getMethod(getter).invoke(null);
        if (!(value instanceof Iterable<?> entries)) return;
        List<BiomeEntry> importedEntries = new ArrayList<>();
        for (Object entry : entries) {
            if (!(entry instanceof Weighted<?> wrapper)) continue;
            Object data = wrapper.value();
            if (!(data instanceof ResourceKey<?> key)) continue;
            ResourceKey<Biome> biomeKey = (ResourceKey<Biome>) key;
            if (biomes.containsKey(biomeKey)) {
                importedEntries.add(new BiomeEntry(biomeKey, weightOf(wrapper)));
            }
        }
        Map<String, Float> normalizationFactors = normalizationFactors(importedEntries);
        for (BiomeEntry entry : importedEntries) {
            ResourceKey<Biome> biomeKey = entry.biome();
            tagWorker.addBiomeToTag(tag, biomes, biomeKey, biomes.getOrThrow(biomeKey));
            imported.putIfAbsent(biomeKey, new WoverBiomeData(1.0F, biomeKey, BiomeGenerationDataContainer.EMPTY, 0.1F, entry.weight() * normalizationFactors.get(biomeKey.identifier().getNamespace()), 0, false, null, null));
        }
    }

    private static Map<String, Float> normalizationFactors(List<BiomeEntry> entries) {
        Map<String, Float> totalWeights = new HashMap<>();
        Map<String, Integer> biomeCounts = new HashMap<>();
        for (BiomeEntry entry : entries) {
            String namespace = entry.biome().identifier().getNamespace();
            totalWeights.merge(namespace, entry.weight(), Float::sum);
            biomeCounts.merge(namespace, 1, Integer::sum);
        }
        Map<String, Float> factors = new HashMap<>();
        for (Map.Entry<String, Float> entry : totalWeights.entrySet()) {
            factors.put(entry.getKey(), entry.getValue() > 0.0F ? biomeCounts.get(entry.getKey()) / entry.getValue() : 1.0F);
        }
        return factors;
    }

    private static float weightOf(Weighted<?> entry) {
        int weight = entry.weight();
        return weight > 0 ? weight : 1.0F;
    }

    private record BiomeEntry(ResourceKey<Biome> biome, float weight) {
    }
}
