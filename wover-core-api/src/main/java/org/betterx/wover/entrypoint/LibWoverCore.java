package org.betterx.wover.entrypoint;


import org.betterx.wover.config.api.Configs;
import org.betterx.wover.core.api.ModCore;
import org.betterx.wover.core.impl.registry.DatapackRegistryBuilderImpl;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
public class LibWoverCore {
    public static final ModCore C = ModCore.create("wover-core", "wover");

    public LibWoverCore(IEventBus modEventBus) {
        C.registerDatapackListener(modEventBus);
        modEventBus.addListener(DataPackRegistryEvent.NewRegistry.class, DatapackRegistryBuilderImpl::registerDatapackRegistries);
        Configs.saveConfigs();
    }
}
