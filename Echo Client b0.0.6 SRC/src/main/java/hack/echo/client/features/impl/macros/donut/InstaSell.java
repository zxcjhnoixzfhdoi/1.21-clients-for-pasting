package hack.echo.client.features.impl.macros.donut;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventTick;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.IntSetting;
import hack.echo.client.features.settings.impl.ItemPickerSetting;
import hack.echo.client.api.InventoryClickType;
import hack.echo.client.utils.ChatUtils;
import hack.echo.client.utils.inventory.ContainerUtils;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.RandomUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * InstaSell
 * Basically it executes '/sell' on DonutSMP and fills the /sell gui with selected sellable items from your config.
 */
public class InstaSell extends Feature {
    public InstaSell() {
        super(new FeatureInfo(
                Concat.of("Instant Sell"),
                Concat.of("Instantly /sells selected sellable items on DonutSMP"),
                Category.MACROS,
                false
        ));
    }

    private int ticks, clickTimer, typeTimer;

    private final IntSetting delay = new IntSetting(Concat.of("Click Delay"), 3, 0, 20);

    private final ItemPickerSetting sellableItemsConfig = new ItemPickerSetting(Concat.of("Sellable"));

    @Override
    public void onDisable() {
        ticks = 0;
        clickTimer = 0;
        typeTimer = 0;
        super.onDisable();
    }

    @EventSubscribe
    private void onTick(EventTick e) {
        if (isNull() || mc.getConnection() == null || hack.echo.client.api.MinecraftCompat.getScreen() instanceof LevelLoadingScreen) {
            return;
        }

        final ServerData serverInfo = mc.getCurrentServer();

        if (serverInfo == null || !serverInfo.ip.toLowerCase().contains("donutsmp")) {
            ChatUtils.chat(Concat.of("[", this.getName(), "] You're not on DonutSMP!"));
            this.toggle();
            return;
        }

        if (typeTimer > 0) {
            typeTimer--;
        }

        final int delayConfig = delay.getValue();

        if (delayConfig > 0 && clickTimer > 0) {
            clickTimer--;
            return;
        }

        switch (ticks) {
            case 0 -> {
                if (hack.echo.client.api.MinecraftCompat.getScreen() != null && !(hack.echo.client.api.MinecraftCompat.getScreen() instanceof PauseScreen)
                        && !(hack.echo.client.api.MinecraftCompat.getScreen() instanceof ChatScreen)) {
                    mc.player.closeContainer();
                    return;
                }

                if (typeTimer == 0) {
                    mc.getConnection().sendCommand("/sell");
                    clickTimer = delayConfig;
                    // 1.75 sec delay for sending commands should be enough
                    typeTimer = 35;
                    ticks++;
                }
            }
            case 1 -> {
                if (hack.echo.client.api.MinecraftCompat.getScreen() instanceof AbstractContainerScreen) {
                    final String title = hack.echo.client.api.MinecraftCompat.getScreen().getTitle().getString();

                    if (!title.contains("ÃƒÂ¡Ã‚Â´Ã‹Å“ÃƒÅ Ã…Â¸ÃƒÂ¡Ã‚Â´Ã¢â€šÂ¬ÃƒÂ¡Ã‚Â´Ã¢â‚¬Å¾ÃƒÂ¡Ã‚Â´Ã¢â‚¬Â¡ Ãƒâ€°Ã‚ÂªÃƒÂ¡Ã‚Â´Ã¢â‚¬ÂºÃƒÂ¡Ã‚Â´Ã¢â‚¬Â¡ÃƒÂ¡Ã‚Â´Ã‚ÂÃƒâ€˜Ã¢â‚¬Â¢")) {
                        mc.player.closeContainer();
                        ticks = 0;
                        return;
                    }

                    final AbstractContainerMenu screenHandler = mc.player.containerMenu;

                    if (screenHandler instanceof final ChestMenu handler) {
                        final int size = handler.getItems().size();
                        boolean sellGuiHasSpace = false;
                        for (int slot = 0; slot < 36; slot++) {
                            if (handler.getSlot(slot).getItem().isEmpty()) {
                                sellGuiHasSpace = true;
                                break;
                            }
                        }

                        if (!sellGuiHasSpace) {
                            mc.player.closeContainer();
                            clickTimer = delayConfig;
                            ticks = 0;
                            return;
                        }

                        final List<Integer> validSlots = new CopyOnWriteArrayList<>();

                        // Loop through our inventory for sellables
                        for (int slot = 36; slot < size; slot++) {
                            final ItemStack stack = handler.getSlot(slot).getItem();

                            if (stack.isEmpty()) {
                                continue;
                            }

                            final Item item = stack.getItem();

                            if (this.isSellable(item)) {
                                validSlots.add(slot);
                            }
                        }

                        // We found some nice sellables
                        if (!validSlots.isEmpty()) {
                            Collections.reverse(validSlots);

                            // Let's pick a random sellable slot from our inv
                            final int slot = validSlots
                                    .get(RandomUtils.nextInt(0, validSlots.size() - 1));
                            // Instantly shiftclick the sellable into the /sell gui
                            ContainerUtils.click(slot, InventoryClickType.QUICK_MOVE);
                            clickTimer = delayConfig;
                            // closing the container in the same tick as handling slot clicks could cause issues
                            return;
                        }

                        // Are we possibly standing in some nice sellables?!
                        boolean hasSellableInside = false;
                        for (final Entity entity : mc.level.entitiesForRendering()) {
                            if (entity instanceof final ItemEntity pItemEntity
                                    && entity.distanceTo(mc.player) <= 1
                                    && this.isSellable(pItemEntity.getItem().getItem())) {
                                hasSellableInside = true;
                                break;
                            }
                        }

                        // No sellable means we're done
                        if (!hasSellableInside) {
                            mc.player.closeContainer();
                            ticks = 0;
                            toggle();
                        }
                    }
                }
            }
        }
    }

    private boolean isSellable(Item item) {
        return sellableItemsConfig.isSelected(item);
    }
}
