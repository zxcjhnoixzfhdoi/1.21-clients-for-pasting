package hack.echo.client.features.impl.render;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.*;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.utils.strings.Concat;

// Cleanest code in the west (im sorry)

public class NoRender extends Feature {

	public NoRender() {
		super(new FeatureInfo(
			Concat.of("No Render"),
			Concat.of("Disables certain rendering"),
			Category.RENDER,
			false
		));
	}
    public final BoolSetting noFireOverlay = new BoolSetting("No Fire Overlay", false);
    public final BoolSetting noNausea = new BoolSetting("No Nausea", false);
    public final BoolSetting noVignette = new BoolSetting("No Vignette", false);
    public final BoolSetting noFloatingItem = new BoolSetting("No Floating Item", false);
    public final BoolSetting noSpyglassOverlay = new BoolSetting("No Spyglass Overlay", false);
    public final BoolSetting noPortalOverlay = new BoolSetting("No Portal Overlay", false);
    public final BoolSetting noCrystalBounce = new BoolSetting("No Crystal Bounce", false);

    private boolean isEnabled = false;

    @EventSubscribe
    public final void setNoFireOverlay(EventRenderFireOverlay event) {
        if (isNull()) return;
        if (!isEnabled) return;
        if (noFireOverlay.getValue()) event.cancel();
    }

    @EventSubscribe
    public final void setNoNausea(EventRenderNauseaOverlay event) {
        if (isNull()) return;
        if (!isEnabled) return;
        if (noNausea.getValue()) event.cancel();
    }

    @EventSubscribe
    public final void setNoVignette(EventRenderVignette event) {
        if (isNull()) return;
        if (!isEnabled) return;
        if (noVignette.getValue()) event.cancel();
    }

    @EventSubscribe
    public final void setNoFloatingItem(EventRenderFloatingItem event) {
        if (isNull()) return;
        if (!isEnabled) return;
        if (noFloatingItem.getValue()) event.cancel();
    }
    @EventSubscribe
    public final void setNoSpyglassOverlay(EventRenderSpyglassOverlay event) {
        if (isNull()) return;
        if (!isEnabled) return;
        if (noSpyglassOverlay.getValue()) event.cancel();
    }
    @EventSubscribe
    public final void setNoPortalOverlay(EventRenderPortalOverlay event) {
        if (isNull()) return;
        if (!isEnabled) return;
        if (noPortalOverlay.getValue()) event.cancel();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        isEnabled = false;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        isEnabled = true;
    }
}
