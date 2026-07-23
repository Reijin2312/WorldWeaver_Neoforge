package org.betterx.wover.generator.impl.compat;

import org.betterx.wover.biome.api.data.BiomeData;
import org.betterx.wover.biome.impl.modification.BiomeTagModificationWorker;
import org.betterx.wover.generator.api.biomesource.WoverBiomeData;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Makes vanilla Nether biomes first-class candidates of the WoVer Nether picker. */
public final class VanillaNetherBiomeCompat {
    private static final List<ResourceKey<Biome>> VANILLA_NETHER_BIOMES = List.of(
            Biomes.NETHER_WASTES,
            Biomes.SOUL_SAND_VALLEY,
            Biomes.CRIMSON_FOREST,
            Biomes.WARPED_FOREST,
            Biomes.BASALT_DELTAS
    );

    private static volatile Map<ResourceKey<Biome>, BiomeData> importedBiomeData = Map.of();

    private VanillaNetherBiomeCompat() {
    }

    public static BiomeData getImportedBiomeData(ResourceKey<Biome> biome) {
        return importedBiomeData.get(biome);
    }

    public static boolean isVanillaNetherBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(VANILLA_NETHER_BIOMES::contains).orElse(false);
    }

    public static int importBiomes(Registry<Biome> biomes) {
        Map<ResourceKey<Biome>, BiomeData> imported = new LinkedHashMap<>();
        BiomeTagModificationWorker tagWorker = new BiomeTagModificationWorker();

        for (ResourceKey<Biome> biome : VANILLA_NETHER_BIOMES) {
            if (!biomes.containsKey(biome)) {
                continue;
            }

            tagWorker.addBiomeToTag(BiomeTags.IS_NETHER, biomes, biome, biomes.getOrThrow(biome));
            imported.put(biome, WoverBiomeData.of(biome));
        }

        tagWorker.finished();
        importedBiomeData = Map.copyOf(imported);
        return imported.size();
    }
}
