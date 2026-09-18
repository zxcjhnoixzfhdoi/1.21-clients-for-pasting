package hack.echo.client.features.impl.movement;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventSlowdown;
import hack.echo.client.event.impl.EventTick;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.IntSetting;
import hack.echo.client.features.settings.impl.ModeSetting;
import hack.echo.client.utils.strings.Concat;

import static hack.echo.client.utils.Imports.mc;

public class NoSlowModule extends Feature {
    public NoSlowModule() {
        super(new FeatureInfo(
            Concat.of("No Slow"),
            Concat.of("Removes slow effect from items"),
            Category.MOVEMENT));
    }

    private final ModeSetting mode = new ModeSetting(Concat.of("Mode"), Concat.of("vanilla"), Concat.of("vanilla")
            ,Concat.of("Grim")
    );
    private final BoolSetting enableSneak = new BoolSetting(Concat.of("Enable Sneak"), false);
    private final IntSetting grimTicks = new IntSetting(Concat.of("Delay Ticks"), 2, 0, 10, obj -> mode.is("Grim"));

    private int grimTickCount = 0;
    private boolean grimActiveThisTick = false;

    @Override
    public void onDisable() {
        super.onDisable();
        grimTickCount = 0;
        grimActiveThisTick = false;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        grimTickCount = 0;
        grimActiveThisTick = false;
    }

    @EventSubscribe
    public void onTick(EventTick event) {
        if (isNull()) return;
        if (mc.player == null) return;

        grimActiveThisTick = false;

        if (!mc.player.isUsingItem()) {
            grimTickCount = 0;
        } else {
            grimTickCount++;
        }
    }

    @EventSubscribe
    public void onSlowdown(EventSlowdown event) {
        if (isNull()) return;

        if (mode.is(Concat.of("Grim"))) {
            if (grimActiveThisTick) {
                event.setSpoofPassenger(true);
                return;
            }
            if (grimTickCount >= grimTicks.getValue()) {
                grimActiveThisTick = true;
                grimTickCount = 0;
                event.setSpoofPassenger(true);
            }
            return;
        }

        event.setSlowdownMultiplier(1.0f);
        event.setSlowDueToItem(false);
        if (enableSneak.getValue()) {
            event.setMovingSlowly(false);
        }
    }
}
