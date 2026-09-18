package hack.echo.client.mixin.accessors;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;


// Pretty useless (Thought this was for movefix at first)
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    
    @Accessor("zza")
    float getZza();
    
    @Accessor("zza")
    @Mutable
    void setZza(float forwardSpeed);
    
    @Accessor("xxa")
    float getXxa();
    
    @Accessor("xxa")
    @Mutable
    void setXxa(float sidewaysSpeed);

    @Accessor("noJumpDelay")
    void setNoJumpDelay(int jumpingCooldown);

    @Accessor("attackStrengthTicker")
    int getAttackStrengthTicker();

    @Accessor("itemSwapTicker")
    int getItemSwapTicker();
}

