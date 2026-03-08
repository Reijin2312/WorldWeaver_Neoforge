package org.betterx.wover.tabs.api;

import org.betterx.wover.core.api.ModCore;
import org.betterx.wover.tabs.api.interfaces.CreativeTabsBuilder;
import org.betterx.wover.tabs.impl.CreativeTabManagerImpl;

public class CreativeTabs {
    public static CreativeTabsBuilder start(ModCore modCore) {
        return new CreativeTabManagerImpl(modCore);
    }
}
