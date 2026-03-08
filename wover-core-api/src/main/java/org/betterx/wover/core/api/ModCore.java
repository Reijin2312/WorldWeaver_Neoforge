package org.betterx.wover.core.api;

import de.ambertation.wunderlib.utils.Version;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.Pack;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.data.loading.DatagenModLoader;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforgespi.locating.IModFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;


/**
 * This class is used to identify your mod and provide some helpfull utilities
 * specific to your Mod.
 * <p>
 * It is considered best practice to create, and store an instance of this class
 * for you mod in your main mod class (the one annotated with
 * {@link net.neoforged.fml.common.Mod}).
 */
public final class ModCore implements Version.ModVersionProvider {
    private static final Map<String, ModCore> cache = new java.util.concurrent.ConcurrentHashMap<>();

    private final List<DatapackInfo> providedDatapacks = new LinkedList<>();
    /**
     * This logger is used to write text to the console and the log file.
     * The mod id is used as the logger's name, making it clear which mod wrote info,
     * warnings, and errors.
     */
    public final Logger LOG;

    /**
     * alias for {@link #LOG}
     */
    public final Logger log;

    /**
     * The mod id is used to identify your mod.
     */
    public final String modId;
    public final String namespace;
    private final Version modVersion;

    public final ModContainer modContainer;

    private record DatapackInfo(Identifier id, DatapackActivationType activationType) {
    }

    private ModCore(String modID, String namespace) {
        LOG = Logger.create(modID);
        log = LOG;
        modId = modID;
        this.namespace = namespace;

        ModList modList = ModList.get();
        Optional<? extends ModContainer> optional = Optional.empty();
        if (modList != null) {
            optional = modList.getModContainerById(modId);
            if (optional.isEmpty() && "wover".equals(namespace) && !"wover".equals(modId)) {
                optional = modList.getModContainerById("wover");
            }
        }
        if (optional.isPresent()) {
            this.modContainer = optional.get();
            modVersion = new Version(modContainer.getModInfo().getVersion().toString());
        } else {
            this.modContainer = null;
            modVersion = new Version(0, 0, 0);
            ;
        }
    }


    /**
     * Returns the {@link Version} of this mod.
     *
     * @return the {@link Version} of this mod.
     */
    public Version getModVersion() {
        return modVersion;
    }

    /**
     * Returns the {@link #modId} of this mod.
     *
     * @return the {@link #modId} of this mod.
     */
    @Override
    public String getModID() {
        return modId;
    }

    /**
     * Returns the namespace of this mod.
     *
     * @return the namespace of this mod.
     */
    @Override
    public String getNamespace() {
        return namespace;
    }

    /**
     * Returns the {@link Identifier} for the given name in the namespace of this mod.
     * <p>
     * You should always prefer this method over {@link Identifier#fromNamespaceAndPath(String, String)}.
     *
     * @param name The name or path of the resource.
     * @return The {@link Identifier} for the given name.
     */
    public Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(namespace, name);
    }


    /**
     * Returns the {@link Identifier} for the given path in the namespace of this mod.
     *
     * @param location The {@link Identifier} to convert.
     * @return The {@link Identifier} for the given path in the namespace of this Mod.
     */
    public Identifier convertNamespace(Identifier location) {
        return id(location.getPath());
    }

    /**
     * Returns the {@link Identifier} for the given path in the namespace of this mod.
     *
     * @param key The {@link ResourceKey} to convert.
     * @return The {@link Identifier} for the given path in the namespace of this Mod.
     */
    public <T> Identifier convertNamespace(ResourceKey<T> key) {
        return convertNamespace(key.identifier());
    }

    /**
     * alias for {@link #id(String)}
     *
     * @param key The name or path of the resource.
     * @return The {@link Identifier} for the given name.
     */
    @Override
    public Identifier mk(String key) {
        return Identifier.fromNamespaceAndPath(namespace, key);
    }

    /**
     * Returns true if the mod is loaded.
     *
     * @return true if the mod is loaded.
     */
    public boolean isLoaded() {
        return modContainer != null;
    }

    /**
     * Returns a stream of all Datapacks {@link Identifier}s that are provided by this mod.
     *
     * @return a stream of all Datapacks {@link Identifier}s that are provided by this mod.
     */
    public Stream<Identifier> providedDatapacks() {
        return providedDatapacks.stream().map(DatapackInfo::id);
    }

    /**
     * Register a Datapack {@link Identifier} that is provided by this mod.
     *
     * @param name           The name of the Datapack.
     * @param activationType The {@link DatapackActivationType} of the Datapack.
     * @return The {@link Identifier} of the Datapack.
     */
    public Identifier addDatapack(String name, DatapackActivationType activationType) {
        final Identifier id = id(name);
        providedDatapacks.add(new DatapackInfo(id, activationType));
        return id;
    }

    /**
     * Registers built-in datapacks for this mod on the mod event bus.
     *
     * @param modEventBus The mod event bus
     */
    public void registerDatapackListener(IEventBus modEventBus) {
        modEventBus.addListener(this::onAddPackFinders);
    }

    private void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA || modContainer == null) {
            return;
        }
        final var modInfo = modContainer.getModInfo();
        final var modFile = modInfo.getOwningFile().getFile();
        final var modVersion = modInfo.getVersion().toString();
        for (DatapackInfo info : providedDatapacks) {
            final var packRoot = resolvePackRoot(modFile, info.id.getPath());
            if (packRoot == null) {
                LOG.warn("Skipping built-in datapack {}: pack.mcmeta not found.", info.id);
                continue;
            }
            final var packId = "mod/" + info.id;
            final var title =
                    Component.translatable(
                            "pack." + info.id.getNamespace() + "." + info.id.getPath() + ".description"
                    );
            final var locationInfo = new PackLocationInfo(
                    packId,
                    title,
                    info.activationType.packSource(),
                    Optional.of(new KnownPack("neoforge", packId, modVersion))
            );
            final Pack.ResourcesSupplier resources = new Pack.ResourcesSupplier() {
                @Override
                public PackResources openPrimary(PackLocationInfo location) {
                    return new PathPackResources(location, packRoot);
                }

                @Override
                public PackResources openFull(PackLocationInfo location, Pack.Metadata metadata) {
                    return new PathPackResources(location, packRoot);
                }
            };
            final var selectionConfig = new PackSelectionConfig(
                    info.activationType.alwaysActive(),
                    Pack.Position.TOP,
                    false
            );
            final var pack = Pack.readMetaAndCreate(
                    locationInfo,
                    resources,
                    PackType.SERVER_DATA,
                    selectionConfig
            );
            if (pack == null) {
                LOG.warn("Skipping built-in datapack {}: invalid pack metadata.", info.id);
                continue;
            }
            event.addRepositorySource(consumer -> consumer.accept(pack));
        }
    }

    private static Path resolvePackRoot(IModFile modFile, String packId) {
        final Path root = findInMod(modFile, packId);
        if (root != null && Files.exists(root.resolve("pack.mcmeta"))) {
            return root;
        }
        final Path resourcepacks = findInMod(modFile, "resourcepacks/" + packId);
        if (resourcepacks != null && Files.exists(resourcepacks.resolve("pack.mcmeta"))) {
            return resourcepacks;
        }
        return null;
    }

    private static Path findInMod(IModFile modFile, String path) {
        var contents = modFile.getContents();
        var uri = contents.findFile(path);
        if (uri.isPresent()) {
            return Path.of(uri.get());
        }
        var base = modFile.getFilePath();
        if (base != null && Files.isDirectory(base)) {
            var resolved = base.resolve(path);
            if (Files.exists(resolved)) {
                return resolved;
            }
        }
        return null;
    }

    /**
     * Register a Datapack {@link Identifier} that is provided by this mod. When the dependency
     * is not loaded, the Datapack will be registered with the {@link ResourcePackActivationType#NORMAL}
     * activation type. When the dependency is loaded, the Datapack will be registered with the
     * {@link ResourcePackActivationType#DEFAULT_ENABLED} activation type.
     *
     * @param dependency The dependency mod.
     * @return The {@link Identifier} of the Datapack.
     */
    public Identifier addDatapack(ModCore dependency) {
        return this.addDatapack(dependency.namespace + "_extensions", dependency.isLoaded()
                ? DatapackActivationType.DEFAULT_ENABLED
                : DatapackActivationType.NORMAL);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModCore modCore)) return false;
        return Objects.equals(modId, modCore.modId) && Objects.equals(namespace, modCore.namespace);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modId, namespace);
    }

    @Override
    public String toString() {
        return "ModCore: " + modId + " (" + namespace + ")";
    }

    /**
     * Returns the instance of {@link ModCore} for the given mod id. Every mod id has a unique, single
     * instance. Calling this method multiple times with the same mod id is guaranteed to return
     * the same instance.
     *
     * @param modID The mod id of the mod.
     * @return The instance of {@link ModCore} for the given mod id.
     */
    public static ModCore create(String modID) {
        return cache.computeIfAbsent(modID, id -> new ModCore(id, id));
    }

    /**
     * Returns the instance of {@link ModCore} for the given mod id. Every mod id has a unique, single
     * instance. Calling this method multiple times with the same mod id is guaranteed to return
     * the same instance.
     *
     * @param modID     The mod id of the mod.
     * @param namespace The namespace of the mod. The namespace is used to create
     *                  {@link Identifier}s in {@link #id(String)} and {@link #mk(String)}.
     * @return The instance of {@link ModCore} for the given mod id.
     */
    public static ModCore create(String modID, String namespace) {
        return cache.computeIfAbsent(modID, id -> new ModCore(id, namespace));
    }

    /**
     * Returns true if the game is currently running in a data generation environment.
     *
     * @return true if the game is currently running in a data generation environment.
     */
    public static boolean isDatagen() {
        return DatagenModLoader.isRunningDataGen();
    }

    /**
     * Returns true if the game is currently running in a development environment.
     *
     * @return true if the game is currently running in a development environment.
     */
    public static boolean isDevEnvironment() {
        return !FMLLoader.getCurrent().isProduction();
    }

    /**
     * Returns true if the game is currently running on the client.
     *
     * @return true if the game is currently running on the client.
     */
    public static boolean isClient() {
        return FMLEnvironment.getDist().isClient();
    }

    /**
     * Returns true if the game is currently running on the server.
     *
     * @return true if the game is currently running on the server.
     */
    public static boolean isServer() {
        return FMLEnvironment.getDist().isDedicatedServer();
    }

}
