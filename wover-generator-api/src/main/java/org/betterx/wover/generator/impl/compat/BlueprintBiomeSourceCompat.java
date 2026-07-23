package org.betterx.wover.generator.impl.compat;

import org.betterx.wover.biome.api.data.BiomeData;
import org.betterx.wover.biome.api.data.BiomeGenerationDataContainer;
import org.betterx.wover.biome.impl.modification.BiomeTagModificationWorker;
import org.betterx.wover.entrypoint.LibWoverWorldGenerator;
import org.betterx.wover.generator.api.biomesource.WoverBiomeData;
import org.betterx.wover.tag.api.predefined.CommonBiomeTags;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.dimension.LevelStem;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public final class BlueprintBiomeSourceCompat {
    private static final String MODDED_BIOME_SOURCE = "com.teamabnormals.blueprint.common.world.modification.ModdedBiomeSource";
    private static final ResourceKey<? extends Registry<?>> SLICES_REGISTRY = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath("blueprint", "modded_biome_slices"));
    private static volatile Map<ResourceKey<Biome>, BiomeData> importedBiomeData = Map.of();
    private static volatile boolean canReplaceEndWrapper;

    private BlueprintBiomeSourceCompat() {
    }

    public static boolean wraps(BiomeSource source, BiomeSource expectedOriginal) {
        BiomeSource cursor = source;
        Set<BiomeSource> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        while (isModdedBiomeSource(cursor) && seen.add(cursor)) {
            BiomeSource original = getOriginalSource(cursor);
            if (original == expectedOriginal) return true;
            cursor = original;
        }
        return false;
    }

    public static BiomeData getImportedBiomeData(ResourceKey<Biome> biome) {
        return importedBiomeData.get(biome);
    }

    public static boolean canReplaceEndWrapper() {
        return canReplaceEndWrapper;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void importActiveEndOverlays(RegistryAccess access, Registry<Biome> biomes) {
        Map<ResourceKey<Biome>, BiomeData> imported = new HashMap<>();
        BiomeTagModificationWorker tagWorker = new BiomeTagModificationWorker();
        boolean importedEveryActiveSlice = true;
        try {
            Registry<?> slices = (Registry<?>) access.lookup((ResourceKey) SLICES_REGISTRY).orElse(null);
            if (slices == null) {
                importedBiomeData = Map.of();
                canReplaceEndWrapper = false;
                return;
            }
            for (Object slice : slices) {
                int weight = (Integer) invoke(slice, "weight");
                Object levels = invoke(slice, "levels");
                if (weight <= 0 || !(levels instanceof Set<?> set) || !set.contains(LevelStem.END)) continue;
                Object provider = invoke(slice, "provider");
                if (provider.getClass().getName().endsWith("BiomeUtil$OriginalModdedBiomeProvider")) continue;
                if (!provider.getClass().getName().endsWith("BiomeUtil$OverlayModdedBiomeProvider")) {
                    importedEveryActiveSlice = false;
                    continue;
                }
                Object overlays = invoke(provider, "overlays");
                if (!(overlays instanceof Iterable<?> iterable)) continue;
                for (Object overlayObject : iterable) {
                    if (!(overlayObject instanceof Pair<?, ?> overlay) || !(overlay.getSecond() instanceof BiomeSource overlaySource)) continue;
                    TagKey<Biome> tag = classifyEndOverlay(overlay.getFirst());
                    if (tag == null) {
                        importedEveryActiveSlice = false;
                        continue;
                    }
                    float chance = Math.max(0.01F, weight / 100.0F);
                    for (Holder<Biome> holder : overlaySource.possibleBiomes()) {
                        ResourceKey<Biome> key = holder.unwrapKey().orElse(null);
                        if (key == null || !biomes.containsKey(key)) continue;
                        tagWorker.addBiomeToTag(tag, biomes, key, holder);
                        imported.putIfAbsent(key, new WoverBiomeData(1.0F, key, BiomeGenerationDataContainer.EMPTY, 0.1F, chance, 0, false, null, null));
                    }
                }
            }
            tagWorker.finished();
        } catch (ReflectiveOperationException | RuntimeException e) {
            importedEveryActiveSlice = false;
            LibWoverWorldGenerator.C.log.warn("Unable to import active Blueprint biome slices", e);
        }
        importedBiomeData = Map.copyOf(imported);
        canReplaceEndWrapper = importedEveryActiveSlice && !imported.isEmpty();
    }

    private static boolean isModdedBiomeSource(BiomeSource source) {
        return source != null && MODDED_BIOME_SOURCE.equals(source.getClass().getName());
    }

    private static TagKey<Biome> classifyEndOverlay(Object matchesObject) {
        if (!(matchesObject instanceof Iterable<?> matches)) return null;
        boolean highlands = false, barrens = false, islands = false, center = false;
        for (Object match : matches) {
            if (!(match instanceof Holder<?> holder)) continue;
            Identifier id = holder.unwrapKey().map(ResourceKey::identifier).orElse(null);
            if (id == null || !"minecraft".equals(id.getNamespace())) continue;
            switch (id.getPath()) {
                case "end_highlands", "end_midlands" -> highlands = true;
                case "end_barrens" -> barrens = true;
                case "small_end_islands" -> islands = true;
                case "the_end" -> center = true;
                default -> { }
            }
        }
        if (highlands) return CommonBiomeTags.IS_END_HIGHLAND;
        if (islands) return CommonBiomeTags.IS_SMALL_END_ISLAND;
        if (center) return CommonBiomeTags.IS_END_CENTER;
        return barrens ? CommonBiomeTags.IS_END_BARRENS : null;
    }

    private static Object invoke(Object target, String methodName) throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(methodName);
        return method.invoke(target);
    }

    private static BiomeSource getOriginalSource(BiomeSource source) {
        try {
            Field field = source.getClass().getDeclaredField("originalSource");
            field.setAccessible(true);
            Object value = field.get(source);
            return value instanceof BiomeSource biomeSource ? biomeSource : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
