package org.betterx.wover.datagen.impl;

import org.betterx.wover.core.api.ModCore;
import org.betterx.wover.datagen.api.WoverAutoProvider;
import org.betterx.wover.datagen.api.WoverDataProvider;
import org.betterx.wover.datagen.api.WoverTagProvider;
import org.betterx.wover.entrypoint.LibWoverTag;
import org.betterx.wover.tag.api.BlockTagDataProvider;
import org.betterx.wover.tag.api.event.context.TagBootstrapContext;
import org.betterx.wover.tag.api.predefined.CommonBlockTags;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

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
        prepareNetherTerrainTags(provider);

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
                .filter(entry -> modIDs.contains(entry.getKey().location().getNamespace()))
                .forEach(entry -> {
                    addBlockTags(provider, entry.getKey(), entry.getValue());
                });
    }

    /**
     * Adds the common Nether tag foundation to every generated block-tag pack.
     * Fabric runs mod auto-providers independently, so the last provider writing
     * a shared tag must not depend on the optional WorldWeaver datagen source set.
     */
    private static void prepareNetherTerrainTags(TagBootstrapContext<Block> provider) {
        provider.add(CommonBlockTags.NETHER_STONES, BlockTags.BASE_STONE_NETHER);
        provider.add(
                CommonBlockTags.NETHERRACK,
                Blocks.NETHERRACK,
                Blocks.NETHER_QUARTZ_ORE,
                Blocks.NETHER_GOLD_ORE,
                Blocks.CRIMSON_NYLIUM,
                Blocks.WARPED_NYLIUM
        );
        provider.add(CommonBlockTags.NETHER_ORES, Blocks.NETHER_QUARTZ_ORE, Blocks.NETHER_GOLD_ORE);
        provider.add(CommonBlockTags.SOUL_GROUND, Blocks.SOUL_SAND, Blocks.SOUL_SOIL);

        provider.add(
                CommonBlockTags.TERRAIN,
                Blocks.MAGMA_BLOCK,
                Blocks.GRAVEL,
                Blocks.SAND,
                Blocks.RED_SAND,
                Blocks.GLOWSTONE,
                Blocks.BONE_BLOCK,
                Blocks.SCULK,
                Blocks.DIRT,
                Blocks.FARMLAND,
                Blocks.GRASS_BLOCK
        );
        provider.add(
                CommonBlockTags.TERRAIN,
                BlockTags.DRIPSTONE_REPLACEABLE,
                BlockTags.BASE_STONE_OVERWORLD,
                BlockTags.NYLIUM
        );
        provider.addOptional(
                CommonBlockTags.TERRAIN,
                CommonBlockTags.NETHER_TERRAIN,
                CommonBlockTags.MYCELIUM,
                CommonBlockTags.END_STONES
        );

        provider.add(
                CommonBlockTags.NETHER_TERRAIN,
                Blocks.MAGMA_BLOCK,
                Blocks.GRAVEL,
                Blocks.RED_SAND,
                Blocks.GLOWSTONE,
                Blocks.BONE_BLOCK,
                Blocks.BLACKSTONE
        );
        provider.add(CommonBlockTags.NETHER_TERRAIN, BlockTags.NYLIUM);
        provider.addOptional(
                CommonBlockTags.NETHER_TERRAIN,
                CommonBlockTags.NETHERRACK,
                CommonBlockTags.NETHER_ORES,
                CommonBlockTags.SOUL_GROUND,
                CommonBlockTags.NETHER_MYCELIUM
        );

        provider.add(
                BlockTags.NETHER_CARVER_REPLACEABLES,
                Blocks.BASALT,
                Blocks.RED_SAND,
                Blocks.MAGMA_BLOCK,
                Blocks.SCULK
        );
        provider.add(
                BlockTags.NETHER_CARVER_REPLACEABLES,
                CommonBlockTags.NETHER_STONES,
                CommonBlockTags.NETHERRACK
        );
    }

    private void addBlockTags(TagBootstrapContext<Block> provider, ResourceKey<Block> blockKey, Block block) {
        if (block instanceof BlockTagDataProvider tagProvider) {
            tagProvider.addBlockTags(provider);
        }

        try {
            var method = block.getClass().getMethod(
                    "registerBlockTags",
                    net.minecraft.resources.ResourceLocation.class,
                    org.betterx.wover.tag.api.event.context.TagBootstrapContext.class
            );
            method.invoke(block, blockKey.location(), provider);
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            LibWoverTag.C.LOG.warn("Failed to call registerBlockTags on {}", blockKey.location(), e);
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
