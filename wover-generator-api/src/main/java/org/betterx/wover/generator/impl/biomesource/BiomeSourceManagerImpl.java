package org.betterx.wover.generator.impl.biomesource;

import org.betterx.wover.biome.api.data.BiomeData;
import org.betterx.wover.biome.api.data.BiomeDataRegistry;
import org.betterx.wover.common.generator.api.biomesource.ReloadableBiomeSource;
import org.betterx.wover.config.api.DatapackConfigs;
import org.betterx.wover.core.api.IntegrationCore;
import org.betterx.wover.core.api.registry.DatapackRegistryBuilder;
import org.betterx.wover.entrypoint.LibWoverWorldGenerator;
import org.betterx.wover.events.api.WorldLifecycle;
import org.betterx.wover.generator.api.biomesource.WoverBiomeData;
import org.betterx.wover.generator.impl.biomesource.end.TheEndBiomesHelper;
import org.betterx.wover.generator.impl.biomesource.end.WoverEndBiomeSource;
import org.betterx.wover.generator.impl.biomesource.nether.NetherBiomesHelper;
import org.betterx.wover.generator.impl.biomesource.nether.WoverNetherBiomeSource;
import org.betterx.wover.legacy.api.LegacyHelper;
import org.betterx.wover.state.api.WorldState;
import org.betterx.wover.tag.api.predefined.CommonBiomeTags;
import org.betterx.wover.util.ResourceLocationSet;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import java.util.LinkedHashMap;
import java.util.Map;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.jetbrains.annotations.ApiStatus;

public class BiomeSourceManagerImpl {
    public static final Identifier BIOME_CONFIG_FILE = LibWoverWorldGenerator.C.id("biome_config.json");
    public static final Identifier MINECRAFT_WILDCARD
            = ResourceLocationSet.WildcardResourceLocation.forAllFrom(IntegrationCore.MINECRAFT);
    public static final String BIOME_EXCLUSION_TAG = "exclude";
    public static final String NO_LOADER_REGISTER_TAG = "no_loader_register";
    public static final String END_CATCH_ALL = "*:is_end";
    public static final String NETHER_CATCH_ALL = "*:is_nether";

    private static final Map<Identifier, MapCodec<? extends BiomeSource>> TYPES = new LinkedHashMap<>();

    public static void register(Identifier location, MapCodec<? extends BiomeSource> codec) {
        TYPES.putIfAbsent(location, codec);
    }

    @ApiStatus.Internal
    public static void initialize() {
        register(LibWoverWorldGenerator.C.id("nether_biome_source"), WoverNetherBiomeSource.CODEC);

        register(LibWoverWorldGenerator.C.id("end_biome_source"), WoverEndBiomeSource.CODEC);

        if (LegacyHelper.isLegacyEnabled()) {
            register(
                    LegacyHelper.BCLIB_CORE.id("nether_biome_source"),
                    LegacyHelper.wrap(WoverNetherBiomeSource.CODEC)
            );
            register(LegacyHelper.BCLIB_CORE.id("end_biome_source"), LegacyHelper.wrap(WoverEndBiomeSource.CODEC));
        }

        WorldLifecycle.RESOURCES_LOADED.subscribe(BiomeSourceManagerImpl::onResourcesLoaded);

        DatapackRegistryBuilder.onElementLoad(
                BiomeDataRegistry.BIOME_DATA_REGISTRY,
                BiomeSourceManagerImpl::didLoadBiomeData
        );
    }

    public static void register(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.BIOME_SOURCE)) {
            event.register(Registries.BIOME_SOURCE, helper -> TYPES.forEach(helper::register));
        }
    }

    private static void didLoadBiomeData(ResourceKey<BiomeData> biomeDataKey, BiomeData biomeData) {
        if (biomeDataKey.identifier().getNamespace().equals("minecraft")) return;

        final ResourceKey<Biome> biomeKey = BiomeDataRegistry.createBiomeKey(biomeDataKey);
        if (!REGISTER_EXCLUDES.contains(biomeKey.identifier())) {
            if (biomeData.isIntendedFor(BiomeTags.IS_NETHER)) {
                for (var param : biomeData.generationData.parameterPoints()) {
                    if (!NetherBiomesHelper.canGenerateInNether(biomeKey)) {
                        LibWoverWorldGenerator.C.log.verbose("Adding Nether biome to vanilla list: " + biomeKey.identifier() + " (" + param + ")");
                        NetherBiomesHelper.addNetherBiome(biomeKey, param);
                    }
                }
            } else if (!TheEndBiomesHelper.canGenerateInEnd(biomeKey)) {
                if (biomeData.isIntendedFor(CommonBiomeTags.IS_END_LAND) || biomeData.isIntendedFor(BiomeTags.IS_END)) {
                    if (!TheEndBiomesHelper.canGenerateAsHighlandsBiome(biomeKey)) {
                        LibWoverWorldGenerator.C.log.verbose("Adding End highlands biome: " + biomeKey.identifier());
                        TheEndBiomesHelper.addHighlandsBiome(biomeKey, genChance(biomeData, 1.0f));
                    }
                    if (!TheEndBiomesHelper.canGenerateAsEndMidlands(biomeKey)) {
                        LibWoverWorldGenerator.C.log.verbose("Adding End midlands biome: " + biomeKey.identifier());
                        TheEndBiomesHelper.addMidlandsBiome(biomeKey, biomeKey, genChance(biomeData, 0.5f));
                    }
                } else if (biomeData.isIntendedFor(CommonBiomeTags.IS_END_HIGHLAND)) {
                    if (!TheEndBiomesHelper.canGenerateAsHighlandsBiome(biomeKey)) {
                        LibWoverWorldGenerator.C.log.verbose("Adding End highlands biome: " + biomeKey.identifier());
                        TheEndBiomesHelper.addHighlandsBiome(biomeKey, genChance(biomeData, 1.0f));
                    }
                } else if (biomeData.isIntendedFor(CommonBiomeTags.IS_END_CENTER)) {
                    if (!TheEndBiomesHelper.canGenerateAsMainIslandBiome(biomeKey)) {
                        LibWoverWorldGenerator.C.log.verbose("Adding End center biome: " + biomeKey.identifier());
                        TheEndBiomesHelper.addMainIslandBiome(biomeKey, genChance(biomeData, 1.0f));
                    }
                } else if (biomeData.isIntendedFor(CommonBiomeTags.IS_SMALL_END_ISLAND)) {
                    if (!TheEndBiomesHelper.canGenerateAsSmallIslandsBiome(biomeKey)) {
                        LibWoverWorldGenerator.C.log.verbose("Adding small end island biome: " + biomeKey.identifier());
                        TheEndBiomesHelper.addSmallIslandsBiome(biomeKey, genChance(biomeData, 1.0f));
                    }
                } else if (biomeData.isIntendedFor(CommonBiomeTags.IS_END_MIDLAND)) {
                    if (!TheEndBiomesHelper.canGenerateAsEndMidlands(biomeKey)) {
                        if (biomeData instanceof WoverBiomeData woverData && woverData.parent != null) {
                            LibWoverWorldGenerator.C.log.verbose("Adding End midlands biome: " + biomeKey.identifier());
                            TheEndBiomesHelper.addMidlandsBiome(woverData.parent, biomeKey, woverData.genChance);
                        } else if (!TheEndBiomesHelper.canGenerateAsHighlandsBiome(biomeKey)) {
                            LibWoverWorldGenerator.C.log.verbose("Adding End highlands biome: " + biomeKey.identifier());
                            TheEndBiomesHelper.addHighlandsBiome(biomeKey, genChance(biomeData, 0.5f));
                        }
                    }
                } else if (biomeData.isIntendedFor(CommonBiomeTags.IS_END_BARRENS)) {
                    if (!TheEndBiomesHelper.canGenerateAsEndBarrens(biomeKey)) {
                        if (biomeData instanceof WoverBiomeData woverData && woverData.parent != null) {
                            LibWoverWorldGenerator.C.log.verbose("Adding End barrens biome: " + biomeKey.identifier());
                            TheEndBiomesHelper.addBarrensBiome(woverData.parent, biomeKey, woverData.genChance);
                        } else if (!TheEndBiomesHelper.canGenerateAsHighlandsBiome(biomeKey)) {
                            LibWoverWorldGenerator.C.log.verbose("Adding End highlands biome: " + biomeKey.identifier());
                            TheEndBiomesHelper.addHighlandsBiome(biomeKey, genChance(biomeData, 0.33f));
                        }
                    }
                } else if (biomeData.isIntendedFor(BiomeTags.IS_END)) {
                    if (!TheEndBiomesHelper.canGenerateAsHighlandsBiome(biomeKey)) {
                        LibWoverWorldGenerator.C.log.verbose("Adding End highlands biome: " + biomeKey.identifier());
                        TheEndBiomesHelper.addHighlandsBiome(biomeKey, genChance(biomeData, 1.0f));
                    }
                }
            }
        }
    }

    private static float genChance(BiomeData data, float defaultChance) {
        return data instanceof WoverBiomeData woverData ? woverData.genChance : defaultChance;
    }

    private static final Map<TagKey<Biome>, Set<Identifier>> EXCLUSIONS = new HashMap<>();
    private static final Set<Identifier> REGISTER_EXCLUDES = new ResourceLocationSet();

    public static void onResourcesLoaded(ResourceManager resourceManager) {
        EXCLUSIONS.clear();
        REGISTER_EXCLUDES.clear();

        // ensure vanilla biomes are never registered as custom additions
        REGISTER_EXCLUDES.add(MINECRAFT_WILDCARD);

        DatapackConfigs
                .instance()
                .runForResource(resourceManager, BIOME_CONFIG_FILE, BiomeSourceManagerImpl::processBiomeConfigs);

        NetherBiomesHelper.syncParameterList(WorldState.allStageRegistryAccess());

        if (WorldState.registryAccess() != null && !EXCLUSIONS.isEmpty()) {
            WorldState.registryAccess()
                      .lookup(Registries.LEVEL_STEM)
                      .ifPresent(levelStems -> levelStems.listElements().forEach(holder -> {
                          if (holder.isBound()
                                  && holder.value().generator().getBiomeSource() instanceof ReloadableBiomeSource bs
                          ) {
                              bs.reloadBiomes();
                          }
                      }));
        }
    }

    public static Set<Identifier> getExcludedBiomes(TagKey<Biome> tag) {
        return EXCLUSIONS.getOrDefault(tag, Set.of());
    }

    private static void addAllExclusions(List<TagKey<Biome>> tags, Identifier biome) {
        tags.forEach(tag -> EXCLUSIONS.computeIfAbsent(tag, k -> new ResourceLocationSet()).add(biome));
    }

    private static void addBiomesToExclusion(
            JsonElement value,
            Consumer<Identifier> adder
    ) {
        if (value.isJsonPrimitive()) {
            adder.accept(ResourceLocationSet.WildcardResourceLocation.parse(value.getAsString()));
        } else if (value.isJsonArray()) {
            value.getAsJsonArray()
                 .forEach(v -> adder.accept(ResourceLocationSet.WildcardResourceLocation.parse(v.getAsString())));
        }
    }

    private static void processBiomeConfigs(Identifier location, JsonObject root) {
        if (root.has(BIOME_EXCLUSION_TAG)) {
            final JsonObject excludes = root.getAsJsonObject(BIOME_EXCLUSION_TAG);
            excludes.asMap().forEach((key, value) -> {
                if (key.equals(END_CATCH_ALL)) {
                    final List<TagKey<Biome>> endTags = WoverEndBiomeSource.TAGS;
                    addBiomesToExclusion(value, id -> addAllExclusions(endTags, id));
                } else if (key.equals(NETHER_CATCH_ALL)) {
                    final List<TagKey<Biome>> netherTags = WoverNetherBiomeSource.TAGS;
                    addBiomesToExclusion(value, id -> addAllExclusions(netherTags, id));
                } else {
                    final TagKey<Biome> tag = TagKey.create(Registries.BIOME, Identifier.parse(key));
                    final Set<Identifier> elements = EXCLUSIONS.computeIfAbsent(
                            tag,
                            k -> new ResourceLocationSet()
                    );
                    addBiomesToExclusion(value, elements::add);
                }
            });
        }

        if (root.has(NO_LOADER_REGISTER_TAG)) {
            final JsonArray excludes = root.getAsJsonArray(NO_LOADER_REGISTER_TAG);
            excludes.forEach(v -> REGISTER_EXCLUDES.add(ResourceLocationSet.WildcardResourceLocation.parse(v.getAsString())));
        }
    }

    public static String printBiomeSourceInfo(BiomeSource biomeSource) {
        Set<Holder<Biome>> biomes = Set.of();
        try {
            biomes = biomeSource.possibleBiomes();
        } catch (Throwable e) {
            LibWoverWorldGenerator.C.log.warn("Error getting possible biomes from BiomeSource", e);
        }
        return biomeSource.getClass()
                          .getSimpleName() + " (" + Integer.toHexString(biomeSource.hashCode()) + ")" +
                "\n    biomes     = " + biomes.size() +
                "\n    namespaces = " + WoverBiomeSourceImpl.getNamespaces(biomes);
    }
}
