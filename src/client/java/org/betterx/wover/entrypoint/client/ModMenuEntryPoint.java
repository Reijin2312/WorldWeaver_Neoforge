package org.betterx.wover.entrypoint.client;

import org.betterx.wover.screens.impl.MainMenu;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@EventBusSubscriber(modid = "wover", value = Dist.CLIENT)
public final class ModMenuEntryPoint {
    private ModMenuEntryPoint() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        IConfigScreenFactory factory = (modContainer, parent) -> new MainMenu(parent);
        ModList.get()
               .getModContainerById("wover")
               .ifPresent(container -> container.registerExtensionPoint(IConfigScreenFactory.class, factory));
    }
}
