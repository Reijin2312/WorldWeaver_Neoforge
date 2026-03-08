package org.betterx.wover.core.mixin.registry;

import org.betterx.wover.core.impl.registry.DatapackRegistryBuilderImpl;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Registries.class, priority = 200)
public class RegistryDataLoaderMixinEarly {
    // Some loaders prefix registry data paths with the mod id. We keep vanilla behavior for
    // registries managed by our DatapackRegistryBuilder.
    @Inject(method = "elementsDirPath", at = @At("RETURN"), cancellable = true)
    private static void prependDirectoryWithNamespace(
            ResourceKey<? extends Registry<?>> resourceKey,
            CallbackInfoReturnable<String> info
    ) {
        if (DatapackRegistryBuilderImpl.isRegistered(resourceKey.identifier())) {
            info.setReturnValue(info.getReturnValue());
        }
    }
}
