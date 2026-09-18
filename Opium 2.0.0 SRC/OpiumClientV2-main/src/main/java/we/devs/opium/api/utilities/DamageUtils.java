package we.devs.opium.api.utilities;

import net.minecraft.item.ItemStack;

public class DamageUtils implements IMinecraft {
    public static int getRoundedDamage(ItemStack stack) {
        return (int)((float)(stack.getMaxDamage() - stack.getDamage()) / (float)stack.getMaxDamage() * 100.0f);
    }
}
