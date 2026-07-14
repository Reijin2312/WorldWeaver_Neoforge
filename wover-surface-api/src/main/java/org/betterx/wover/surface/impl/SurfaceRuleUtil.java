package org.betterx.wover.surface.impl;

import org.betterx.wover.common.surface.api.InjectableSurfaceRules;
import org.betterx.wover.common.surface.api.SurfaceRuleProvider;
import org.betterx.wover.common.generator.impl.compat.LithostitchedBiomeSourceCompat;
import org.betterx.wover.entrypoint.LibWoverSurface;
import org.betterx.wover.state.api.WorldState;
import org.betterx.wover.surface.api.AssignedSurfaceRule;
import org.betterx.wover.surface.api.SurfaceRuleRegistry;

import net.minecraft.core.Holder;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;

import com.google.common.base.Stopwatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.lang.reflect.Field;
import org.jetbrains.annotations.ApiStatus;

public class SurfaceRuleUtil {
    private static List<SurfaceRules.RuleSource> getRulesForBiome(ResourceKey<Biome> biomeKey) {
        Registry<AssignedSurfaceRule> registry = null;
        if (WorldState.registryAccess() != null)
            registry = WorldState.registryAccess()
                                 .registry(SurfaceRuleRegistry.SURFACE_RULES_REGISTRY).orElse(null);

        if (registry == null) {
            LibWoverSurface.C.LOG.warn("No Surface Rule Registry found. Skipping Surface Rule Injection for Biome {}", biomeKey.location());
            return List.of();
        }

        var list = registry.stream()
                           .filter(a -> a != null && a.biomeID != null && a.biomeID.equals(biomeKey.location()))
                           .sorted((a, b) -> b.priority - a.priority)
                           .map(a -> a.ruleSource)
                           .toList();

        if (list.size() == 0) return List.of();

        return List.of(SurfaceRules.ifTrue(SurfaceRules.isBiome(biomeKey), new SurfaceRules.SequenceRuleSource(list)));
    }

    private static List<SurfaceRules.RuleSource> getRulesForBiomes(List<Optional<ResourceKey<Biome>>> biomes) {
        List<ResourceKey<Biome>> biomeIDs = biomes.stream()
                                                  .filter(Optional::isPresent)
                                                  .map(Optional::orElseThrow)
                                                  .toList();

        return biomeIDs.stream()
                       .map(SurfaceRuleUtil::getRulesForBiome)
                       .flatMap(List::stream)
                       .collect(Collectors.toCollection(LinkedList::new));
    }

    private static List<SurfaceRules.RuleSource> getCompatRulesForDimension(
            ResourceKey<LevelStem> dimensionKey,
            BiomeSource source
    ) {
        if (LevelStem.NETHER.equals(dimensionKey)) {
            SurfaceRules.RuleSource incisionRule = tryBuildIncisionSurfaceRule(source);
            return incisionRule == null ? List.of() : List.of(incisionRule);
        } else if (!LevelStem.END.equals(dimensionKey)) {
            return List.of();
        }

        final boolean hasEnderscapeBiome = source.possibleBiomes()
                                             .stream()
                                             .map(Holder::unwrapKey)
                                             .flatMap(Optional::stream)
                                             .map(ResourceKey::location)
                                             .map(ResourceLocation::getNamespace)
                                             .anyMatch("enderscape"::equals);
        if (!hasEnderscapeBiome) {
            return List.of();
        }

        final SurfaceRules.RuleSource enderscapeRules = tryBuildEnderscapeSurfaceRules();
        if (enderscapeRules == null) {
            return List.of();
        }

        // Enderscape normally prepends these rules to the End noise settings. Wover rebuilds surface rules from the
        // original base sequence, so we re-inject them here when Enderscape biomes are present.
        return List.of(enderscapeRules);
    }

    private static SurfaceRules.RuleSource tryBuildEnderscapeSurfaceRules() {
        try {
            final Class<?> rulesClass = Class.forName("net.bunten.enderscape.registry.EnderscapeSurfaceRuleData");
            final Object result = rulesClass.getMethod("makeRules").invoke(null);
            if (result instanceof SurfaceRules.RuleSource ruleSource) {
                return ruleSource;
            }
        } catch (ReflectiveOperationException e) {
            LibWoverSurface.C.LOG.verbose("Unable to import Enderscape surface rules: {}", e.getMessage());
        }

        return null;
    }

    private static SurfaceRules.RuleSource tryBuildIncisionSurfaceRule(BiomeSource source) {
        final ResourceKey<Biome> biomeKey = ResourceKey.create(
                Registries.BIOME,
                ResourceLocation.fromNamespaceAndPath("incision", "eroded_yard")
        );
        if (source.possibleBiomes().stream().noneMatch(holder -> holder.is(biomeKey))) {
            return null;
        }

        try {
            Class.forName("net.incision.init.IncisionModBiomes");
        } catch (ClassNotFoundException ignored) {
            return null;
        }

        Registry<Block> blocks = WorldState.registryAccess() == null
                ? null
                : WorldState.registryAccess().registryOrThrow(Registries.BLOCK);
        if (blocks == null) {
            return null;
        }
        Block block = blocks.get(ResourceLocation.fromNamespaceAndPath("incision", "weatherrack"));
        if (block == null) {
            return null;
        }
        BlockState state = block.defaultBlockState();
        return SurfaceRules.ifTrue(
                SurfaceRules.isBiome(biomeKey),
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(
                                SurfaceRules.stoneDepthCheck(0, false, 0, CaveSurface.FLOOR),
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-1, 0), SurfaceRules.state(state)),
                                        SurfaceRules.state(state)
                                )
                        ),
                        SurfaceRules.ifTrue(
                                SurfaceRules.stoneDepthCheck(0, true, 0, CaveSurface.FLOOR),
                                SurfaceRules.state(state)
                        )
                )
        );
    }

    @SuppressWarnings("unchecked")
    private static Collection<Holder<Biome>> getBiomesWithWoverSurfaceRules(BiomeSource source) {
        final BiomeSource unwrappedSource = LithostitchedBiomeSourceCompat.unwrap(source);
        Collection<Holder<Biome>> ownedBiomes = null;
        try {
            final var method = unwrappedSource.getClass().getMethod("ownedPossibleBiomes");
            final Object result = method.invoke(unwrappedSource);
            if (result instanceof Collection<?> collection) {
                ownedBiomes = (Collection<Holder<Biome>>) collection;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        if (ownedBiomes == null || ownedBiomes.isEmpty() || source == unwrappedSource) {
            return ownedBiomes == null ? source.possibleBiomes() : ownedBiomes;
        }

        // Elysium can return a BetterNether/BetterEnd biome through an external source. It still needs this mod's
        // surface rules, but external namespaces must remain untouched.
        Set<String> ownedNamespaces = ownedBiomes.stream()
                                                  .map(Holder::unwrapKey)
                                                  .flatMap(Optional::stream)
                                                  .map(ResourceKey::location)
                                                  .map(ResourceLocation::getNamespace)
                                                  .collect(Collectors.toSet());
        if (ownedNamespaces.isEmpty()) {
            return ownedBiomes;
        }

        Set<Holder<Biome>> result = new HashSet<>(ownedBiomes);
        source.possibleBiomes()
              .stream()
              .filter(holder -> holder.unwrapKey()
                                      .map(ResourceKey::location)
                                      .map(ResourceLocation::getNamespace)
                                      .filter(ownedNamespaces::contains)
                                      .isPresent())
              .forEach(result::add);
        return result;
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

            // when we are in the nether, we want to keep the nether roof and floor rules in the beginning of the sequence
            // we will add our rules whne the first biome test sequence is found
            if (dimensionKey.equals(LevelStem.NETHER)) {
                final List<SurfaceRules.RuleSource> combined = new ArrayList<>(existingSequence.size() + additionalRules.size());
                boolean inserted = false;
                for (SurfaceRules.RuleSource rule : existingSequence) {
                    if (!inserted
                            && rule instanceof SurfaceRules.TestRuleSource testRule
                            && testRule.ifTrue() instanceof SurfaceRules.BiomeConditionSource) {
                        combined.addAll(additionalRules);
                        inserted = true;
                    }
                    combined.add(rule);
                }
                if (!inserted) {
                    combined.addAll(additionalRules);
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
                dimensionKey.location(),
                additionalRules.size(),
                sw.stop(),
                source
        );

        return new SurfaceRules.SequenceRuleSource(additionalRules);
    }

    @ApiStatus.Internal
    public static void injectNoiseBasedSurfaceRules(
            ResourceKey<LevelStem> dimensionKey,
            Holder<NoiseGeneratorSettings> noiseSettings,
            BiomeSource loadedBiomeSource
    ) {
        Object o = noiseSettings.value();
        if (o instanceof SurfaceRuleProvider srp) {
            SurfaceRules.RuleSource originalRules = srp.wover_getOriginalSurfaceRules();
            final Collection<Holder<Biome>> biomesWithWoverRules = getBiomesWithWoverSurfaceRules(loadedBiomeSource);
            final List<SurfaceRules.RuleSource> additionalRules = new LinkedList<>(getRulesForBiomes(
                    biomesWithWoverRules.stream().map(Holder::unwrapKey).toList()
            ));
            final Collection<SurfaceRules.RuleSource> compatRules = getCompatRulesForDimension(dimensionKey, loadedBiomeSource);
            if (!compatRules.isEmpty()) {
                additionalRules.addAll(0, compatRules);
            }
            srp.wover_overwriteSurfaceRules(mergeSurfaceRules(
                    dimensionKey,
                    originalRules,
                    loadedBiomeSource,
                    additionalRules
            ));
            invalidateTerraBlenderSurfaceRuleCache(noiseSettings.value());
        }
    }

    /**
     * TerraBlender decorates {@code NoiseGeneratorSettings.surfaceRule()} with a lazily-created namespaced rule.
     * Once that value has been read, replacing the underlying rule is otherwise invisible to chunk generation.
     */
    private static void invalidateTerraBlenderSurfaceRuleCache(NoiseGeneratorSettings noiseSettings) {
        try {
            Field cache = noiseSettings.getClass().getDeclaredField("namespacedSurfaceRuleSource");
            cache.setAccessible(true);
            cache.set(noiseSettings, null);
        } catch (NoSuchFieldException ignored) {
            // TerraBlender is optional.
        } catch (ReflectiveOperationException e) {
            LibWoverSurface.C.LOG.warn("Unable to invalidate TerraBlender surface-rule cache", e);
        }
    }

    static void injectSurfaceRulesToAllDimensions(
            LevelStorageSource.LevelStorageAccess ignoredStorageAccess,
            PackRepository ignoredPackRepository,
            LayeredRegistryAccess<RegistryLayer> registries,
            WorldData ignoredWorldData
    ) {
        final var registryAccess = registries.compositeAccess();
        final Registry<LevelStem> dimensionRegistry = registryAccess.registryOrThrow(Registries.LEVEL_STEM);
        LibWoverSurface.C.LOG.info(
                "Injecting surface rules into {} dimensions",
                dimensionRegistry.size()
        );

        for (var entry : dimensionRegistry.entrySet()) {
            ResourceKey<LevelStem> dimensionKey = entry.getKey();
            LevelStem stem = entry.getValue();

            if (stem.generator() instanceof InjectableSurfaceRules<?> generator) {
                generator.wover_injectSurfaceRules(dimensionRegistry, dimensionKey);
            }
        }
    }
}
