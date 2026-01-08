package org.betterx.wover.biome.impl.modification;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import org.jetbrains.annotations.Nullable;

public class FeatureMap extends ArrayList<LinkedList<Holder<PlacedFeature>>> {
    public static final Codec<List<List<Holder<PlacedFeature>>>> CODEC = PlacedFeature.CODEC.listOf().listOf();
    public static final Codec<List<List<ResourceKey<PlacedFeature>>>> NETWORK_CODEC =
            ResourceKey.codec(Registries.PLACED_FEATURE).listOf().listOf();

    @Nullable
    private List<List<ResourceKey<PlacedFeature>>> keyFeatures;
    private boolean resolved;

    public void addFeature(GenerationStep.Decoration decoration, Holder<PlacedFeature> feature) {
        this.getFeatures(decoration).add(feature);
    }

    public List<Holder<PlacedFeature>> getFeatures(GenerationStep.Decoration decoration) {
        final int index = decoration.ordinal();

        while (this.size() <= index) {
            this.add(new LinkedList<>());
        }

        return this.get(index);
    }

    public static HolderSet<PlacedFeature> getFeatures(
            List<HolderSet<PlacedFeature>> features,
            GenerationStep.Decoration decoration
    ) {
        final int index = decoration.ordinal();

        while (features.size() <= index) {
            features.add(HolderSet.direct(Collections.emptyList()));
        }

        return features.get(index);
    }

    public void forEach(BiConsumer<GenerationStep.Decoration, Holder<PlacedFeature>> consumer) {
        for (int i = 0; i < this.size(); i++) {
            final GenerationStep.Decoration decoration = GenerationStep.Decoration.values()[i];
            for (final Holder<PlacedFeature> feature : getFeatures(decoration)) {
                consumer.accept(decoration, feature);
            }
        }
    }

    public List<List<Holder<PlacedFeature>>> generic() {
        @SuppressWarnings("unchecked") final List<List<Holder<PlacedFeature>>> res = (List<List<Holder<PlacedFeature>>>) (List<?>) this;
        return res;
    }

    public static FeatureMap of(List<List<Holder<PlacedFeature>>> features) {
        FeatureMap map = new FeatureMap();
        for (final List<Holder<PlacedFeature>> list : features) {
            if (list instanceof LinkedList lList) {
                map.add(lList);
            } else {
                final LinkedList<Holder<PlacedFeature>> nList = new LinkedList<>(list);
                map.add(nList);
            }
        }
        return map;
    }

    public static FeatureMap ofKeys(List<List<ResourceKey<PlacedFeature>>> features) {
        FeatureMap map = new FeatureMap();
        map.keyFeatures = features;
        if (features != null) {
            for (int i = 0; i < features.size(); i++) {
                map.add(new LinkedList<>());
            }
        }
        return map;
    }

    public List<List<ResourceKey<PlacedFeature>>> keys() {
        if (keyFeatures != null) {
            return keyFeatures;
        }
        List<List<ResourceKey<PlacedFeature>>> keys = new ArrayList<>(this.size());
        for (int i = 0; i < this.size(); i++) {
            LinkedList<Holder<PlacedFeature>> list = this.get(i);
            List<ResourceKey<PlacedFeature>> keyList = new ArrayList<>(list.size());
            for (Holder<PlacedFeature> holder : list) {
                holder.unwrapKey().ifPresent(keyList::add);
            }
            keys.add(keyList);
        }
        return keys;
    }

    public void resolve(Registry<PlacedFeature> registry) {
        if (resolved || keyFeatures == null) {
            return;
        }
        for (int i = 0; i < keyFeatures.size(); i++) {
            List<ResourceKey<PlacedFeature>> keys = keyFeatures.get(i);
            while (this.size() <= i) {
                this.add(new LinkedList<>());
            }
            LinkedList<Holder<PlacedFeature>> holders = this.get(i);
            if (!holders.isEmpty()) {
                continue;
            }
            for (ResourceKey<PlacedFeature> key : keys) {
                registry.getHolder(key).ifPresent(holders::add);
            }
        }
        resolved = true;
    }
}
