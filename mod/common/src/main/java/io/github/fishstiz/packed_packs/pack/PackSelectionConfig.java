package io.github.fishstiz.packed_packs.pack;

import net.minecraft.server.packs.repository.Pack;

/** Version-independent pack selection settings used by Packed Packs. */
public record PackSelectionConfig(boolean required, Pack.Position defaultPosition, boolean fixedPosition) {
}
