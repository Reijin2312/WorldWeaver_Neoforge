package org.betterx.wover.entrypoint;

import org.betterx.wover.core.api.ModCore;
import org.betterx.wover.recipe.datagen.LibWoverRecipeDatagen;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.data.event.GatherDataEvent;
public class LibWoverRecipe {
    public static final ModCore C = ModCore.create("wover-recipe", "wover");

    public LibWoverRecipe(IEventBus modEventBus) {
        C.registerDatapackListener(modEventBus);
        LibWoverRecipeDatagen datagen = new LibWoverRecipeDatagen();
        modEventBus.addListener(GatherDataEvent.Client.class, datagen::onGatherData);
        modEventBus.addListener(GatherDataEvent.Server.class, datagen::onGatherData);
    }
}
