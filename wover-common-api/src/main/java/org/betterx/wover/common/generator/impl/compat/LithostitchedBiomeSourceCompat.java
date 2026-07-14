package org.betterx.wover.common.generator.impl.compat;

import org.betterx.wover.common.mixin.BiomeSourceAccessor;

import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Lithostitched wraps a generator's source after other worldgen hooks have run. Its public delegate accessor keeps
 * the original source alive, so integrations must operate on that delegate rather than discard the wrapper.
 */
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
            final String className = current.getClass().getName();
            try {
                Object next;
                if (INJECTOR_BIOME_SOURCE.equals(className)) {
                    Method delegate = current.getClass().getMethod("directDelegate");
                    next = delegate.invoke(current);
                } else if (BLUEPRINT_MODDED_BIOME_SOURCE.equals(className)) {
                    // Blueprint exposes this accessor in current releases. Keep the field fallback for older builds.
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

    /**
     * InjectorBiomeSource computes its possible-biome cache before late biome sources such as MosaicBiomeSource are
     * initialized. Refresh the cache while retaining all biomes contributed by Lithostitched injectors.
     */
    public static boolean refreshPossibleBiomes(BiomeSource source, Collection<Holder<Biome>> delegateBiomes) {
        if (source == null || !INJECTOR_BIOME_SOURCE.equals(source.getClass().getName())) {
            return false;
        }

        Set<Holder<Biome>> combined = new HashSet<>(source.possibleBiomes());
        combined.addAll(delegateBiomes);
        return replacePossibleBiomes(source, combined);
    }

    /**
     * Replaces Minecraft's memoized possible-biome supplier. BiomeSource exposes no invalidation API, although several
     * ecosystem biome sources are initialized after their first possibleBiomes() read.
     */
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
