package org.betterx.wover.tag.mixin;

import org.betterx.wover.tag.impl.TagManagerImpl;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagLoader;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(TagLoader.class)
public class TagLoaderMixin {
    @Final
    @Shadow
    private String directory;

    private static final Set<String> WOVER_LOGGED_DIRS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Inject(method = "load", at = @At("RETURN"), cancellable = true)
    private void wover_modifyTags(
            CallbackInfoReturnable<Map<Identifier, List<TagLoader.EntryWithSource>>> cir
    ) {
        Map<Identifier, List<TagLoader.EntryWithSource>> tagsMap = cir.getReturnValue();
        if (WOVER_LOGGED_DIRS.add(directory)) {
            // Avoid referencing split module entrypoint/loggers from injected MC code in datagen/dev.
        }
        cir.setReturnValue(TagManagerImpl.didLoadTagMap(directory, tagsMap));
    }
}
