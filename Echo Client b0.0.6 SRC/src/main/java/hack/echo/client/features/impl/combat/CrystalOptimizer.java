package hack.echo.client.features.impl.combat;

import hack.echo.client.event.impl.EventStartAttack;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.utils.inventory.InventoryUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import hack.echo.client.event.EventSubscribe;

public class CrystalOptimizer extends Feature {
    public CrystalOptimizer() {
        super(new FeatureInfo("Crystal Optimizer", "Optimizes crystal speeds", Category.COMBAT));
    }

    @EventSubscribe
    public void onEventDoAttack(EventStartAttack event) {
        if (isNull())
            return;
        if (mc.hitResult == null)
            return;

        if (mc.hitResult.getType() != HitResult.Type.ENTITY)
            return;
        if (!(mc.hitResult instanceof EntityHitResult hit))
            return;

        Entity target = hit.getEntity();
        if (!(target instanceof EndCrystal crystal))
            return;

        MobEffectInstance weakness = mc.player.getEffect(MobEffects.WEAKNESS);
        MobEffectInstance strength = mc.player.getEffect(MobEffects.STRENGTH);

        boolean canAttack = (weakness == null)
                || (strength != null && strength.getAmplifier() > weakness.getAmplifier())
                || (InventoryUtils.isHoldingPickaxeItem())
                || (InventoryUtils.isHoldingSwordItem());

        if (!canAttack)
            return;

        crystal.setRemoved(Entity.RemovalReason.KILLED);
        crystal.onClientRemoval();
    }
}
