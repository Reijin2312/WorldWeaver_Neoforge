package org.betterx.wover.common.generator.impl.compat;

import org.betterx.wover.common.mixin.BiomeSourceAccessor;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public final class LithostitchedBiomeSourceCompat {
    private static final String INJECTOR_BIOME_SOURCE =
            "dev.worldgen.lithostitched.impl.worldgen.biomeinjector.internal.InjectorBiomeSource";
    private static final String BLUEPRINT_MODDED_BIOME_SOURCE =
            "com.teamabnormals.blueprint.common.world.modification.ModdedBiomeSource";

    private LithostitchedBiomeSourceCompat() {
    }

    public static BiomeSource unwrap(BiomeSource source) {
        BiomeSource current = source;
        for (int i = 0; i < 8 && current != null; i++) {
            try {
                Object next;
                String className = current.getClass().getName();
                if (INJECTOR_BIOME_SOURCE.equals(className)) {
                    Method delegate = current.getClass().getMethod("directDelegate");
                    next = delegate.invoke(current);
                } else if (BLUEPRINT_MODDED_BIOME_SOURCE.equals(className)) {
                    try {
                        next = current.getClass().getMethod("getOriginalSource").invoke(current);
                    } catch (NoSuchMethodException ignored) {
                        Field originalSource = current.getClass().getDeclaredField("originalSource");
                        originalSource.setAccessible(true);
                        next = originalSource.get(current);
                    }
                } else {
                    break;
                }

                if (!(next instanceof BiomeSource biomeSource) || biomeSource == current) {
                    break;
                }
                current = biomeSource;
            } catch (ReflectiveOperationException ignored) {
                break;
            }
        }
        return current;
    }

    public static boolean refreshPossibleBiomes(BiomeSource source, Collection<Holder<Biome>> delegateBiomes) {
        if (source == null || !INJECTOR_BIOME_SOURCE.equals(source.getClass().getName())) {
            return false;
        }

        Set<Holder<Biome>> combined = new HashSet<>(source.possibleBiomes());
        combined.addAll(delegateBiomes);
        return replacePossibleBiomes(source, combined);
    }

    public static boolean replacePossibleBiomes(BiomeSource source, Collection<Holder<Biome>> biomes) {
        if (source == null) {
            return false;
        }

        try {
            Set<Holder<Biome>> immutable = Set.copyOf(biomes);
            ((BiomeSourceAccessor) source).wover_setPossibleBiomes((Supplier<Set<Holder<Biome>>>) () -> immutable);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
