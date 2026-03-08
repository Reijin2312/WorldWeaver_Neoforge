package org.betterx.wover.entrypoint.client;

import org.betterx.wover.config.api.client.ClientConfigs;
import org.betterx.wover.events.api.client.ClientWorldLifecycle;
import org.betterx.wover.ui.impl.client.VersionCheckerClient;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = "wover", value = Dist.CLIENT)
public class LibWoverUiClient {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ClientWorldLifecycle.ALLOW_EXPERIMENTAL_WARNING_SCREEN.subscribe((show) -> {
            if (ClientConfigs.CLIENT.disableExperimentalWarning.get()) {
                return false;
            }
            return show;
        });

        ClientWorldLifecycle.ENUMERATE_STARTUP_SCREENS.subscribe(VersionCheckerClient::presentUpdateScreen);

        ClientConfigs.saveConfigs();
    }
}
