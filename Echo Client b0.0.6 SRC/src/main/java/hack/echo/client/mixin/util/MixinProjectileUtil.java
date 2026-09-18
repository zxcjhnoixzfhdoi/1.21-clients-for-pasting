package hack.echo.client.mixin.util;

import hack.echo.client.Echo;
import hack.echo.client.features.impl.combat.HitboxMod;
import hack.echo.client.features.impl.player.AntiTrap;
import hack.echo.client.utils.combat.TargetUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static hack.echo.client.utils.Imports.mc;

@Mixin(ProjectileUtil.class)
public class MixinProjectileUtil {
    private static final double PLAYER_STANDING_HITBOX_HEIGHT = 1.8;

    private static AABB getBaseBox(Entity entity, HitboxMod hitboxMod) {
        AABB baseBox = entity.getBoundingBox();
        if (hitboxMod.staticHitbox.getValue() && entity instanceof Player player && player.isFallFlying()) {
            baseBox = new AABB(baseBox.minX, baseBox.minY, baseBox.minZ, baseBox.maxX, baseBox.minY + PLAYER_STANDING_HITBOX_HEIGHT, baseBox.maxZ);
        }

        return baseBox;
    }

    @Redirect(method = "getEntityHitResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getBoundingBox()Lnet/minecraft/world/phys/AABB;"))
    private static AABB expandHitbox(Entity entity) {
        if (Echo.isDestroyed) return entity.getBoundingBox().inflate(entity.getPickRadius());

        if (AntiTrap.shouldBlockEntityInteraction(entity)) {
            double hiddenY = entity.getY() - 1024.0;
            return new AABB(entity.getX(), hiddenY, entity.getZ(), entity.getX(), hiddenY, entity.getZ());
        }

        if (Echo.featureManager == null) return entity.getBoundingBox().inflate(entity.getPickRadius());
        HitboxMod hitboxMod = Echo.featureManager.getFeatureByClass(HitboxMod.class);
        if(hitboxMod != null && hitboxMod.isEnabled()){
            if (entity instanceof Player player) {
                if (!TargetUtils.isTargetAllowed(player)) {
                    return entity.getBoundingBox().inflate(entity.getPickRadius());
                }
            }
            if(hitboxMod.playersOnly.getValue() && !(entity instanceof Player)){
                return entity.getBoundingBox().inflate(entity.getPickRadius());
            }
            if (mc.player != null && mc.player.distanceToSqr(entity) >
                    Math.pow(hitboxMod.distanceSetting.getValue(), 2)) {
                return entity.getBoundingBox().inflate(entity.getPickRadius());
            }

            AABB originalBox = getBaseBox(entity, hitboxMod).inflate(entity.getPickRadius());
            if (hitboxMod.uniformExpand.getValue()) {
                float expandSize = hitboxMod.uniformExpandSize.getValue();
                return originalBox.inflate(expandSize, expandSize, expandSize);
            } else {
                float expandX = hitboxMod.expandX.getValue();
                float expandY = hitboxMod.expandY.getValue();
                float expandZ = hitboxMod.ExpandZ.getValue();
                return originalBox.inflate(expandX, expandY, expandZ);
            }
        }
        return entity.getBoundingBox().inflate(entity.getPickRadius());
    }
}
