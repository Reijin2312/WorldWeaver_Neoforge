package org.betterx.wover.datagen.api;

import org.betterx.wover.core.api.ModCore;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;

import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import org.jetbrains.annotations.ApiStatus;

/**
 * Handles the bootstrapping as well as the serialization of a {@link Registry} to
 * a DataPack. This version of the Registry Provider will serialize all elements
 * of the Registry, that have a valid namespace.
 *
 * @param <T> The element type of the registry.
 */
public abstract class WoverFullRegistryProvider<T> extends WoverRegistryProvider<T> {
    /**
     * The predicate to check if a namespace is valid.
     */
    public final Predicate<String> validNamespace;


    /**
     * Creates a new instance of {@link WoverFullRegistryProvider} with a {@link Predicate} to
     * determine valid namespaces
     *
     * @param modCore        The ModCore instance of the Mod that is providing this instance.
     * @param title          The title of the provider. Mainly used for logging.
     * @param registryKey    The Key to the Registry.
     * @param validNamespace The predicate to check if a namespace is valid. All elements
     *                       from the Registry with a valid namespace will be serialized.
     */
    public WoverFullRegistryProvider(
            ModCore modCore,
            String title,
            ResourceKey<Registry<T>> registryKey,
            Predicate<String> validNamespace
    ) {
        super(modCore, title, registryKey);
        this.validNamespace = validNamespace;
    }

    /**
     * Creates a new instance of {@link WoverFullRegistryProvider} with a list of valid namespaces.
     *
     * @param modCore         The ModCore instance of the Mod that is providing this instance.
     * @param title           The title of the provider. Mainly used for logging.
     * @param registryKey     The Key to the Registry.
     * @param validNamespaces A list of valid namespaces. All elements from the registry whose
     *                        namespace is contained in the list will be serialized.
     */
    public WoverFullRegistryProvider(
            ModCore modCore,
            String title,
            ResourceKey<Registry<T>> registryKey,
            List<String> validNamespaces
    ) {
        this(modCore, title, registryKey, validNamespaces::contains);
    }

    /**
     * Creates a new instance of {@link WoverFullRegistryProvider} that will only
     * allow elements with the namespace of the Mod (as determined by {@link ModCore#namespace}).
     *
     * @param modCore     The ModCore instance of the Mod that is providing this instance.
     * @param title       The title of the provider. Mainly used for logging.
     * @param registryKey The Key to the Registry.
     */
    public WoverFullRegistryProvider(
            ModCore modCore,
            String title,
            ResourceKey<Registry<T>> registryKey
    ) {
        this(modCore, title, registryKey, List.of(modCore.namespace));
    }

    /**
     * Called, when the Elements of the Registry need to be created and registered.
     * <p>
     * Which elements will be serialized is determined by the {@link #validNamespace}
     * {@link Predicate}.
     *
     * @param context The context to add the elements to.
     */
    protected abstract void bootstrap(BootstrapContext<T> context);

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
        registryBuilder.add(registryKey, this::bootstrap);
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
    public DataProvider getProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> registriesFuture
    ) {
        RegistrySetBuilder registryBuilder = new RegistrySetBuilder();
        buildRegistry(registryBuilder);
        return new DatapackBuiltinEntriesProvider(output, registriesFuture, registryBuilder, modIdSet());
    }
}
