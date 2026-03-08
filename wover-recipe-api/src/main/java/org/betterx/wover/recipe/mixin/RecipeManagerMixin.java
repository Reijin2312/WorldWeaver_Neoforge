package org.betterx.wover.recipe.mixin;

import org.betterx.wover.entrypoint.LibWoverRecipe;
import org.betterx.wover.recipe.impl.RecipeRuntimeProviderImpl;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeManager;

import com.google.common.base.Stopwatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {
    @Shadow
    private RecipeMap recipes;

    @Inject(method = "apply(Lnet/minecraft/world/item/crafting/RecipeMap;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("TAIL"))
    void wover_apply(
            RecipeMap map,
            ResourceManager resourceManager,
            ProfilerFiller profilerFiller,
            CallbackInfo ci
    ) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        final int count = this.recipes.values().size();
        RecipeRuntimeProviderImpl.LoadedRecipes loaded = RecipeRuntimeProviderImpl.loadedRecipes(
                new RecipeRuntimeProviderImpl.LoadedRecipes(this.recipes.values().stream().toList())
        );
        this.recipes = RecipeMap.create(loaded.recipes());
        stopwatch.stop();

        LibWoverRecipe.C.LOG.info(
                "Added {} recipes in {}ms",
                this.recipes.values().size() - count,
                stopwatch.elapsed().toMillis()
        );
    }
}
