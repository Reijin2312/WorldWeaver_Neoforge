package org.betterx.wover.structure.api.builders;

import org.betterx.wover.structure.api.structures.StructurePlacement;
import org.betterx.wover.structure.api.structures.nbt.RandomNbtStructure;

import net.minecraft.resources.Identifier;

public interface RandomNbtBuilder extends BaseStructureBuilder<RandomNbtStructure, RandomNbtBuilder> {
    RandomNbtBuilder placement(StructurePlacement value);
    RandomNbtBuilder keepAir(boolean value);

    RandomNbtBuilder addElement(Identifier elementId, int yOffset, double weight);
}
