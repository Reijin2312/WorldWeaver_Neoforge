package org.betterx.wover.generator.impl.compat;

import org.betterx.wover.entrypoint.LibWoverWorldGenerator;
import org.betterx.wover.state.api.WorldState;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class IncisionBiomeSourceCompat {
    private static final ResourceKey<Biome> ERODED_YARD = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("incision", "eroded_yard"));

    private IncisionBiomeSourceCompat() {
    }

    public static BiomeSource prepare(BiomeSource source) {
        if (!(source instanceof MultiNoiseBiomeSource multiNoise)) return source;
        RegistryAccess access = WorldState.registryAccess();
        if (access == null) access = WorldState.allStageRegistryAccess();
        if (access == null || access.lookupOrThrow(Registries.BIOME).get(ERODED_YARD).isEmpty()) return source;

        try {
            Class.forName("net.incision.init.IncisionModBiomes");
            Holder<Biome> biome = access.lookupOrThrow(Registries.BIOME).getOrThrow(ERODED_YARD);
            Climate.ParameterList<Holder<Biome>> original = getParameters(multiNoise);
            List<Pair<Climate.ParameterPoint, Holder<Biome>>> parameters = new ArrayList<>(original.values());
            add(parameters, biome, parameter(Climate.Parameter.point(0.0F)));
            add(parameters, biome, parameter(Climate.Parameter.point(1.0F)));
            LibWoverWorldGenerator.C.log.info("Added Incision biome to the Nether fallback source");
            return MultiNoiseBiomeSource.createFromList(new Climate.ParameterList<>(parameters));
        } catch (ClassNotFoundException ignored) {
            return source;
        } catch (ReflectiveOperationException | RuntimeException e) {
            LibWoverWorldGenerator.C.log.warn("Unable to apply Incision biome source compatibility", e);
            return source;
        }
    }

    @SuppressWarnings("unchecked")
    private static Climate.ParameterList<Holder<Biome>> getParameters(MultiNoiseBiomeSource source) throws ReflectiveOperationException {
        for (Method method : source.getClass().getDeclaredMethods()) {
            if (method.getParameterCount() == 0 && Climate.ParameterList.class.isAssignableFrom(method.getReturnType())) {
                method.setAccessible(true);
                return (Climate.ParameterList<Holder<Biome>>) method.invoke(source);
            }
        }
        throw new NoSuchMethodException("No Climate.ParameterList getter in " + source.getClass().getName());
    }

    private static Climate.ParameterPoint parameter(Climate.Parameter depth) {
        return Climate.parameters(Climate.Parameter.span(0.1F, 0.3F), Climate.Parameter.span(0.1F, 0.3F), Climate.Parameter.span(0.0F, 1.0F), Climate.Parameter.span(0.0F, 1.0F), depth, Climate.Parameter.span(0.0F, 0.1F), 0.0F);
    }

    private static void add(List<Pair<Climate.ParameterPoint, Holder<Biome>>> parameters, Holder<Biome> biome, Climate.ParameterPoint point) {
        Pair<Climate.ParameterPoint, Holder<Biome>> pair = Pair.of(point, biome);
        if (!parameters.contains(pair)) parameters.add(pair);
    }
}
