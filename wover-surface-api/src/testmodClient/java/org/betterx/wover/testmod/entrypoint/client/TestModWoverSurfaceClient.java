package org.betterx.wover.testmod.entrypoint.client;

import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = "wover-surface-testmod", value = Dist.CLIENT)
public class TestModWoverSurfaceClient {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        CreateWorldScreen s;
    }
}
