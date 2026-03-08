package org.betterx.wover.entrypoint.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = "wover", value = Dist.CLIENT)
public final class LibWoverCommonClient {
    private LibWoverCommonClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // intentionally empty: presence satisfies NeoForge automatic subscriber checks
    }
}
