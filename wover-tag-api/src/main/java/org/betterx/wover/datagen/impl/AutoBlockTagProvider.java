package org.betterx.wover.datagen.impl;

import org.betterx.wover.core.api.ModCore;
import org.betterx.wover.datagen.api.WoverAutoProvider;
import org.betterx.wover.datagen.api.WoverDataProvider;
import org.betterx.wover.datagen.api.WoverTagProvider;
import org.betterx.wover.entrypoint.LibWoverTag;
import org.betterx.wover.tag.api.BlockTagDataProvider;
import org.betterx.wover.tag.api.event.context.TagBootstrapContext;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;

import java.util.LinkedList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Creates block tags for all blocks that implement {@link BlockTagDataProvider}
 * and are registered in the namespace of this mod.
 * <p>
 * This provider is automatically registered to the global datapack by {@link org.betterx.wover.datagen.api.WoverDataGenEntryPoint}.
 */
public class AutoBlockTagProvider extends WoverTagProvider.ForBlocks implements WoverAutoProvider.WithRedirect {
    private final List<WoverTagProvider<Block, TagBootstrapContext<Block>>> redirects = new LinkedList<>();

    public AutoBlockTagProvider(
            ModCore modCore
    ) {
        super(modCore);
    }

    @Override
    public void prepareTags(TagBootstrapContext<Block> provider) {
        redirects.forEach(redirect -> {
            LibWoverTag.C.LOG.debug(
                    "   {} includes {} for {}",
                    this.getClass().getSimpleName(),
                    redirect.getClass().getSimpleName(),
                    redirect.modCore.namespace
            );
            redirect.prepareTags(provider);
        });

        BuiltInRegistries.BLOCK
                .entrySet()
                .stream()
                .filter(entry -> modIDs.contains(entry.getKey().identifier().getNamespace()))
                .forEach(entry -> {
                    addBlockTags(provider, entry.getKey(), entry.getValue());
                });
    }

    private void addBlockTags(TagBootstrapContext<Block> provider, ResourceKey<Block> blockKey, Block block) {
        if (block instanceof BlockTagDataProvider tagDataProvider) {
            tagDataProvider.addBlockTags(provider);
        }

        // Compatibility: avoid hard dependency on wover-block-api by invoking registerBlockTags via reflection
        try {
            var method = block.getClass().getMethod(
                    "registerBlockTags",
                    net.minecraft.resources.Identifier.class,
                    org.betterx.wover.tag.api.event.context.TagBootstrapContext.class
            );
            method.invoke(block, blockKey.identifier(), provider);
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            LibWoverTag.C.LOG.warn("Failed to call registerBlockTags on {}", blockKey.identifier(), e);
        }
    }


    @Override
    public @Nullable <T extends DataProvider> WoverDataProvider<T> redirect(@Nullable WoverDataProvider<T> provider) {
        if (provider instanceof WoverTagProvider<?, ?> tagProvider) {
            if (tagProvider.tagRegistry == this.tagRegistry) {
                LibWoverTag.C.LOG.debug("Redirecting {}  to {} ({})",
                        tagProvider.getClass().getName(),
                        this.getClass().getName(), this.modIDs
                );
                this.mergeAllowedAndForced((WoverTagProvider<Block, TagBootstrapContext<Block>>) tagProvider);
                redirects.add((WoverTagProvider<Block, TagBootstrapContext<Block>>) tagProvider);
                return null;
            }
        }
        return provider;
    }
}
