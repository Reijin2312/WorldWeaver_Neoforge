package org.betterx.wover.surface.impl.conditions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.FloatProviders;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class RoughNoiseConditionImpl extends WoverRoughNoiseCondition {
    public static final MapCodec<RoughNoiseConditionImpl> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(
                    ResourceKey.codec(Registries.NOISE).fieldOf("noise").forGetter(o -> o.noise),
                    Codec.DOUBLE.fieldOf("min_threshold").forGetter(o -> o.minThreshold),
                    Codec.DOUBLE.fieldOf("max_threshold").orElse(Double.MAX_VALUE).forGetter(o -> o.maxThreshold),
                    FloatProviders.CODEC.fieldOf("roughness").forGetter(o -> o.roughness)
            )
            .apply(
                    instance,
                    (noise1, minThreshold1, maxThreshold1, roughness1) -> new RoughNoiseConditionImpl(
                            noise1,
                            roughness1,
                            minThreshold1,
                            maxThreshold1
                    )
            ));

    private final ResourceKey<NormalNoise.NoiseParameters> noise;
    private final double minThreshold;
    private final double maxThreshold;
    private final FloatProvider roughness;

    public RoughNoiseConditionImpl(
            ResourceKey<NormalNoise.NoiseParameters> noise,
            FloatProvider roughness,
            double minThreshold,
            double maxThreshold
    ) {
        this.noise = noise;
        this.minThreshold = minThreshold;

        this.maxThreshold = maxThreshold;
        this.roughness = roughness;
    }

    public RoughNoiseConditionImpl(
            ResourceKey<NormalNoise.NoiseParameters> noise,
            double minThreshold,
            double maxThreshold
    ) {
        this(noise, UniformFloat.of(-0.1f, 0.4f), minThreshold, maxThreshold);
    }

    @Override
    public MapCodec<? extends SurfaceRules.ConditionSource> codec() {
        return CODEC;
    }

    @Override
    protected ResourceKey<NormalNoise.NoiseParameters> noise() {
        return noise;
    }

    @Override
    protected double minThreshold() {
        return minThreshold;
    }

    @Override
    protected double maxThreshold() {
        return maxThreshold;
    }

    @Override
    protected FloatProvider roughness() {
        return roughness;
    }
}
