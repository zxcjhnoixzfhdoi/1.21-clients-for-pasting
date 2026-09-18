package hack.echo.client.utils;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Holder;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

import static hack.echo.client.utils.Imports.mc;

public class StatusUtils {

    public static boolean hasStatusEffect(Holder<MobEffect> effect) {
        Player player = mc.player;
        if (player == null) return false;
        return player.hasEffect(effect);
    }

    public static Optional<MobEffectInstance> getStatusEffectInstance(Holder<MobEffect> effect) {
        Player player = mc.player;
        if (player == null) return Optional.empty();
        return Optional.ofNullable(player.getEffect(effect));
    }

    public static List<MobEffectInstance> getActiveStatusEffects() {
        Player player = mc.player;
        if (player == null) return new ArrayList<>();
        return new ArrayList<>(player.getActiveEffects());
    }

    public static boolean hasBeneficialEffect() {
        Player player = mc.player;
        if (player == null) return false;
        for (MobEffectInstance instance : player.getActiveEffects()) {
            if (instance.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasHarmfulEffect() {
        Player player = mc.player;
        if (player == null) return false;
        for (MobEffectInstance instance : player.getActiveEffects()) {
            if (instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasSpeed() { return hasStatusEffect(MobEffects.SPEED); }
    public static boolean hasSlowness() { return hasStatusEffect(MobEffects.SLOWNESS); }
    public static boolean hasHaste() { return hasStatusEffect(MobEffects.HASTE); }
    public static boolean hasMiningFatigue() { return hasStatusEffect(MobEffects.MINING_FATIGUE); }
    public static boolean hasStrength() { return hasStatusEffect(MobEffects.STRENGTH); }
    public static boolean hasInstantHealth() { return hasStatusEffect(MobEffects.INSTANT_HEALTH); }
    public static boolean hasInstantDamage() { return hasStatusEffect(MobEffects.INSTANT_DAMAGE); }
    public static boolean hasJumpBoost() { return hasStatusEffect(MobEffects.JUMP_BOOST); }
    public static boolean hasNausea() { return hasStatusEffect(MobEffects.NAUSEA); }
    public static boolean hasRegeneration() { return hasStatusEffect(MobEffects.REGENERATION); }
    public static boolean hasResistance() { return hasStatusEffect(MobEffects.RESISTANCE); }
    public static boolean hasFireResistance() { return hasStatusEffect(MobEffects.FIRE_RESISTANCE); }
    public static boolean hasWaterBreathing() { return hasStatusEffect(MobEffects.WATER_BREATHING); }
    public static boolean hasInvisibility() { return hasStatusEffect(MobEffects.INVISIBILITY); }
    public static boolean hasBlindness() { return hasStatusEffect(MobEffects.BLINDNESS); }
    public static boolean hasNightVision() { return hasStatusEffect(MobEffects.NIGHT_VISION); }
    public static boolean hasHunger() { return hasStatusEffect(MobEffects.HUNGER); }
    public static boolean hasWeakness() { return hasStatusEffect(MobEffects.WEAKNESS); }
    public static boolean hasPoison() { return hasStatusEffect(MobEffects.POISON); }
    public static boolean hasWither() { return hasStatusEffect(MobEffects.WITHER); }
    public static boolean hasHealthBoost() { return hasStatusEffect(MobEffects.HEALTH_BOOST); }
    public static boolean hasAbsorption() { return hasStatusEffect(MobEffects.ABSORPTION); }
    public static boolean hasSaturation() { return hasStatusEffect(MobEffects.SATURATION); }
    public static boolean hasGlowing() { return hasStatusEffect(MobEffects.GLOWING); }
    public static boolean hasLevitation() { return hasStatusEffect(MobEffects.LEVITATION); }
    public static boolean hasLuck() { return hasStatusEffect(MobEffects.LUCK); }
    public static boolean hasUnluck() { return hasStatusEffect(MobEffects.UNLUCK); }
    public static boolean hasSlowFalling() { return hasStatusEffect(MobEffects.SLOW_FALLING); }
    public static boolean hasConduitPower() { return hasStatusEffect(MobEffects.CONDUIT_POWER); }
    public static boolean hasDolphinsGrace() { return hasStatusEffect(MobEffects.DOLPHINS_GRACE); }
    public static boolean hasBadOmen() { return hasStatusEffect(MobEffects.BAD_OMEN); }
    public static boolean hasHeroOfTheVillage() { return hasStatusEffect(MobEffects.HERO_OF_THE_VILLAGE); }
    public static boolean hasDarkness() { return hasStatusEffect(MobEffects.DARKNESS); }
    public static boolean hasTrialOmen() { return hasStatusEffect(MobEffects.TRIAL_OMEN); }
    public static boolean hasRaidOmen() { return hasStatusEffect(MobEffects.RAID_OMEN); }
    public static boolean hasWindCharged() { return hasStatusEffect(MobEffects.WIND_CHARGED); }
    public static boolean hasWeaving() { return hasStatusEffect(MobEffects.WEAVING); }
    public static boolean hasOozing() { return hasStatusEffect(MobEffects.OOZING); }
    public static boolean hasInfested() { return hasStatusEffect(MobEffects.INFESTED); }
    public static boolean hasBreathOfTheNautilus() { return hasStatusEffect(MobEffects.BREATH_OF_THE_NAUTILUS); }
}
