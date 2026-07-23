package org.betterx.wover.generator.impl.compat;

import org.betterx.wover.entrypoint.LibWoverWorldGenerator;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class RegionsUnexploredBiomeConfigCompat {
    private static final String CONFIG_HANDLER = "net.regions_unexplored.config.RUConfigHandler";

    private RegionsUnexploredBiomeConfigCompat() {
    }

    @SuppressWarnings("unchecked")
    public static Set<ResourceKey<Biome>> disabledBiomes() {
        try {
            Class<?> configHandler = Class.forName(CONFIG_HANDLER, false, RegionsUnexploredBiomeConfigCompat.class.getClassLoader());
            Object common = configHandler.getField("COMMON").get(null);
            if (common == null) return Set.of();
            Field biomePlacementsField = common.getClass().getField("biomePlacements");
            Object biomePlacements = biomePlacementsField.get(common);
            if (biomePlacements == null) return Set.of();
            Object placements = biomePlacements.getClass().getField("placements").get(biomePlacements);
            if (!(placements instanceof Map<?, ?> placementMap)) return Set.of();

            Set<ResourceKey<Biome>> disabled = new HashSet<>();
            for (Map.Entry<?, ?> entry : placementMap.entrySet()) {
                if (!(entry.getKey() instanceof ResourceKey<?> key) || entry.getValue() == null) continue;
                Method canGenerate = entry.getValue().getClass().getMethod("canGenerate");
                if (Boolean.FALSE.equals(canGenerate.invoke(entry.getValue()))) {
                    disabled.add((ResourceKey<Biome>) key);
                }
            }
            return Set.copyOf(disabled);
        } catch (ClassNotFoundException ignored) {
            return Set.of();
        } catch (ReflectiveOperationException | RuntimeException e) {
            LibWoverWorldGenerator.C.log.warn("Unable to read Regions Unexplored biome generation settings", e);
            return Set.of();
        }
    }
}
