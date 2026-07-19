package org.betterx.wover.recipe.impl;

import org.betterx.wover.recipe.api.CraftingRecipeBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CraftingRecipeBuilderImpl extends BaseRecipeBuilderImpl<CraftingRecipeBuilder> implements
        CraftingRecipeBuilder {
    private String[] shape;
    protected boolean showNotification;
    protected final Map<Character, Ingredient> materials;

    public CraftingRecipeBuilderImpl(Identifier id, ItemLike output) {
        super(id, output);
        this.showNotification = true;
        this.materials = new HashMap<>();
    }

    @Override
    public CraftingRecipeBuilder addMaterial(char key, TagKey<Item> value) {
        unlockedBy(value);
        return _addMaterial(key, ingredientOf(value));
    }

    @Override
    public CraftingRecipeBuilder addMaterial(char key, ItemStack... values) {
        unlockedBy(values);
        return _addMaterial(key, Ingredient.of(Arrays.stream(values).map(ItemStack::getItem)));
    }

    @Override
    public CraftingRecipeBuilder addMaterial(char key, ItemLike... values) {
        unlockedBy(values);
        return _addMaterial(key, Ingredient.of(values));
    }

    @Override
    public CraftingRecipeBuilderImpl addMaterial(char key, Ingredient value) {
        unlockedBy(value);
        return _addMaterial(key, value);
    }

    public CraftingRecipeBuilderImpl _addMaterial(char key, Ingredient value) {
        materials.put(key, value);
        return this;
    }

    @Override
    public CraftingRecipeBuilderImpl shape(String... shape) {
        if (shape.length != 0) {
            for (int i = 1; i < shape.length; i++) {
                if (shape[i].length() != shape[0].length()) {
                    throw new IllegalArgumentException("Shape must be rectangular");
                }
            }
        }
        this.shape = shape;
        return this;
    }

    @Override
    public CraftingRecipeBuilder shapeless() {
        this.shape = null;
        return this;
    }

    @Override
    public CraftingRecipeBuilder showNotification() {
        this.showNotification = true;
        return this;
    }

    protected boolean isShaped() {
        return shape != null && shape.length > 0;
    }

    @Override
    protected void validate() {
        super.validate();
        if (isShaped()) {
            for (String row : shape) {
                for (char c : row.toCharArray()) {
                    if (c != ' ' && materials.get(c) == null)
                        throwIllegalStateException("Material for character '" + c + "' is not set");
                }
            }

            for (Character m : materials.keySet()) {
                if (m == ' ') {
                    throwIllegalStateException("Material-key for character ' ' is not allowed");
                }
                //if letter is not used in shape array then throw
                if (Arrays.stream(shape).noneMatch(row -> row.contains(m.toString()))) {
                    throwIllegalStateException("Material for character '" + m + "' is not used in shape");
                }
            }
        }
    }

    private void buildShaped(RecipeOutput ctx) {
        var builder = ShapedRecipeBuilder.shaped(BuiltInRegistries.ITEM, category, outputItem(), outputCount());

        for (Map.Entry<Character, Ingredient> mat : materials.entrySet()) {
            builder.define(mat.getKey(), mat.getValue());
        }

        for (String row : shape) builder.pattern(row);

        if (shouldUnlockAdvancements) {
            for (var item : unlocks.entrySet()) {
                builder.unlockedBy(item.getKey(), item.getValue());
            }
        }

        builder.showNotification(this.showNotification);
        builder.group(this.group);
        builder.save(ctx, recipeKey(id));
    }

    private void buildShapeless(RecipeOutput ctx) {
        var builder = ShapelessRecipeBuilder.shapeless(BuiltInRegistries.ITEM, category, outputTemplate());

        for (Map.Entry<Character, Ingredient> mat : materials.entrySet()) {
            builder.requires(mat.getValue());
        }

        if (shouldUnlockAdvancements) {
            for (var item : unlocks.entrySet()) {
                builder.unlockedBy(item.getKey(), item.getValue());
            }
        }

        builder.group(this.group);
        builder.save(ctx, recipeKey(id));
    }

    @Override
    public void build(RecipeOutput ctx) {
        validate();
        if (isShaped()) {
            buildShaped(ctx);
        } else {
            buildShapeless(ctx);
        }
    }
}

