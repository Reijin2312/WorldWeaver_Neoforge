package org.betterx.wover.events.mixin;

import org.betterx.wover.events.api.types.OnRegistryReady;
import org.betterx.wover.events.impl.WorldLifecycleImpl;

import net.minecraft.commands.Commands;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.flag.FeatureFlagSet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ReloadableServerResources.class)
public class ReloadableServerResourcesMixin {
    @Inject(method = "loadResources", at = @At("HEAD"))
    private static void wover_onLoadResources(
            ResourceManager resourceManager,
            LayeredRegistryAccess<RegistryLayer> layeredRegistryAccess,
            List<?> pendingTags,
            FeatureFlagSet featureFlagSet,
            Commands.CommandSelection commandSelection,
            PermissionSet permissionSet,
            Executor executor,
            Executor executor2,
            CallbackInfoReturnable<CompletableFuture<ReloadableServerResources>> cir
    ) {
        WorldLifecycleImpl.WORLD_REGISTRY_READY.emit(
                layeredRegistryAccess.getAccessForLoading(RegistryLayer.RELOADABLE),
                OnRegistryReady.Stage.PREPARATION
        );
        WorldLifecycleImpl.BEFORE_LOADING_RESOURCES.emit((c) -> c.didLoad(
                resourceManager,
                featureFlagSet
        ));
    }

}
