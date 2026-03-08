package org.betterx.wover.loot.api;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.packs.VanillaBlockLoot;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.FunctionUserBuilder;
import net.minecraft.world.level.storage.loot.predicates.ConditionUserBuilder;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

public class WoverVanillaBlockLoot extends VanillaBlockLoot {
    public WoverVanillaBlockLoot(HolderLookup.Provider provider) {
        super(provider);
    }

    @Override
    public LootItemCondition.Builder hasSilkTouch() {
        return super.hasSilkTouch();
    }

    @Override
    public LootItemCondition.Builder doesNotHaveSilkTouch() {
        return super.doesNotHaveSilkTouch();
    }

    public LootItemCondition.Builder hasShearsOrSilkTouch() {
        return hasShears().or(hasSilkTouch());
    }

    public LootItemCondition.Builder doesNotHaveShearsOrSilkTouch() {
        return hasShearsOrSilkTouch().invert();
    }

    @Override
    public <T extends FunctionUserBuilder<T>> T applyExplosionDecay(
            ItemLike item,
            FunctionUserBuilder<T> builder
    ) {
        return super.applyExplosionDecay(item, builder);
    }

    @Override
    public <T extends ConditionUserBuilder<T>> T applyExplosionCondition(
            ItemLike item,
            ConditionUserBuilder<T> builder
    ) {
        return super.applyExplosionCondition(item, builder);
    }

    @Override
    public LootTable.Builder createSilkTouchOnlyTable(ItemLike item) {
        return super.createSilkTouchOnlyTable(item);
    }

    @Override
    public LootTable.Builder createSingleItemTableWithSilkTouch(Block block, ItemLike item) {
        return super.createSingleItemTableWithSilkTouch(block, item);
    }

    @Override
    public LootTable.Builder createSingleItemTableWithSilkTouch(
            Block block,
            ItemLike item,
            NumberProvider numberProvider
    ) {
        return super.createSingleItemTableWithSilkTouch(block, item, numberProvider);
    }

    @Override
    public LootTable.Builder createPotFlowerItemTable(ItemLike item) {
        return super.createPotFlowerItemTable(item);
    }

    @Override
    public LootTable.Builder createOreDrop(Block block, Item item) {
        return super.createOreDrop(block, item);
    }

    @Override
    public LootTable.Builder createSilkTouchDispatchTable(
            Block block,
            LootPoolEntryContainer.Builder<?> builder
    ) {
        return super.createSilkTouchDispatchTable(block, builder);
    }

    @Override
    public LootTable.Builder createShearsDispatchTable(
            Block block,
            LootPoolEntryContainer.Builder<?> builder
    ) {
        return super.createShearsDispatchTable(block, builder);
    }

    @Override
    public LootTable.Builder createSilkTouchOrShearsDispatchTable(
            Block block,
            LootPoolEntryContainer.Builder<?> builder
    ) {
        return super.createSilkTouchOrShearsDispatchTable(block, builder);
    }

    @Override
    public LootTable.Builder createDoorTable(Block block) {
        return super.createDoorTable(block);
    }

    @Override
    public <T extends Comparable<T> & StringRepresentable> LootTable.Builder createSinglePropConditionTable(
            Block block,
            Property<T> property,
            T comparable
    ) {
        return super.createSinglePropConditionTable(block, property, comparable);
    }

    @Override
    public LootTable.Builder createLeavesDrops(Block leaves, Block sapling, float... chances) {
        return super.createLeavesDrops(leaves, sapling, chances);
    }

    @Override
    public LootTable.Builder createDoublePlantShearsDrop(Block block) {
        return super.createDoublePlantShearsDrop(block);
    }

    @Override
    public LootTable.Builder createDoublePlantWithSeedDrops(Block block, Block seed) {
        return super.createDoublePlantWithSeedDrops(block, seed);
    }
}
