package org.betterx.wover.datagen.impl;

import org.betterx.wover.core.api.ModCore;
import org.betterx.wover.datagen.api.WoverAutoProvider;
import org.betterx.wover.datagen.api.WoverDataProvider;
import org.betterx.wover.datagen.api.WoverTagProvider;
import org.betterx.wover.entrypoint.LibWoverTag;
import org.betterx.wover.tag.api.ItemTagDataProvider;
import org.betterx.wover.tag.api.event.context.ItemTagBootstrapContext;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.LinkedList;
import java.util.List;
import org.jetbrains.annotations.Nullable;


/**
 * Creates item tags for all blocks or items that implement {@link ItemTagDataProvider}
 * and are registered in the namespace of this mod.
 * <p>
 * This provider is automatically registered to the global datapack by {@link org.betterx.wover.datagen.api.WoverDataGenEntryPoint}.
 */
public class AutoItemTagProvider extends WoverTagProvider.ForItems implements WoverAutoProvider.WithRedirect {
    private final List<WoverTagProvider<Item, ItemTagBootstrapContext>> redirects = new LinkedList<>();

    public AutoItemTagProvider(
            ModCore modCore
    ) {
        super(modCore);
    }

    @Override
    public void prepareTags(ItemTagBootstrapContext provider) {
        redirects.forEach(redirect -> {
            LibWoverTag.C.LOG.debug(
                    "   {} includes {} for {}",
                    this.getClass().getSimpleName(),
                    redirect.getClass().getSimpleName(),
                    redirect.modCore.namespace
            );
            redirect.prepareTags(provider);
        });

        BuiltInRegistries.BLOCK
                .entrySet()
                .stream()
                .filter(entry -> {
                    assert modIDs != null;
                    return modIDs.contains(entry.getKey().identifier().getNamespace());
                })
                .forEach(entry -> addBlockItemTags(provider, entry.getKey(), entry.getValue()));

        BuiltInRegistries.ITEM
                .entrySet()
                .stream()
                .filter(entry -> {
                    assert modIDs != null;
                    return modIDs.contains(entry.getKey().identifier().getNamespace());
                })
                .forEach(entry -> addItemTags(provider, entry.getKey(), entry.getValue()));
    }

    private void addBlockItemTags(ItemTagBootstrapContext provider, ResourceKey<Block> blockKey, Block block) {
        if (block instanceof ItemTagDataProvider tagProvider) {
            tagProvider.addItemTags(provider);
        }
    }

    private void addItemTags(ItemTagBootstrapContext provider, ResourceKey<Item> itemKey, Item item) {
        if (item instanceof ItemTagDataProvider tagProvider) {
            tagProvider.addItemTags(provider);
        }
    }

    @Override
    public @Nullable <T extends DataProvider> WoverDataProvider<T> redirect(@Nullable WoverDataProvider<T> provider) {
        if (provider instanceof WoverTagProvider<?, ?> tagProvider) {
            if (tagProvider.tagRegistry == this.tagRegistry) {
                LibWoverTag.C.LOG.debug("Redirecting {} to {}", tagProvider.getClass().getName(), this
                        .getClass()
                        .getName());
                this.mergeAllowedAndForced((WoverTagProvider<Item, ItemTagBootstrapContext>) tagProvider);
                redirects.add((WoverTagProvider<Item, ItemTagBootstrapContext>) tagProvider);
                return null;
            }
        }
        return provider;
    }
}
