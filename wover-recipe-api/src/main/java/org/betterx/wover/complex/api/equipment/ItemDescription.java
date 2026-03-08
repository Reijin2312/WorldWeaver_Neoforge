package org.betterx.wover.complex.api.equipment;

import org.betterx.wover.core.api.ModCore;
import org.betterx.wover.item.api.ItemRegistry;
import org.betterx.wover.recipe.api.CraftingRecipeBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

import java.util.function.Supplier;

abstract class ItemDescription<I extends Item> {
    public final I item;
    public final Identifier location;

    public ItemDescription(
            ModCore modCore,
            String path,
            Supplier<I> creator,
            TagKey<Item>... tags
    ) {
        this.location = modCore.mk(path);
        this.item = ItemRegistry.withConstructionId(this.location, creator::get);
        ItemRegistry.forMod(modCore).registerAsTool(path, item, tags);
    }

    static boolean buildToolRecipe(ToolSlot slot, ItemLike stick, CraftingRecipeBuilder builder) {
        if (slot == ToolSlot.SHEARS_SLOT) {
            builder.shape(" #", "# ");
            return false;
        }

        builder.addMaterial('I', stick);
        switch (slot) {
            case PICKAXE_SLOT, HAMMER_SLOT -> builder.shape("###", " I ", " I ");
            case AXE_SLOT -> builder.shape("##", "#I", " I");
            case HOE_SLOT -> builder.shape("##", " I", " I");
            case SHOVEL_SLOT -> builder.shape("#", "I", "I");
            case SWORD_SLOT -> builder.shape("#", "#", "I");
            default -> {
                return true;
            }
        }

        return false;
    }

    static boolean buildArmorRecipe(ArmorSlot slot, CraftingRecipeBuilder builder) {
        return switch (slot) {
            case BOOTS_SLOT -> {
                builder.shape("# #", "# #");
                yield false;
            }
            case HELMET_SLOT -> {
                builder.shape("###", "# #");
                yield false;
            }
            case CHESTPLATE_SLOT -> {
                builder.shape("# #", "###", "###");
                yield false;
            }
            case LEGGINGS_SLOT -> {
                builder.shape("###", "# #", "# #");
                yield false;
            }
            default -> true;
        };
    }

    public I getItem() {
        return item;
    }
}
