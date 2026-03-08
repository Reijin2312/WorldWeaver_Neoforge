package org.betterx.wover.events.mixin.client.create_new_world_folder;

import org.betterx.wover.events.impl.WorldLifecycleImpl;
import org.betterx.wover.state.api.WorldState;

import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.world.level.storage.WorldData;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CreateWorldScreen.class, priority = 5000)
public abstract class CreateWorldScreenMixin {
    @Shadow
    public abstract WorldCreationUiState getUiState();

    @Shadow
    private boolean recreated;

    //this is called when a new world is first created
    @Inject(method = "createNewWorld", at = @At("RETURN"), require = 0)
    private void wover_createNewWorld(
            LayeredRegistryAccess<RegistryLayer> layeredRegistryAccess,
            WorldData worldData,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }

        WorldLifecycleImpl.CREATED_NEW_WORLD_FOLDER.emit(c -> c.init(
                        WorldState.storageAccess(),
                        this.getUiState().getSettings().worldgenLoadContext(),
                        this.getUiState().getWorldType().preset(),
                        this.getUiState().getSettings().selectedDimensions(),
                        this.recreated
                )
        );
    }
}
