package io.github.fishstiz.packed_packs.transform.mixin.folders;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.pack.FolderLocationInfo;
import io.github.fishstiz.packed_packs.transform.interfaces.FilePack;
import io.github.fishstiz.packed_packs.util.PackUtil;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Mixin(FolderRepositorySource.class)
public abstract class FolderRepositorySourceMixin {
    @Unique
    private static final ThreadLocal<@Nullable FolderLocationInfo> PARENT_CONTEXT = new ThreadLocal<>();

    @Shadow
    @Final
    private Path folder;

    @Inject(method = "loadPacks", at = @At("RETURN"))
    private void ensureRemoveThreadLocals(Consumer<Pack> result, CallbackInfo ci) {
        PARENT_CONTEXT.remove();
        if (PackedPacks.FOLDER_COLLECTOR.get() == null) {
            PackedPacks.FOLDER_COLLECTOR.remove();
        }
    }

    @WrapOperation(method = "discoverPacks", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/packs/repository/FolderRepositorySource;detectPackResources(Ljava/nio/file/Path;Z)Lnet/minecraft/server/packs/repository/Pack$ResourcesSupplier;"
    ))
    private static Pack.ResourcesSupplier discoverNestedPacks(
            Path path,
            boolean validate,
            Operation<Pack.ResourcesSupplier> original,
            @Local(argsOnly = true) BiConsumer<Path, Pack.ResourcesSupplier> output
    ) {
        Pack.ResourcesSupplier resourcesSupplier;
        try {
            resourcesSupplier = original.call(path, validate);
        } catch (Exception e) {
            PackedPacks.LOGGER.warn("[packed_packs] Failed to inspect '{}', ignoring", path, e);
            return null;
        }

        if (resourcesSupplier == null && PackUtil.isNonPackDirectory(path)) {
            FolderLocationInfo parent = PARENT_CONTEXT.get();
            try {
                FolderLocationInfo parentInfo = FolderLocationInfo.fromPath(path.toAbsolutePath().normalize(), parent);
                Consumer<FolderLocationInfo> collector = PackedPacks.FOLDER_COLLECTOR.get();
                if (collector != null) {
                    collector.accept(parentInfo);
                }
                PARENT_CONTEXT.set(parentInfo);
                discoverPacks(path, validate, output);
            } catch (IOException e) {
                PackedPacks.LOGGER.warn("[packed_packs] Failed to list packs in {}", path, e);
            } finally {
                if (parent == null) {
                    PARENT_CONTEXT.remove();
                } else {
                    PARENT_CONTEXT.set(parent);
                }
            }
        }

        return resourcesSupplier;
    }

    @ModifyArg(method = "method_45272", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/packs/repository/Pack;readMetaAndCreate(Ljava/lang/String;Lnet/minecraft/network/chat/Component;ZLnet/minecraft/server/packs/repository/Pack$ResourcesSupplier;Lnet/minecraft/server/packs/PackType;Lnet/minecraft/server/packs/repository/Pack$Position;Lnet/minecraft/server/packs/repository/PackSource;)Lnet/minecraft/server/packs/repository/Pack;"
    ), index = 0)
    private String relativizePackId(String id, @Local(argsOnly = true) Path path) {
        if (PARENT_CONTEXT.get() == null) return id;

        String relativePath = this.folder.relativize(path).toString().replace('\\', '/');
        return "file/" + relativePath;
    }

    @ModifyArg(method = "method_45272", at = @At(
            value = "INVOKE",
            target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"
    ), index = 0)
    private Object bindDirToNestedPack(Object arg, @Local(argsOnly = true) Path path) {
        if (arg instanceof FilePack pack) {
            pack.packed_packs$setPath(path);
            FolderLocationInfo parent = PARENT_CONTEXT.get();
            if (parent != null) {
                pack.packed_packs$setParent(parent);
            }
        }
        return arg;
    }

    @Shadow
    public static void discoverPacks(Path folder, boolean validate, BiConsumer<Path, Pack.ResourcesSupplier> output) throws IOException {
        throw new AssertionError();
    }
}
