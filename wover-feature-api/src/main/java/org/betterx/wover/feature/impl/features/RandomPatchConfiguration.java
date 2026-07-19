package org.betterx.wover.feature.impl.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.stream.Stream;

public record RandomPatchConfiguration(
        int tries,
        int xzSpread,
        int ySpread,
        Holder<PlacedFeature> feature
) implements FeatureConfiguration {
    public static final Codec<RandomPatchConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    Codec.INT.fieldOf("tries").forGetter(RandomPatchConfiguration::tries),
                    Codec.INT.fieldOf("xz_spread").forGetter(RandomPatchConfiguration::xzSpread),
                    Codec.INT.fieldOf("y_spread").forGetter(RandomPatchConfiguration::ySpread),
                    PlacedFeature.CODEC.fieldOf("feature").forGetter(RandomPatchConfiguration::feature)
            )
            .apply(instance, RandomPatchConfiguration::new));

    @Override
    public Stream<Holder<ConfiguredFeature<?, ?>>> getSubFeatures() {
        return feature.value().getFeatures();
    }
}


