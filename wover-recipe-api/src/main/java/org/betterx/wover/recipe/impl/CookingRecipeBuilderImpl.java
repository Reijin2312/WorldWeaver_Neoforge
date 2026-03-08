package org.betterx.wover.recipe.impl;

import org.betterx.wover.recipe.api.CookingRecipeBuilder;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

public class CookingRecipeBuilderImpl extends BaseRecipeBuilderImpl<CookingRecipeBuilder> implements CookingRecipeBuilder {
    protected float xp;
    protected int cookingTime;
    protected Ingredient input;

    protected boolean blasting, campfire, smoker, smelting;

    public CookingRecipeBuilderImpl(
            Identifier id,
            ItemLike output,
            boolean blasting,
            boolean campfire,
            boolean smoker,
            boolean smelting
    ) {
        super(id, output);
        this.xp = 0;
        this.cookingTime = 200;
        this.blasting = blasting;
        this.campfire = campfire;
        this.smoker = smoker;
        this.smelting = smelting;
    }

    public CookingRecipeBuilder experience(float xp) {
        this.xp = xp;
        return this;
    }

    public CookingRecipeBuilder cookingTime(int time) {
        this.cookingTime = time;
        return this;
    }

    public CookingRecipeBuilder enableSmelter() {
        this.smelting = true;
        return this;
    }

    public CookingRecipeBuilder disableSmelter() {
        this.smelting = false;
        return this;
    }

    public CookingRecipeBuilder enableBlastFurnace() {
        this.blasting = true;
        return this;
    }

    public CookingRecipeBuilder disableBlastFurnace() {
        this.blasting = false;
        return this;
    }

    public CookingRecipeBuilder enableCampfire() {
        this.campfire = true;
        return this;
    }

    public CookingRecipeBuilder disableCampfire() {
        this.campfire = false;
        return this;
    }

    public CookingRecipeBuilder enableSmoker() {
        this.smoker = true;
        return this;
    }

    public CookingRecipeBuilder disableSmoker() {
        this.smoker = false;
        return this;
    }

    public CookingRecipeBuilder input(TagKey<Item> input) {
        return input(ingredientOf(input));
    }

    public CookingRecipeBuilder input(ItemLike input) {
        return input(Ingredient.of(input));
    }

    public CookingRecipeBuilder input(Ingredient input) {
        this.input = input;
        unlockedBy(input);
        return this;
    }

    @Override
    protected void validate() {
        super.validate();

        if (!smelting && !blasting && !campfire && !smoker) {
            throwIllegalStateException("No target (smelting, blasting, campfire or somer) for cooking recipe was selected");
        }

        if (cookingTime < 0) {
            throwIllegalStateException("cooking time must be positive. Recipe {} will be ignored!");
        }
    }

    @Override
    public void build(RecipeOutput ctx) {
        if (smelting) {
            buildRecipe(
                    ctx, "smelting",
                    SimpleCookingRecipeBuilder.smelting(
                            input,
                            category,
                            output.getItem(),
                            xp,
                            cookingTime
                    )
            );
        }

        if (blasting) {
            buildRecipe(
                    ctx, "blasting",
                    SimpleCookingRecipeBuilder.blasting(
                            input,
                            category,
                            output.getItem(),
                            xp,
                            cookingTime / 2
                    )
            );
        }

        if (campfire) {
            buildRecipe(
                    ctx, "campfire",
                    SimpleCookingRecipeBuilder.campfireCooking(
                            input,
                            category,
                            output.getItem(),
                            xp,
                            cookingTime * 3
                    )
            );
        }

        if (smoker) {
            buildRecipe(
                    ctx, "smoker",
                    SimpleCookingRecipeBuilder.campfireCooking(
                            input,
                            category,
                            output.getItem(),
                            xp,
                            cookingTime / 2
                    )
            );
        }
    }

    private void buildRecipe(RecipeOutput ctx, String suffix, SimpleCookingRecipeBuilder builder) {
        Identifier loc = id.withSuffix("_" + suffix);

        for (var item : unlocks.entrySet()) {
            builder.unlockedBy(item.getKey(), item.getValue());
        }
        builder.save(ctx, recipeKey(loc));
    }
}
