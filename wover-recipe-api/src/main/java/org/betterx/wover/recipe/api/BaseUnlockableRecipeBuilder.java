package org.betterx.wover.recipe.api;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

public interface BaseUnlockableRecipeBuilder<I extends BaseRecipeBuilder<I>> {
    I shouldUnlockAdvancements(boolean shouldUnlockAdvancements);

    I unlockedBy(ItemLike item);
    I unlockedBy(TagKey<Item> tag);
    I unlockedBy(ItemLike... items);
    I unlocks(String name, ItemLike... items);
}
