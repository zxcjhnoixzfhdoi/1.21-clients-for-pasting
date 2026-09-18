package hack.echo.client.features.impl.movement;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventMovementInput;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AutoWalkModule extends Feature {

    public AutoWalkModule() {
        super(new FeatureInfo(
            Concat.of("Auto Walk"),
            Concat.of("Automatically walks forward"),
            Category.MOVEMENT,
            false
        ));
    }

    private final BoolSetting autoWalk = new BoolSetting(Concat.of("Auto walk"), false);
//    private final BoolSetting autoFly = new BoolSetting(Concat.of("Auto fly"), false);

    @EventSubscribe
    public void onMovement(EventMovementInput e) {
        if (mc.player == null || mc.level == null) return;
        if (mc.player.isDeadOrDying()) return;
//        if (hack.echo.client.api.MinecraftCompat.getScreen() != null) return;
        if (autoWalk.getValue()) e.forward = true; e.forceMovement = true;

//        if (autoFly.getValue() && shouldStartElytra()) {
//            e.jump = true;
//        }
    }
    
    private boolean shouldStartElytra() {
        if (mc.player == null || mc.level == null) return false;
        if (hack.echo.client.api.MinecraftCompat.getScreen() != null) return false;
        if (mc.player.isDeadOrDying()) return false;
        
        if (mc.player.isFallFlying()) return false;
        if (mc.player.getAbilities().flying) return false;
        if (mc.player.isPassenger()) return false;
        if (mc.player.onClimbable()) return false;
        
        ItemStack chest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.is(Items.ELYTRA)) return false;
        if (chest.getDamageValue() >= chest.getMaxDamage() - 1) return false;
        
        if (mc.player.onGround()) return false;
        if (mc.player.isInWater()) return false;
        if (mc.player.getDeltaMovement().y >= 0.0) return false;
        
        return true;
    }
}
