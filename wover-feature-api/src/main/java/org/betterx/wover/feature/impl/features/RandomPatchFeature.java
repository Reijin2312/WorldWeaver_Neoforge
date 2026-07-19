package org.betterx.wover.feature.impl.features;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class RandomPatchFeature extends Feature<RandomPatchConfiguration> {
    public RandomPatchFeature(Codec<RandomPatchConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<RandomPatchConfiguration> context) {
        RandomPatchConfiguration config = context.config();
        RandomSource random = context.random();
        WorldGenLevel level = context.level();
        ChunkGenerator generator = context.chunkGenerator();
        BlockPos origin = context.origin();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int placed = 0;

        for (int i = 0; i < config.tries(); i++) {
            mutable.setWithOffset(
                    origin,
                    random.nextInt(config.xzSpread() + 1) - random.nextInt(config.xzSpread() + 1),
                    random.nextInt(config.ySpread() + 1) - random.nextInt(config.ySpread() + 1),
                    random.nextInt(config.xzSpread() + 1) - random.nextInt(config.xzSpread() + 1)
            );

            if (config.feature().value().place(level, generator, random, mutable)) {
                placed++;
            }
        }

        return placed > 0;
    }
}


