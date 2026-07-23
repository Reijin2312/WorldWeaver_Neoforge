package org.betterx.wover.surface.impl;

import org.betterx.wover.common.surface.api.InjectableSurfaceRules;
import org.betterx.wover.common.surface.api.SurfaceRuleProvider;
import org.betterx.wover.core.api.IntegrationCore;
import org.betterx.wover.entrypoint.LibWoverSurface;
import org.betterx.wover.state.api.WorldState;
import org.betterx.wover.surface.api.AssignedSurfaceRule;
import org.betterx.wover.surface.api.SurfaceRuleRegistry;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.level.levelgen.WorldGenSettings;

import com.google.common.base.Stopwatch;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.lang.reflect.Method;
import org.jetbrains.annotations.ApiStatus;

public class SurfaceRuleUtil {
    private static List<SurfaceRules.RuleSource> getRulesForBiome(HolderGetter<Biome> biomes, ResourceKey<Biome> biomeKey) {
        Registry<AssignedSurfaceRule> registry = null;
        if (WorldState.registryAccess() != null)
            registry = WorldState.registryAccess()
                                 .lookup(SurfaceRuleRegistry.SURFACE_RULES_REGISTRY).orElse(null);

        if (registry == null) {
            LibWoverSurface.C.LOG.warn("No Surface Rule Registry found. Skipping Surface Rule Injection for Biome {}", biomeKey.identifier());
            return List.of();
        }

        var list = registry.stream()
                           .filter(a -> a != null && a.biomeID != null && a.biomeID.equals(biomeKey.identifier()))
                           .sorted((a, b) -> b.priority - a.priority)
                           .map(a -> a.ruleSource)
                           .toList();

        if (list.size() == 0) return List.of();

        return List.of(SurfaceRules.ifTrue(SurfaceRules.isBiome(biomes, biomeKey), new SurfaceRules.SequenceRuleSource(list)));
    }

    private static List<SurfaceRules.RuleSource> getRulesForBiomes(List<Optional<ResourceKey<Biome>>> biomes) {
        if (WorldState.registryAccess() == null) {
            throw new IllegalStateException("Registry access is not available while collecting biome surface rules");
        }
        HolderGetter<Biome> biomeLookup = WorldState.registryAccess().lookupOrThrow(Registries.BIOME);
        List<ResourceKey<Biome>> biomeIDs = biomes.stream()
                                                  .filter(Optional::isPresent)
                                                  .map(Optional::orElseThrow)
                                                  .toList();

        return biomeIDs.stream()
                       .map(biome -> getRulesForBiome(biomeLookup, biome))
                       .flatMap(List::stream)
                       .collect(Collectors.toCollection(LinkedList::new));
    }

    private static SurfaceRules.RuleSource mergeSurfaceRules(
            ResourceKey<LevelStem> dimensionKey,
            SurfaceRules.RuleSource org,
            BiomeSource source,
            List<SurfaceRules.RuleSource> additionalRules
    ) {
        if (additionalRules == null || additionalRules.isEmpty()) return null;
        Stopwatch sw = Stopwatch.createStarted();
        final int count = additionalRules.size();
        if (org instanceof SurfaceRules.SequenceRuleSource sequenceRule) {
            List<SurfaceRules.RuleSource> existingSequence = sequenceRule.sequence();
            additionalRules = additionalRules
                    .stream()
                    .filter(r -> !existingSequence.contains(r))
                    .collect(Collectors.toList());
            if (additionalRules.isEmpty()) return null;

            // Minecraft 26.2 keeps bedrock and ceiling rules before the first biome-scoped Nether rule.
            // Insert at that exact structural boundary so custom rules cannot replace the roof or floor.
            if (dimensionKey.equals(LevelStem.NETHER)) {
                final List<SurfaceRules.RuleSource> combined = new ArrayList<>(existingSequence.size() + additionalRules.size());
                boolean inserted = false;
                for (SurfaceRules.RuleSource rule : existingSequence) {
                    if (!inserted && containsBiomeCondition(rule)) {
                        combined.addAll(additionalRules);
                        inserted = true;
                    }
                    combined.add(rule);
                }
                if (!inserted) {
                    throw new IllegalStateException(
                            "Minecraft 26.2 Nether surface rules have no biome-rule anchor for "
                                    + source.getClass().getName()
                    );
                }
                additionalRules = combined;
            } else {
                additionalRules.addAll(existingSequence);
            }
        } else {
            if (!additionalRules.contains(org))
                additionalRules.add(org);
        }

        LibWoverSurface.C.LOG.verbose(
                "Merged {} additional Surface Rules for Dimension {} => {} ({}) using {}",
                count,
                dimensionKey.identifier(),
                additionalRules.size(),
                sw.stop(),
                source
        );

        return new SurfaceRules.SequenceRuleSource(additionalRules);
    }

    private static boolean containsBiomeCondition(SurfaceRules.RuleSource rule) {
        if (rule instanceof SurfaceRules.TestRuleSource testRule) {
            if (testRule.ifTrue() instanceof SurfaceRules.BiomeConditionSource) {
                return true;
            }
            return containsBiomeCondition(testRule.thenRun());
        }

        if (rule instanceof SurfaceRules.SequenceRuleSource sequenceRule) {
            for (SurfaceRules.RuleSource nestedRule : sequenceRule.sequence()) {
                if (containsBiomeCondition(nestedRule)) {
                    return true;
                }
            }
        }

        return false;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void tryApplyTerraBlenderRuleCategory(
            Holder<NoiseGeneratorSettings> holder,
            ResourceKey<LevelStem> dimensionKey,
            BiomeSource source
    ) {
        if (!IntegrationCore.RUNS_TERRABLENDER || dimensionKey == null || holder == null || !holder.isBound()) {
            return;
        }

        final String categoryName;
        if (dimensionKey.equals(LevelStem.NETHER)) {
            categoryName = "NETHER";
        } else if (dimensionKey.equals(LevelStem.END)) {
            categoryName = "END";
        } else if (dimensionKey.equals(LevelStem.OVERWORLD)) {
            categoryName = "OVERWORLD";
        } else {
            return;
        }

        try {
            final Class<?> ruleCategoryClass = Class.forName("terrablender.api.SurfaceRuleManager$RuleCategory");
            final Enum ruleCategory = Enum.valueOf((Class<Enum>) ruleCategoryClass.asSubclass(Enum.class), categoryName);
            final Method setRuleCategory = holder.value().getClass().getMethod("setRuleCategory", ruleCategoryClass);
            setRuleCategory.invoke(holder.value(), ruleCategory);
        } catch (ReflectiveOperationException e) {
            LibWoverSurface.C.LOG.verbose(
                    "Unable to set TerraBlender surface rule category {} for {}: {}",
                    categoryName,
                    source.getClass().getName(),
                    e.getMessage()
            );
        }
    }

    @ApiStatus.Internal
    public static void injectNoiseBasedSurfaceRules(
            ResourceKey<LevelStem> dimensionKey,
            Holder<NoiseGeneratorSettings> noiseSettings,
            BiomeSource loadedBiomeSource
    ) {
        tryApplyTerraBlenderRuleCategory(noiseSettings, dimensionKey, loadedBiomeSource);
        Object o = noiseSettings.value();
        if (o instanceof SurfaceRuleProvider srp) {
            SurfaceRules.RuleSource originalRules = srp.wover_getOriginalSurfaceRules();
            srp.wover_overwriteSurfaceRules(mergeSurfaceRules(
                    dimensionKey,
                    originalRules,
                    loadedBiomeSource,
                    getRulesForBiomes(loadedBiomeSource.possibleBiomes().stream().map(Holder::unwrapKey).toList())
            ));
        }
    }

    static void injectSurfaceRulesToAllDimensions(
            LevelStorageSource.LevelStorageAccess ignoredStorageAccess,
            PackRepository ignoredPackRepository,
            LayeredRegistryAccess<RegistryLayer> registries,
            WorldData ignoredWorldData,
            WorldGenSettings ignoredWorldGenSettings
    ) {
        final Registry<LevelStem> dimensionRegistry = registries
                .compositeAccess()
                .lookupOrThrow(Registries.LEVEL_STEM);

        for (var entry : dimensionRegistry.entrySet()) {
            ResourceKey<LevelStem> dimensionKey = entry.getKey();
            LevelStem stem = entry.getValue();

            if (stem.generator() instanceof InjectableSurfaceRules<?> generator) {
                generator.wover_injectSurfaceRules(dimensionRegistry, dimensionKey.identifier().toString());
            }
        }
    }
}
