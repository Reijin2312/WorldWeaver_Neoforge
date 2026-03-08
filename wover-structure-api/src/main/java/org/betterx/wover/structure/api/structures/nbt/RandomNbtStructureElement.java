package org.betterx.wover.structure.api.structures.nbt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

public record RandomNbtStructureElement(Identifier nbtLocation, int yOffset) {
    public static final Codec<RandomNbtStructureElement> CODEC =
            RecordCodecBuilder.create((instance) ->
                    instance.group(
                                    Identifier.CODEC
                                            .fieldOf("location")
                                            .forGetter((cfg) -> cfg.nbtLocation),
                                    Codec
                                            .INT
                                            .fieldOf("offset_y")
                                            .orElse(0)
                                            .forGetter((cfg) -> cfg.yOffset)
                            )
                            .apply(instance, RandomNbtStructureElement::new)
            );
}
