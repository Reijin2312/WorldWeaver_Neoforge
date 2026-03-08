package org.betterx.wover.core.impl.registry;

import org.betterx.wover.core.api.registry.OnElementLoad;
import org.betterx.wover.util.PriorityLinkedList;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.ApiStatus;

public class DatapackLoadElementImpl {
    public static final int DEFAULT_PRIORITY = DatapackRegistryBuilderImpl.DEFAULT_PRIORITY;
    public static final int MAX_READONLY_PRIORITY = DatapackRegistryBuilderImpl.MAX_READONLY_PRIORITY;
    private static final Map<ResourceKey<Registry<?>>, PriorityLinkedList<OnElementLoad>> WATCHERS = new HashMap<>();


    public static <E> boolean isRegistered(ResourceKey<Registry<E>> registryId) {
        return WATCHERS.containsKey(registryId);
    }

    public static <E, R extends Registry<E>> PriorityLinkedList<OnElementLoad> getWatchers(ResourceKey<R> registryId) {
        ResourceKey<Registry<?>> rKey = (ResourceKey<Registry<?>>) registryId;
        return WATCHERS.computeIfAbsent(
                rKey,
                k -> new PriorityLinkedList<>()
        );
    }

    public static <E> void register(
            ResourceKey<Registry<E>> registryKey,
            OnElementLoad<E> watcher
    ) {
        getWatchers(registryKey).add(watcher, DEFAULT_PRIORITY);
    }

    public static <E> void register(
            ResourceKey<Registry<E>> registryKey,
            OnElementLoad<E> watcher,
            int priority
    ) {
        getWatchers(registryKey).add(watcher, Math.max(MAX_READONLY_PRIORITY + 1, priority));
    }

    @ApiStatus.Internal
    public static <E> void didLoadFromDatapack(
            ResourceKey<E> elementKey,
            E element
    ) {
        if (isRegistered(elementKey.registryKey())) {
            getWatchers(elementKey.registryKey())
                    .forEach(watcher -> watcher.didLoadFromDatapack(elementKey, element));
        }
    }

    /**
     * Classloader-safe variant used from mixin-injected code inside Minecraft classes.
     * It avoids directly linking Minecraft classes in the caller's classloader.
     */
    @ApiStatus.Internal
    public static void didLoadFromDatapackRaw(
            Object elementKey,
            Object element
    ) {
        if (elementKey == null || element == null) return;

        try {
            final ResourceKey<?> resourceKey = toLocalResourceKey(elementKey);
            final PriorityLinkedList<OnElementLoad> watchers = WATCHERS.get(resourceKey.registryKey());
            if (watchers == null) return;

            watchers.forEach(watcher -> invokeWatcher(watcher, resourceKey, element));
        } catch (Exception ex) {
            throw new RuntimeException("Failed to dispatch datapack load callback", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static ResourceKey<?> toLocalResourceKey(Object rawKey) throws ReflectiveOperationException {
        if (rawKey instanceof ResourceKey<?> localKey) {
            return localKey;
        }

        Object rawRegistryKey = rawKey.getClass().getMethod("registryKey").invoke(rawKey);
        Object rawRegistryId = rawRegistryKey.getClass().getMethod("identifier").invoke(rawRegistryKey);
        Object rawElementId = rawKey.getClass().getMethod("identifier").invoke(rawKey);

        ResourceKey<Registry<Object>> localRegistryKey = ResourceKey.createRegistryKey(Identifier.parse(rawRegistryId.toString()));
        return ResourceKey.create(localRegistryKey, Identifier.parse(rawElementId.toString()));
    }

    @SuppressWarnings("unchecked")
    private static void invokeWatcher(OnElementLoad watcher, ResourceKey<?> elementKey, Object element) {
        if (watcher == null) return;

        try {
            watcher.didLoadFromDatapack((ResourceKey) elementKey, element);
        } catch (RuntimeException ex) {
            throw new RuntimeException("Failed to invoke datapack load watcher " + watcher.getClass().getName(), ex);
        }
    }
}
