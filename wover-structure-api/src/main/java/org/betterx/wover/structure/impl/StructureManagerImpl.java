package org.betterx.wover.structure.impl;

import org.betterx.wover.core.api.registry.DatapackRegistryBuilder;
import org.betterx.wover.entrypoint.LibWoverStructure;
import org.betterx.wover.events.api.types.OnBootstrapRegistry;
import org.betterx.wover.events.impl.EventImpl;
import org.betterx.wover.legacy.api.LegacyHelper;
import org.betterx.wover.structure.api.StructureTypeKey;
import org.betterx.wover.structure.api.structures.nbt.RandomNbtStructure;
import org.betterx.wover.structure.api.structures.nbt.RandomNbtStructurePiece;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

import java.util.Optional;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.registries.RegisterEvent;

public class StructureManagerImpl {
    public static final EventImpl<OnBootstrapRegistry<Structure>> BOOTSTRAP_STRUCTURES =
            new EventImpl<>("BOOTSTRAP_STRUCTURES");

    private static final Map<ResourceKey<StructureType<?>>, StructureType<?>> TYPES = new LinkedHashMap<>();
    private static final Map<ResourceKey<StructurePieceType>, StructurePieceType> PIECES = new LinkedHashMap<>();

    public static final ResourceKey<StructureType<?>> RANDOM_NBT_STRUCTURE_TYPE_KEY = ResourceKey.create(
            Registries.STRUCTURE_TYPE,
            LibWoverStructure.C.id("random_nbt_structure")
    );
    public static final StructureType<RandomNbtStructure> RANDOM_NBT_STRUCTURE_TYPE =
            createType(RandomNbtStructure.simpleRandomCodec(RandomNbtStructure::new));

    public static final ResourceKey<StructurePieceType> RANDOM_NBT_STRUCTURE_PIECE_KEY = ResourceKey.create(
            Registries.STRUCTURE_PIECE,
            LibWoverStructure.C.id("random_nbt_structure_piece")
    );
    public static final StructurePieceType RANDOM_NBT_STRUCTURE_PIECE = RandomNbtStructurePiece::new;

    static {
        TYPES.put(RANDOM_NBT_STRUCTURE_TYPE_KEY, RANDOM_NBT_STRUCTURE_TYPE);
        PIECES.put(RANDOM_NBT_STRUCTURE_PIECE_KEY, RANDOM_NBT_STRUCTURE_PIECE);
        if (LegacyHelper.isLegacyEnabled()) {
            registerPiece(
                    LegacyHelper.BCLIB_CORE.id("template_piece"),
                    RandomNbtStructurePiece::new
            );
        }
    }

    @Nullable
    public static Holder<Structure> getHolder(
            @Nullable HolderGetter<Structure> getter,
            @NotNull ResourceKey<Structure> key
    ) {
        if (getter == null) return null;

        final Optional<Holder.Reference<Structure>> h = getter.get(key);
        return h.orElse(null);
    }

    @Nullable
    public static Holder<Structure> getHolder(
            @Nullable HolderLookup.Provider lookup,
            @NotNull ResourceKey<Structure> key
    ) {
        if (lookup == null) return null;

        return lookup.lookup(Registries.STRUCTURE).flatMap(r -> r.get(key)).orElse(null);
    }

    private static boolean didInit = false;

    @ApiStatus.Internal
    public static void initialize() {
        if (didInit) return;
        didInit = true;

        DatapackRegistryBuilder.addBootstrap(
                Registries.STRUCTURE,
                StructureManagerImpl::onBootstrap
        );
    }


    public static boolean isValidBiome(Structure.GenerationContext context) {
        return isValidBiome(context, 5);
    }


    public static boolean isValidBiome(Structure.GenerationContext context, int yPos) {
        BlockPos blockPos = context.chunkPos().getMiddleBlockPosition(yPos);
        return context.validBiome().test(
                context
                        .chunkGenerator()
                        .getBiomeSource()
                        .getNoiseBiome(
                                QuartPos.fromBlock(blockPos.getX()),
                                QuartPos.fromBlock(blockPos.getY()),
                                QuartPos.fromBlock(blockPos.getZ()),
                                context.randomState().sampler()
                        )
        );
    }

    private static void onBootstrap(BootstrapContext<Structure> context) {
        BOOTSTRAP_STRUCTURES.emit(c -> c.bootstrap(context));
    }

    @SuppressWarnings("unchecked")
    private static <S extends Structure> StructureType<S> createType(MapCodec<S> codec) {
        return () -> (MapCodec<S>) codec;
    }

    public static <S extends Structure> @NotNull StructureTypeKey<S> registerType(
            @NotNull Identifier location,
            @NotNull StructureTypeKey.StructureFactory<S> structureFactory,
            @NotNull MapCodec<S> codec
    ) {
        final ResourceKey<StructureType<?>> key = ResourceKey.create(Registries.STRUCTURE_TYPE, location);
        @SuppressWarnings("unchecked") final StructureType<S> type = (StructureType<S>) TYPES.computeIfAbsent(
                key,
                k -> createType(codec)
        );

        return new StructureTypeKeyImpl<>(key, type, structureFactory);
    }

    public static <S extends Structure> @NotNull StructureType<S> registerType(
            @NotNull Identifier location,
            @NotNull MapCodec<S> codec
    ) {
        final ResourceKey<StructureType<?>> key = ResourceKey.create(Registries.STRUCTURE_TYPE, location);
        @SuppressWarnings("unchecked") final StructureType<S> type = (StructureType<S>) TYPES.computeIfAbsent(
                key,
                k -> createType(codec)
        );

        return type;
    }

    public static @NotNull StructurePieceType registerPiece(
            @NotNull Identifier location,
            @NotNull StructurePieceType pieceType
    ) {
        final ResourceKey<StructurePieceType> key = ResourceKey.create(Registries.STRUCTURE_PIECE, location);
        PIECES.putIfAbsent(key, pieceType);
        return pieceType;
    }

    public static void register(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.STRUCTURE_TYPE)) {
            event.register(Registries.STRUCTURE_TYPE, helper -> TYPES.forEach((k, v) -> helper.register(k.identifier(), v)));
        } else if (event.getRegistryKey().equals(Registries.STRUCTURE_PIECE)) {
            event.register(Registries.STRUCTURE_PIECE, helper -> PIECES.forEach((k, v) -> helper.register(k.identifier(), v)));
        }
    }
}
