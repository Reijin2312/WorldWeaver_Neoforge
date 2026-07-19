package org.betterx.wover.core.mixin.registry;

import org.betterx.wover.core.impl.registry.RegistryLoadTaskContextHolder;

import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceManagerRegistryLoadTask;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ResourceManagerRegistryLoadTask.class)
public class ResourceManagerRegistryLoadTaskMixin {
    @Inject(method = "load", at = @At("HEAD"))
    private void wover_captureRegistryContext(
            RegistryOps.RegistryInfoLookup context,
            Executor executor,
            CallbackInfoReturnable<CompletableFuture<?>> cir
    ) {
        ((RegistryLoadTaskContextHolder) this).wover_setRegistryInfoLookup(context);
    }
}
