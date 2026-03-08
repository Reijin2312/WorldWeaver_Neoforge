package org.betterx.wover.entrypoint;

import org.betterx.wover.core.api.ModCore;
import org.betterx.wover.datagen.api.WoverDataGenEntryPoint;
import org.betterx.wover.item.impl.AutoItemRegistryTagProvider;
import org.betterx.wover.item.datagen.LibWoverItemDatagen;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.data.event.GatherDataEvent;
public class LibWoverItem {
    public static final ModCore C = ModCore.create("wover-item", "wover");

    public LibWoverItem(IEventBus modEventBus) {
        C.registerDatapackListener(modEventBus);
        LibWoverItemDatagen datagen = new LibWoverItemDatagen();
        modEventBus.addListener(GatherDataEvent.Client.class, datagen::onGatherData);
        modEventBus.addListener(GatherDataEvent.Server.class, datagen::onGatherData);
        //EnchantmentManagerImpl.initialize(); //done in the wover.datapack.registry entrypoint
        WoverDataGenEntryPoint.registerAutoProvider(AutoItemRegistryTagProvider::new);
    }
}
