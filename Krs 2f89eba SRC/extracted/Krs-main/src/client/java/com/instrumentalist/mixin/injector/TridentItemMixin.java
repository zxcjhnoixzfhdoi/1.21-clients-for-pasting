package com.instrumentalist.mixin.injector;

import com.instrumentalist.krs.hacks.features.movement.MovementFix;
import com.instrumentalist.krs.utils.IMinecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TridentItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TridentItem.class)
public class TridentItemMixin implements IMinecraft {

    @Redirect(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getYRot()F"))
    private float movementFixRiptideYaw(Player player) {
        if (player == mc.player && MovementFix.shouldFixMovement())
            return MovementFix.getMovementYaw();

        return player.getYRot();
    }

    @Redirect(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getXRot()F"))
    private float movementFixRiptidePitch(Player player) {
        if (player == mc.player && MovementFix.shouldFixMovement())
            return MovementFix.getMovementPitch();

        return player.getXRot();
    }
}
