package io.github.fishstiz.packed_packs.util;

import io.github.fishstiz.packed_packs.PackedPacks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class ToastUtil {
    private ToastUtil() {
    }

    public static void onDevModeToggleToast(boolean enabled) {
        Component enableText = enabled ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF;
        addOrUpdate(
                SystemToast.SystemToastIds.PERIODIC_NOTIFICATION,
                Component.literal(PackedPacks.MOD_NAME),
                Component.translatable("packed_packs.dev_mode", enableText)
        );
    }

    public static void onFileFailToast(Component message) {
        addOrUpdate(SystemToast.SystemToastIds.PACK_COPY_FAILURE, Component.translatable("packed_packs.file.fail"), message);
    }

    private static void addOrUpdate(SystemToast.SystemToastIds token, Component title, Component message) {
        ToastComponent toasts = Minecraft.getInstance().getToasts();
        SystemToast.addOrUpdate(toasts, token, title, message);
    }

    public static Component getRenameFailText(String from, String to) {
        return Component.translatable("packed_packs.file.rename.fail", from, to);
    }

    public static Component getDeleteFailText(String fileName) {
        return Component.translatable("packed_packs.file.delete.fail", fileName);
    }

    public static void onDeleteFailToast(Component fileName) {
        onFileFailToast(getDeleteFailText(fileName.getString()));
    }

    public static void onRenameFailToast(Component from, String to) {
        onFileFailToast(getRenameFailText(from.getString(), to));
    }
}
