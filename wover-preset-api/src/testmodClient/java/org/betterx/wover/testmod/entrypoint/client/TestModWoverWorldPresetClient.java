package org.betterx.wover.testmod.entrypoint.client;

import de.ambertation.wunderlib.ui.layout.components.LayoutComponent;
import de.ambertation.wunderlib.ui.layout.components.VerticalStack;
import de.ambertation.wunderlib.ui.vanilla.LayoutScreen;
import org.betterx.wover.preset.api.client.WorldPresetsUI;
import org.betterx.wover.testmod.entrypoint.TestModWoverWorldPreset;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.network.chat.Component;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = "wover-preset-testmod", value = Dist.CLIENT)
public class TestModWoverWorldPresetClient {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        WorldPresetsUI.registerCustomUI(TestModWoverWorldPreset.END_START, new PresetEditor() {
            @Override
            public Screen createEditScreen(
                    CreateWorldScreen createWorldScreen,
                    WorldCreationContext worldCreationContext
            ) {
                return new LayoutScreen(createWorldScreen, Component.literal("End Start")) {

                    @Override
                    protected LayoutComponent<?, ?> initContent() {
                        return new VerticalStack(fill(), fit()).centerHorizontal();
                    }
                };
            }
        });

        WorldPresetsUI.registerCustomUI(TestModWoverWorldPreset.NETHER_START, new PresetEditor() {
            @Override
            public Screen createEditScreen(
                    CreateWorldScreen createWorldScreen,
                    WorldCreationContext worldCreationContext
            ) {
                return new LayoutScreen(createWorldScreen, Component.literal("Nether Start")) {

                    @Override
                    protected LayoutComponent<?, ?> initContent() {
                        return new VerticalStack(fill(), fit()).centerHorizontal();
                    }
                };
            }
        });
    }
}
