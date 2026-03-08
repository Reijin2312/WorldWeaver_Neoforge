package org.betterx.wover.datagen.api;

import org.betterx.wover.core.api.ModCore;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;

import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.ApiStatus;


/**
 * Handles the boostrapping as well as the serialization of a {@link Registry} to
 * a DataPack. This is a special version of {@link WoverRegistryProvider} that
 * will only serilaize the elements that were registered in the #bootstrap method.
 *
 * @param <T> The element type of the registry.
 */
public abstract class WoverRegistryContentProvider<T> extends WoverRegistryProvider<T> {
    private final List<ResourceKey<T>> content;

    /**
     * Creates a new instance of {@link WoverRegistryContentProvider}.
     *
     * @param modCore     The ModCore instance of the Mod that is providing this instance.
     * @param title       The title of the provider. Mainly used for logging.
     * @param registryKey The Key to the Registry.
     */
    public WoverRegistryContentProvider(
            ModCore modCore,
            String title,
            ResourceKey<Registry<T>> registryKey
    ) {
        super(modCore, title, registryKey);
        this.content = new LinkedList<>();
    }

    /**
     * Called, when the Elements of the Registry need to be created and registered.
     * <p>
     * Only Elements that are registered in this method
     * (using {@link BootstrapContext#register(ResourceKey, Object)} or
     * {@link BootstrapContext#register(ResourceKey, Object, Lifecycle)}) will be serialized.
     *
     * @param context The context to add the elements to.
     */
    protected abstract void bootstrap(BootstrapContext<T> context);

    private void addContent(ResourceKey<T> resourceKey) {
        content.add(resourceKey);
    }

    private void wrappedBoostrap(BootstrapContext<T> context) {
        BootstrapContext<T> wrapped = new BootstrapContext<T>() {
            @Override
            public Holder.Reference<T> register(ResourceKey<T> resourceKey, T object, Lifecycle lifecycle) {
                addContent(resourceKey);
                return context.register(resourceKey, object, lifecycle);
            }

            @Override
            public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> resourceKey) {
                return context.lookup(resourceKey);
            }
        };
        bootstrap(wrapped);
    }

    /**
     * Adds the Registry to the given {@link RegistrySetBuilder}. This method is
     * called internally by {@link WoverDataGenEntryPoint#buildRegistry(RegistrySetBuilder)}
     *
     * @param registryBuilder The builder to add the registry to.
     */
    @ApiStatus.Internal
    @Override
    public final void buildRegistry(RegistrySetBuilder registryBuilder) {
        modCore.log.info("Registering " + title);
        registryBuilder.add(registryKey, this::wrappedBoostrap);
    }

    /**
     * Gets the {@link DataProvider} that will serialize the Registry to the DataPack.
     *
     * @param output             The output to write the data to.
     * @param registriesFuture   A future sent from the data generator
     * @return The {@link DataProvider} that will serialize the Registry to the DataPack.
     */
    @ApiStatus.Internal
    @Override
    public final DataProvider getProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> registriesFuture
    ) {
        RegistrySetBuilder registryBuilder = new RegistrySetBuilder();
        buildRegistry(registryBuilder);
        return new DatapackBuiltinEntriesProvider(output, registriesFuture, registryBuilder, modIdSet());
    }
}
