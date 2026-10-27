package io.github.fishstiz.packed_packs.util;

import com.sun.jna.platform.FileUtils;
import io.github.fishstiz.fidgetz.v0.utils.CollectionUtils;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.config.FolderPackMeta;
import io.github.fishstiz.packed_packs.platform.Services;
import io.github.fishstiz.packed_packs.transform.mixin.UtilAccess;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class PackUtil {
    public static final String HIGH_CONTRAST_ID = "high_contrast";
    public static final String VANILLA_ID = "vanilla";
    public static final String FABRIC_ID = "fabric";
    public static final String NEOFORGE_ID = "mod_resources";
    public static final String ZIP_PACK_EXTENSION = ".zip";
    public static final String ICON_FILENAME = "pack.png";
    public static final PackSource PACK_SOURCE = PackSource.create(name ->
            Component.translatable(
                    "pack.nameAndSource",
                    name,
                    Component.literal(PackedPacks.MOD_NAME).withStyle(ChatFormatting.YELLOW)
            ).withStyle(ChatFormatting.GRAY), false);

    private PackUtil() {
    }

    public static String replaceDirsWithRelative(String packId) {
        return packId.replaceFirst("^file/.*(?=/[^/]+$)", "relative");
    }

    public static String joinPackNames(Collection<Path> paths) {
        return String.join(", ", CollectionUtils.map(paths, path -> path.getFileName().toString()));
    }

    public static boolean hasMcmeta(Path path) {
        return Files.isRegularFile(path.resolve(PackResources.PACK_META));
    }

    public static boolean hasFolderConfig(Path path) {
        return Files.isRegularFile(path.resolve(FolderPackMeta.FILENAME));
    }

    public static boolean isBuiltIn(PackSource packSource) {
        return packSource == PackSource.BUILT_IN || Services.PLATFORM.isBuiltInPack(packSource);
    }

    public static boolean isEssential(String packId) {
        return packId.equals(VANILLA_ID) || packId.equals(FABRIC_ID) || packId.equals(NEOFORGE_ID);
    }

    public static boolean isFeature(PackSource packSource) {
        return packSource == PackSource.FEATURE;
    }

    public static boolean isNonPackDirectory(Path path) {
        return Files.isDirectory(path) && !hasMcmeta(path) && !isHidden(path);
    }

    public static boolean isHidden(Path path) {
        try {
            return Files.isHidden(path);
        } catch (IOException e) {
            PackedPacks.LOGGER.warn("[packed_packs] Failed to check if path is hidden at {}", path, e);
            return false;
        }
    }

    public static boolean isZipPath(@Nullable Path path) {
        return path != null && Files.isRegularFile(path) && path.getFileName().toString().endsWith(ZIP_PACK_EXTENSION);
    }

    public static List<Path> mapValidDirectories(Collection<String> paths) {
        if (paths == null || paths.isEmpty()) return Collections.emptyList();

        List<Path> validPaths = new ObjectArrayList<>(paths.size());
        for (String path : paths) {
            if (path == null || path.isBlank()) continue;

            try {
                Path resolved = Paths.get(path);
                if (Files.isDirectory(resolved)) {
                    validPaths.add(resolved.toAbsolutePath().normalize());
                } else {
                    PackedPacks.LOGGER.error("[packed_packs] Path is not a valid directory: '{}', ignoring.", path);
                }
            } catch (Exception e) {
                PackedPacks.LOGGER.error("[packed_packs] Failed to resolve path: '{}', ignoring.", path, e);
            }
        }

        return validPaths;
    }

    public static void openPack(@Nullable Path path) {
        if (path != null) {
            Util.getPlatform().openFile(path.toFile());
        }
    }

    public static void openParent(@Nullable Path path) {
        if (path == null) return;

        File file = path.toFile();
        if (!file.exists()) return;

        try {
            switch (Util.getPlatform()) {
                case WINDOWS -> new ProcessBuilder("explorer.exe", "/select,", file.getAbsolutePath()).start();
                case OSX -> new ProcessBuilder("open", "-R", file.getAbsolutePath()).start();
                case LINUX -> {
                    File parentFile = file.getParentFile();
                    if (parentFile != null) new ProcessBuilder("xdg-open", parentFile.getAbsolutePath()).start();
                }
                default -> {
                    Path parent = path.getParent();
                    if (parent != null) Util.getPlatform().openFile(parent.toFile());
                }
            }
        } catch (IOException e) {
            Path parent = path.getParent();
            if (parent != null) Util.getPlatform().openFile(parent.toFile());
        }
    }

    public static boolean deletePath(Path path) {
        FileUtils fileUtils = FileUtils.getInstance();

        if (fileUtils.hasTrash()) {
            try {
                fileUtils.moveToTrash(path.toFile());
                return true;
            } catch (IOException e) {
                PackedPacks.LOGGER.warn("[packed_packs] Failed to move to trash: '{}'", path, e);
            }
        }

        if (Files.isDirectory(path)) {
            try {
                org.apache.commons.io.FileUtils.deleteDirectory(path.toFile());
                return true;
            } catch (IOException e) {
                PackedPacks.LOGGER.error("[packed_packs] Failed to delete path: '{}'", path, e);
                return false;
            }
        }

        return UtilAccess.packed_packs$createDeleter(path).getAsBoolean();
    }

    public static boolean renamePath(Path path, Path newName) {
        return UtilAccess.packed_packs$createRenamer(path, newName).getAsBoolean();
    }

    public static String getNewIdOnRename(String previousId, String newName) {
        return previousId.replaceFirst("[^/]+?$", newName);
    }

    public static PathValidationResults validatePaths(List<Path> packs) {
        PathValidationResults results = new PathValidationResults(packs);
        for (Path path : packs) {
            try {
                if (containsPack(path, results.symlinkWarnings)) {
                    results.addValid(path);
                }
            } catch (Exception e) {
                PackedPacks.LOGGER.warn("Failed to check {} for packs", path, e);
            }
        }

        return results;
    }

    private static boolean containsPack(Path path, List<ForbiddenSymlinkInfo> symlinkWarnings) throws IOException {
        if (validatePath(path, symlinkWarnings)) {
            return true;
        }
        if (!isNonPackDirectory(path)) {
            return false;
        }

        boolean containsPack = false;
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(path)) {
            for (Path child : paths) {
                containsPack |= containsPack(child, symlinkWarnings);
            }
        }
        return containsPack;
    }

    private static boolean validatePath(Path path, List<ForbiddenSymlinkInfo> symlinkWarnings) throws IOException {
        if (Files.isSymbolicLink(path)) {
            symlinkWarnings.add(new ForbiddenSymlinkInfo(path, Files.readSymbolicLink(path)));
            return false;
        }

        Pack.ResourcesSupplier detectedPack = FolderRepositorySource.detectPackResources(path, false);
        if (detectedPack == null) {
            return false;
        }

        if (Files.isDirectory(path)) {
            try (var paths = Files.walk(path)) {
                var iterator = paths.iterator();
                while (iterator.hasNext()) {
                    Path entry = iterator.next();
                    if (Files.isSymbolicLink(entry)) {
                        symlinkWarnings.add(new ForbiddenSymlinkInfo(entry, Files.readSymbolicLink(entry)));
                    }
                }
            }
        }

        return true;
    }

    public record PathValidationResults(
            List<Path> valid,
            Set<Path> rejected,
            List<ForbiddenSymlinkInfo> symlinkWarnings
    ) {
        private PathValidationResults(Collection<Path> packs) {
            this(new ArrayList<>(packs.size()), new ObjectOpenHashSet<>(packs), new ArrayList<>());
        }

        private void addValid(Path path) {
            this.valid.add(path);
            this.rejected.remove(path);
        }
    }
}
