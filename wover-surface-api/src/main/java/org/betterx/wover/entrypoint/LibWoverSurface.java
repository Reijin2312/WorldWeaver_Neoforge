package org.betterx.wover.entrypoint;


import org.betterx.wover.core.api.ModCore;
import org.betterx.wover.surface.impl.SurfaceRuleRegistryImpl;
import org.betterx.wover.surface.impl.conditions.MaterialConditionRegistryImpl;
import org.betterx.wover.surface.impl.numeric.NumericProviderRegistryImpl;
import org.betterx.wover.surface.impl.rules.MaterialRuleRegistryImpl;
import org.betterx.wover.surface.datagen.WoverSurfaceDatagen;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.data.event.GatherDataEvent;
public class LibWoverSurface {
    public static final ModCore C = ModCore.create("wover-surface", "wover");

    public LibWoverSurface(IEventBus modEventBus) {
        C.registerDatapackListener(modEventBus);
        WoverSurfaceDatagen datagen = new WoverSurfaceDatagen();
        modEventBus.addListener(GatherDataEvent.Client.class, datagen::onGatherData);
        modEventBus.addListener(GatherDataEvent.Server.class, datagen::onGatherData);
        modEventBus.addListener(net.neoforged.neoforge.registries.RegisterEvent.class, MaterialConditionRegistryImpl::register);
        modEventBus.addListener(net.neoforged.neoforge.registries.RegisterEvent.class, MaterialRuleRegistryImpl::register);
        NumericProviderRegistryImpl.bootstrap();
        SurfaceRuleRegistryImpl.initialize();
    }
}
