package org.betterx.wover.tabs.api.interfaces;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public interface CreativeTabPredicate {
    CreativeTabPredicate BLOCKS = item -> item instanceof BlockItem;
    CreativeTabPredicate ITEMS = item -> !(item instanceof BlockItem);

    boolean contains(Item item);
}
