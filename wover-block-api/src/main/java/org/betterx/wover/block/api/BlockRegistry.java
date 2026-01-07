package org.betterx.wover.block.api;

import org.betterx.wover.block.impl.WoverBlockItemImpl;
import org.betterx.wover.core.api.ModCore;
import org.betterx.wover.item.api.ItemRegistry;
import org.betterx.wover.loot.api.BlockLootProvider;
import org.betterx.wover.loot.api.LootLookupProvider;
import org.betterx.wover.loot.api.LootTableManager;
import org.betterx.wover.tag.api.event.context.TagBootstrapContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.jetbrains.annotations.NotNull;

public class BlockRegistry {
    private static final Map<ModCore, BlockRegistry> REGISTRIES = new HashMap<>();
    private static final Comparator<BlockRegistry> REGISTRY_ORDER = Comparator
            .comparing((BlockRegistry registry) -> registry.C.modId)
            .thenComparing(registry -> registry.C.namespace);
    private static final Comparator<ResourceLocation> ID_ORDER = Comparator
            .comparing(ResourceLocation::getNamespace)
            .thenComparing(ResourceLocation::getPath);
    public final ModCore C;
    private final Map<ResourceLocation, Block> blocks = new HashMap<>();
    private Map<Block, TagKey<Block>[]> datagenTags;
    private final ItemRegistry itemRegistry;
    private Runnable initializer;
    private boolean initialized;

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
            ensureIntrusiveHolder(block);
            final ResourceLocation id = tags == null
                    ? _registerBlockOnly(path, block)
                    : _registerBlockOnly(path, block, tags);

            final BlockItem item;
            if (block instanceof CustomBlockItemProvider provider) {
                item = provider.getCustomBlockItem(id, defaultBlockItemSettings());
            } else {
                item = WoverBlockItemImpl.create(block, defaultBlockItemSettings());
            }
            if (itemTags == null)
                registerBlockItem(path, item);
            else
                registerBlockItem(path, item, itemTags);

            if (block.defaultBlockState().ignitedByLava()) {
                FireBlock fire = (FireBlock) Blocks.FIRE;
                if (fire.getBurnOdds(block.defaultBlockState()) == 0) {
                    fire.setFlammable(block, 5, 5);
                }
            }
        }
        return block;
    }

    @SafeVarargs
    private ResourceLocation _registerBlockOnly(String path, Block block, TagKey<Block>... tags) {
        ResourceLocation id = C.mk(path);
        blocks.put(id, block);

        if (datagenTags != null && tags != null && tags.length > 0) datagenTags.put(block, tags);
        return id;
    }

    @SafeVarargs
    public final <T extends Block> T registerBlockOnly(String path, T block, TagKey<Block>... tags) {
        if (block != null && block != Blocks.AIR) {
            ensureIntrusiveHolder(block);
            _registerBlockOnly(path, block, tags);
        }

        return block;
    }

    @SafeVarargs
    private BlockItem registerBlockItem(String path, BlockItem item, TagKey<Item>... tags) {
        this.itemRegistry.register(path, item, tags); // enqueues item registration
        return item;
    }

    public void setInitializer(Runnable initializer) {
        this.initializer = initializer;
    }

    private void ensureInitialized() {
        if (!initialized && initializer != null) {
            initialized = true;
            initializer.run();
        }
    }

    protected Item.Properties defaultBlockItemSettings() {
        return new Item.Properties();
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

    private void performBlockRegistration(RegisterEvent.RegisterHelper<Block> helper) {
        ensureInitialized();
        blocks.entrySet()
              .stream()
              .sorted(Map.Entry.comparingByKey(ID_ORDER))
              .forEach(entry -> {
                  ResourceLocation id = entry.getKey();
                  Block block = entry.getValue();
                  ensureIntrusiveHolder(block);
                  helper.register(id, block);
                  var key = ResourceKey.create(Registries.BLOCK, id);
                  BuiltInRegistries.BLOCK
                          .getHolder(key)
                          .ifPresent(holder -> {
                              ((org.betterx.wover.block.impl.BlockHolderBridge) block).wover$setBuiltInRegistryHolder(holder);
                          });
              });
    }

    private static void ensureIntrusiveHolder(Block block) {
        BuiltInRegistries.BLOCK.createIntrusiveHolder(block);
    }

    public static void onRegister(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.BLOCK)) {
            event.register(
                    Registries.BLOCK,
                    helper -> REGISTRIES.values()
                                        .stream()
                                        .sorted(REGISTRY_ORDER)
                                        .forEach(reg -> reg.performBlockRegistration(helper))
            );
        }
    }

    public static void hook(IEventBus bus) {
        bus.addListener(RegisterEvent.class, BlockRegistry::onRegister);
    }
}
