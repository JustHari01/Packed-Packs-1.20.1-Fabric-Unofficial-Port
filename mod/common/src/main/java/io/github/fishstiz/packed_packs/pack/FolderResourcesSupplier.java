package io.github.fishstiz.packed_packs.pack;

import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public record FolderResourcesSupplier(Path path) implements Pack.ResourcesSupplier {
        @Override
    public @NotNull FolderResources open(String id) {
        return new FolderResources(id, this.path);
    }
}
