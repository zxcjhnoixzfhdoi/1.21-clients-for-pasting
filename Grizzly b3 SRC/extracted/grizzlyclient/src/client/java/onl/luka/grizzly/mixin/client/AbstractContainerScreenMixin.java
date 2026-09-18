package onl.luka.grizzly.mixin.client;

import onl.luka.grizzly.module.modules.minigames.Bedwars;
import onl.luka.grizzly.module.modules.movement.InvMove;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Inject(method = "extractSlot", at = @At("HEAD"))
    private void medved$renderBedwarsShopHighlight(
            GuiGraphicsExtractor graphics,
            Slot slot,
            int mouseX,
            int mouseY,
            CallbackInfo ci
    ) {
        String title = ((Screen) (Object) this).getTitle().getString();
        Bedwars.renderShopSlot(graphics, slot, title);
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void medved$handleBedwarsShopClick(
            Slot slot,
            int slotId,
            int mouseButton,
            ContainerInput input,
            CallbackInfo ci
    ) {
        InvMove.onContainerSlotClicked();
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        AbstractContainerMenu menu = screen.getMenu();
        if (Bedwars.handleShopSlotClick(
                slot,
                slotId,
                mouseButton,
                input,
                screen.getTitle().getString(),
                menu.containerId
        )) {
            ci.cancel();
        }
    }
}
