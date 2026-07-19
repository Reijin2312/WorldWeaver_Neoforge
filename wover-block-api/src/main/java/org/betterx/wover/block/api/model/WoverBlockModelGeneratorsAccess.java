package org.betterx.wover.block.api.model;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelOutput;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class WoverBlockModelGeneratorsAccess extends BlockModelGenerators {
    private final Set<Item> skippedAutoModels;

    @SuppressWarnings("rawtypes")
    public WoverBlockModelGeneratorsAccess(
            Consumer<Object> blockStateOutput,
            ItemModelOutput itemModelOutput,
            BiConsumer<Identifier, net.minecraft.client.data.models.model.ModelInstance> modelOutput,
            Set<Item> skippedAutoModels
    ) {
        super((Consumer) blockStateOutput, itemModelOutput, modelOutput);
        this.skippedAutoModels = skippedAutoModels;
    }

    @SuppressWarnings("unchecked")
    public Consumer<Object> blockStateOutput() {
        return (Consumer<Object>) (Consumer<?>) this.blockStateOutput;
    }

    public BiConsumer<Identifier, net.minecraft.client.data.models.model.ModelInstance> modelOutput() {
        return this.modelOutput;
    }

    public Map<Block, TexturedModel> texturedModels() {
        return BlockModelGenerators.TEXTURED_MODELS;
    }

    public void skipAutoItemBlock(Block block) {
        Item item = block.asItem();
        if (item != Items.AIR) {
            skippedAutoModels.add(item);
        }
    }

    public void delegateItemModel(Block block, Identifier model) {
        this.registerSimpleItemModel(block, model);
    }

    public void delegateTintedItemModel(Block block, Identifier model, ItemTintSource tintSource) {
        this.itemModelOutput.accept(block.asItem(), ItemModelUtils.tintedModel(model, tintSource));
    }

    public void createSimpleFlatItemModel(Item item) {
        this.registerSimpleItemModel(item, this.createFlatItemModel(item));
    }

    public void createSimpleFlatItemModel(Block block) {
        Item item = block.asItem();
        if (item == Items.AIR) {
            return;
        }
        Identifier modelLocation = this.createFlatItemModelWithBlockTexture(item, block);
        this.registerSimpleItemModel(item, modelLocation);
    }

    public void createCraftingTableLike(
            Block craftingTableBlock,
            Block craftingTableMaterialBlock,
            BiFunction<Block, Block, TextureMapping> textureMappingGetter
    ) {
        TextureMapping textureMapping = textureMappingGetter.apply(craftingTableBlock, craftingTableMaterialBlock);
        Identifier modelLocation = ModelTemplates.CUBE.create(craftingTableBlock, textureMapping, this.modelOutput);
        this.blockStateOutput.accept(createSimpleBlock(craftingTableBlock, modelLocation));
        this.delegateItemModel(craftingTableBlock, modelLocation);
    }

    public void createNonTemplateHorizontalBlock(Block block) {
        MultiVariant variant = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(block));
        Object dispatch = DatagenModelDispatch.multiVariantDispatch(block, variant);
        this.blockStateOutput().accept(DatagenModelDispatch.withDispatch(dispatch, createHorizontalFacingDispatch()));
    }

    public Object createColumnWithFacing() {
        return BlockModelGenerators.ROTATIONS_COLUMN_WITH_FACING;
    }

    public static Object createHorizontalFacingDispatch() {
        return BlockModelGenerators.ROTATION_HORIZONTAL_FACING;
    }

    public static MultiVariantGenerator createSimpleBlock(Block block, Identifier modelLocation) {
        return BlockModelGenerators.createSimpleBlock(block, BlockModelGenerators.plainVariant(modelLocation));
    }

    public static Object createButton(Block block, Identifier button, Identifier pressed) {
        return BlockModelGenerators.createButton(
                block,
                BlockModelGenerators.plainVariant(button),
                BlockModelGenerators.plainVariant(pressed)
        );
    }

    public static Object createCustomFence(
            Block block,
            Identifier post,
            Identifier sideNorth,
            Identifier sideEast,
            Identifier sideSouth,
            Identifier sideWest
    ) {
        return BlockModelGenerators.createCustomFence(
                block,
                BlockModelGenerators.plainVariant(post),
                BlockModelGenerators.plainVariant(sideNorth),
                BlockModelGenerators.plainVariant(sideEast),
                BlockModelGenerators.plainVariant(sideSouth),
                BlockModelGenerators.plainVariant(sideWest)
        );
    }

    public static Object createFence(Block block, Identifier post, Identifier side) {
        return BlockModelGenerators.createFence(
                block,
                BlockModelGenerators.plainVariant(post),
                BlockModelGenerators.plainVariant(side)
        );
    }

    public static Object createWall(
            Block block,
            Identifier post,
            Identifier sideLow,
            Identifier sideTall
    ) {
        return BlockModelGenerators.createWall(
                block,
                BlockModelGenerators.plainVariant(post),
                BlockModelGenerators.plainVariant(sideLow),
                BlockModelGenerators.plainVariant(sideTall)
        );
    }

    public static Object createFenceGate(
            Block block,
            Identifier open,
            Identifier closed,
            Identifier wallOpen,
            Identifier wallClosed,
            boolean uvlock
    ) {
        return BlockModelGenerators.createFenceGate(
                block,
                BlockModelGenerators.plainVariant(open),
                BlockModelGenerators.plainVariant(closed),
                BlockModelGenerators.plainVariant(wallOpen),
                BlockModelGenerators.plainVariant(wallClosed),
                uvlock
        );
    }

    public static Object createStairs(
            Block block,
            Identifier inner,
            Identifier straight,
            Identifier outer
    ) {
        return BlockModelGenerators.createStairs(
                block,
                BlockModelGenerators.plainVariant(inner),
                BlockModelGenerators.plainVariant(straight),
                BlockModelGenerators.plainVariant(outer)
        );
    }

    public static Object createAxisAlignedPillarBlock(Block block, Identifier modelLocation) {
        return BlockModelGenerators.createAxisAlignedPillarBlock(block, BlockModelGenerators.plainVariant(modelLocation));
    }

    public static Object createPressurePlate(
            Block block,
            Identifier up,
            Identifier down
    ) {
        return BlockModelGenerators.createPressurePlate(
                block,
                BlockModelGenerators.plainVariant(up),
                BlockModelGenerators.plainVariant(down)
        );
    }

    public static Object createSlab(
            Block block,
            Identifier bottom,
            Identifier top,
            Identifier fullBlock
    ) {
        return BlockModelGenerators.createSlab(
                block,
                BlockModelGenerators.plainVariant(bottom),
                BlockModelGenerators.plainVariant(top),
                BlockModelGenerators.plainVariant(fullBlock)
        );
    }
}
