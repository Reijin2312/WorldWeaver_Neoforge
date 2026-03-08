package org.betterx.wover.enchantment.api;

import org.betterx.wover.enchantment.impl.EnchantmentKeyImpl;
import org.betterx.wover.enchantment.impl.EnchantmentManagerImpl;
import org.betterx.wover.events.api.Event;
import org.betterx.wover.events.api.types.OnBootstrapRegistry;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.function.UnaryOperator;

public class EnchantmentManager {
    public static final Event<OnBootstrapRegistry<Enchantment>> BOOTSTRAP_ENCHANTMENTS = EnchantmentManagerImpl.BOOTSTRAP_ENCHANTMENTS;

    public static EnchantmentKey createKey(Identifier id) {
        return new EnchantmentKeyImpl(ResourceKey.create(Registries.ENCHANTMENT, id));
    }

    public static <T> DataComponentType<T> registerEffectComponent(
            Identifier id,
            UnaryOperator<DataComponentType.Builder<T>> unaryOperator
    ) {
        return Registry.register(BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE, id, unaryOperator
                .apply(DataComponentType.builder())
                .build());
    }
}
