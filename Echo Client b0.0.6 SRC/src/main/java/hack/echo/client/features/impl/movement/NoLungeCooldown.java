package hack.echo.client.features.impl.movement;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventStartAttack;
import hack.echo.client.event.impl.EventSwingHand;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.IntSetting;
import hack.echo.client.utils.inventory.InventoryUtils;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;

public class NoLungeCooldown extends Feature {

    public NoLungeCooldown() {
        super(new FeatureInfo(
            Concat.of("No Lunge Cooldown"),
            Concat.of("Modifies spear lunge velocity"),
            Category.MOVEMENT
        ));
    }

    private final IntSetting multiplier = new IntSetting(Concat.of("Multiplier"), 1, 1, 10);

    @EventSubscribe
    public void onSwingHand(EventSwingHand swing) {
        if (InventoryUtils.isHoldingSpearItem()) {
            swing.cancel();
        }
    }

    @EventSubscribe
    public void onAttack(EventStartAttack attack) {
        if (isNull()) return;
        if (InventoryUtils.isHoldingSpearItem()) {
            attack.cancel();
            sendStabPackets();
        }
    }

    private void sendStabPackets() {
        for (int i = 0; i < multiplier.getValue(); i++) {
            mc.player.connection.send(
                new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STAB, BlockPos.ZERO, Direction.DOWN)
            );
        }
    }
}
