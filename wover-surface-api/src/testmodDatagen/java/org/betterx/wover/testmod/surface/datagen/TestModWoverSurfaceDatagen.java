package org.betterx.wover.testmod.surface.datagen;

import org.betterx.wover.core.api.ModCore;
import org.betterx.wover.datagen.api.PackBuilder;
import org.betterx.wover.datagen.api.WoverDataGenEntryPoint;
import org.betterx.wover.testmod.entrypoint.TestModWoverSurface;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import net.neoforged.neoforge.data.event.GatherDataEvent;

public class TestModWoverSurfaceDatagen extends WoverDataGenEntryPoint {

    @Override
    protected void onInitializeProviders(PackBuilder globalPackBuilder) {
        globalPackBuilder
                .addRegistryProvider(SurfaceRuleProvider::new);

        addDatapack(TestModWoverSurface.ADDON_PACK)
                .callOnInitializeDatapack(this::onInitializeAddonDatapack)
                .addRegistryProvider(AddonSurfaceRuleProvider::new);
    }

    @Override
    protected ModCore modCore() {
        return TestModWoverSurface.C;
    }

    void onInitializeAddonDatapack(
            GatherDataEvent event,
            PackOutput packOutput,
            Identifier location
    ) {
        modCore().log.info("Initializing addon datapack: " + location);
    }

}
