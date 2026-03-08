package org.betterx.wover.feature.impl.placed;

import org.betterx.wover.feature.api.configured.ConfiguredFeatureKey;
import org.betterx.wover.feature.api.configured.ConfiguredFeatureManager;
import org.betterx.wover.feature.api.configured.configurators.FeatureConfigurator;
import org.betterx.wover.feature.api.placed.FeaturePlacementBuilder;
import org.betterx.wover.feature.api.placed.PlacedFeatureKey;
import org.betterx.wover.feature.impl.configured.InlineBuilderImpl;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import org.jetbrains.annotations.NotNull;


public class PlacedFeatureKeyImpl extends BaseFeatureKeyImpl<PlacedFeatureKey> implements PlacedFeatureKey {
    public PlacedFeatureKeyImpl(Identifier featureId) {
        super(featureId);
    }

    @Override
    public FeaturePlacementBuilder place(
            BootstrapContext<PlacedFeature> bootstrapContext,
            ResourceKey<ConfiguredFeature<?, ?>> key
    ) {
        return super.place(
                bootstrapContext,
                ConfiguredFeatureManager.getHolder(bootstrapContext.lookup(Registries.CONFIGURED_FEATURE), key)
        );
    }

    @Override
    public FeaturePlacementBuilder place(
            @NotNull BootstrapContext<PlacedFeature> bootstrapContext,
            Holder<ConfiguredFeature<?, ?>> holder
    ) {
        return super.place(bootstrapContext, holder);
    }

    @Override
    public <B extends FeatureConfigurator<?, ?>> FeaturePlacementBuilder place(
            @NotNull BootstrapContext<PlacedFeature> bootstrapContext,
            ConfiguredFeatureKey<B> key
    ) {
        return super.place(bootstrapContext, key.getHolder(bootstrapContext));
    }

    @Override
    public ConfiguredFeatureManager.InlineBuilder inlineConfiguration(@NotNull BootstrapContext<PlacedFeature> bootstrapContext) {
        return new InlineBuilderImpl(bootstrapContext, this.key);
    }
}
