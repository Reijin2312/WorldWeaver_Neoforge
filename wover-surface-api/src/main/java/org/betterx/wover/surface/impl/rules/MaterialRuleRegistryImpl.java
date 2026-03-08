package org.betterx.wover.surface.impl.rules;

import org.betterx.wover.entrypoint.LibWoverSurface;
import org.betterx.wover.legacy.api.LegacyHelper;
import org.betterx.wover.surface.api.rules.MaterialRuleManager;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.SurfaceRules;

import net.neoforged.neoforge.registries.RegisterEvent;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public class MaterialRuleRegistryImpl {
    public static ResourceKey<MapCodec<? extends SurfaceRules.RuleSource>> SWITCH_RULE
            = MaterialRuleManager.createKey(LibWoverSurface.C.id("switch_rule"));

    public static ResourceKey<MapCodec<? extends SurfaceRules.RuleSource>> register(
            ResourceKey<MapCodec<? extends SurfaceRules.RuleSource>> key,
            MapCodec<? extends SurfaceRules.RuleSource> rule
    ) {
        return key;
    }

    @NotNull
    public static ResourceKey<MapCodec<? extends SurfaceRules.RuleSource>> createKey(Identifier location) {
        return ResourceKey.create(
                Registries.MATERIAL_RULE,
                location
        );
    }

    @ApiStatus.Internal
    public static void register(RegisterEvent event) {
        event.register(Registries.MATERIAL_RULE, helper -> {
            helper.register(SWITCH_RULE.identifier(), SwitchRuleSource.CODEC);

            if (LegacyHelper.isLegacyEnabled()) {
                helper.register(
                        LegacyHelper.BCLIB_CORE.convertNamespace(SWITCH_RULE.identifier()),
                        LegacyHelper.wrap(SwitchRuleSource.CODEC)
                );
            }
        });
    }
}
