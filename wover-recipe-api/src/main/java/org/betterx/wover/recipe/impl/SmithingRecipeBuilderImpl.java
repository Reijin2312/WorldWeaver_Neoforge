package org.betterx.wover.recipe.impl;

import org.betterx.wover.recipe.api.SmithingRecipeBuilder;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import org.jetbrains.annotations.NotNull;

public class SmithingRecipeBuilderImpl extends BaseRecipeBuilderImpl<SmithingRecipeBuilder> implements SmithingRecipeBuilder {
    protected Ingredient template;
    protected Ingredient base;
    protected Ingredient addon;

    public SmithingRecipeBuilderImpl(
            @NotNull Identifier id,
            @NotNull ItemLike output
    ) {
        super(id, output);
    }

    @Override
    public SmithingRecipeBuilderImpl template(SmithingTemplateItem in) {
        this.template = Ingredient.of(in);
        unlockedBy(in);
        return this;
    }

    @Override
    public SmithingRecipeBuilderImpl base(TagKey<Item> in) {
        this.base = ingredientOf(in);
        return this;
    }

    @Override
    public SmithingRecipeBuilderImpl base(ItemLike in) {
        this.base = Ingredient.of(in);
        return this;
    }

    @Override
    public SmithingRecipeBuilderImpl base(Ingredient in) {
        this.base = in;
        return this;
    }

    @Override
    public SmithingRecipeBuilderImpl addon(TagKey<Item> in) {
        this.addon = ingredientOf(in);
        return this;
    }

    @Override
    public SmithingRecipeBuilderImpl addon(ItemLike in) {
        this.addon = Ingredient.of(in);
        return this;
    }

    @Override
    public SmithingRecipeBuilderImpl addon(Ingredient in) {
        this.addon = in;
        return this;
    }

    @Override
    protected void validate() {
        super.validate();

        if (template == null) {
            throwIllegalStateException("Template must be set");
        }
        if (base == null) {
            throwIllegalStateException("Base must be set");
        }
        if (addon == null) {
            throwIllegalStateException("Addon must be set");
        }
        if (output.getCount() != 1) {
            throwIllegalStateException("Output count must be 1");
        }
    }

    @Override
    public void build(RecipeOutput ctx) {
        final SmithingTransformRecipeBuilder builder = SmithingTransformRecipeBuilder.smithing(
                template, base, addon, category, output.getItem()
        );

        for (var item : unlocks.entrySet()) {
            builder.unlocks(item.getKey(), item.getValue());
        }
        builder.save(ctx, recipeKey(id));
    }
}
