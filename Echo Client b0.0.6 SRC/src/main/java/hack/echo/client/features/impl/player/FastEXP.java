package hack.echo.client.features.impl.player;

import hack.echo.client.utils.strings.Concat;
import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventHandleInput;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.IntSetting;
import hack.echo.client.features.settings.impl.KeybindSetting;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import org.lwjgl.glfw.GLFW;
import hack.echo.client.handlers.InputHandler;

public class FastEXP extends Feature {

	public FastEXP() {
		super(new FeatureInfo(
			Concat.of("Fast EXP"),
			Concat.of("Faster experience collection"),
			Category.UTILITY
		));
	}
    private final IntSetting clockSpeed = new IntSetting(Concat.of("S", "p", "e", "e", "d"), 4, 1, 12);
    private int clock = 0;
    private final BoolSetting autoEXP = new BoolSetting(Concat.of("A", "u", "t", "o", " ", "E", "X", "P"), true);
    private final KeybindSetting activateKey = new KeybindSetting(Concat.of("A", "c", "t", "i", "v", "a", "t", "e", " ", "K", "e", "y"), -1, o -> autoEXP.getValue());

    @EventSubscribe
    public void onPlayerTick(EventHandleInput.Early event) {
        if (isNull() || hack.echo.client.api.MinecraftCompat.getScreen() != null) return;

        if (GLFW.glfwGetMouseButton(mc.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_2) == GLFW.GLFW_PRESS) {
            if (!autoEXP.getValue() || activateKey.isListening()) {
                return;
            }

            boolean mainHasXP = mc.player.getMainHandItem().is(Items.EXPERIENCE_BOTTLE);
            boolean offHasXP = mc.player.getOffhandItem().is(Items.EXPERIENCE_BOTTLE);
            boolean hasXP = mainHasXP || offHasXP;

            if (!hasXP && autoEXP.getValue() && activateKey.isListening()) {
                for (int i = 0; i < 9; i++) {
                    if (mc.player.getInventory().getItem(i).is(Items.EXPERIENCE_BOTTLE)) {
                        mc.player.getInventory().setSelectedSlot(i);
                        hasXP = true;
                        break;
                    }
                }
            }
            if (!hasXP) {
                return;
            }

            int speed = this.clockSpeed.getValue();
            speed = 13 - speed;

            ++this.clock;

            if (this.clock >= speed) {
                this.clock = 0;
                InteractionHand handToUse = offHasXP ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                mc.gameMode.useItem(mc.player, handToUse);
                mc.player.swing(handToUse);
            }
            return;
        }
        if (!autoEXP.getValue() || !InputHandler.isBindDown(activateKey.getKey()) || activateKey.isListening()) {
            return;
        }

        boolean mainHasXP = mc.player.getMainHandItem().is(Items.EXPERIENCE_BOTTLE);
        boolean offHasXP = mc.player.getOffhandItem().is(Items.EXPERIENCE_BOTTLE);
        boolean hasXP = mainHasXP || offHasXP;

        if (!hasXP) {
            for (int i = 0; i < 9; i++) {
                if (mc.player.getInventory().getItem(i).is(Items.EXPERIENCE_BOTTLE)) {
                    mc.player.getInventory().setSelectedSlot(i);
                    hasXP = true;
                    break;
                }
            }
        }
        if (!hasXP) {
            return;
        }

    int speed = this.clockSpeed.getValue();
    speed = 13 - speed;

        ++this.clock;

        if (this.clock >= speed) {
            this.clock = 0;
            InteractionHand handToUse = offHasXP ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            mc.gameMode.useItem(mc.player, handToUse);
            mc.player.swing(handToUse);
        }
    }
}
