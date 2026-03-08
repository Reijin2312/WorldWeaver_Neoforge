package org.betterx.wover.recipe.impl;

import org.betterx.wover.recipe.api.StonecutterRecipeBuilder;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import org.jetbrains.annotations.NotNull;

public class StonecutterRecipeBuilderImpl extends BaseRecipeBuilderImpl<StonecutterRecipeBuilder> implements StonecutterRecipeBuilder {
    Ingredient input;

    public StonecutterRecipeBuilderImpl(
            @NotNull Identifier id,
            @NotNull ItemLike output
    ) {
        super(id, output);
    }


    public StonecutterRecipeBuilder input(TagKey<Item> input) {
        return input(ingredientOf(input));
    }

    public StonecutterRecipeBuilder input(ItemLike input) {
        return input(Ingredient.of(input));
    }

    public StonecutterRecipeBuilder input(Ingredient input) {
        this.input = input;
        unlockedBy(input);
        return this;
    }

    @Override
    protected void validate() {
        super.validate();

        if (input == null) {
            throwIllegalStateException("Input must be set");
        }
    }

    @Override
    public void build(RecipeOutput ctx) {
        final SingleItemRecipeBuilder builder = SingleItemRecipeBuilder.stonecutting(
                input, category, output.getItem(), output.getCount()
        );

        for (var item : unlocks.entrySet()) {
            builder.unlockedBy(item.getKey(), item.getValue());
        }

        builder.group(group);
        builder.save(ctx, recipeKey(id));
    }
}
