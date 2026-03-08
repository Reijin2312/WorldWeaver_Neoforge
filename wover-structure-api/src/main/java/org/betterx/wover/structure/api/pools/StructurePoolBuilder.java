package org.betterx.wover.structure.api.pools;

import org.betterx.wover.structure.api.processors.StructureProcessorKey;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

public interface StructurePoolBuilder {
    /**
     * Registers the {@link StructureTemplatePool} with the currently active
     * {@link net.minecraft.data.worldgen.BootstrapContext}.
     * <p>
     * Will fail if either the key of this Feature or the {@link net.minecraft.data.worldgen.BootstrapContext}
     * are null.
     *
     * @return the holder
     */
    @NotNull
    Holder<StructureTemplatePool> register();

    /**
     * Creates an unnamed {@link Holder} for this {@link StructurePoolBuilder}.
     * <p>
     * This method is useful, if you want to create an anonymous {@link StructureTemplatePool}
     * that is directly inlined
     *
     * @return the holder
     */
    @NotNull
    Holder<StructureTemplatePool> directHolder();

    @NotNull
    StructurePoolBuilder add(
            @NotNull Function<StructureTemplatePool.Projection, ? extends StructurePoolElement> element,
            int weight
    );

    @NotNull
    StructurePoolBuilder addFeature(@NotNull ResourceKey<PlacedFeature> feature, int weight);

    @NotNull
    StructurePoolBuilder addFeature(@NotNull Holder<PlacedFeature> feature, int weight);

    @NotNull
    StructurePoolBuilder projection(@NotNull StructureTemplatePool.Projection projection);

    @NotNull
    StructurePoolBuilder terminator(@NotNull Holder<StructureTemplatePool> terminator);

    @NotNull
    StructurePoolBuilder terminator(@NotNull ResourceKey<StructureTemplatePool> terminator);

    @NotNull
    StructurePoolBuilder terminator(@NotNull StructurePoolKey terminator);

    @NotNull
    StructurePoolBuilder emptyTerminator();

    @NotNull
    ElementBuilder startSingle(@NotNull Identifier nbtLocation);
    @NotNull
    ElementBuilder startSingleEnd(@NotNull Identifier nbtLocation);
    @NotNull
    ElementBuilder startLegacySingle(@NotNull Identifier nbtLocation);
    @NotNull
    StructurePoolBuilder addEmptyElement(int weight);

    interface ElementBuilder {
        @NotNull
        ElementBuilder processor(@NotNull Holder<StructureProcessorList> processor);
        @NotNull
        ElementBuilder processor(@NotNull ResourceKey<StructureProcessorList> processor);
        @NotNull
        ElementBuilder processor(@NotNull StructureProcessorKey processor);
        @NotNull
        ElementBuilder emptyProcessor();

        @NotNull
        ElementBuilder weight(int weight);

        @NotNull
        ElementBuilder liquidSettingsOverride(LiquidSettings value);

        @NotNull
        StructurePoolBuilder endElement();
    }
}
