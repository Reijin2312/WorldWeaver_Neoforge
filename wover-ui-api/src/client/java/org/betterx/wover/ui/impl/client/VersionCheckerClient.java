package org.betterx.wover.ui.impl.client;

import org.betterx.wover.config.api.client.ClientConfigs;
import org.betterx.wover.core.api.ModCore;
import org.betterx.wover.ui.api.VersionChecker;

import net.minecraft.client.gui.screens.Screen;


import java.util.List;
import java.util.function.Function;

public class VersionCheckerClient extends VersionChecker {
    public static void presentUpdateScreen(List<Function<Runnable, Screen>> screens) {
        VersionChecker.startCheck(ModCore.isClient());

        if (!ClientConfigs.CLIENT.didPresentWelcomeScreen.get()) {
            screens.add(WelcomeScreen::new);
        } else if (ClientConfigs.CLIENT.checkForNewVersions.get() && !VersionChecker.isEmpty()) {
            screens.add(UpdatesScreen::new);
        }
    }
}
