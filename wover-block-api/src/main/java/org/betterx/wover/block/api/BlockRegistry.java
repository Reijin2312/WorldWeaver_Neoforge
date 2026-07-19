package org.betterx.wover.block.api;

import org.betterx.wover.block.impl.WoverBlockItemImpl;
import org.betterx.wover.core.api.ModCore;
import org.betterx.wover.item.api.ItemRegistry;
import org.betterx.wover.loot.api.BlockLootProvider;
import org.betterx.wover.loot.api.LootLookupProvider;
import org.betterx.wover.loot.api.LootTableManager;
import org.betterx.wover.tag.api.event.context.TagBootstrapContext;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootTable;

import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

public class BlockRegistry {
    private static final Map<ModCore, BlockRegistry> REGISTRIES = new HashMap<>();
    private static final ThreadLocal<ArrayDeque<ResourceKey<Block>>> CONSTRUCTION_IDS = ThreadLocal.withInitial(ArrayDeque::new);
    public final ModCore C;
    private final Map<Identifier, Block> blocks = new HashMap<>();
    private Map<Block, TagKey<Block>[]> datagenTags;
    private final ItemRegistry itemRegistry;

    private BlockRegistry(ModCore modeCore) {
        this.C = modeCore;
        this.itemRegistry = ItemRegistry.forMod(modeCore);

        if (ModCore.isDatagen()) {
            datagenTags = new HashMap<>();
        }
    }

    public static Stream<BlockRegistry> streamAll() {
        return REGISTRIES.values().stream();
    }

    public static BlockRegistry forMod(ModCore modCore) {
        return REGISTRIES.computeIfAbsent(modCore, c -> new BlockRegistry(modCore));
    }

    public Stream<Block> allBlocks() {
        return blocks.values().stream();
    }

    public Stream<BlockItem> allBlockItems() {
        return blocks
                .values()
                .stream()
                .filter(block -> block.asItem() instanceof BlockItem)
                .map(block -> (BlockItem) block.asItem());
    }

    @SafeVarargs
    public final <T extends Block> T register(String path, T block, TagKey<Block>... tags) {
        return register(path, block, tags, null);
    }

    public <T extends Block> T register(String path, T block, TagKey<Block>[] tags, TagKey<Item>[] itemTags) {
        if (block != null && block != Blocks.AIR) {
            final Identifier id = tags == null
                    ? _registerBlockOnly(path, block)
                    : _registerBlockOnly(path, block, tags);

            final BlockItem item;
            ItemRegistry.pushConstructionId(id);
            try {
                if (block instanceof CustomBlockItemProvider provider) {
                    item = provider.getCustomBlockItem(id, defaultBlockItemSettings());
                } else {
                    item = WoverBlockItemImpl.create(block, defaultBlockItemSettings());
                }
            } finally {
                ItemRegistry.popConstructionId();
            }
            if (itemTags == null)
                registerBlockItem(path, item);
            else
                registerBlockItem(path, item, itemTags);

            if (block.defaultBlockState().ignitedByLava()
                    && FlammableBlockRegistry.getDefaultInstance()
                                             .get(block)
                                             .getBurnOdds() == 0) {
                FlammableBlockRegistry.getDefaultInstance().add(block, 5, 5);
            }
        }
        return block;
    }

    @SafeVarargs
    private Identifier _registerBlockOnly(String path, Block block, TagKey<Block>... tags) {
        Identifier id = C.mk(path);
        Registry.register(BuiltInRegistries.BLOCK, id, block);
        blocks.put(id, block);

        if (datagenTags != null && tags != null && tags.length > 0) datagenTags.put(block, tags);
        return id;
    }

    @SafeVarargs
    public final <T extends Block> T registerBlockOnly(String path, T block, TagKey<Block>... tags) {
        if (block != null && block != Blocks.AIR) {
            _registerBlockOnly(path, block, tags);
        }

        return block;
    }

    @SafeVarargs
    private BlockItem registerBlockItem(String path, BlockItem item, TagKey<Item>... tags) {
        return this.itemRegistry.register(path, item, tags);
    }

    public String prepareConstructionPath(String path) {
        pushConstructionId(C.mk(path));
        return path;
    }

    public void finishConstructionPath() {
        popConstructionId();
    }

    private static void pushConstructionId(Identifier id) {
        CONSTRUCTION_IDS.get().push(ResourceKey.create(Registries.BLOCK, id));
    }

    private static void popConstructionId() {
        ArrayDeque<ResourceKey<Block>> stack = CONSTRUCTION_IDS.get();
        if (!stack.isEmpty()) stack.pop();
    }

    public static ResourceKey<Block> peekConstructionId() {
        return CONSTRUCTION_IDS.get().peek();
    }

    public static ResourceKey<Block> takeConstructionId() {
        ArrayDeque<ResourceKey<Block>> stack = CONSTRUCTION_IDS.get();
        return stack.isEmpty() ? null : stack.pop();
    }

    public static String takeConstructionIdString() {
        ResourceKey<Block> key = takeConstructionId();
        return key == null ? null : key.identifier().toString();
    }

    public static <T> T withConstructionId(Identifier id, Supplier<T> factory) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
        ArrayDeque<ResourceKey<Block>> stack = CONSTRUCTION_IDS.get();
        stack.push(key);
        try {
            return factory.get();
        } finally {
            if (!stack.isEmpty() && key.equals(stack.peek())) stack.pop();
        }
    }

    protected Item.Properties defaultBlockItemSettings() {
        return new Item.Properties().useBlockDescriptionPrefix();
    }

    public void bootstrapBlockTags(TagBootstrapContext<Block> ctx) {
        if (datagenTags != null) {
            datagenTags.forEach(ctx::add);
        }

        blocks
                .entrySet()
                .stream()
                .filter(b -> b.getValue() instanceof BlockTagProvider)
                .forEach(b -> ((BlockTagProvider) b.getValue()).registerBlockTags(b.getKey(), ctx));
    }

    public void bootstrapBlockLoot(
            @NotNull HolderLookup.Provider lookup,
            @NotNull BiConsumer<ResourceKey<LootTable>, LootTable.Builder> biConsumer
    ) {
        LootLookupProvider provider = new LootLookupProvider(lookup);
        blocks
                .entrySet()
                .stream()
                .filter(b -> b.getValue() instanceof BlockLootProvider)
                .forEach(b -> {
                    var key = LootTableManager.getBlockLootTableKey(C, b.getKey());
                    var builder = ((BlockLootProvider) b.getValue()).registerBlockLoot(b.getKey(), provider, key);

                    if (builder != null)
                        biConsumer.accept(key, builder);
                });
    }
}
