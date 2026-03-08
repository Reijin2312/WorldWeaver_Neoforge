package org.betterx.wover.block.impl.predicate;

import org.betterx.wover.block.api.predicate.IsFullShape;
import org.betterx.wover.entrypoint.LibWoverBlock;
import org.betterx.wover.legacy.api.LegacyHelper;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicateType;

import net.neoforged.neoforge.registries.RegisterEvent;

public class BlockPredicatesImpl {
    public static BlockPredicateType<IsFullShape> FULL_SHAPE;

    private static <P extends BlockPredicate> BlockPredicateType<P> createType(MapCodec<P> codec) {
        return () -> codec;
    }

    public static void register(RegisterEvent event) {
        event.register(Registries.BLOCK_PREDICATE_TYPE, helper -> {
            FULL_SHAPE = createType(IsFullShape.CODEC);
            helper.register(LibWoverBlock.C.id("full_shape"), FULL_SHAPE);

            if (LegacyHelper.isLegacyEnabled()) {
                helper.register(LegacyHelper.BCLIB_CORE.id("full_shape"), createType(IsFullShape.CODEC));
            }
        });
    }

    public static void ensureStaticInitialization() {
        // invoked to trigger class loading; actual registration is handled in the RegisterEvent listener
    }
}
