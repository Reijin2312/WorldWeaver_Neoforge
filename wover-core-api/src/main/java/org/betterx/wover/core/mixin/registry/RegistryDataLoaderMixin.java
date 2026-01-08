package org.betterx.wover.core.mixin.registry;

import org.betterx.wover.core.impl.registry.DatapackRegistryBuilderImpl;
import org.betterx.wover.entrypoint.LibWoverCore;

import com.mojang.serialization.Decoder;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(RegistryDataLoader.class)
public class RegistryDataLoaderMixin {
    @Accessor("WORLDGEN_REGISTRIES")
    @Mutable
    static void wt_set_WORLDGEN_REGISTRIES(List<RegistryDataLoader.RegistryData<?>> list) {
        //SHADOWED
    }

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void wover_init(CallbackInfo ci) {
        LibWoverCore.C.log.debug("Skipping WORLDGEN_REGISTRIES injection; DataPackRegistryEvent handles custom registries.");
    }

    @Inject(method = "loadContentsFromManager", at = @At("TAIL"))
    private static <E> void wover_bootstrap(
            ResourceManager resourceManager,
            RegistryOps.RegistryInfoLookup registryInfoLookup,
            WritableRegistry<E> writableRegistry,
            Decoder<E> decoder,
            Map<ResourceKey<?>, Exception> map,
            CallbackInfo ci
    ) {
        DatapackRegistryBuilderImpl.bootstrap(registryInfoLookup, writableRegistry.key(), writableRegistry);
    }

    //we moved this over to the register Method in MappedRegistryMixin to catch all registered values, even those
    //that are registered at run time and not loaded from a datapack
//    @ModifyArg(
//            method = "loadElementFromResource",
//            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/WritableRegistry;register(Lnet/minecraft/resources/ResourceKey;Ljava/lang/Object;Lnet/minecraft/core/RegistrationInfo;)Lnet/minecraft/core/Holder$Reference;")
//    )
//    private static <T> T wover_loadElementFromResource(
//            ResourceKey<T> resourceKey,
//            T value,
//            RegistrationInfo registrationInfo
//    ) {
//        DatapackLoadElementImpl.didLoadFromDatapack(resourceKey, value);
//        return value;
//    }

}
