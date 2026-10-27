package io.github.fishstiz.packed_packs.pack;

import io.github.fishstiz.packed_packs.util.PackUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackSource;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
public record FolderLocationInfo(Path path, String id, Component title, PackSource source, @Nullable FolderLocationInfo parent) {
    public static FolderLocationInfo fromPath(Path path, @Nullable FolderLocationInfo parent) {
        String name = path.getFileName().toString();
        String id = parent == null ? "file/" + name : parent.id() + '/' + name;
        return new FolderLocationInfo(path, id, Component.literal(name), PackUtil.PACK_SOURCE, parent);
    }
}
