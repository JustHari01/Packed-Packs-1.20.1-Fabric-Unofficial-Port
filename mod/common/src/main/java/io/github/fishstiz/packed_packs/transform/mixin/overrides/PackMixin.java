package io.github.fishstiz.packed_packs.transform.mixin.overrides;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.fishstiz.packed_packs.config.PackOverride;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.config.ProfileManager;
import io.github.fishstiz.packed_packs.transform.interfaces.ConfiguredPack;
import io.github.fishstiz.packed_packs.pack.PackMetadata;
import io.github.fishstiz.packed_packs.pack.PackSelectionConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.world.flag.FeatureFlagSet;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Pack.class)
public abstract class PackMixin implements ConfiguredPack {
    @Shadow
    @Final
    private Component description;

    @Shadow
    @Final
    private PackCompatibility compatibility;

    @Shadow
    @Final
    private FeatureFlagSet requestedFeatures;

    @Shadow
    @Final
    private boolean required;

    @Shadow
    @Final
    private Pack.Position defaultPosition;

    @Shadow
    @Final
    private boolean fixedPosition;

    @Shadow
    public abstract String getId();

    @Unique
    @Nullable
    private PackType packed_packs$packType;

    @Override
    public void packed_packs$setPackType(PackType packType) {
        this.packed_packs$packType = packType;
    }

    @Unique
    private @Nullable Profile packed_packs$getDefaultProfile() {
        return this.packed_packs$packType == null ? null : ProfileManager.get(this.packed_packs$packType).getDefault();
    }

    @Override
    public boolean packed_packs$isHidden() {
        Profile profile = packed_packs$getDefaultProfile();
        return profile != null && profile.isHidden(getId());
    }

    @WrapMethod(method = "isRequired")
    private boolean resolveRequired(Operation<Boolean> original) {
        Profile profile = packed_packs$getDefaultProfile();
        if (profile != null && profile.overridesRequired(getId())) {
            return profile.isRequired(getId());
        }
        return original.call();
    }

    @WrapMethod(method = "isFixedPosition")
    private boolean resolveFixed(Operation<Boolean> original) {
        Profile profile = packed_packs$getDefaultProfile();
        if (profile != null && profile.overridesPosition(getId())) {
            return profile.isFixed(getId());
        }
        return original.call();
    }

    @WrapMethod(method = "getDefaultPosition")
    private Pack.Position resolvePosition(Operation<Pack.Position> original) {
        Profile profile = packed_packs$getDefaultProfile();
        if (profile != null) {
            Pack.Position position = profile.getPosition(getId());
            if (position != null) {
                return position;
            }
        }
        return original.call();
    }

    @WrapMethod(method = "getCompatibility")
    private PackCompatibility resolveCompatibility(Operation<PackCompatibility> original) {
        Profile profile = packed_packs$getDefaultProfile();
        if (profile != null && profile.includes(getId())) {
            return PackCompatibility.COMPATIBLE;
        }
        return original.call();
    }

    @Override
    public boolean packed_packs$isConfigured() {
        Profile profile = packed_packs$getDefaultProfile();
        if (profile != null) {
            return profile.includes(getId());
        }
        return false;
    }

    @Override
    public PackMetadata packed_packs$getMetadata() {
        return new PackMetadata(this.description, this.compatibility, this.requestedFeatures);
    }

    @Override
    public PackSelectionConfig packed_packs$originalConfig() {
        return new PackSelectionConfig(this.required, this.defaultPosition, this.fixedPosition);
    }
}
