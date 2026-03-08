package org.betterx.wover.surface.impl.conditions;

import org.betterx.wover.entrypoint.LibWoverSurface;
import org.betterx.wover.legacy.api.LegacyHelper;
import org.betterx.wover.surface.api.conditions.ConditionManager;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.SurfaceRules;

import net.neoforged.neoforge.registries.RegisterEvent;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public class MaterialConditionRegistryImpl {
    public static final ResourceKey<MapCodec<? extends SurfaceRules.ConditionSource>> THRESHOLD_CONDITION
            = ConditionManager.createKey(LibWoverSurface.C.id("threshold_condition"));
    public static final ResourceKey<MapCodec<? extends SurfaceRules.ConditionSource>> VOLUME_THRESHOLD_CONDITION
            = ConditionManager.createKey(LibWoverSurface.C.id("volume_threshold_condition"));
    public static final ResourceKey<MapCodec<? extends SurfaceRules.ConditionSource>> ROUGH_NOISE_CONDITION
            = ConditionManager.createKey(LibWoverSurface.C.id("rough_noise_condition"));

    public static ResourceKey<MapCodec<? extends SurfaceRules.ConditionSource>> register(
            ResourceKey<MapCodec<? extends SurfaceRules.ConditionSource>> key,
            MapCodec<? extends SurfaceRules.ConditionSource> codec,
            boolean withBCLibLegacy
    ) {
        return key;
    }

    @NotNull
    public static ResourceKey<MapCodec<? extends SurfaceRules.ConditionSource>> createKey(Identifier location) {
        return ResourceKey.create(
                Registries.MATERIAL_CONDITION,
                location
        );
    }

    @ApiStatus.Internal
    public static void register(RegisterEvent event) {
        event.register(Registries.MATERIAL_CONDITION, helper -> {
            helper.register(THRESHOLD_CONDITION.identifier(), ThresholdConditionImpl.CODEC);
            helper.register(VOLUME_THRESHOLD_CONDITION.identifier(), VolumeThresholdConditionImpl.CODEC);
            helper.register(ROUGH_NOISE_CONDITION.identifier(), RoughNoiseConditionImpl.CODEC);

            if (LegacyHelper.isLegacyEnabled()) {
                helper.register(
                        LegacyHelper.BCLIB_CORE.convertNamespace(THRESHOLD_CONDITION.identifier()),
                        LegacyHelper.wrap(ThresholdConditionImpl.CODEC)
                );
                helper.register(
                        LegacyHelper.BCLIB_CORE.convertNamespace(VOLUME_THRESHOLD_CONDITION.identifier()),
                        LegacyHelper.wrap(VolumeThresholdConditionImpl.CODEC)
                );
                helper.register(
                        LegacyHelper.BCLIB_CORE.convertNamespace(ROUGH_NOISE_CONDITION.identifier()),
                        LegacyHelper.wrap(RoughNoiseConditionImpl.CODEC)
                );
            }
        });
    }
}
