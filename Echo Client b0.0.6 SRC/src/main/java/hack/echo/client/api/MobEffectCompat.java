package hack.echo.client.api;

import net.minecraft.world.effect.MobEffect;

public final class MobEffectCompat {

    private MobEffectCompat() {
    }

    public static boolean isInstantaneous(MobEffect effect) {
        //? if >26.1.2 {
        /*return effect.isInstantaneous();
        *///?} else {
        return effect.isInstantenous();
        //?}
    }
}
