package org.betterx.wover.data_components;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.function.UnaryOperator;

public class DataComponentManager {
    public static <T> DataComponentType<T> registerDataComponent(
            Identifier id,
            UnaryOperator<DataComponentType.Builder<T>> builder
    ) {
        return Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                id,
                builder.apply(DataComponentType.builder()).build()
        );
    }

    public static Holder<Attribute> registerAttribute(Identifier string, Attribute attribute) {
        return Registry.registerForHolder(BuiltInRegistries.ATTRIBUTE, string, attribute);
    }
}
