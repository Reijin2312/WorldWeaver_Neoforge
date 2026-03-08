package org.betterx.wover.item.mixin;

import org.betterx.wover.item.api.ItemRegistry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Item.class)
public class ItemConstructorMixin {
    @Inject(method = "<init>", at = @At("HEAD"))
    private static void wover$setPendingId(Item.Properties properties, CallbackInfo ci) {
        String idString = ItemRegistry.takeConstructionIdString();
        if (idString != null) {
            ResourceKey<Item> id = ResourceKey.create(Registries.ITEM, Identifier.parse(idString));
            properties.setId(id);
        }
    }
}
