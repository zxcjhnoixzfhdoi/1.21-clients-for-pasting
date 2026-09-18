package hack.echo.client.api;

import net.minecraft.world.entity.Entity;
//? if >26.1.2 {
/*import net.minecraft.world.entity.monster.cubemob.Slime;
*///?} else {
import net.minecraft.world.entity.monster.Slime;
//?}

public final class SlimeCompat {

    private SlimeCompat() {
    }

    public static boolean isTinySlime(Entity entity) {
        return entity instanceof Slime slimeEntity && slimeEntity.isTiny();
    }
}
