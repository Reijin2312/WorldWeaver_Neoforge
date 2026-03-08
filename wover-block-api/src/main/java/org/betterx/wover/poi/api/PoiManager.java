package org.betterx.wover.poi.api;

import org.betterx.wover.poi.impl.PoiManagerImpl;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;
import org.jetbrains.annotations.Nullable;

public class PoiManager {
    /**
     * Register a new PoiType
     *
     * @param location       The location of the PoiType
     * @param matchingStates The states that this PoiType should match
     * @param maxTickets     The maximum number of tickets
     * @param validRanges    The valid ranges
     * @return The new PoiType
     */
    public static WoverPoiType register(
            Identifier location,
            Set<BlockState> matchingStates,
            int maxTickets,
            int validRanges
    ) {
        return PoiManagerImpl.register(location, matchingStates, maxTickets, validRanges, null);
    }

    /**
     * Register a new PoiType
     *
     * @param location       The location of the PoiType
     * @param matchingStates The states that this PoiType should match
     * @param maxTickets     The maximum number of tickets
     * @param validRanges    The valid ranges
     * @param tag            The tag to associate with this PoiType or null
     * @return The new PoiType
     */
    public static WoverPoiType register(
            Identifier location,
            Set<BlockState> matchingStates,
            int maxTickets,
            int validRanges,
            @Nullable TagKey<Block> tag
    ) {
        return PoiManagerImpl.register(location, matchingStates, maxTickets, validRanges, tag);
    }
}
