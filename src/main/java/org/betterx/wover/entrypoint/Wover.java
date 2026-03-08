package org.betterx.wover.entrypoint;

import org.betterx.wover.core.api.ModCore;
import org.betterx.wover.core.impl.registry.ModCoreImpl;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

import java.lang.reflect.InvocationTargetException;

@Mod("wover")
public class Wover {
    public static final ModCore C = ModCoreImpl.GLOBAL_MOD;
    private static final ClassLoader CLASS_LOADER = Wover.class.getClassLoader();

    public Wover(IEventBus modEventBus) {
        C.registerDatapackListener(modEventBus);
        tryInvokeStatic("org.betterx.wover.block.api.BlockRegistry", "hook", IEventBus.class, modEventBus);
        tryInvokeStatic("org.betterx.wover.item.api.ItemRegistry", "hook", IEventBus.class, modEventBus);
        tryInvokeStatic("org.betterx.wover.ui.api.VersionChecker", "registerMod", ModCore.class, C);

        tryInit("org.betterx.wover.entrypoint.LibWoverCommon", modEventBus);
        tryInit("org.betterx.wover.entrypoint.LibWoverCore", modEventBus);
        tryInit("org.betterx.wover.entrypoint.LibWoverMath", modEventBus);
        tryInit("org.betterx.wover.entrypoint.LibWoverDatagen", modEventBus);
        tryInit("org.betterx.wover.entrypoint.LibWoverEvents", modEventBus);
        tryInit("org.betterx.wover.entrypoint.LibWoverUi", modEventBus);
        tryInit("org.betterx.wover.entrypoint.LibWoverTag", modEventBus);
        tryInit("org.betterx.wover.entrypoint.LibWoverItem", modEventBus);
        tryInit("org.betterx.wover.entrypoint.LibWoverBlock", modEventBus);
        tryInit("org.betterx.wover.entrypoint.LibWoverRecipe", modEventBus);
        tryInit("org.betterx.wover.entrypoint.LibWoverWorldPreset", modEventBus);
        tryInit("org.betterx.wover.entrypoint.LibWoverSurface", modEventBus);
        tryInit("org.betterx.wover.entrypoint.LibWoverStructure", modEventBus);
        tryInit("org.betterx.wover.entrypoint.LibWoverFeature", modEventBus);
        tryInit("org.betterx.wover.entrypoint.LibWoverBiome", modEventBus);
        tryInit("org.betterx.wover.entrypoint.LibWoverWorldGenerator", modEventBus);
    }

    private static void tryInit(String className, IEventBus modEventBus) {
        try {
            Class.forName(className, true, CLASS_LOADER)
                 .getConstructor(IEventBus.class)
                 .newInstance(modEventBus);
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
            // Datagen/dev runs may not include every split API module on the mod classpath.
        } catch (InvocationTargetException ex) {
            throw rethrow("Failed to initialize " + className, ex.getCause());
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException("Failed to initialize " + className, ex);
        }
    }

    private static void tryInvokeStatic(String className, String methodName, Class<?> parameterType, Object arg) {
        try {
            Class.forName(className, true, CLASS_LOADER)
                 .getMethod(methodName, parameterType)
                 .invoke(null, arg);
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
            // Datagen/dev runs may not include every split API module on the mod classpath.
        } catch (LinkageError ignored) {
            // Datagen/dev may resolve split modules from the Gradle app classpath, causing loader conflicts.
        } catch (InvocationTargetException ex) {
            throw rethrow("Failed to call " + className + "#" + methodName, ex.getCause());
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException("Failed to call " + className + "#" + methodName, ex);
        }
    }

    private static RuntimeException rethrow(String message, Throwable cause) {
        if (cause instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new RuntimeException(message, cause);
    }
}
