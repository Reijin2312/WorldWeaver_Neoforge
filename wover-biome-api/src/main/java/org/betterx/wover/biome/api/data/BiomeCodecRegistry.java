package org.betterx.wover.biome.api.data;

import org.betterx.wover.biome.impl.data.BiomeCodecRegistryImpl;
import org.betterx.wover.core.api.registry.DatapackRegistryBuilder;
import org.betterx.wover.entrypoint.LibWoverSurface;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.KeyDispatchDataCodec;

import org.jetbrains.annotations.Nullable;

public class BiomeCodecRegistry {
    public static final ResourceKey<Registry<MapCodec<? extends BiomeData>>> BIOME_CODEC_REGISTRY =
            DatapackRegistryBuilder.createRegistryKey(LibWoverSurface.C.id("wover/biome_codec"));

    public static final Registry<MapCodec<? extends BiomeData>> BIOME_CODECS = BiomeCodecRegistryImpl.BIOME_CODECS;

    public static MapCodec<? extends BiomeData> register(
            ResourceLocation location,
            KeyDispatchDataCodec<? extends BiomeData> keyDispatchDataCodec
    ) {
        return BiomeCodecRegistryImpl.register(BiomeCodecRegistryImpl.BIOME_CODECS, location, keyDispatchDataCodec);
    }

    public static MapCodec<? extends BiomeData> register(
            ResourceLocation location,
            KeyDispatchDataCodec<? extends BiomeData> keyDispatchDataCodec,
            @Nullable KeyDispatchDataCodec<? extends BiomeData> networkKeyDispatchDataCodec
    ) {
        return BiomeCodecRegistryImpl.register(
                BiomeCodecRegistryImpl.BIOME_CODECS,
                location,
                keyDispatchDataCodec,
                networkKeyDispatchDataCodec
        );
    }
}
