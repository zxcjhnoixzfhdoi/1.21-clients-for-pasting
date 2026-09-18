package hack.echo.client.mixin.accessors;

import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FireworkRocketEntity.class)
public interface FireworkRocketAccessor {

    @Accessor("life")
    int echo$getLife();

    @Accessor("lifetime")
    int echo$getLifetime();
}
