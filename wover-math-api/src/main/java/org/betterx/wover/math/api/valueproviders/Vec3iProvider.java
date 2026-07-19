package org.betterx.wover.math.api.valueproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;

public class Vec3iProvider {
    public static final Codec<Vec3iProvider> CODEC = RecordCodecBuilder.create((instance) -> instance
            .group(
                    IntProviders.CODEC.fieldOf("x").forGetter(o -> o.x),
                    IntProviders.CODEC.fieldOf("y").forGetter(o -> o.y),
                    IntProviders.CODEC.fieldOf("z").forGetter(o -> o.z)
            )
            .apply(instance, Vec3iProvider::new));

    public static Codec<Vec3iProvider> codec(int i, int j) {
        return CODEC.validate((provider) -> validate(i, j, provider));
    }

    private static DataResult<Vec3iProvider> validate(int i, int j, Vec3iProvider provider) {
        if (provider.getMinValue() < i) {
            return DataResult.error(() -> {
                return "Value provider too low: " + i + " [" + provider.getMinValue() + "-" + provider.getMaxValue() + "]";
            });
        } else {
            return provider.getMaxValue() > j ? DataResult.error(() -> {
                return "Value provider too high: " + j + " [" + provider.getMinValue() + "-" + provider.getMaxValue() + "]";
            }) : DataResult.success(provider);
        }
    }

    public final IntProvider x;
    public final IntProvider y;
    public final IntProvider z;

    public Vec3iProvider(IntProvider x, IntProvider y, IntProvider z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3i sample(RandomSource rnd) {
        return new Vec3i(x.sample(rnd), y.sample(rnd), z.sample(rnd));
    }

    public int getMinValue() {
        return Math.min(Math.min(x.minInclusive(), y.minInclusive()), z.minInclusive());
    }

    public int getMaxValue() {
        return Math.max(Math.max(x.maxInclusive(), y.maxInclusive()), z.maxInclusive());
    }
}
