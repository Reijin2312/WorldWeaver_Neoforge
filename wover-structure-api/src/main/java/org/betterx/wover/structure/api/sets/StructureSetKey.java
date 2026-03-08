package org.betterx.wover.structure.api.sets;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import org.jetbrains.annotations.NotNull;

public class StructureSetKey {
    /**
     * The key for the {@link StructureSet} you can use to reference it.
     */
    @NotNull
    public final ResourceKey<StructureSet> key;


    public StructureSetBuilder bootstrap(@NotNull BootstrapContext<StructureSet> context) {
        return new StructureSetBuilder(key, context);
    }

    StructureSetKey(@NotNull Identifier location) {
        this.key = ResourceKey.create(Registries.STRUCTURE_SET, location);
    }
}
