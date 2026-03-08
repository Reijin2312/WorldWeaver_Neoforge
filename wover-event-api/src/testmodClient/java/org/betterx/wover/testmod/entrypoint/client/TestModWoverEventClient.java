package org.betterx.wover.testmod.entrypoint.client;

import org.betterx.wover.events.api.client.ClientWorldLifecycle;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = "wover-events-testmod", value = Dist.CLIENT)
public class TestModWoverEventClient {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ClientWorldLifecycle.BEFORE_CLIENT_LOAD_SCREEN.subscribe((levelStorageAccess, continueWith) -> {
            System.out.println("Before client load screen: \n - " + levelStorageAccess + "\n - " + continueWith);
            continueWith.loadingScreen();
        });

        ClientWorldLifecycle.BEFORE_CLIENT_LOAD_SCREEN.subscribe((levelStorageAccess, continueWith) -> {
            System.out.println("Before client load screen II: \n - " + levelStorageAccess + "\n - " + continueWith);
            continueWith.loadingScreen();
        });

        ClientWorldLifecycle.BEFORE_CLIENT_LOAD_SCREEN.subscribe((levelStorageAccess, continueWith) -> {
            System.out.println("Before client load screen III: \n - " + levelStorageAccess + "\n - " + continueWith);
            continueWith.loadingScreen();
        });

        ClientWorldLifecycle.ALLOW_EXPERIMENTAL_WARNING_SCREEN.subscribe((bl) -> {
            System.out.println("Allow experimental warning screen: " + bl);
            return false;
        });
    }
}
