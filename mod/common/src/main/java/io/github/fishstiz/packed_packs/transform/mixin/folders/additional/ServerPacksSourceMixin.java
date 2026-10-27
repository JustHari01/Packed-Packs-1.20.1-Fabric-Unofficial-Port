package io.github.fishstiz.packed_packs.transform.mixin.folders.additional;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.util.PackUtil;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.repository.ServerPacksSource;
import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPacksSource.class)
public abstract class ServerPacksSourceMixin {
    @WrapOperation(method = "createPackRepository(Ljava/nio/file/Path;)Lnet/minecraft/server/packs/repository/PackRepository;", at = @At(
            value = "NEW",
            target = "([Lnet/minecraft/server/packs/repository/RepositorySource;)Lnet/minecraft/server/packs/repository/PackRepository;"
    ))
    private static PackRepository addAdditionalFolders(
            RepositorySource[] sources,
            Operation<PackRepository> original
    ) {
        RepositorySource[] folders = PackUtil.mapValidDirectories(Config.get().getDatapacks().getAdditionalFolders())
                .stream()
                .map(path -> path.toAbsolutePath().normalize())
                .distinct()
                .map(path -> new FolderRepositorySource(path, PackType.SERVER_DATA, PackUtil.PACK_SOURCE))
                .toArray(RepositorySource[]::new);

        return original.call((Object) ArrayUtils.addAll(sources, folders));
    }
}
