package we.devs.opium.asm.mixins;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.TrackedData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import we.devs.opium.Opium;
import we.devs.opium.client.events.DeathEvent;
import we.devs.opium.api.utilities.IMinecraft;

@Mixin(LivingEntity.class)
public class LivingEntityMixin extends EntityMixin implements IMinecraft {

    @Shadow
    @Final
    private static TrackedData<Float> HEALTH;

    @Unique
    private static int t;


    @Inject(method = "onTrackedDataSet", at = @At("RETURN"))
    public void onTrackedDataSet(TrackedData<?> data, CallbackInfo ci) {
        if (data.equals(HEALTH) && this.dataTracker.get(HEALTH) <= 0.0 && mc.world != null && mc.world.isClient()) {
            DeathEvent deathEvent = new DeathEvent(LivingEntity.class.cast(this));
            Opium.EVENT_MANAGER.call(deathEvent);
        }
    }
}
