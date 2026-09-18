package hack.echo.client.utils.inventory;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.core.Holder;

import java.util.Optional;
import java.util.function.Predicate;

import static hack.echo.client.utils.Imports.mc;

// Wallahi why did I let copilot autocomplete these comments

public class PotionUtils {
    /**
     * Finds the first hotbar slot containing a splash potion that matches the given predicate.
     *
     * @param predicate The predicate to match against the potion type.
     * @return The slot number (0-8), or -1 if not found.
     */
    public static int findPotionInHotbar(Predicate<Holder<Potion>> predicate) {
        Inventory inventory = mc.player.getInventory();
        for (int i = 0; i <= 8; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() == Items.SPLASH_POTION) {
                PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
                if (contents != null) {
                    Optional<Holder<Potion>> potion = contents.potion();
                    if (potion.isPresent() && predicate.test(potion.get())) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }


    /*
     * Gets night vision splash potions in hotbar
     */
    public static int findNightVisionPotionInHotbar() {
        return findPotionInHotbar(potion -> potion == Potions.NIGHT_VISION || potion == Potions.LONG_NIGHT_VISION);
    }

    /*
     * Gets invisibility splash potions in hotbar
     */
    public static int findInvisibilityPotionInHotbar() {
        return findPotionInHotbar(potion -> potion == Potions.INVISIBILITY || potion == Potions.LONG_INVISIBILITY);
    }

    /*
     * Gets jump boost splash potions in hotbar
     */
    public static int findJumpBoostPotionInHotbar() {
        return findPotionInHotbar(potion -> potion == Potions.LONG_LEAPING || potion == Potions.LEAPING);
    }

    public static int findFireResistancePotionInHotbar() {
        return findPotionInHotbar(potion -> potion == Potions.FIRE_RESISTANCE || potion == Potions.LONG_FIRE_RESISTANCE);
    }

    /*
     * Gets speed splash potions in hotbar
     */
    public static int findSpeedPotionInHotbar() {
        return findPotionInHotbar(potion -> potion == Potions.STRONG_SWIFTNESS || potion == Potions.SWIFTNESS);
    }

    public static int findSlownessPotionInHotbar() {
        return findPotionInHotbar(potion -> potion == Potions.STRONG_SLOWNESS || potion == Potions.SLOWNESS || potion == Potions.LONG_SLOWNESS);
    }

    public static int findTurtleMasterPotionInHotbar() {
        return findPotionInHotbar(potion -> potion == Potions.TURTLE_MASTER || potion == Potions.LONG_TURTLE_MASTER || potion == Potions.STRONG_TURTLE_MASTER);
    }

    public static int findWaterBreathingPotionInHotbar() {
        return findPotionInHotbar(potion -> potion == Potions.WATER_BREATHING || potion == Potions.LONG_WATER_BREATHING);
    }

    /*
     * Gets instant health splash potions in hotbar
     */
    public static int findInstantHealthPotionInHotbar() {
        return findPotionInHotbar(potion -> potion == Potions.STRONG_HEALING || potion == Potions.HEALING);
    }

    public static int findHarmingPotionInHotbar() {
        return findPotionInHotbar(potion -> potion == Potions.STRONG_HARMING || potion == Potions.HARMING);
    }

    public static int findPoisonPotionInHotbar() {
        return findPotionInHotbar(potion -> potion == Potions.STRONG_POISON || potion == Potions.POISON || potion == Potions.LONG_POISON);
    }

    /*
     * Gets strength splash potions in hotbar
     */
    public static int findStrengthPotionInHotbar() {
        return findPotionInHotbar(potion -> potion == Potions.STRONG_STRENGTH || potion == Potions.STRENGTH);
    }

    /*
     * Gets regeneration splash potions in hotbar
     */
    public static int findRegenerationPotionInHotbar() {
        return findPotionInHotbar(potion -> potion == Potions.REGENERATION || potion == Potions.LONG_REGENERATION || potion == Potions.STRONG_REGENERATION);
    }

    /*
     * Gets weakness splash potions in hotbar
     */
    public static int findWeaknessPotionInHotbar() {
        return findPotionInHotbar(potion -> potion == Potions.WEAKNESS || potion == Potions.LONG_WEAKNESS);
    }

    /*
     * Gets luck splash potions in hotbar
     */
    public static int findLuckPotionInHotbar() {
        return findPotionInHotbar(potion -> potion == Potions.LUCK);
    }

    /*
     * Gets slow falling splash potions in hotbar
     */
    public static int findSlowFallingPotionInHotbar() {
        return findPotionInHotbar(potion -> potion == Potions.SLOW_FALLING || potion == Potions.LONG_SLOW_FALLING);
    }

    /*
     * Gets wind charged splash potions in hotbar
     */
    public static int findWindChargedPotionInHotbar() {
        return findPotionInHotbar(potion -> potion == Potions.WIND_CHARGED);
    }

    /*
     * Gets weaving splash potions in hotbar
     */
    public static int findWeavingPotionInHotbar() {
        return findPotionInHotbar(potion -> potion == Potions.WEAVING);
    }

    /*
     * Gets oozing splash potions in hotbar
     */
    public static int findOozingPotionInHotbar() {
        return findPotionInHotbar(potion -> potion == Potions.OOZING);
    }

    /*
     * Gets infested splash potions in hotbar
     */
    public static int findInfestedPotionInHotbar() {
        return findPotionInHotbar(potion -> potion == Potions.INFESTED);
    }

    /**
     * Checks if the offhand contains a splash potion that matches the given predicate.
     *
     * @param predicate The predicate to match against the potion type.
     * @return true if offhand contains a matching splash potion.
     */
    public static boolean hasPotionInOffhand(Predicate<Holder<Potion>> predicate) {
        ItemStack stack = mc.player.getOffhandItem();
        if (stack.getItem() == Items.SPLASH_POTION) {
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents != null) {
                Optional<Holder<Potion>> potion = contents.potion();
                return potion.isPresent() && predicate.test(potion.get());
            }
        }
        return false;
    }

    public static boolean hasInstantHealthPotionInOffhand() {
        return hasPotionInOffhand(potion -> potion == Potions.STRONG_HEALING || potion == Potions.HEALING);
    }

    public static boolean hasSpeedPotionInOffhand() {
        return hasPotionInOffhand(potion -> potion == Potions.STRONG_SWIFTNESS || potion == Potions.SWIFTNESS);
    }

    public static boolean hasStrengthPotionInOffhand() {
        return hasPotionInOffhand(potion -> potion == Potions.STRONG_STRENGTH || potion == Potions.STRENGTH);
    }

}
