package org.betterx.wover.generator.impl.biomesource.nether;

import org.betterx.wover.entrypoint.LibWoverWorldGenerator;
import org.betterx.wover.generator.mixin.biomesource.MultiNoiseBiomeSourceParameterListAccessor;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.ApiStatus;

public final class NetherBiomesHelper {
    private static final Set<ResourceKey<Biome>> VANILLA_NETHER
            = new HashSet<>(MultiNoiseBiomeSourceParameterList.Preset.NETHER.usedBiomes().toList());
    private static final List<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> ADDITIONS = new ArrayList<>();
    private static final Set<ResourceKey<Biome>> ADDED_BIOMES = new HashSet<>();

    private NetherBiomesHelper() {
    }

    @ApiStatus.Internal
    public static synchronized void addNetherBiome(ResourceKey<Biome> biome, Climate.ParameterPoint parameters) {
        if (biome == null || parameters == null) return;
        if (VANILLA_NETHER.contains(biome) || ADDED_BIOMES.contains(biome)) return;
        ADDITIONS.add(Pair.of(parameters, biome));
        ADDED_BIOMES.add(biome);
    }

    public static boolean canGenerateInNether(ResourceKey<Biome> biome) {
        return biome != null && (VANILLA_NETHER.contains(biome) || ADDED_BIOMES.contains(biome));
    }

    @ApiStatus.Internal
    public static synchronized List<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> getAdditions() {
        return List.copyOf(ADDITIONS);
    }

    @ApiStatus.Internal
    public static void syncParameterList(RegistryAccess access) {
        if (access == null) return;
        Registry<MultiNoiseBiomeSourceParameterList> registry
                = access.lookup(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).orElse(null);
        if (registry == null) return;
        MultiNoiseBiomeSourceParameterList list = registry.get(MultiNoiseBiomeSourceParameterLists.NETHER)
                                                         .map(Holder.Reference::value)
                                                         .orElse(null);
        if (!(list instanceof MultiNoiseBiomeSourceParameterListAccessor accessor)) return;

        List<Pair<Climate.ParameterPoint, Holder<Biome>>> updated = new ArrayList<>(accessor.wover_getParameters().values());
        Set<ResourceKey<Biome>> existing = new HashSet<>();
        for (Pair<Climate.ParameterPoint, Holder<Biome>> entry : updated) {
            entry.getSecond().unwrapKey().ifPresent(existing::add);
        }

        Registry<Biome> biomes = access.lookup(Registries.BIOME).orElse(null);
        if (biomes == null) return;
        boolean changed = false;
        for (Pair<Climate.ParameterPoint, ResourceKey<Biome>> entry : getAdditions()) {
            if (!existing.contains(entry.getSecond())) {
                boolean added = biomes.get(entry.getSecond())
                                      .map(holder -> updated.add(Pair.of(entry.getFirst(), holder)))
                                      .orElse(false);
                if (added) {
                    changed = true;
                }
            }
        }

        if (changed) {
            accessor.wover_setParameters(new Climate.ParameterList<>(updated));
            LibWoverWorldGenerator.C.log.debug("Updated nether biome parameter list with {} entries", updated.size());
        }
    }
}
