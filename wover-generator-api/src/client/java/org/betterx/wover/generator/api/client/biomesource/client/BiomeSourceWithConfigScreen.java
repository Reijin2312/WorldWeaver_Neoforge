package org.betterx.wover.generator.api.client.biomesource.client;

import org.betterx.wover.common.generator.api.biomesource.BiomeSourceConfig;
import org.betterx.wover.common.generator.api.biomesource.BiomeSourceWithConfig;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.biome.BiomeSource;


import org.jetbrains.annotations.NotNull;


public interface BiomeSourceWithConfigScreen<B extends BiomeSource, C extends BiomeSourceConfig<B>> extends BiomeSourceWithConfig<B, C> {
    BiomeSourceConfigPanel<B, C> biomeSourceConfigPanel(
            @NotNull Screen parent
    );
}
