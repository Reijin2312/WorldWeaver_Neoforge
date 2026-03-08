package org.betterx.wover.preset.mixin.client;

import org.betterx.wover.preset.impl.WorldPresetsManagerImpl;

import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.presets.WorldPreset;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Optional;

@Mixin(CreateWorldScreen.class)
public class CreateWorldScreenMixin {
    //Change the WorldPreset that is selected by default on the Create World Screen
    @ModifyArg(
            method = "openCreateWorldScreen",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;<init>(Lnet/minecraft/client/Minecraft;Ljava/lang/Runnable;Lnet/minecraft/client/gui/screens/worldselection/WorldCreationContext;Ljava/util/Optional;Ljava/util/OptionalLong;Lnet/minecraft/client/gui/screens/worldselection/CreateWorldCallback;)V"
            ),
            index = 3
    )
    private static Optional<ResourceKey<WorldPreset>> wover_newDefault(Optional<ResourceKey<WorldPreset>> preset) {
        return Optional.of(WorldPresetsManagerImpl.getDefault());
    }
}
