package org.betterx.wover.feature.impl.placed.modifiers;

import org.betterx.wover.entrypoint.LibWoverFeature;
import org.betterx.wover.feature.api.placed.modifiers.*;
import org.betterx.wover.legacy.api.LegacyHelper;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import org.jetbrains.annotations.ApiStatus;
import net.neoforged.neoforge.registries.RegisterEvent;

public class PlacementModifiersImpl {
    private static final java.util.Map<Identifier, PlacementModifierType<?>> TYPES = new java.util.LinkedHashMap<>();
    private static final java.util.Map<Identifier, PlacementModifierType<?>> LEGACY_TYPES = new java.util.LinkedHashMap<>();

    public static PlacementModifierType<Stencil> STENCIL = registerLegacy(
            "stencil",
            Stencil.CODEC
    );
    public static PlacementModifierType<IsNextTo> IS_NEXT_TO = registerLegacy(
            "is_next_to",
            IsNextTo.CODEC
    );
    public static PlacementModifierType<NoiseFilter> NOISE_FILTER = registerLegacy(
            "noise_filter",
            NoiseFilter.CODEC
    );
    public static PlacementModifierType<Debug> DEBUG = registerLegacy(
            "debug",
            Debug.CODEC
    );

    public static PlacementModifierType<Merge> FOR_ALL = registerLegacy(
            "for_all",
            Merge.CODEC
    );

    public static PlacementModifierType<FindInDirection> SOLID_IN_DIR = registerLegacy(
            "solid_in_dir",
            FindInDirection.CODEC
    );

    public static PlacementModifierType<All> ALL = registerLegacy(
            "all",
            All.CODEC
    );

    public static PlacementModifierType<IsBasin> IS_BASIN = registerLegacy(
            "is_basin",
            IsBasin.CODEC
    );

    public static PlacementModifierType<Is> IS = registerLegacy(
            "is",
            Is.CODEC
    );

    public static PlacementModifierType<Offset> OFFSET = registerLegacy(
            "offset",
            Offset.CODEC
    );

    public static PlacementModifierType<OffsetProvider> OFFSET_PROVIDER = register(
            "offset_provider",
            OffsetProvider.CODEC
    );

    public static PlacementModifierType<Extend> EXTEND = registerLegacy(
            "extend",
            Extend.CODEC
    );

    public static PlacementModifierType<InBiome> IN_BIOME = registerLegacy(
            "in_biome",
            InBiome.CODEC
    );

    public static PlacementModifierType<ExtendXYZ> EXTEND_XZ = register(
            "extend_xyz",
            ExtendXYZ.CODEC
    );

    public static PlacementModifierType<EveryLayer> EVERY_LAYER = register(
            "every_layer",
            EveryLayer.CODEC
    );


    private static <P extends PlacementModifier> PlacementModifierType<P> registerLegacy(
            String path,
            MapCodec<P> codec
    ) {
        var id = LibWoverFeature.C.id(path);
        return register(id, codec, true);
    }

    private static <P extends PlacementModifier> PlacementModifierType<P> register(String path, MapCodec<P> codec) {
        var id = LibWoverFeature.C.id(path);
        return register(id, codec, false);
    }

    public static <P extends PlacementModifier> PlacementModifierType<P> register(
            Identifier location,
            MapCodec<P> codec,
            boolean withLegacyBCLib
    ) {
        PlacementModifierType<P> res = () -> codec;
        TYPES.put(location, res);

        if (withLegacyBCLib && LegacyHelper.isLegacyEnabled()) {
            PlacementModifierType<P> legacy = () -> codec;
            LEGACY_TYPES.put(LegacyHelper.BCLIB_CORE.convertNamespace(location), legacy);
        }
        return res;
    }

    @ApiStatus.Internal
    public static void ensureStaticInitialization() {

    }

    static {
        if (LegacyHelper.isLegacyEnabled()) {
            register(
                    LegacyHelper.BCLIB_CORE.id("on_every_layer"),
                    EveryLayer.CODEC,
                    false
            );
            register(
                    LegacyHelper.BCLIB_CORE.id("under_every_layer"),
                    EveryLayer.CODEC_LEGACY_UNDER,
                    false
            );
        }
    }

    public static void register(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.PLACEMENT_MODIFIER_TYPE)) {
            event.register(Registries.PLACEMENT_MODIFIER_TYPE, helper -> {
                TYPES.forEach(helper::register);
                LEGACY_TYPES.forEach(helper::register);
            });
        }
    }
}
