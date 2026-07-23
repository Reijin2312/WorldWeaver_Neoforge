package org.betterx.wover.generator.api.biomesource;

import org.betterx.wover.biome.api.data.BiomeData;
import org.betterx.wover.biome.impl.modification.BiomeTagModificationWorker;
import org.betterx.wover.common.generator.api.biomesource.BiomeSourceWithNoiseRelatedSettings;
import org.betterx.wover.common.generator.api.biomesource.BiomeSourceWithSeed;
import org.betterx.wover.common.generator.api.biomesource.MergeableBiomeSource;
import org.betterx.wover.common.generator.api.biomesource.ReloadableBiomeSource;
import org.betterx.wover.common.generator.impl.compat.LithostitchedBiomeSourceCompat;
import org.betterx.wover.entrypoint.LibWoverWorldGenerator;
import org.betterx.wover.generator.impl.biomesource.WoverBiomeSourceImpl;
import org.betterx.wover.generator.impl.biomesource.nether.WoverNetherBiomeSource;
import org.betterx.wover.generator.impl.compat.ElysiumBiomeSourceCompat;
import org.betterx.wover.generator.impl.compat.IncisionBiomeSourceCompat;
import org.betterx.wover.generator.impl.compat.RegionsUnexploredBiomeConfigCompat;
import org.betterx.wover.generator.impl.compat.TerraBlenderBiomeSourceCompat;
import org.betterx.wover.state.api.WorldState;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

import com.google.common.base.Stopwatch;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class WoverBiomeSource extends BiomeSource implements
        ReloadableBiomeSource,
        BiomeSourceWithNoiseRelatedSettings,
        BiomeSourceWithSeed,
        MergeableBiomeSource<WoverBiomeSource> {
    private boolean didCreatePickers;
    private Set<Holder<Biome>> ownedPossibleBiomes;
    private Set<Holder<Biome>> externalPossibleBiomes;
    Set<Holder<Biome>> dynamicPossibleBiomes;
    @Nullable
    private BiomeSource fallbackBiomeSource;
    private Set<ResourceKey<Biome>> managedPossibleBiomeKeys;
    private Set<ResourceKey<Biome>> disabledExternalBiomeKeys;
    protected long currentSeed;
    protected int maxHeight;

    @FunctionalInterface
    public interface PickerAdder {
        boolean add(BiomeData bclBiome, TagKey<Biome> type, WoverBiomePicker picker);
    }

    @FunctionalInterface
    public interface PickerMapFactory {
        List<TagToPicker> create(Registry<BiomeData> biomeDataRegistry);
    }

    public record TagToPicker(TagKey<Biome> tag, WoverBiomePicker picker) {
    }

    public WoverBiomeSource(long seed) {
        didCreatePickers = false;
        ownedPossibleBiomes = Set.of();
        externalPossibleBiomes = Set.of();
        dynamicPossibleBiomes = Set.of();
        fallbackBiomeSource = null;
        managedPossibleBiomeKeys = Set.of();
        disabledExternalBiomeKeys = Set.of();
        currentSeed = seed;
    }

    @Override
    protected @NotNull Stream<Holder<Biome>> collectPossibleBiomes() {
        reloadBiomes();
        return dynamicPossibleBiomes.stream();
    }

    @Override
    final public void setSeed(long seed) {
        if (seed != currentSeed) {
            LibWoverWorldGenerator.C.log.debug(this.toShortString() + "\n    --> new seed = " + seed);
            this.currentSeed = seed;
            initMap(seed);
        }
    }

    /**
     * Set world height
     *
     * @param maxHeight height of the World.
     */
    final public void setMaxHeight(int maxHeight) {
        if (this.maxHeight != maxHeight) {
            LibWoverWorldGenerator.C.log.debug(this.toShortString() + "\n    --> new height = " + maxHeight);
            this.maxHeight = maxHeight;
            onHeightChange(maxHeight);
        }
    }

    protected boolean wasBound() {
        return didCreatePickers;
    }

    protected abstract List<TagKey<Biome>> acceptedTags();

    protected abstract ResourceKey<Biome> fallbackBiome();

    public abstract String toShortString();

    protected abstract void onInitMap(long newSeed);
    protected abstract void onHeightChange(int newHeight);

    protected TagKey<Biome> defaultBiomeTag() {
        return acceptedTags().get(0);
    }

    protected List<TagToPicker> createFreshPickerMap() {
        return acceptedTags().stream()
                             .map(tag -> new TagToPicker(tag, new WoverBiomePicker(fallbackBiome())))
                             .toList();
    }

    @Override
    public void onLoadGeneratorSettings(NoiseGeneratorSettings generator) {
        this.setMaxHeight(generator.noiseSettings().height());
    }

    protected void onFinishBiomeRebuild(List<TagToPicker> pickerMap) {
        for (TagToPicker tagToPicker : pickerMap) {
            tagToPicker.picker.rebuild();
        }
    }

    @NotNull
    protected String getNamespaces() {
        return WoverBiomeSourceImpl.getNamespaces(possibleBiomes());
    }

    protected TagKey<Biome> tagForUnknownBiome(
            Holder<Biome> biomeHolder,
            ResourceKey<Biome> biomeKey
    ) {
        for (TagKey<Biome> type : acceptedTags()) {
            if (biomeHolder.is(type)) {
                return type;
            }
        }
        return defaultBiomeTag();
    }

    protected boolean addToPicker(BiomeData biomeData, TagKey<Biome> type, WoverBiomePicker picker) {
        picker.addBiome(biomeData);
        return true;
    }

    private void rememberManagedPossibleBiomeKeys() {
        if (!managedPossibleBiomeKeys.isEmpty() || ownedPossibleBiomes == null || ownedPossibleBiomes.isEmpty()) {
            return;
        }

        HashSet<ResourceKey<Biome>> keys = new HashSet<>();
        for (Holder<Biome> biomeHolder : ownedPossibleBiomes) {
            biomeHolder.unwrapKey().ifPresent(keys::add);
        }
        this.managedPossibleBiomeKeys = keys;
    }

    private boolean isWoverManagedBiome(@Nullable Holder<Biome> biomeHolder) {
        if (biomeHolder == null) {
            return false;
        }

        if (biomeHolder.unwrapKey().isPresent()) {
            return managedPossibleBiomeKeys.contains(biomeHolder.unwrapKey().orElseThrow());
        }

        return ownedPossibleBiomes.contains(biomeHolder);
    }

    protected boolean isRealExternalBiome(@Nullable Holder<Biome> biomeHolder) {
        ResourceKey<Biome> key = biomeHolder == null ? null : biomeHolder.unwrapKey().orElse(null);
        if (key == null || isWoverManagedBiome(biomeHolder) || disabledExternalBiomeKeys.contains(key)) {
            return false;
        }

        Identifier id = key.identifier();
        String namespace = id.getNamespace();
        String path = id.getPath();
        if ("minecraft".equals(namespace)
                || "bclib".equals(namespace)
                || "betternether".equals(namespace)
                || "betterend".equals(namespace)
                || "wover".equals(namespace)
                || "worldweaver".equals(namespace)
                || namespace.startsWith("wover-")) {
            return false;
        }

        if ("terrablender".equals(namespace) && "deferred_placeholder".equals(path)) {
            return false;
        }

        return !path.contains("placeholder")
                && !path.contains("deferred")
                && !path.contains("internal")
                && !path.contains("technical");
    }

    protected final Holder<Biome> applyFallbackBiomeSource(
            Holder<Biome> biome,
            int biomeX,
            int biomeY,
            int biomeZ,
            Climate.Sampler sampler
    ) {
        final BiomeSource fallbackSource = this.fallbackBiomeSource;
        if (fallbackSource == null || fallbackSource == this) {
            return biome;
        }

        try {
            final Holder<Biome> fallbackBiome = fallbackSource.getNoiseBiome(biomeX, biomeY, biomeZ, sampler);
            if (isRealExternalBiome(fallbackBiome)) {
                return fallbackBiome;
            }
        } catch (Throwable ignored) {
            // If the fallback source is not ready yet, keep Wover's result.
        }

        return biome;
    }

    private void setFallbackBiomeSource(BiomeSource source) {
        if (source == this || source instanceof WoverBiomeSource) {
            return;
        }
        if (this instanceof WoverNetherBiomeSource) {
            source = IncisionBiomeSourceCompat.prepare(source);
        }
        this.fallbackBiomeSource = source;
        try {
            this.externalPossibleBiomes = Set.copyOf(source.possibleBiomes());
        } catch (RuntimeException e) {
            LibWoverWorldGenerator.C.log.warn("Unable to read external possible biomes for {}", toShortString(), e);
            this.externalPossibleBiomes = Set.of();
        }
    }

    private void updateCombinedPossibleBiomes() {
        if (externalPossibleBiomes.isEmpty()) {
            this.dynamicPossibleBiomes = ownedPossibleBiomes;
        } else {
            HashSet<Holder<Biome>> combined = new HashSet<>(ownedPossibleBiomes);
            combined.addAll(externalPossibleBiomes);
            this.dynamicPossibleBiomes = Set.copyOf(combined);
        }
        LithostitchedBiomeSourceCompat.replacePossibleBiomes(this, dynamicPossibleBiomes);
    }

    private void refreshDisabledExternalBiomeKeys() {
        this.disabledExternalBiomeKeys = RegionsUnexploredBiomeConfigCompat.disabledBiomes();
        if (!disabledExternalBiomeKeys.isEmpty()) {
            this.externalPossibleBiomes = externalPossibleBiomes.stream()
                                                                 .filter(holder -> holder.unwrapKey()
                                                                                         .map(key -> !disabledExternalBiomeKeys.contains(key))
                                                                                         .orElse(true))
                                                                 .collect(java.util.stream.Collectors.toUnmodifiableSet());
            LibWoverWorldGenerator.C.log.info(
                    "Filtered {} disabled Regions Unexplored biome(s) from the external source for {}",
                    disabledExternalBiomeKeys.size(),
                    toShortString()
            );
        }
    }

    public boolean initializeExternalBiomeSource(
            long seed,
            RegistryAccess registryAccess,
            Holder<DimensionType> dimensionType,
            ResourceKey<LevelStem> dimensionKey,
            ChunkGenerator settingsOwner
    ) {
        BiomeSource source = this.fallbackBiomeSource;
        if (source == null) {
            return false;
        }

        boolean initialized = ElysiumBiomeSourceCompat.initialize(source, seed, dimensionKey);
        initialized |= TerraBlenderBiomeSourceCompat.initialize(
                source,
                registryAccess,
                dimensionType,
                dimensionKey,
                settingsOwner,
                seed
        );
        this.externalPossibleBiomes = Set.copyOf(source.possibleBiomes());
        refreshDisabledExternalBiomeKeys();
        updateCombinedPossibleBiomes();
        LibWoverWorldGenerator.C.log.info(
                "External biome source for {}: source={}, initialized={}, possibleBiomes={}",
                dimensionKey.identifier(),
                source.getClass().getName(),
                initialized,
                externalPossibleBiomes.size()
        );
        return initialized;
    }


    protected final void rebuildBiomes(boolean force) {
        if (!force && didCreatePickers) return;

        LibWoverWorldGenerator.C.log.verbose("Updating Pickers for " + this.toShortString());

        final List<TagToPicker> pickers = createFreshPickerMap();
        this.ownedPossibleBiomes = WoverBiomeSourceImpl.populateBiomePickers(
                pickers,
                this::addToPicker
        );

        if (this.ownedPossibleBiomes == null) {
            this.ownedPossibleBiomes = Set.of();
        }
        rememberManagedPossibleBiomeKeys();
        updateCombinedPossibleBiomes();
        this.didCreatePickers = true;

        onFinishBiomeRebuild(pickers);
    }

    protected void reloadBiomes(boolean force) {
        rebuildBiomes(force);
        this.initMap(currentSeed);
    }

    @Override
    public void reloadBiomes() {
        reloadBiomes(true);
    }

    protected final void initMap(long seed) {
        LibWoverWorldGenerator.C.log.debug(this.toShortString() + "\n    --> Map Update");
        onInitMap(seed);
    }

    @Override
    public WoverBiomeSource mergeWithBiomeSource(BiomeSource inputBiomeSource) {
        if (managedPossibleBiomeKeys.isEmpty()) {
            rebuildBiomes(false);
            rememberManagedPossibleBiomeKeys();
        }
        setFallbackBiomeSource(inputBiomeSource);

        Stopwatch sw = Stopwatch.createStarted();
        RegistryAccess access = WorldState.registryAccess();
        if (access == null) {
            access = WorldState.allStageRegistryAccess();
            if (access != null) {
                LibWoverWorldGenerator.C.log.verbose("Registries were not finalized before merging biome sources!");
            } else {
                LibWoverWorldGenerator.C.log.error("Unable to merge Biome Sources");
                return this;
            }
        }
        final Registry<Biome> biomes = access.lookupOrThrow(Registries.BIOME);

        final BiomeTagModificationWorker biomeTagWorker = new BiomeTagModificationWorker();
        int biomesAdded = 0;
        try {
            for (Holder<Biome> biomeHolder : inputBiomeSource.possibleBiomes()) {
                if (biomeHolder.unwrapKey().isPresent()) {
                    final ResourceKey<Biome> key = biomeHolder.unwrapKey().orElseThrow();
                    TagKey<Biome> tag = tagForUnknownBiome(biomeHolder, key);

                    if (tag != null && !biomeHolder.is(tag)) {
                        biomeTagWorker.addBiomeToTag(tag, biomes, key, biomeHolder);
                        biomesAdded++;
                    }
                }
            }

            biomeTagWorker.finished();
        } catch (RuntimeException e) {
            LibWoverWorldGenerator.C.log.error("Error while rebuilding BiomeSources!", e);
        } catch (Exception e) {
            LibWoverWorldGenerator.C.log.error("Error while rebuilding BiomeSources!", e);
        }

        this.reloadBiomes();
        if (biomesAdded > 0) {
            LibWoverWorldGenerator.C.log.info("Merged {} biomes to {} in {}", biomesAdded, toShortString(), sw);
        }
        return this;
    }
}
