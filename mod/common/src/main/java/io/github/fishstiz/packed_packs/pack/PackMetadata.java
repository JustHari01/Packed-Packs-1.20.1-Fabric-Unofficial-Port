package io.github.fishstiz.packed_packs.pack;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.world.flag.FeatureFlagSet;

public record PackMetadata(Component description, PackCompatibility compatibility, FeatureFlagSet requestedFeatures) {
}
