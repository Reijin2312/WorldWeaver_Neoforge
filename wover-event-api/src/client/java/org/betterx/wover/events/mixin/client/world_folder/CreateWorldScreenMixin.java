package org.betterx.wover.events.mixin.client.world_folder;

import org.betterx.wover.events.impl.WorldLifecycleImpl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.world.level.storage.LevelStorageSource;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;
import java.util.Optional;

@Mixin(value = CreateWorldScreen.class, priority = 4090)
public abstract class CreateWorldScreenMixin {
    @Inject(method = "createNewWorldDirectory", at = @At("RETURN"))
    private static void wover_captureStorage(
            Minecraft minecraft,
            String levelName,
            Path savesDir,
            CallbackInfoReturnable<Optional<LevelStorageSource.LevelStorageAccess>> cir
    ) {
        //called when a new world is created on the client
        WorldLifecycleImpl.WORLD_FOLDER_READY.emit(cir.getReturnValue().orElse(null));
    }
}
