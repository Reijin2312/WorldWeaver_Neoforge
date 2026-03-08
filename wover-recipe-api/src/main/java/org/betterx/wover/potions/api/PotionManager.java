package org.betterx.wover.potions.api;

import org.betterx.wover.core.api.ModCore;
import org.betterx.wover.events.api.Event;
import org.betterx.wover.potions.impl.PotionManagerImpl;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;

public class PotionManager {
    public static final Event<OnBootstrapPotions> BOOTSTRAP_POTIONS =
            PotionManagerImpl.BOOTSTRAP_POTIONS;

    public static Holder<Potion> registerPotion(ModCore modCore, String name, Holder<MobEffect> effect, int duration) {
        return registerPotion(modCore.id(name), new Potion(modCore.namespace + "_" + name, new MobEffectInstance(effect, duration)));
    }

    public static Holder<Potion> registerPotion(Identifier id, Potion potion) {
        return Registry.registerForHolder(BuiltInRegistries.POTION, id, potion);
    }

    private PotionManager() {
    }
}
