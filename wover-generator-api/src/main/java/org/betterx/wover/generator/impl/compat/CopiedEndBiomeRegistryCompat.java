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
import net.minecraft.world.level.biome.Biomes;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Imports active registrations from mods shipping a private copy of the End Biomes API. */
public final class CopiedEndBiomeRegistryCompat {
    private static final List<String> REGISTRY_CLASSES = List.of(
            "net.lyof.phantasm.world.gen.TheEndBiomeData",
            "net.mcreator.outerendwilds.endbiomes.TheEndBiomeData"
    );
    private static volatile Map<ResourceKey<Biome>, BiomeData> importedBiomeData = Map.of();

    private CopiedEndBiomeRegistryCompat() {
    }

    public static BiomeData getImportedBiomeData(ResourceKey<Biome> biome) {
        return importedBiomeData.get(biome);
    }

    public static int importRegisteredBiomes(Registry<Biome> biomes) {
        final Map<ResourceKey<Biome>, BiomeData> imported = new LinkedHashMap<>();
        final BiomeTagModificationWorker tagWorker = new BiomeTagModificationWorker();

        for (String registryClassName : REGISTRY_CLASSES) {
            try {
                final Class<?> registryClass = Class.forName(registryClassName);
                importEntries(registryClass, "endBiomes", null, biomes, tagWorker, imported);
                importEntries(registryClass, "midlandsBiomes", CommonBiomeTags.IS_END_MIDLAND, biomes, tagWorker, imported);
                importEntries(registryClass, "barrensBiomes", CommonBiomeTags.IS_END_BARRENS, biomes, tagWorker, imported);
            } catch (ClassNotFoundException ignored) {
                // Optional integration.
            } catch (ReflectiveOperationException | RuntimeException e) {
                LibWoverWorldGenerator.C.log.warn(
                        "Unable to import End biome registrations from {}", registryClassName, e);
            }
        }

        tagWorker.finished();
        importedBiomeData = Map.copyOf(imported);
        return imported.size();
    }

    private static void importEntries(
            Class<?> registryClass,
            String fieldName,
            TagKey<Biome> fixedTag,
            Registry<Biome> biomes,
            BiomeTagModificationWorker tagWorker,
            Map<ResourceKey<Biome>, BiomeData> imported
    ) throws ReflectiveOperationException {
        final Field field = registryClass.getField(fieldName);
        final Object value = field.get(null);
        if (!(value instanceof Iterable<?> entries)) {
            return;
        }

        final List<BiomeEntry> importedEntries = new ArrayList<>();
        for (Object entryObject : entries) {
            if (!(entryObject instanceof List<?> entry) || entry.size() < 3
                    || !(entry.get(0) instanceof ResourceKey<?> parent)
                    || !(entry.get(1) instanceof ResourceKey<?> biome)
                    || !(entry.get(2) instanceof Number weight)) {
                continue;
            }

            @SuppressWarnings("unchecked")
            final ResourceKey<Biome> biomeKey = (ResourceKey<Biome>) biome;
            final TagKey<Biome> tag = fixedTag != null ? fixedTag : tagForParent(parent);
            final float entryWeight = Math.max(0.01F, weight.floatValue());
            if (tag != null && biomes.containsKey(biomeKey)) {
                importedEntries.add(new BiomeEntry(tag, biomeKey, entryWeight));
            }
        }

        final Map<String, Float> normalizationFactors = normalizationFactors(importedEntries);
        for (BiomeEntry entry : importedEntries) {
            tagWorker.addBiomeToTag(entry.tag(), biomes, entry.biome(), biomes.getOrThrow(entry.biome()));
            imported.putIfAbsent(entry.biome(), new WoverBiomeData(
                    1.0F,
                    entry.biome(),
                    BiomeGenerationDataContainer.EMPTY,
                    0.1F,
                    entry.weight() * normalizationFactors.get(entry.biome().identifier().getNamespace()),
                    0,
                    false,
                    null,
                    null
            ));
        }
    }

    private static TagKey<Biome> tagForParent(ResourceKey<?> parent) {
        if (Biomes.THE_END.equals(parent)) return CommonBiomeTags.IS_END_CENTER;
        if (Biomes.END_HIGHLANDS.equals(parent)) return CommonBiomeTags.IS_END_HIGHLAND;
        if (Biomes.SMALL_END_ISLANDS.equals(parent)) return CommonBiomeTags.IS_SMALL_END_ISLAND;
        return null;
    }

    private static Map<String, Float> normalizationFactors(List<BiomeEntry> entries) {
        final Map<String, Float> totalWeights = new HashMap<>();
        final Map<String, Integer> biomeCounts = new HashMap<>();
        for (BiomeEntry entry : entries) {
            final String namespace = entry.biome().identifier().getNamespace();
            totalWeights.merge(namespace, entry.weight(), Float::sum);
            biomeCounts.merge(namespace, 1, Integer::sum);
        }

        final Map<String, Float> factors = new HashMap<>();
        for (Map.Entry<String, Float> entry : totalWeights.entrySet()) {
            factors.put(entry.getKey(), biomeCounts.get(entry.getKey()) / entry.getValue());
        }
        return factors;
    }

    private record BiomeEntry(TagKey<Biome> tag, ResourceKey<Biome> biome, float weight) {
    }
}
