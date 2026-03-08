package org.betterx.wover.testmod.block;

import org.betterx.wover.loot.api.BlockLootProvider;
import org.betterx.wover.loot.api.LootLookupProvider;

import net.minecraft.advancements.criterion.StatePropertiesPredicate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.ApplyExplosionDecay;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.AllOfCondition;
import net.minecraft.world.level.storage.loot.predicates.InvertedLootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TestBlock extends Block implements BlockLootProvider {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 2);

    public TestBlock(Properties properties) {
        super(properties);
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> stateManager) {
        super.createBlockStateDefinition(stateManager);
        stateManager.add(AGE);
    }

    @Override
    @Nullable
    public LootTable.Builder registerBlockLoot(
            @NotNull Identifier location,
            @NotNull LootLookupProvider provider,
            @NotNull ResourceKey<LootTable> tableKey
    ) {
        //return provider.dropWithSilkTouch(this, Items.DIAMOND, ConstantValue.exactly(3.0F));
//        return provider.dropWithSilkTouch(this, List.of(
//                        new LootLookupProvider.DropInfo(Items.DIAMOND, 3),
//                        new LootLookupProvider.DropInfo(Items.NETHERITE_SCRAP, 1)
//                )
//        );
        //return provider.dropWithSilkTouch(this);
        return LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .when(provider.hasSilkTouch())
                                        .setRolls(ConstantValue.exactly(1))
                                        .add(LootItem.lootTableItem(this)
                                                     .apply(SetItemCountFunction
                                                             .setCount(UniformGenerator.between(1, 3))
                                                             .when(ageCondition(3))
                                                     )
                                                     .apply(SetItemCountFunction
                                                             .setCount(ConstantValue.exactly(1))
                                                             .when(InvertedLootItemCondition.invert(ageCondition(3)))
                                                     )
                                                     .apply(ApplyBonusCount
                                                             .addOreBonusCount(provider.fortune())
                                                             .when(ageCondition(3))
                                                     )
                                                     .apply(applyAgeBonus(provider, 2))
                                                     .apply(applyAgeBonus(provider, 1))
                                                     .apply(ApplyExplosionDecay.explosionDecay())
                                        )
                        )
                        .withPool(
                                LootPool.lootPool()
                                        .when(AllOfCondition.allOf(InvertedLootItemCondition.invert(provider.hasSilkTouch()), ageCondition(3)))
                                        .setRolls(ConstantValue.exactly(1))
                                        .add(LootItem.lootTableItem(this)
                                                     .apply(SetItemCountFunction
                                                             .setCount(UniformGenerator.between(1, 3))
                                                     )
                                                     .apply(ApplyExplosionDecay.explosionDecay())
                                        )
                        );
    }

    private LootItemConditionalFunction.@NotNull Builder<?> applyAgeBonus(@NotNull LootLookupProvider provider, int i) {
        return ApplyBonusCount
                .addUniformBonusCount(provider.fortune(), i)
                .when(ageCondition(i));
    }

    private LootItemBlockStatePropertyCondition.@NotNull Builder ageCondition(int i) {
        return LootItemBlockStatePropertyCondition
                .hasBlockStateProperties(this)
                .setProperties(StatePropertiesPredicate.Builder
                        .properties()
                        .hasProperty(AGE, i));
    }
}
