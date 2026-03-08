package org.betterx.wover.structure.impl;

import org.betterx.wover.structure.api.StructureKey;
import org.betterx.wover.structure.api.builders.RandomNbtBuilder;
import org.betterx.wover.structure.api.structures.nbt.RandomNbtStructure;
import org.betterx.wover.structure.impl.builders.RandomNbtBuilderImpl;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import org.jetbrains.annotations.NotNull;

public class RandomNbtKeyImpl
        extends StructureKeyImpl<RandomNbtStructure, RandomNbtBuilder, StructureKey.RandomNbt>
        implements StructureKey.RandomNbt {
    public RandomNbtKeyImpl(
            @NotNull Identifier structureId
    ) {
        super(structureId);
    }

    @Override
    public RandomNbtBuilder bootstrap(BootstrapContext<Structure> context) {
        return new RandomNbtBuilderImpl(this, context);
    }

    public StructureType<RandomNbtStructure> type() {
        return StructureManagerImpl.RANDOM_NBT_STRUCTURE_TYPE;
    }
}
