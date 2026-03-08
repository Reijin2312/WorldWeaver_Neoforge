package org.betterx.wover.entrypoint;

import org.betterx.wover.block.impl.predicate.BlockPredicatesImpl;
import org.betterx.wover.core.api.ModCore;
import org.betterx.wover.datagen.api.WoverDataGenEntryPoint;
import org.betterx.wover.datagen.api.provider.AutoBlockRegistryTagProvider;
import org.betterx.wover.poi.impl.PoiManagerImpl;
import net.neoforged.bus.api.IEventBus;

public class LibWoverBlock {
    public static final ModCore C = ModCore.create("wover-block", "wover");

    public LibWoverBlock(IEventBus modEventBus) {
        C.registerDatapackListener(modEventBus);
        //make sure the Datagen will automatically include all Tags assigned to Blocks in the BlockRegistry
        WoverDataGenEntryPoint.registerAutoProvider(AutoBlockRegistryTagProvider::new);

        modEventBus.addListener(BlockPredicatesImpl::register);
        BlockPredicatesImpl.ensureStaticInitialization();
        PoiManagerImpl.registerAll();
    }
}
