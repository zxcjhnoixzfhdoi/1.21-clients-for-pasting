package hack.echo.client.features.impl.player;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventSetScreen;
import hack.echo.client.event.impl.EventTick;
import hack.echo.client.event.impl.MouseUpdateEvent;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.utils.strings.Concat;
import hack.echo.client.features.settings.impl.ModeSetting;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.IntSetting;
import hack.echo.client.features.settings.impl.RangeSetting;
import hack.echo.client.features.impl.player.totem.CursorModeTotem;
import hack.echo.client.features.impl.player.totem.HoverAutoTotem;

//Todo: Add WindMouse movement
//Todo: flagging

public class TotemRefill extends Feature {

	public TotemRefill() {
		super(new FeatureInfo(
			Concat.of("Totem Refill"),
			Concat.of("Automatically refills totems"),
			Category.UTILITY
		));
	}
    private static final CharSequence CURSOR_MODE = Concat.of("Cursor");
    private static final CharSequence HOVER_MODE = Concat.of("Hover");

    private static final ModeSetting modeSetting = new ModeSetting(
        Concat.of("Mode"),
        CURSOR_MODE,
        CURSOR_MODE,
        HOVER_MODE
    );

    // Needs to be referenced to appear in the GUI settings
    @SuppressWarnings("unused")
    private static final BoolSetting autoOpenCursor = CursorModeTotem.autoOpenCursor;
    @SuppressWarnings("unused")
    private static final BoolSetting autoCloseCursor = CursorModeTotem.autoCloseCursor;
    @SuppressWarnings("unused")
    private static final BoolSetting autoSwapCursor = CursorModeTotem.autoSwapCursor;
    @SuppressWarnings("unused")
    private static final BoolSetting autoHotbarSlotCursor = CursorModeTotem.autoHotbarSlotCursor;
    @SuppressWarnings("unused")
    private static final IntSetting hotbarSlotCursor = CursorModeTotem.hotbarSlotCursor;
    @SuppressWarnings("unused")
    private static final RangeSetting cursorTimeMS = CursorModeTotem.cursorTimeMS;

    @SuppressWarnings("unused")
    private static final BoolSetting autoOpenHover = HoverAutoTotem.autoOpenHover;
    @SuppressWarnings("unused")
    private static final BoolSetting autoOffhandHover = HoverAutoTotem.autoOffhandHover;
    @SuppressWarnings("unused")
    private static final BoolSetting autoHotbarSlotHover = HoverAutoTotem.autoHotbarSlotHover;
    @SuppressWarnings("unused")
    private static final IntSetting hotbarSlotHover = HoverAutoTotem.hotbarSlotHover;

    static {
        CursorModeTotem.autoOpenCursor.setDependency(o -> modeSetting.is(CURSOR_MODE));
        CursorModeTotem.autoCloseCursor.setDependency(o -> modeSetting.is(CURSOR_MODE));
        CursorModeTotem.autoSwapCursor.setDependency(o -> modeSetting.is(CURSOR_MODE));
        CursorModeTotem.autoHotbarSlotCursor.setDependency(o -> modeSetting.is(CURSOR_MODE));
        CursorModeTotem.hotbarSlotCursor.setDependency(o -> modeSetting.is(CURSOR_MODE) && CursorModeTotem.autoHotbarSlotCursor.getValue());
        CursorModeTotem.cursorTimeMS.setDependency(o -> modeSetting.is(CURSOR_MODE));

        HoverAutoTotem.autoOpenHover.setDependency(o -> modeSetting.is(HOVER_MODE));
        HoverAutoTotem.autoOffhandHover.setDependency(o -> modeSetting.is(HOVER_MODE));
        HoverAutoTotem.autoHotbarSlotHover.setDependency(o -> modeSetting.is(HOVER_MODE));
        HoverAutoTotem.hotbarSlotHover.setDependency(o -> modeSetting.is(HOVER_MODE) && HoverAutoTotem.autoHotbarSlotHover.getValue());
    }

    private final CursorModeTotem cursorMode = new CursorModeTotem();
    private final HoverAutoTotem hoverMode = new HoverAutoTotem();

    @EventSubscribe
    public void onTick(EventTick e) {
        if (modeSetting.is(CURSOR_MODE)) {
            cursorMode.onTick(e);
        } else if (modeSetting.is(HOVER_MODE)) {
            hoverMode.onTick(e);
        }
    }

    @EventSubscribe
    public void onScreenOpen(EventSetScreen e) {
        if (modeSetting.is(CURSOR_MODE)) {
            cursorMode.onScreenOpen(e);
        }
    }

    @EventSubscribe
    public void onRender2D(MouseUpdateEvent e) {
        if (modeSetting.is(CURSOR_MODE)) {
            cursorMode.onRender2D(e);
        }
    }
}
