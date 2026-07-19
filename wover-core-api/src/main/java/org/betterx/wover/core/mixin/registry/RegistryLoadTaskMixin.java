package org.betterx.wover.core.mixin.registry;

import org.betterx.wover.core.impl.registry.DatapackRegistryBuilderImpl;
import org.betterx.wover.core.impl.registry.RegistryLoadTaskContextHolder;

import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryLoadTask;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(RegistryLoadTask.class)
public abstract class RegistryLoadTaskMixin<T> implements RegistryLoadTaskContextHolder {
    @Shadow
    private WritableRegistry<T> registry;

    @Unique
    private RegistryOps.RegistryInfoLookup wover_registryInfoLookup;

    @Shadow
    protected abstract ResourceKey<? extends Registry<T>> registryKey();

    @Override
    public void wover_setRegistryInfoLookup(RegistryOps.RegistryInfoLookup registryInfoLookup) {
        this.wover_registryInfoLookup = registryInfoLookup;
    }

    @Inject(method = "freezeRegistry", at = @At("HEAD"))
    private void wover_bootstrapBeforeFreeze(
            Map<ResourceKey<?>, Exception> loadingErrors,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (wover_registryInfoLookup != null) {
            DatapackRegistryBuilderImpl.bootstrap(wover_registryInfoLookup, registryKey(), registry);
        }
    }
}
