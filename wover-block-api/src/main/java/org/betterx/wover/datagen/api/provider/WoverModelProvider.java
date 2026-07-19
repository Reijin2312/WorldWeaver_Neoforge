package org.betterx.wover.datagen.api.provider;

import org.betterx.wover.block.api.BlockRegistry;
import org.betterx.wover.block.api.model.BlockModelProvider;
import org.betterx.wover.block.api.model.WoverBlockModelGenerators;
import org.betterx.wover.block.impl.ModelProviderExclusions;
import org.betterx.wover.core.api.ModCore;
import org.betterx.wover.datagen.api.WoverDataProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ItemModelOutput;
import net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator;
import net.minecraft.client.data.models.model.DelegatedModel;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelDispatcher;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import org.betterx.wover.block.api.model.WoverBlockModelGeneratorsAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class WoverModelProvider implements WoverDataProvider<DataProvider> {
    /**
     * The title of the provider. Mainly used for logging.
     */
    public final String title;

    /**
     * The ModCore instance of the Mod that is providing this instance.
     */
    protected final ModCore modCore;

    public WoverModelProvider(ModCore modCore) {
        this(modCore, modCore.namespace);
    }

    public WoverModelProvider(ModCore modCore, String title) {
        this.modCore = modCore;
        this.title = title;
    }

    protected void addFromRegistry(
            WoverBlockModelGenerators generator,
            BlockRegistry registry,
            boolean validate
    ) {
        addFromRegistry(generator, registry, validate, ModelOverides.create());
    }

    public static class ModelOverides {
        public interface BlockModelProvider {
            void provideModels(Block block);
        }

        private final Map<Block, BlockModelProvider> OVERRIDES = new HashMap<>();
        private static final BlockModelProvider IGNORE = (block) -> {
        };

        public static ModelOverides create() {
            return new ModelOverides();
        }

        public ModelOverides override(@Nullable Block block, @NotNull BlockModelProvider provider) {
            if (block == Blocks.AIR || block == null) return this;

            final var old = OVERRIDES.put(block, provider);
            if (old != null) {
                throw new IllegalStateException("Block " + block + " already has an override.");
            }
            return this;
        }

        public ModelOverides overrideLike(@Nullable Block block, @NotNull Block copyFromBlock) {
            if (block == Blocks.AIR || block == null) return this;
            return this.override(block, OVERRIDES.get(copyFromBlock));
        }

        public ModelOverides ignore(@Nullable Block block) {
            if (block == Blocks.AIR || block == null) return this;
            return this.override(block, IGNORE);
        }

        public boolean contain(Block block) {
            return OVERRIDES.containsKey(block);
        }

        boolean provideBlockModel(Block block) {
            final var override = OVERRIDES.get(block);
            if (override != null) {
                override.provideModels(block);
                return true;
            }
            return false;
        }

        private ModelOverides() {
        }
    }


    protected void addFromRegistry(
            WoverBlockModelGenerators generator,
            BlockRegistry registry,
            boolean validateMissing,
            ModelOverides overrides
    ) {
        final Set<Block> processedBlocks = new HashSet<>();
        registry
                .allBlocks()
                .forEach(block -> {
                    // Some registries may expose the same Block instance under multiple ids.
                    // Datagen must only emit models once per concrete Block instance.
                    if (!processedBlocks.add(block)) {
                        return;
                    }

                    // If the block is not in the overrides, and it is a BlockModelProvider, provide the models.
                    if (!overrides.provideBlockModel(block) && block instanceof BlockModelProvider bmp) {
                        bmp.provideBlockModels(generator);
                    } else if (validateMissing) {
                        ModelProviderExclusions.excludeFromBlockModelValidation(block);
                    }

                    if (!validateMissing) {
                        ModelProviderExclusions.excludeFromBlockModelValidation(block);
                    }

                });
    }

    protected abstract void bootstrapBlockStateModels(WoverBlockModelGenerators generator);
    protected abstract void bootstrapItemModels(ItemModelGenerators itemModelGenerator);

    @Override
    public DataProvider getProvider(
            FabricPackOutput output,
            CompletableFuture<HolderLookup.Provider> registriesFuture
    ) {
        return new DataProvider() {
            private final PackOutput.PathProvider blockStatePathProvider =
                    output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
            private final PackOutput.PathProvider itemInfoPathProvider =
                    output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "items");
            private final PackOutput.PathProvider modelPathProvider =
                    output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");

            @Override
            public CompletableFuture<?> run(CachedOutput cache) {
                Map<Block, BlockModelDefinitionGenerator> blockStates = Maps.newHashMap();
                Consumer<BlockModelDefinitionGenerator> blockStateConsumer = generator -> {
                    Block block = generator.block();
                    if (blockStates.put(block, generator) != null) {
                        throw new IllegalStateException("Duplicate blockstate definition for " + block);
                    }
                };

                Map<Identifier, ModelInstance> models = new HashMap<>();
                BiConsumer<Identifier, ModelInstance> modelOutput = (id, supplier) -> {
                    if (models.put(id, supplier) != null) {
                        throw new IllegalStateException("Duplicate model definition for " + id);
                    }
                };

                Set<Item> skippedItems = new HashSet<>();
                Map<Item, ClientItem> itemInfos = new HashMap<>();
                Map<Item, Item> copiedItemInfos = new HashMap<>();
                ItemModelOutput itemModelOutput = new ItemModelOutput() {
                    @Override
                    public void accept(Item item, net.minecraft.client.renderer.item.ItemModel.Unbaked model, ClientItem.Properties properties) {
                        ClientItem prev = itemInfos.put(item, new ClientItem(model, properties));
                        if (prev != null) {
                            throw new IllegalStateException("Duplicate item model definition for " + item);
                        }
                    }

                    @Override
                    public void copy(Item target, Item source) {
                        copiedItemInfos.put(target, source);
                    }
                };

                WoverBlockModelGeneratorsAccess blockModelGenerators = new WoverBlockModelGeneratorsAccess(
                        (Consumer<Object>) (Consumer<?>) blockStateConsumer, itemModelOutput, modelOutput, skippedItems
                );
                bootstrapBlockStateModels(new WoverBlockModelGenerators(blockModelGenerators));

                ItemModelGenerators itemModelGenerators = new ItemModelGenerators(itemModelOutput, modelOutput);
                bootstrapItemModels(itemModelGenerators);

                validateBlockStates(blockStates);
                addItemModelDelegates(models, skippedItems);
                finalizeItemModels(itemInfos, copiedItemInfos, skippedItems, models);

                return CompletableFuture.allOf(
                        DataProvider.saveAll(
                                cache,
                                generator -> BlockStateModelDispatcher.CODEC.encodeStart(JsonOps.INSTANCE, generator.create()).getOrThrow(),
                                b -> blockStatePathProvider.json(
                                        b.builtInRegistryHolder().key().identifier()
                                ),
                                blockStates
                        ),
                        DataProvider.saveAll(cache, ClientItem.CODEC, item -> itemInfoPathProvider.json(item.builtInRegistryHolder().key().identifier()), itemInfos),
                        saveCollection(cache, models, modelPathProvider::json)
                );
            }

            private void validateBlockStates(Map<Block, BlockModelDefinitionGenerator> blockStates) {
                for (Block block : BuiltInRegistries.BLOCK) {
                    if (ModelProviderExclusions.isExcluded(block)) {
                        continue;
                    }
                    Identifier id = BuiltInRegistries.BLOCK.getKey(block);
                    if (id == null || !id.getNamespace().equals(modCore.namespace)) {
                        continue;
                    }
                    if (!blockStates.containsKey(block)) {
                        throw new IllegalStateException("Missing blockstate definition for " + block);
                    }
                }
            }

            private void finalizeItemModels(
                    Map<Item, ClientItem> itemInfos,
                    Map<Item, Item> copiedItemInfos,
                    Set<Item> skippedItems,
                    Map<Identifier, ModelInstance> models
            ) {
                copiedItemInfos.forEach((target, source) -> {
                    ClientItem donor = itemInfos.get(source);
                    if (donor == null) {
                        throw new IllegalStateException("Missing donor item model: " + source + " -> " + target);
                    }
                    ClientItem prev = itemInfos.put(target, donor);
                    if (prev != null && prev != donor) {
                        throw new IllegalStateException("Duplicate copied item model definition for " + target);
                    }
                });

                for (Item item : BuiltInRegistries.ITEM) {
                    Identifier id = BuiltInRegistries.ITEM.getKey(item);
                    if (id == null || !id.getNamespace().equals(modCore.namespace)) {
                        continue;
                    }
                    if (itemInfos.containsKey(item)) {
                        continue;
                    }

                    // If an explicit item model exists (generated or static), use it directly.
                    Identifier itemModelId = ModelLocationUtils.getModelLocation(item);
                    if (hasModel(itemModelId, models)) {
                        itemInfos.put(item, new ClientItem(ItemModelUtils.plainModel(itemModelId), ClientItem.Properties.DEFAULT));
                        continue;
                    }

                    if (item instanceof BlockItem blockItem && !skippedItems.contains(item)) {
                        Identifier blockModelId = ModelLocationUtils.getModelLocation(blockItem.getBlock());
                        if (hasModel(blockModelId, models)) {
                            itemInfos.put(item, new ClientItem(ItemModelUtils.plainModel(blockModelId), ClientItem.Properties.DEFAULT));
                            continue;
                        }
                    }

                    throw new IllegalStateException("Missing item model definition for " + item);
                }
            }

            private void addItemModelDelegates(
                    Map<Identifier, ModelInstance> models,
                    Set<Item> skippedItems
            ) {
                BuiltInRegistries.BLOCK.forEach(block -> {
                    Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
                    if (blockId == null || !blockId.getNamespace().equals(modCore.namespace)) {
                        return;
                    }
                    Item item = Item.BY_BLOCK.get(block);
                    if (item == null || skippedItems.contains(item)) {
                        return;
                    }
                    Identifier modelId = ModelLocationUtils.getModelLocation(item);
                    if (hasModel(modelId, models)) {
                        return;
                    }
                    Identifier blockModelId = ModelLocationUtils.getModelLocation(block);
                    if (!hasModel(blockModelId, models)) {
                        return;
                    }
                    models.put(modelId, new DelegatedModel(blockModelId));
                });
            }

            private boolean hasModel(
                    Identifier modelId,
                    Map<Identifier, ModelInstance> models
            ) {
                if (models.containsKey(modelId)) {
                    return true;
                }
                if (modCore.modContainer == null) {
                    return false;
                }
                var path = "assets/" + modelId.getNamespace() + "/models/" + modelId.getPath() + ".json";
                return modCore.modContainer.findPath(path).isPresent();
            }

            private <T> CompletableFuture<?> saveCollection(
                    CachedOutput cache,
                    Map<T, ? extends Supplier<JsonElement>> objectToJsonMap,
                    Function<T, Path> resolveObjectPath
            ) {
                return CompletableFuture.allOf(objectToJsonMap.entrySet().stream().map(entry -> {
                    Path path = resolveObjectPath.apply(entry.getKey());
                    JsonElement element = entry.getValue().get();
                    return DataProvider.saveStable(cache, element, path);
                }).toArray(CompletableFuture[]::new));
            }

            @Override
            public String getName() {
                return "Model Definitions - " + title;
            }
        };
    }
}
