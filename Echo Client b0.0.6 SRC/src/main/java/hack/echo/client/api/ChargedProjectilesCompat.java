package hack.echo.client.api;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ChargedProjectiles;

import java.util.List;

public final class ChargedProjectilesCompat {

    private ChargedProjectilesCompat() {
    }

    public static List<ItemStack> itemCopies(ChargedProjectiles chargedProjectiles) {
        if (chargedProjectiles == null) {
            return List.of();
        }

        //? if >=26.1 {
        /*return chargedProjectiles.itemCopies();
        *///?} else {
        return chargedProjectiles.getItems();
        //?}
    }
}
