package org.betterx.wover.generator.api.chunkgenerator;

import org.betterx.wover.generator.impl.chunkgenerator.ChunkGeneratorManagerImpl;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.chunk.ChunkGenerator;

public class ChunkGeneratorManager {
    public static final int CREATE_DIMENSION_CONFIG_PRIORITY = 20000;

    public static void register(Identifier location, MapCodec<ChunkGenerator> codec) {
        ChunkGeneratorManagerImpl.register(location, codec);
    }
}
