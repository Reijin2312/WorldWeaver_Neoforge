package org.betterx.wover.surface.impl.conditions;

import org.betterx.wover.surface.api.noise.NoiseParameterManager;
import org.betterx.wover.surface.mixin.SurfaceRulesContextAccessor;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * Rough noise condition implementation that can access package-private
 * SurfaceRules types in 1.21.1.
 */
public abstract class WoverRoughNoiseCondition implements SurfaceRules.ConditionSource {
    protected abstract ResourceKey<NormalNoise.NoiseParameters> noise();

    protected abstract double minThreshold();

    protected abstract double maxThreshold();

    protected abstract FloatProvider roughness();

    @Override
    public SurfaceRules.Condition apply(final SurfaceRules.Context context2) {
        final SurfaceRulesContextAccessor ctx = SurfaceRulesContextAccessor.class.cast(context2);
        final NormalNoise normalNoise = ctx.getRandomState().getOrCreateNoise(noise());
        final RandomSource roughnessSource = ctx.getRandomState()
                                                .getOrCreateRandomFactory(NoiseParameterManager.ROUGHNESS_NOISE.identifier())
                                                .fromHashOf(NoiseParameterManager.ROUGHNESS_NOISE.identifier());

        class NoiseThresholdCondition extends SurfaceRules.LazyCondition {
            NoiseThresholdCondition() {
                super(context2);
            }

            @Override
            protected long getContextLastUpdate() {
                final SurfaceRulesContextAccessor ctx = SurfaceRulesContextAccessor.class.cast(this.context);
                return ctx.getLastUpdateY() + ctx.getLastUpdateXZ();
            }

            @Override
            protected boolean compute() {
                double d = normalNoise
                        .getValue(
                                ctx.getBlockX(),
                                ctx.getBlockY(),
                                ctx.getBlockZ()
                        ) + roughness().sample(roughnessSource);
                return d >= minThreshold() && d <= maxThreshold();
            }
        }

        return new NoiseThresholdCondition();
    }
}
