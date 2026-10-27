package io.github.fishstiz.packed_packs.transform.mixin.gui;

import io.github.fishstiz.fidgetz.v0.gui.components.FZIconButton;
import io.github.fishstiz.fidgetz.v0.gui.components.WidgetElements;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.gui.screens.PackSelectionScreenArgs;
import io.github.fishstiz.packed_packs.gui.screens.PackedPacksScreen;
import io.github.fishstiz.packed_packs.gui.screens.PackedPacksScreenPreloader;
import io.github.fishstiz.packed_packs.transform.mixin.PackSelectionScreenAccessor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackRepository;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.function.Consumer;

@Mixin(PackSelectionScreen.class)
public abstract class PackSelectionScreenMixin extends Screen implements PackSelectionScreenAccessor {
    protected PackSelectionScreenMixin(Component title) {
        super(title);
    }

    @Unique
    private PackSelectionScreenArgs packed_packs$original;

    @Unique
    private FZIconButton packed_packs$button;

    @Unique
    private Screen packed_packs$previous;

    @Override
    public void packed_packs$setPrevious(@Nullable Screen previous) {
        this.packed_packs$previous = previous;
    }

    @Override
    public @Nullable Screen packed_packs$getPrevious() {
        return this.packed_packs$previous;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void setRepository(PackRepository repository, Consumer<PackRepository> output, Path packDir, Component title, CallbackInfo ci) {
        this.packed_packs$original = new PackSelectionScreenArgs(repository, output, packDir, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addPackedPacksButton(CallbackInfo ci) {
        if (minecraft == null) return;
        Screen previous = this.packed_packs$previous != null ? this.packed_packs$previous : this;

        this.packed_packs$button = FZIconButton.builder()
                .square()
                .tooltip(Component.literal(PackedPacks.MOD_NAME))
                .icon(new WidgetElements(PackedPacks.id("icon/packed_packs"), 16, 16))
                .onPress(() -> minecraft.setScreen(new PackedPacksScreen(minecraft, previous, packed_packs$original)))
                .build();

        PackedPacksScreenPreloader.attach(this, this.packed_packs$button);
        this.addRenderableWidget(this.packed_packs$button);
        int x = this.width / 2 - 154 - this.packed_packs$button.getWidth() - 8;
        this.packed_packs$button.setPosition(Math.max(0, x), this.height - 48);
    }
}
