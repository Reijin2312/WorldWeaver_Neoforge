package org.betterx.wover.block.mixin;

import org.betterx.wover.block.api.BlockRegistry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockBehaviour.class)
public class BlockBehaviourMixin {
    @Inject(method = "<init>", at = @At("HEAD"))
    private static void wover$setPendingId(BlockBehaviour.Properties properties, CallbackInfo ci) {
        String idString = BlockRegistry.takeConstructionIdString();
        if (idString != null) {
            ResourceKey<Block> id = ResourceKey.create(Registries.BLOCK, Identifier.parse(idString));
            properties.setId(id);
        }
    }
}
