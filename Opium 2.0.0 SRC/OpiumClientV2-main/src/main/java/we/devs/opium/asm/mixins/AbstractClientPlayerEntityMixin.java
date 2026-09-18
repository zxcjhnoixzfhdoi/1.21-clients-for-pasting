package we.devs.opium.asm.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import we.devs.opium.api.manager.miscellaneous.UUIDManager;

import static we.devs.opium.api.manager.CapeManager.getCape;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {

    @Shadow @Nullable protected abstract PlayerListEntry getPlayerListEntry();

    @Unique
    private static @NotNull SkinTextures getModifiedSkinTexture(SkinTextures original) {
        return new SkinTextures(original.texture(), original.textureUrl(), getCape(), original.elytraTexture(), original.model(), original.secure());
    }

    @ModifyReturnValue(method = "getSkinTextures", at = @At(value = "RETURN"))
    SkinTextures getSkinTextures(SkinTextures original) {
        if (this.getPlayerListEntry() != null && (UUIDManager.isAdded(this.getPlayerListEntry().getProfile().getId()))) {
            return getModifiedSkinTexture(original);
        }
        return original;
    }
}