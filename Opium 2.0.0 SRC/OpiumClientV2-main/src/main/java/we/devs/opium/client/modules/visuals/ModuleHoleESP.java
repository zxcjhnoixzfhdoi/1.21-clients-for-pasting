package we.devs.opium.client.modules.visuals;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.api.utilities.FastHoleUtil;
import we.devs.opium.api.utilities.Renderer3d;
import we.devs.opium.client.events.EventRender3D;
import we.devs.opium.client.events.EventTick;
import we.devs.opium.client.values.impl.*;

import java.awt.*;

/**
 * todo fix double hole detection
 */
@RegisterModule(name = "HoleEsp", description = "HoleEsp", tag = "Hole ESP", category = Module.Category.VISUALS)
public class ModuleHoleESP extends Module {

    ValueNumber range = new ValueNumber("Range", "Range", "Range", 5, 0, 15);

    // render
    ValueCategory render = new ValueCategory("Render", "Render settings");
    ValueEnum renderMode = new ValueEnum("RenderMode", "Render Mode", "Determines how the Block is rendered.", RenderMode.Both);
    ValueColor safeFill = getSetting("Safe fill", new Color(20, 250, 20));
    ValueColor safeOutline = getSetting("Safe outline", new Color(20, 250, 20).darker());
    ValueColor mixedFill = getSetting("Mixed fill", new Color(246, 120, 65));
    ValueColor mixedOutline = getSetting("Mixed outline", new Color(246, 120, 65).darker());
    ValueColor unsafeFill = getSetting("Unsafe fill", new Color(250, 20, 20));
    ValueColor unsafeOutline = getSetting("Unsafe outline", new Color(250, 20, 20).darker());
    ValueNumber height = new ValueNumber("Height", "Height", "Box height", 0.05, 0, 0.5);

    ValueColor getSetting(String name, Color defaultC) {
        return new ValueColor(name, name, name, render, defaultC);
    }

    private boolean disable;

    @Override
    public void onEnable() {
        super.onEnable();
        disable = false;
    }

    @Override
    public void onTick(EventTick event) {
        if (!disable){
            FastHoleUtil.INSTANCE.onTick(event);
        }
    }

    @Override
    public void onRender3D(EventRender3D event) {
        for (FastHoleUtil.Hole hole : FastHoleUtil.holes) {
            if (hole.safety() == FastHoleUtil.HoleSafety.UNSAFE || Vec3d.of(hole.air().get(0)).distanceTo(mc.player.getPos()) > range.getValue().doubleValue())
                continue;
            Color fill = switch (hole.safety()) {
                case UNBREAKABLE -> safeFill.getValue();
                case PARTIALLY_UNBREAKABLE -> mixedFill.getValue();
                case BREAKABLE -> unsafeFill.getValue();
                default -> throw new IllegalStateException("Unexpected value: " + hole.safety());
            };

            Color outline = switch (hole.safety()) {
                case UNBREAKABLE -> safeOutline.getValue();
                case PARTIALLY_UNBREAKABLE -> mixedOutline.getValue();
                case BREAKABLE -> unsafeOutline.getValue();
                default -> throw new IllegalStateException("Unexpected value: " + hole.safety());
            };

            for (BlockPos blockPos : hole.air()) {
                if (this.renderMode.getValue().equals(ModuleHoleESP.RenderMode.Both)) {
                    Renderer3d.renderEdged(event.getMatrices(), injectAlpha(fill), injectAlpha(outline), Vec3d.of(blockPos), new Vec3d(1, height.getValue().doubleValue(), 1));
                } else if (this.renderMode.getValue().equals(ModuleHoleESP.RenderMode.Fill)) {
                    Renderer3d.renderFilled(event.getMatrices(), injectAlpha(fill), Vec3d.of(blockPos), new Vec3d(1, height.getValue().doubleValue(), 1));
                } else if (this.renderMode.getValue().equals(ModuleHoleESP.RenderMode.Outline)) {
                    Renderer3d.renderOutline(event.getMatrices(), injectAlpha(outline), Vec3d.of(blockPos), new Vec3d(1, height.getValue().doubleValue(), 1));
                }
            }
        }
    }

    public void onDisable() {
        super.onDisable();
        disable = true;
    }

    Color injectAlpha(Color color) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), 40);
    }

    public enum RenderMode {
        Both,
        Fill,
        Outline
    }
}
