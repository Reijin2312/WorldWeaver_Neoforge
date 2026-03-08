package org.betterx.wover.feature.impl;

import org.betterx.wover.entrypoint.LibWoverFeature;
import org.betterx.wover.feature.api.features.*;
import org.betterx.wover.feature.api.features.config.*;
import org.betterx.wover.legacy.api.LegacyHelper;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.function.Function;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import net.neoforged.neoforge.registries.RegisterEvent;

public class FeatureManagerImpl {
    private static final Map<ResourceKey<Feature<?>>, Feature<?>> FEATURES = new LinkedHashMap<>();


    public static <C extends FeatureConfiguration, F extends Feature<C>> F register(
            @NotNull Identifier id,
            @NotNull F feature
    ) {
        return register(createKey(id), feature);
    }

    public static <C extends FeatureConfiguration, F extends Feature<C>> F register(
            @NotNull ResourceKey<Feature<?>> key,
            @NotNull F feature
    ) {
        FEATURES.putIfAbsent(key, feature);
        return feature;
    }

    private static <C extends FeatureConfiguration, F extends Feature<C>> F registerWithLegacy(
            @NotNull Identifier id,
            @NotNull Function<Codec<C>, F> feature,
            Codec<C> codec
    ) {
        final var key = createKey(id);
        F res = register(key, feature.apply(codec));
        if (LegacyHelper.isLegacyEnabled()) {
            register(LegacyHelper.BCLIB_CORE.convertNamespace(key.identifier()), feature.apply(codec));
        }
        return res;
    }

    @NotNull
    public static ResourceKey<Feature<?>> createKey(Identifier location) {
        return ResourceKey.create(
                Registries.FEATURE,
                location
        );
    }

    public static final Feature<PlaceFacingBlockConfig> PLACE_BLOCK = registerWithLegacy(
            LibWoverFeature.C.id("place_block"),
            PlaceBlockFeature::new,
            PlaceFacingBlockConfig.CODEC
    );


    public static final Feature<NoneFeatureConfiguration> MARK_POSTPROCESSING = registerWithLegacy(
            LibWoverFeature.C.id("mark_postprocessing"),
            (codec) -> new MarkPostProcessingFeature(),
            null
    );

    public static final Feature<SequenceFeatureConfig> SEQUENCE = registerWithLegacy(
            LibWoverFeature.C.id("sequence"),
            (codec) -> new SequenceFeature(),
            null
    );

    public static final Feature<ConditionFeatureConfig> CONDITION = registerWithLegacy(
            LibWoverFeature.C.id("condition"),
            codec -> new ConditionFeature(),
            null
    );

    public static final Feature<PillarFeatureConfig> PILLAR = registerWithLegacy(
            LibWoverFeature.C.id("pillar"),
            codec -> new PillarFeature(),
            null
    );

    public static final Feature<TemplateFeatureConfig> TEMPLATE = registerWithLegacy(
            LibWoverFeature.C.id("template"),
            TemplateFeature::new,
            TemplateFeatureConfig.CODEC
    );

    public static void register(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.FEATURE)) {
            event.register(Registries.FEATURE, helper -> FEATURES.forEach((k, v) -> helper.register(k.identifier(), v)));
        }
    }

    @ApiStatus.Internal
    public static void ensureStaticInitialization() {
        // no-op
    }


}
