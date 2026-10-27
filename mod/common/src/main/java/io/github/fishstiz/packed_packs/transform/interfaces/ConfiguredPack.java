package io.github.fishstiz.packed_packs.transform.interfaces;

import io.github.fishstiz.packed_packs.pack.PackMetadata;
import io.github.fishstiz.packed_packs.pack.PackSelectionConfig;
import net.minecraft.server.packs.PackType;

public interface ConfiguredPack {
    default void packed_packs$setPackType(PackType packType) {
    }

    default boolean packed_packs$isHidden() {
        return false;
    }

    default boolean packed_packs$isConfigured() {
        return false;
    }

    PackSelectionConfig packed_packs$originalConfig();

    PackMetadata packed_packs$getMetadata();
}
