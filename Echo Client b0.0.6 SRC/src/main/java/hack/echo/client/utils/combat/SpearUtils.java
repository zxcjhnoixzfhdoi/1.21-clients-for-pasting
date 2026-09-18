package hack.echo.client.utils.combat;

import hack.echo.client.mixin.accessors.LivingEntityAccessor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class SpearUtils {
    /**
     * Tracks the player's tickCount at the time of the last hand swing.
     * On the server, ServerPlayer.swing() calls resetAttackStrengthTicker() which
     * resets both attackStrengthTicker and itemSwapTicker to 0. The client does NOT
     * reset these tickers on swing (only on attack/item change), causing a desync
     * when the player uses an item or places a block. We track swings ourselves to
     * mirror the server's behavior.
     */
    private static int lastSwingPlayerTick = Integer.MIN_VALUE;

    private SpearUtils() {
    }

    public static void onPlayerSwing(Player player) {
        if (player != null) {
            lastSwingPlayerTick = player.tickCount;
        }
    }

    private static int getSwingTicker(Player player) {
        if (player == null) return Integer.MAX_VALUE;
        return player.tickCount - lastSwingPlayerTick;
    }

    public static boolean isSpearFullyCooled(Player player, ItemStack spearStack, float partialTicks) {
        return getProjectedSpearAttackStrengthScale(player, spearStack, partialTicks) >= 1.0F;
    }

    public static boolean isSpearSwapFullyCooled(Player player, ItemStack spearStack, float partialTicks) {
        int effectiveTicker = Math.min(getItemSwapTicker(player), getSwingTicker(player));
        return getProjectedSpearScale(player, spearStack, partialTicks, effectiveTicker) >= 1.0F;
    }

    public static float getProjectedSpearAttackStrengthScale(Player player, ItemStack spearStack, float partialTicks) {
        return getProjectedSpearScale(player, spearStack, partialTicks, getAttackStrengthTicker(player));
    }

    public static float getProjectedSpearSwapStrengthScale(Player player, ItemStack spearStack, float partialTicks) {
        int effectiveTicker = Math.min(getItemSwapTicker(player), getSwingTicker(player));
        return getProjectedSpearScale(player, spearStack, partialTicks, effectiveTicker);
    }

    public static double getRemainingSpearAttackCooldownSeconds(Player player, ItemStack spearStack, float partialTicks) {
        return getRemainingCooldownSeconds(player, spearStack, partialTicks, getAttackStrengthTicker(player));
    }

    public static double getRemainingSpearSwapCooldownSeconds(Player player, ItemStack spearStack, float partialTicks) {
        int effectiveTicker = Math.min(getItemSwapTicker(player), getSwingTicker(player));
        return getRemainingCooldownSeconds(player, spearStack, partialTicks, effectiveTicker);
    }

    private static int getAttackStrengthTicker(Player player) {
        return player == null ? 0 : ((LivingEntityAccessor) player).getAttackStrengthTicker();
    }

    private static int getItemSwapTicker(Player player) {
        return player == null ? 0 : ((LivingEntityAccessor) player).getItemSwapTicker();
    }

    private static float getProjectedSpearScale(Player player, ItemStack spearStack, float partialTicks, int ticker) {
        float projectedDelay = getProjectedSpearAttackDelay(player, spearStack);
        if (projectedDelay <= 0.0F) {
            return 0.0F;
        }

        return Mth.clamp((ticker + partialTicks) / projectedDelay, 0.0F, 1.0F);
    }

    private static double getRemainingCooldownSeconds(Player player, ItemStack spearStack, float partialTicks, int ticker) {
        float projectedDelay = getProjectedSpearAttackDelay(player, spearStack);
        if (projectedDelay <= 0.0F) {
            return 0.0D;
        }

        return Math.max(0.0D, (projectedDelay - (ticker + partialTicks)) / 20.0D);
    }

    private static float getProjectedSpearAttackDelay(Player player, ItemStack spearStack) {
        if (player == null || spearStack == null || spearStack.isEmpty()) {
            return 0.0F;
        }

        AttributeInstance attackSpeedInstance = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeedInstance == null) {
            return player.getCurrentItemAttackStrengthDelay();
        }

        double[] add = {0.0D};
        double[] addMultBase = {0.0D};
        double[] addMultTotal = {1.0D};

        /**
         * AttributeInstance.class
         *     private double calculateValue() {
         *         double d = this.getBaseValue();
         *
         *         for (AttributeModifier attributeModifier : this.getModifiersOrEmpty(AttributeModifier.Operation.ADD_VALUE)) {
         *             d += attributeModifier.amount();
         *         }
         *
         *         double e = d;
         *
         *         for (AttributeModifier attributeModifier2 : this.getModifiersOrEmpty(AttributeModifier.Operation.ADD_MULTIPLIED_BASE)) {
         *             e += d * attributeModifier2.amount();
         *         }
         *
         *         for (AttributeModifier attributeModifier2 : this.getModifiersOrEmpty(AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)) {
         *             e *= 1.0 + attributeModifier2.amount();
         *         }
         *
         *         return this.attribute.value().sanitizeValue(e);
         *     }
         */
        for (AttributeModifier modifier : attackSpeedInstance.getModifiers()) {
            if (modifier.is(Item.BASE_ATTACK_SPEED_ID)) {
                continue;
            }
            applyModifier(modifier, add, addMultBase, addMultTotal);
        }

        /**
         * ItemStack.class
         *     public void forEachModifier(EquipmentSlot equipmentSlot, BiConsumer<Holder<Attribute>, AttributeModifier> biConsumer) {
         *         ItemAttributeModifiers itemAttributeModifiers = this.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
         *         itemAttributeModifiers.forEach(equipmentSlot, biConsumer);
         *         EnchantmentHelper.forEachModifier(this, equipmentSlot, biConsumer);
         *     }
         */
        spearStack.forEachModifier(EquipmentSlot.MAINHAND, (holder, modifier) -> {
            if (holder.equals(Attributes.ATTACK_SPEED)) {
                applyModifier(modifier, add, addMultBase, addMultTotal);
            }
        });

        double base = attackSpeedInstance.getBaseValue();
        double withAdds = base + add[0];
        double withBaseMultiplier = withAdds + (withAdds * addMultBase[0]);
        double projectedSpeed = withBaseMultiplier * addMultTotal[0];
        projectedSpeed = Attributes.ATTACK_SPEED.value().sanitizeValue(projectedSpeed);

        if (projectedSpeed <= 0.0D) {
            return 0.0F;
        }

        return (float) (20.0D / projectedSpeed);
    }

    private static void applyModifier(
        AttributeModifier modifier,
        double[] add,
        double[] addMultBase,
        double[] addMultTotal
    ) {
        switch (modifier.operation()) {
            case ADD_VALUE -> add[0] += modifier.amount();
            case ADD_MULTIPLIED_BASE -> addMultBase[0] += modifier.amount();
            case ADD_MULTIPLIED_TOTAL -> addMultTotal[0] *= 1.0D + modifier.amount();
        }
    }
}
