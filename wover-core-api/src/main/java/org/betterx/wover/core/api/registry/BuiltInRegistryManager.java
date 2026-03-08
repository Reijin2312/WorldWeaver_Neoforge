package org.betterx.wover.core.api.registry;

import org.betterx.wover.entrypoint.LibWoverCore;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;

import java.util.function.Function;

public class BuiltInRegistryManager {
    public static <V, T extends V> T register(Registry<V> registry, Identifier Identifier, T object) {
        return Registry.register(registry, Identifier, object);
    }

    public static <V, T extends V> T register(Registry<V> registry, ResourceKey<V> resourceKey, T object) {
        return Registry.register(registry, resourceKey, object);
    }

    public static <V, T extends V> Holder.Reference<V> registerForHolder(
            Registry<V> registry,
            Identifier Identifier,
            T object
    ) {
        return Registry.registerForHolder(registry, Identifier, object);
    }

    public static <T> Registry<T> createRegistry(
            ResourceKey<? extends Registry<T>> resourceKey,
            Function<Registry<T>, T> registryBootstrap
    ) {
        LibWoverCore.C.log.debug("Creating registry: " + resourceKey.identifier());
        MappedRegistry<T> registry = new MappedRegistry<>(resourceKey, Lifecycle.stable(), false);
        registryBootstrap.apply(registry);
        return registry;
    }

    @Deprecated
    public static <T> Registry<T> createRegistry(
            ResourceKey<? extends Registry<T>> resourceKey,
            Lifecycle lifecycle,
            Function<Registry<T>, T> registryBootstrap
    ) {
        LibWoverCore.C.log.debug("Creating registry: " + resourceKey.identifier());
        return createRegistry(resourceKey, registryBootstrap);
    }
}
