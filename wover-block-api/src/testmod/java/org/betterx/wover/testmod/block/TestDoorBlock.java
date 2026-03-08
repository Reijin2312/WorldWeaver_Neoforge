package org.betterx.wover.testmod.block;

import org.betterx.wover.loot.api.BlockLootProvider;
import org.betterx.wover.loot.api.LootLookupProvider;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootTable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TestDoorBlock extends DoorBlock implements BlockLootProvider {
    public TestDoorBlock(
            BlockSetType blockSetType,
            Properties properties
    ) {
        super(blockSetType, properties);
    }

    @Override
    public @Nullable LootTable.Builder registerBlockLoot(
            @NotNull Identifier location,
            @NotNull LootLookupProvider provider,
            @NotNull ResourceKey<LootTable> tableKey
    ) {
        return provider.dropWithSilkTouchAndCondition(
                this,
                DoorBlock.HALF,
                DoubleBlockHalf.LOWER
        );
    }
}
