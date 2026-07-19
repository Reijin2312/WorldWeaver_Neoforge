package org.betterx.wover.events.mixin.client.world_registry;

import org.betterx.wover.events.api.types.OnRegistryReady;
import org.betterx.wover.events.impl.WorldLifecycleImpl;

import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelDataAndDimensions;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(value = CreateWorldScreen.class, priority = 4095)
public abstract class CreateWorldScreenMixin {
    @Shadow
    public abstract WorldCreationUiState getUiState();

    @Inject(method = "createNewWorld", at = @At("HEAD"), require = 0)
    private void wover_captureRegistry(
            LayeredRegistryAccess<RegistryLayer> layeredRegistryAccess,
            LevelDataAndDimensions.WorldDataAndGenSettings worldDataAndGenSettings,
            Optional<GameRules> gameRules,
            CallbackInfoReturnable<Boolean> cir
    ) {
        WorldLifecycleImpl.WORLD_REGISTRY_READY.emit(
                this.getUiState()
                    .getSettings()
                    .worldgenLoadContext(),
                OnRegistryReady.Stage.LOADING
        );
    }
}
