package org.betterx.wover.loot.api;

import org.betterx.wover.core.api.ModCore;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootTable;

public class LootTableManager {
    public static ResourceKey<LootTable> getBlockLootTableKey(ModCore modCore, Identifier blockId) {
        return ResourceKey.create(Registries.LOOT_TABLE, blockId.withPrefix("blocks/"));
    }

    public static ResourceKey<LootTable> createLootTableKey(ModCore modCore, String name) {
        return ResourceKey.create(Registries.LOOT_TABLE, modCore.mk(name));
    }
}
