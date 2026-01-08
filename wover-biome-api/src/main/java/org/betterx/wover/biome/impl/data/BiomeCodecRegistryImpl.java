package org.betterx.wover.biome.impl.data;

import org.betterx.wover.biome.api.data.BiomeCodecRegistry;
import org.betterx.wover.biome.api.data.BiomeData;
import org.betterx.wover.core.api.registry.BuiltInRegistryManager;
import org.betterx.wover.entrypoint.LibWoverBiome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.KeyDispatchDataCodec;

import java.util.function.Function;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

public class BiomeCodecRegistryImpl {
    private static final ResourceKey<Registry<MapCodec<? extends BiomeData>>> BIOME_NETWORK_CODEC_REGISTRY =
            ResourceKey.createRegistryKey(LibWoverBiome.C.id("wover/biome_codec_network"));

    public static final Registry<MapCodec<? extends BiomeData>> BIOME_NETWORK_CODECS = BuiltInRegistryManager.createRegistry(
            BIOME_NETWORK_CODEC_REGISTRY,
            registry -> null
    );

    public static final Registry<MapCodec<? extends BiomeData>> BIOME_CODECS = BuiltInRegistryManager.createRegistry(
            BiomeCodecRegistry.BIOME_CODEC_REGISTRY,
            BiomeCodecRegistryImpl::onBootstrap
    );

    public static final Codec<BiomeData> CODEC = BIOME_CODECS
            .byNameCodec()
            .dispatch(b -> b.codec().codec(), Function.identity());

    public static final Codec<BiomeData> NETWORK_CODEC = BIOME_NETWORK_CODECS
            .byNameCodec()
            .dispatch(b -> b.networkCodec().codec(), Function.identity());

    public static MapCodec<? extends BiomeData> register(
            Registry<MapCodec<? extends BiomeData>> registry,
            ResourceLocation location,
            KeyDispatchDataCodec<? extends BiomeData> keyDispatchDataCodec
    ) {
        return register(registry, location, keyDispatchDataCodec, null);
    }

    public static MapCodec<? extends BiomeData> register(
            Registry<MapCodec<? extends BiomeData>> registry,
            ResourceLocation location,
            KeyDispatchDataCodec<? extends BiomeData> keyDispatchDataCodec,
            @Nullable KeyDispatchDataCodec<? extends BiomeData> networkKeyDispatchDataCodec
    ) {
        MapCodec<? extends BiomeData> result = BuiltInRegistryManager.register(registry, location, keyDispatchDataCodec.codec());
        KeyDispatchDataCodec<? extends BiomeData> networkCodec = networkKeyDispatchDataCodec == null
                ? keyDispatchDataCodec
                : networkKeyDispatchDataCodec;
        BuiltInRegistryManager.register(BIOME_NETWORK_CODECS, location, networkCodec.codec());
        return result;
    }

    @ApiStatus.Internal
    public static void initialize() {
        onBootstrap(BIOME_CODECS);
    }

    private static MapCodec<? extends BiomeData> onBootstrap(Registry<MapCodec<? extends BiomeData>> registry) {
        final var biomeData = LibWoverBiome.C.id("vanilla_data");
        if (registry.containsKey(biomeData)) {
            return registry.get(biomeData);
        }

        return register(registry, biomeData, BiomeData.KEY_CODEC);
    }
}
