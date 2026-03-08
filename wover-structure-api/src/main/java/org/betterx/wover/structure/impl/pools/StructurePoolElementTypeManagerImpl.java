package org.betterx.wover.structure.impl.pools;

import org.betterx.wover.entrypoint.LibWoverStructure;
import org.betterx.wover.legacy.api.LegacyHelper;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;

import net.neoforged.neoforge.registries.RegisterEvent;

import org.jetbrains.annotations.ApiStatus;

public class StructurePoolElementTypeManagerImpl {
    private static final java.util.Map<ResourceKey<StructurePoolElementType<?>>, StructurePoolElementType<?>> TYPES = new java.util.LinkedHashMap<>();
    public static final ResourceKey<StructurePoolElementType<?>> END_KEY = ResourceKey.create(
            Registries.STRUCTURE_POOL_ELEMENT,
            LibWoverStructure.C.id("single_end_pool_element")
    );
    public static StructurePoolElementType<SingleEndPoolElement> END;

    public static <P extends StructurePoolElement> StructurePoolElementType<P> createType(MapCodec<P> codec) {
        return () -> codec;
    }

    public static void register(RegisterEvent event) {
        event.register(Registries.STRUCTURE_POOL_ELEMENT, helper -> {
            END = createType(SingleEndPoolElement.CODEC);
            TYPES.put(END_KEY, END);
            TYPES.forEach((key, type) -> helper.register(key.identifier(), type));

            if (LegacyHelper.isLegacyEnabled()) {
                helper.register(
                        LegacyHelper.BCLIB_CORE.convertNamespace(END_KEY.identifier()),
                        createType(LegacyHelper.wrap(SingleEndPoolElement.CODEC))
                );
            }
        });
    }

    @ApiStatus.Internal
    public static void ensureStaticallyLoaded() {
        // NO-OP
    }

    public static <P extends StructurePoolElement> StructurePoolElementType<P> registerExternal(
            Identifier location,
            MapCodec<P> codec
    ) {
        ResourceKey<StructurePoolElementType<?>> key = ResourceKey.create(Registries.STRUCTURE_POOL_ELEMENT, location);
        @SuppressWarnings("unchecked")
        StructurePoolElementType<P> type = (StructurePoolElementType<P>) TYPES.computeIfAbsent(key, k -> createType(codec));
        return type;
    }
}
