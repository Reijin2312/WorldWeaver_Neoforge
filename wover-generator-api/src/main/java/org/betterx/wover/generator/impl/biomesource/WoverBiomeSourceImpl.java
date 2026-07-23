package org.betterx.wover.generator.impl.biomesource;

import org.betterx.wover.biome.api.data.BiomeData;
import org.betterx.wover.biome.api.data.BiomeDataRegistry;
import org.betterx.wover.biome.impl.data.BiomeDataRegistryImpl;
import org.betterx.wover.core.api.ModCore;
import org.betterx.wover.entrypoint.LibWoverWorldGenerator;
import org.betterx.wover.generator.api.biomesource.WoverBiomeSource;
import org.betterx.wover.generator.impl.compat.BlueprintBiomeSourceCompat;
import org.betterx.wover.generator.impl.compat.CopiedEndBiomeRegistryCompat;
import org.betterx.wover.generator.impl.compat.TerraBlenderEndBiomeCompat;
import org.betterx.wover.generator.impl.compat.VanillaNetherBiomeCompat;
import org.betterx.wover.state.api.WorldState;
import org.betterx.wover.util.Pair;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;

import java.util.*;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;

public class WoverBiomeSourceImpl {
    /**
     * Get a list of namespaces from a collection of biomes.
     *
     * @param biomes The collection of biomes.
     * @return A comma-separated list of namespaces including the number of biomes found in each namespace.
     */
    public static String getNamespaces(Collection<Holder<Biome>> biomes) {
        var namespaces = biomes
                .stream()
                .filter(h -> h.unwrapKey().isPresent())
                .map(h -> h.unwrapKey().get().identifier().getNamespace())
                .toList();

        return namespaces
                .stream()
                .distinct()
                .map(n -> n + "(" + namespaces.stream().filter(n::equals).count() + ")")
                .collect(Collectors.joining(", "));

    }

    public record PopulateResult(Set<Holder<Biome>> possibleBiomes, List<WoverBiomeSource.TagToPicker> pickers) {
    }

    public static @Nullable Set<Holder<Biome>> populateBiomePickers(
            List<WoverBiomeSource.TagToPicker> pickers,
            WoverBiomeSource.PickerAdder pickerAdder
    ) {
        RegistryAccess access = WorldState.registryAccess();
        if (access == null) {
            access = WorldState.allStageRegistryAccess();
            if (access != null) {
                LibWoverWorldGenerator.C.log.verbose("Registries were not finalized before populating BiomePickers!");
            } else {
                if (!ModCore.isDatagen()) {
                    LibWoverWorldGenerator.C.log.verbose("Unable to build Biome List yet");
                }
                return null;
            }
        }

        final Set<Holder<Biome>> allBiomes = new HashSet<>();
        final Set<BiomePlacement> addedBiomePlacements = new HashSet<>();
        final Registry<Biome> biomes = access.lookupOrThrow(Registries.BIOME);
        final Registry<BiomeData> biomeData = access.lookup(BiomeDataRegistry.BIOME_DATA_REGISTRY).orElse(null);

        for (WoverBiomeSource.TagToPicker mapper : pickers) {
            final Optional<HolderSet.Named<Biome>> optionalTag = biomes.get(mapper.tag());
            if (optionalTag.isPresent()) {
                final HolderSet.Named<Biome> tag = optionalTag.get();
                final Set<Identifier> excluded = BiomeSourceManagerImpl.getExcludedBiomes(tag.key());

                tag.stream()
                   .filter(holder -> holder.unwrapKey().isPresent())
                   .map(holder -> new Pair<>(holder, holder.unwrapKey().get()))
                   .filter(pair -> !addedBiomePlacements.contains(new BiomePlacement(mapper.tag(), pair.second)))
                   .filter(pair -> !excluded.contains(pair.second.identifier()))
                   .sorted(Comparator.comparing(pair -> pair.second.identifier().toString()))
                   .forEach(pair -> {
                       final boolean isPossible;
                       BiomeData data = BiomeDataRegistryImpl.getFromRegistryOrTemp(
                               biomeData,
                               pair.second
                       );

                       if (data != null && !data.isTemp()) {
                           isPossible = data.isPickable()
                                   ? pickerAdder.add(data, mapper.tag(), mapper.picker())
                                   : true;
                       } else {
                           data = BlueprintBiomeSourceCompat.getImportedBiomeData(pair.second);
                           if (data == null || data.isTemp()) {
                               data = TerraBlenderEndBiomeCompat.getImportedBiomeData(pair.second);
                           }
                           if (data == null || data.isTemp()) {
                               data = CopiedEndBiomeRegistryCompat.getImportedBiomeData(pair.second);
                           }
                           if (data == null || data.isTemp()) {
                               data = VanillaNetherBiomeCompat.getImportedBiomeData(pair.second);
                           }
                           isPossible = data != null && !data.isTemp() && data.isPickable()
                                   && pickerAdder.add(data, mapper.tag(), mapper.picker());
                       }

                       if (isPossible) {
                           addedBiomePlacements.add(new BiomePlacement(mapper.tag(), pair.second));
                           allBiomes.add(pair.first);
                       }
                   });
            }
        }

        return allBiomes;
    }

    private record BiomePlacement(net.minecraft.tags.TagKey<Biome> tag, ResourceKey<Biome> biome) {
    }
}
