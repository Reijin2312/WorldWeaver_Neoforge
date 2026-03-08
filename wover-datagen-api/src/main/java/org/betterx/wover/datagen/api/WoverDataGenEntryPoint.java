package org.betterx.wover.datagen.api;

import org.betterx.wover.core.api.ModCore;
import org.betterx.wover.datagen.impl.WoverDataGenEntryPointImpl;
import org.betterx.wover.entrypoint.LibWoverDatagen;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.metadata.PackMetadataGenerator;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.registries.RegistryPatchGenerator;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;

import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * A simplified entrypoint for NeoForge data generators with support for multiple datapacks
 * and registry providers.
 */
public abstract class WoverDataGenEntryPoint {
    /**
     * Creates a new {@link WoverDataGenEntryPoint}.
     */
    protected WoverDataGenEntryPoint() {
    }

    private List<PackBuilder> builders = null;
    private PackBuilder globalBuilder = null;
    private static final Map<Path, RegistryPackContext> REGISTRY_CONTEXTS = new HashMap<>();
    private static Path registryContextRoot = null;

    /**
     * Creates a new {@link PackBuilder} for an additional Datapack.
     *
     * @param location The {@link Identifier} of the Datapack
     * @return The new {@link PackBuilder} for the Datapack
     */
    protected PackBuilder addDatapack(@Nullable Identifier location) {
        PackBuilder res = new PackBuilder(modCore(), location);
        builders.add(res);
        return res;
    }

    /**
     * Called when the Datagenerator is initialized. This is the place where you should
     * register your Registry Providers to the {@code globalPack} or add {@link PackBuilder}s
     * for additional Datapacks.
     *
     * @param globalPack The {@link PackBuilder} for the global Datapack
     * @see #addDatapack(Identifier)
     */
    protected abstract void onInitializeProviders(PackBuilder globalPack);

    /**
     * Returns the {@link ModCore} instance that is responsible for the
     * Datagenerator. This is used to get the namespace and the logger of
     * the mod.
     *
     * @return The {@link ModCore} instance
     */
    protected abstract ModCore modCore();

    private void initialize() {
        synchronized (this) {
            if (builders == null) {
                LibWoverDatagen.C.LOG.debug("Initializing WoverDataGenEntryPoint:" +
                        this.getClass().getName() + " for " + modCore());
                this.builders = new LinkedList<>();
                this.globalBuilder = addDatapack(null);

                // call the custom providers for the global Datapack
                addDefaultGlobalProviders(this.globalBuilder);

                // run mod specific init
                onInitializeProviders(this.globalBuilder);
            }
        }
    }

    /**
     * Entry point for NeoForge datagen. Register this from the mod event bus.
     *
     * @param event The {@link GatherDataEvent}
     */
    @ApiStatus.Internal
    public final void onGatherData(GatherDataEvent event) {
        if (ignoreRun(event)) {
            LibWoverDatagen.C.LOG.debug("Ignoring run for " + this);
            return;
        }
        initialize();

        final PackOutput baseOutput = event.getGenerator().getPackOutput();

        for (PackBuilder builder : builders) {
            PackOutput packOutput = createPackOutput(event, baseOutput, builder.location);
            builder.pack(packOutput);

            RegistryPackContext registryContext = getRegistryContext(event, packOutput);
            final List<? extends WoverRegistryProvider<?>> registryProviders = builder.registryProviders();
            if (!registryProviders.isEmpty()) {
                registryContext.addModCore(builder.modCore);
                registryContext.addRegistryProviders(registryProviders);
                onBuildRegistry(registryContext.registryBuilder);
                if (!registryContext.providerAdded) {
                    event.addProvider(registryContext.createProvider());
                    registryContext.providerAdded = true;
                }
            }
            CompletableFuture<HolderLookup.Provider> registryLookup = registryContext.registryLookup();

            List<WoverLootProvider> lootProviders = new ArrayList<>();
            List<WoverRecipeGenerator> recipeGenerators = new ArrayList<>();
            for (WoverDataProvider<?> provider : builder.providerFactories()) {
                if (provider instanceof WoverRegistryProvider<?>) {
                    continue;
                }
                if (provider instanceof WoverLootProvider lootProvider) {
                    lootProviders.add(lootProvider);
                    continue;
                }
                if (provider instanceof WoverRecipeGenerator recipeGenerator) {
                    recipeGenerators.add(recipeGenerator);
                    continue;
                }
                event.addProvider(provider.getProvider(packOutput, registryLookup));
                addMultiProviders(event, provider, packOutput, registryLookup);
            }

            if (!lootProviders.isEmpty()) {
                final List<LootTableProvider.SubProviderEntry> entries = lootProviders
                        .stream()
                        .map(WoverLootProvider::toSubProviderEntry)
                        .toList();
                event.addProvider(new LootTableProvider(
                        packOutput,
                        Set.of(),
                        entries,
                        registryLookup
                ));
            }

            if (!recipeGenerators.isEmpty()) {
                final List<WoverRecipeGenerator> generators = List.copyOf(recipeGenerators);
                final String recipeName = builder.modCore.modId + " Recipes" +
                        (builder.location != null ? " [" + builder.location + "]" : "");
                RecipeProvider.Runner delegate = new RecipeProvider.Runner(packOutput, registryLookup) {
                    @Override
                    public String getName() {
                        return recipeName;
                    }

                    @Override
                    protected RecipeProvider createRecipeProvider(HolderLookup.Provider lookup, RecipeOutput output) {
                        return new RecipeProvider(lookup, output) {
                            @Override
                            protected void buildRecipes() {
                                generators.forEach(gen -> gen.buildRecipes(lookup, output));
                            }
                        };
                    }
                };
                event.addProvider(new NamedDataProvider(recipeName, delegate));
            }

            if (builder.datapackBootstrap != null) {
                builder.datapackBootstrap.bootstrap(event, packOutput, builder.location);
            }
        }
    }

    private record NamedDataProvider(String name, net.minecraft.data.DataProvider delegate)
            implements net.minecraft.data.DataProvider {
        @Override
        public CompletableFuture<?> run(CachedOutput output) {
            return delegate.run(output);
        }

        @Override
        public String getName() {
            return name;
        }
    }

    private PackOutput createPackOutput(GatherDataEvent event, PackOutput baseOutput, @Nullable Identifier location) {
        if (location == null) {
            return baseOutput;
        }
        final Path root = baseOutput
                .getOutputFolder()
                .resolve("data")
                .resolve(location.getNamespace())
                .resolve("datapacks")
                .resolve(location.getPath());
        final PackOutput packOutput = new PackOutput(root);
        final DataProvider packMetaProvider = PackMetadataGenerator.forFeaturePack(
                packOutput,
                Component.translatable("pack." + location.getNamespace() + "." + location.getPath() + ".description")
        );
        event.addProvider(new NamedDataProvider(
                "Pack Metadata [" + location + "]",
                packMetaProvider
        ));
        return packOutput;
    }

    private static RegistryPackContext getRegistryContext(GatherDataEvent event, PackOutput packOutput) {
        Path root = event.getGenerator().getPackOutput().getOutputFolder();
        if (registryContextRoot == null || !registryContextRoot.equals(root)) {
            REGISTRY_CONTEXTS.clear();
            registryContextRoot = root;
        }
        Path key = packOutput.getOutputFolder();
        return REGISTRY_CONTEXTS.computeIfAbsent(
                key,
                path -> new RegistryPackContext(packOutput, event.getLookupProvider())
        );
    }

    private static void addMultiProviders(
            GatherDataEvent event,
            WoverDataProvider<?> provider,
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> registriesFuture
    ) {
        if (provider instanceof WoverDataProvider.Secondary<?> wpp) {
            event.addProvider(wpp.getSecondaryProvider(output, registriesFuture));
        }
        if (provider instanceof WoverDataProvider.Tertiary<?> wpp) {
            event.addProvider(wpp.getTertiaryProvider(output, registriesFuture));
        }
    }

    private void addDefaultGlobalProviders(PackBuilder globalPack) {
        WoverDataGenEntryPointImpl.addDefaultGlobalProviders(globalPack);
    }

    /**
     * Called when the Registry set is built for a pack. This is the place where you can add
     * custom Registry bootstrap methods to the Datagenerator.
     *
     * @param registryBuilder The {@link RegistrySetBuilder} instance
     */
    protected void onBuildRegistry(RegistrySetBuilder registryBuilder) {
    }

    /**
     * Register an automatic provider that is automatically added to all global packs.
     *
     * @param providerFactory The {@link PackBuilder.ProviderFactory} to register
     * @param <T>             The type of the provider
     */
    public static <T extends DataProvider> void registerAutoProvider(PackBuilder.ProviderFactory<T> providerFactory) {
        WoverDataGenEntryPointImpl.registerAutoProvider(providerFactory);
    }

    /**
     * Returns whether the Datagenerator should be run for the given event.
     *
     * @return Whether the Datagenerator should be run
     */
    protected boolean ignoreRun(GatherDataEvent event) {
        return !runsForMod(modCore(), Set.of(event.getModContainer().getModId()));
    }

    /**
     * Returns whether the Datagenerator was started by the Mod specified in the
     * passed {@link ModCore} instance.
     *
     * @param modCore The {@link ModCore} instance to check
     * @param mods    The set of mod ids requested by datagen
     * @return Whether the Datagenerator was started by the Mod
     */
    public static boolean runsForMod(ModCore modCore, Set<String> mods) {
        if (mods == null || mods.isEmpty()) return true;
        return mods.contains(modCore.modId) || mods.contains(modCore.namespace);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " for " + modCore();
    }

    private static final class RegistryPackContext {
        private final PackOutput packOutput;
        private final RegistrySetBuilder registryBuilder = new RegistrySetBuilder();
        private final Map<ResourceKey<?>, RegistryBootstrapGroup> bootstrapGroups = new LinkedHashMap<>();
        private final CompletableFuture<HolderLookup.Provider> baseLookup;
        @Nullable
        private CompletableFuture<RegistrySetBuilder.PatchedRegistries> patchedRegistries = null;
        @Nullable
        private CompletableFuture<HolderLookup.Provider> registryLookup = null;
        private final Set<String> modIds = new HashSet<>();
        private boolean providerAdded = false;

        private RegistryPackContext(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> baseLookup) {
            this.packOutput = packOutput;
            this.baseLookup = baseLookup;
        }

        private synchronized CompletableFuture<RegistrySetBuilder.PatchedRegistries> patchedRegistries() {
            if (patchedRegistries == null) {
                patchedRegistries = RegistryPatchGenerator.createLookup(baseLookup, registryBuilder);
            }
            return patchedRegistries;
        }

        private synchronized CompletableFuture<HolderLookup.Provider> registryLookup() {
            if (registryLookup == null) {
                registryLookup = patchedRegistries().thenApply(RegistrySetBuilder.PatchedRegistries::full);
            }
            return registryLookup;
        }

        private void addModCore(ModCore modCore) {
            modIds.add(modCore.modId);
            if (!modCore.modId.equals(modCore.namespace)) {
                modIds.add(modCore.namespace);
            }
        }

        private void addRegistryProviders(List<? extends WoverRegistryProvider<?>> registryProviders) {
            RegistrySetBuilderCollector collector = new RegistrySetBuilderCollector();
            registryProviders.forEach(provider -> provider.buildRegistry(collector));
            collector.entries().forEach(this::addBootstraps);
        }

        private void addBootstraps(
                ResourceKey<?> registryKey,
                List<RegistryBootstrapEntry<?>> entries
        ) {
            for (RegistryBootstrapEntry<?> entry : entries) {
                addBootstrap(registryKey, entry.lifecycle, entry.bootstrap);
            }
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private void addBootstrap(
                ResourceKey registryKey,
                @Nullable Lifecycle lifecycle,
                RegistrySetBuilder.RegistryBootstrap bootstrap
        ) {
            RegistryBootstrapGroup group = bootstrapGroups.get(registryKey);
            if (group == null) {
                RegistryBootstrapGroup newGroup = new RegistryBootstrapGroup(lifecycle);
                bootstrapGroups.put(registryKey, newGroup);
                RegistrySetBuilder.RegistryBootstrap combined = context -> {
                    for (RegistrySetBuilder.RegistryBootstrap entry : newGroup.bootstraps) {
                        entry.run(context);
                    }
                };
                if (newGroup.lifecycle != null) {
                    registryBuilder.add(registryKey, newGroup.lifecycle, combined);
                } else {
                    registryBuilder.add(registryKey, combined);
                }
                group = newGroup;
            }
            group.bootstraps.add(bootstrap);
        }

        private DataProvider createProvider() {
            return new DeferredRegistryProvider(packOutput, patchedRegistries(), modIds);
        }
    }

    private static final class DeferredRegistryProvider implements DataProvider {
        private final PackOutput output;
        private final CompletableFuture<RegistrySetBuilder.PatchedRegistries> registries;
        private final Set<String> modIds;

        private DeferredRegistryProvider(
                PackOutput output,
                CompletableFuture<RegistrySetBuilder.PatchedRegistries> registries,
                Set<String> modIds
        ) {
            this.output = output;
            this.registries = registries;
            this.modIds = modIds;
        }

        @Override
        public CompletableFuture<?> run(CachedOutput cache) {
            return new DatapackBuiltinEntriesProvider(output, registries, modIds).run(cache);
        }

        @Override
        public String getName() {
            return "Registries";
        }
    }

    private static final class RegistryBootstrapGroup {
        @Nullable
        private final Lifecycle lifecycle;
        private final List<RegistrySetBuilder.RegistryBootstrap<?>> bootstraps = new LinkedList<>();

        private RegistryBootstrapGroup(@Nullable Lifecycle lifecycle) {
            this.lifecycle = lifecycle;
        }
    }

    private static final class RegistryBootstrapEntry<T> {
        @Nullable
        private final Lifecycle lifecycle;
        private final RegistrySetBuilder.RegistryBootstrap<T> bootstrap;

        private RegistryBootstrapEntry(
                @Nullable Lifecycle lifecycle,
                RegistrySetBuilder.RegistryBootstrap<T> bootstrap
        ) {
            this.lifecycle = lifecycle;
            this.bootstrap = bootstrap;
        }
    }

    private static final class RegistrySetBuilderCollector extends RegistrySetBuilder {
        private final Map<ResourceKey<?>, List<RegistryBootstrapEntry<?>>> entries = new LinkedHashMap<>();

        @Override
        public <T> RegistrySetBuilder add(
                ResourceKey<? extends Registry<T>> registryKey,
                RegistrySetBuilder.RegistryBootstrap<T> bootstrap
        ) {
            addEntry(registryKey, null, bootstrap);
            return this;
        }

        @Override
        public <T> RegistrySetBuilder add(
                ResourceKey<? extends Registry<T>> registryKey,
                Lifecycle lifecycle,
                RegistrySetBuilder.RegistryBootstrap<T> bootstrap
        ) {
            addEntry(registryKey, lifecycle, bootstrap);
            return this;
        }

        private <T> void addEntry(
                ResourceKey<? extends Registry<T>> registryKey,
                @Nullable Lifecycle lifecycle,
                RegistrySetBuilder.RegistryBootstrap<T> bootstrap
        ) {
            entries.computeIfAbsent(registryKey, key -> new LinkedList<>())
                   .add(new RegistryBootstrapEntry<>(lifecycle, bootstrap));
        }

        private Map<ResourceKey<?>, List<RegistryBootstrapEntry<?>>> entries() {
            return entries;
        }
    }
}
