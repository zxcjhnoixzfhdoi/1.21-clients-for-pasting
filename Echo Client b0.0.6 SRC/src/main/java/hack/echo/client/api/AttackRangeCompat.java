package hack.echo.client.api;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class AttackRangeCompat {

    private AttackRangeCompat() {
    }

    public static boolean isTargetInRange(Player player, Entity target) {
        if (player == null || target == null) return false;

        //? if >=26.1 {
        /*return player.isWithinAttackRange(player.getItemInHand(InteractionHand.MAIN_HAND), target.getBoundingBox(), 0.0);
        *///?} else {
        return player.isWithinAttackRange(target.getBoundingBox(), 0.0);
        //?}
    }

    public static boolean isLocationInRange(Player player, Vec3 location) {
        if (player == null || location == null) return false;

        //? if >=26.1 {
        /*return player.getAttackRangeWith(player.getItemInHand(InteractionHand.MAIN_HAND)).isInRange(player, location);
        *///?} else {
        return player.entityAttackRange().isInRange(player, location);
        //?}
    }

    public static double getMaxAttackRange(Player player) {
        //? if >=26.1 {
        /*return player.getAttackRangeWith(player.getItemInHand(InteractionHand.MAIN_HAND)).effectiveMaxRange(player);
        *///?} else {
        return player.entityAttackRange().effectiveMaxRange(player);
        //?}
    }
}
