package hack.echo.client.mixin.entity;

import hack.echo.client.handlers.RotationHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static hack.echo.client.utils.Imports.mc;

// Not in item package because it affects silent rotations for items
@Mixin(Item.class)
public class MixinItem {

    @Redirect(
        method = "getPlayerPOVHitResult",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getYRot()F"
        )
    )
    private static float redirectYaw(Player instance) {
        if (instance == mc.player && RotationHandler.isHasSilentRotation()) {
            return RotationHandler.getServerYaw();
        }
        return instance.getYRot();
    }

    @Redirect(
        method = "getPlayerPOVHitResult",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getXRot()F"
        )
    )
    private static float redirectPitch(Player instance) {
        if (instance == mc.player && RotationHandler.isHasSilentRotation()) {
            return RotationHandler.getServerPitch();
        }
        return instance.getXRot();
    }
}

