package org.betterx.wover.testmod.entrypoint.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = "wover-common-testmod", value = Dist.CLIENT)
public class TestModWoverCommonClient {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // no-op placeholder to satisfy automatic subscriber requirements
    }
}
