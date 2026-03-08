package org.betterx.wover.core.api;

import net.minecraft.server.packs.repository.PackSource;


public enum DatapackActivationType {
    NORMAL(PackSource.create(PackSource.BUILT_IN::decorate, false), false),
    DEFAULT_ENABLED(PackSource.BUILT_IN, false),
    ALWAYS_ENABLED(PackSource.BUILT_IN, true);

    private final PackSource packSource;
    private final boolean alwaysActive;

    DatapackActivationType(PackSource packSource, boolean alwaysActive) {
        this.packSource = packSource;
        this.alwaysActive = alwaysActive;
    }

    public PackSource packSource() {
        return packSource;
    }

    public boolean alwaysActive() {
        return alwaysActive;
    }
}
