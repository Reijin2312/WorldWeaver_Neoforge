package org.betterx.wover.block.impl;

import org.betterx.wover.item.api.ItemTagProvider;
import org.betterx.wover.tag.api.event.context.ItemTagBootstrapContext;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

import org.jetbrains.annotations.NotNull;

public class WoverBlockItemImpl extends BlockItem implements ItemTagProvider {
    private final @NotNull ItemTagProvider tagProvider;

    private WoverBlockItemImpl(@NotNull Block block, Properties properties) {
        super(block, properties);
        this.tagProvider = (ItemTagProvider) block;
    }

    public static BlockItem create(Block block, Properties properties) {
        if (block instanceof ItemTagProvider) {
            return new WoverBlockItemImpl(block, properties);
        }
        return new BlockItem(block, properties);
    }

    @Override
    public void registerItemTags(Identifier location, ItemTagBootstrapContext context) {
        this.tagProvider.registerItemTags(location, context);
    }
}
