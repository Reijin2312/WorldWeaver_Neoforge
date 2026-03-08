package org.betterx.wover.datagen.api.provider;

import org.betterx.wover.core.api.ModCore;
import org.betterx.wover.datagen.api.WoverDataProvider;
import org.betterx.wover.datagen.api.WoverRecipeGenerator;
import org.betterx.wover.recipe.impl.WoverRecipeProviderAccess;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;

import java.util.concurrent.CompletableFuture;

public abstract class WoverRecipeProvider implements WoverDataProvider<net.minecraft.data.DataProvider>, WoverRecipeGenerator {
    /**
     * The title of the provider. Mainly used for logging.
     */
    public final String title;

    /**
     * The ModCore instance of the Mod that is providing this instance.
     */
    protected final ModCore modCore;

    public WoverRecipeProvider(
            ModCore modCore,
            String title
    ) {
        this.title = title;
        this.modCore = modCore;
    }

    protected abstract void bootstrap(HolderLookup.Provider provider, RecipeOutput context);

    @Override
    public void buildRecipes(HolderLookup.Provider lookup, RecipeOutput exporter) {
        WoverRecipeProviderAccess.withLookup(lookup, () -> bootstrap(lookup, exporter));
    }

    @Override
    public net.minecraft.data.DataProvider getProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> registriesFuture
    ) {
        return new RecipeProvider.Runner(output, registriesFuture) {
            @Override
            public String getName() {
                return "Recipes: " + modCore.namespace + "/" + title;
            }

            @Override
            protected RecipeProvider createRecipeProvider(HolderLookup.Provider lookup, RecipeOutput output) {
                return new RecipeProvider(lookup, output) {
                    @Override
                    protected void buildRecipes() {
                        WoverRecipeProviderAccess.withLookup(lookup, () -> bootstrap(lookup, output));
                    }
                };
            }
        };
    }
}
