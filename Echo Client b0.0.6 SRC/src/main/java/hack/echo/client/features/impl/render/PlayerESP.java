package hack.echo.client.features.impl.render;

import hack.echo.client.Echo;
import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventRender2DGui;
import hack.echo.client.event.impl.EventRender3D;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.ColorSetting;
import hack.echo.client.features.settings.impl.ModeSetting;
import hack.echo.client.features.settings.impl.MultiModeSetting;
import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.render2.impl.opengl.utils.RenderUtil;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Queue;

public class PlayerESP extends Feature {

    private final MultiModeSetting enabledModes = new MultiModeSetting(Concat.of("Modes"), Concat.of("Box"), Concat.of("2D"));
    private final BoolSetting fill = new BoolSetting(Concat.of("Fill"), false);
    private final ModeSetting healthBar = new ModeSetting(Concat.of("Health Bar"), Concat.of("None"), Concat.of("None"), Concat.of("2D"), Concat.of("Slim"));
    private final BoolSetting tracers = new BoolSetting(Concat.of("Tracers"), true);
    private final ColorSetting colorSetting = new ColorSetting(Concat.of("Color"), 0x7F8C0000);

    private final Queue<Runnable> queue = new ArrayDeque<>();

    public PlayerESP() {
        super(new FeatureInfo(
                Concat.of("Player ESP"),
                Concat.of("Highlights players"),
                Category.RENDER
        ));
    }

    @EventSubscribe
    public void event(EventRender3D event) {
        if (mc.level == null) return;

        var draw3 = event.getDraw3D().getCustomTarget();
        var draw2 = Echo.draw2D;
        Matrix4f mat = new Matrix4f();
        float tickDelta = event.getTickDelta();
        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        Frustum frustum = new Frustum(event.getModelViewMatrix(), event.getProjectionMatrix());
        frustum.prepare(cam.x, cam.y, cam.z);
        boolean box = enabledModes.isEnabled(Concat.of("Box"));
        boolean twoD = enabledModes.isEnabled(Concat.of("2D"));
        boolean bar = healthBar.is(Concat.of("2D"));
        boolean slim = healthBar.is(Concat.of("Slim"));
        int fillColor = colorSetting.getARGB();
        int outlineColor = fillColor | 0xFF000000;
        int outlineBg = 0xFF000000;

        for (var entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof Player player)) continue;
            if (player == mc.player) continue;

            Vec3 pos = player.getPosition(tickDelta);
            AABB bb = player.getBoundingBox();
            double sx = bb.getXsize();
            double sy = bb.getYsize();
            double sz = bb.getZsize();
            double minX = pos.x - sx / 2.0;
            double minY = pos.y;
            double minZ = pos.z - sz / 2.0;

            if (box) {
                draw3.box(minX, minY, minZ, sx, sy, sz, fillColor);
                double maxX = minX + sx;
                double maxY = minY + sy;
                double maxZ = minZ + sz;
                draw3.line(minX, minY, minZ, maxX, minY, minZ, outlineColor);
                draw3.line(maxX, minY, minZ, maxX, minY, maxZ, outlineColor);
                draw3.line(maxX, minY, maxZ, minX, minY, maxZ, outlineColor);
                draw3.line(minX, minY, maxZ, minX, minY, minZ, outlineColor);
                draw3.line(minX, maxY, minZ, maxX, maxY, minZ, outlineColor);
                draw3.line(maxX, maxY, minZ, maxX, maxY, maxZ, outlineColor);
                draw3.line(maxX, maxY, maxZ, minX, maxY, maxZ, outlineColor);
                draw3.line(minX, maxY, maxZ, minX, maxY, minZ, outlineColor);
                draw3.line(minX, minY, minZ, minX, maxY, minZ, outlineColor);
                draw3.line(maxX, minY, minZ, maxX, maxY, minZ, outlineColor);
                draw3.line(maxX, minY, maxZ, maxX, maxY, maxZ, outlineColor);
                draw3.line(minX, minY, maxZ, minX, maxY, maxZ, outlineColor);
            }
            if (tracers.getValue()) {
                draw3.tracer(pos.x, pos.y + player.getEyeHeight(), pos.z, outlineColor);
            }

            if (twoD || bar || slim) {
                AABB worldBB = new AABB(minX, minY, minZ, minX + sx, minY + sy, minZ + sz);
                if (!frustum.isVisible(worldBB)) continue;

                Rectangle rect = projectAABB(event, worldBB);
                if (!rect.visible) continue;

                float x = (float) rect.x;
                float y = (float) rect.y;
                float w = (float) (rect.z - rect.x);
                float h = (float) (rect.w - rect.y);

                if (twoD) {
                    if (fill.getValue()) {
                        queue.add(() -> draw2.rect(mat, x, y, w, h, 0, fillColor));
                    }
                    queue.add(() -> drawOutlineRect(draw2, mat, x - 1, y - 1, w + 2, h + 2, 1, outlineBg));
                    queue.add(() -> drawOutlineRect(draw2, mat, x, y, w, h, 1, outlineColor));
                }

                if (bar) {
                    float percent = healthPercent(player);
                    int healthColor = healthColor(percent);
                    float barW = Math.max(2f, w * 0.08f);
                    float gap = 2f;
                    float bx = x - barW - gap;
                    float fillH = h * percent;
                    queue.add(() -> draw2.rect(mat, bx - 1, y - 1, barW + 2, h + 2, 0, outlineBg));
                    queue.add(() -> draw2.rect(mat, bx, y, barW, h, 0, 0xFF202020));
                    queue.add(() -> draw2.rect(mat, bx, y + h - fillH, barW, fillH, 0, healthColor));
                } else if (slim) {
                    float percent = healthPercent(player);
                    int healthColor = healthColor(percent);
                    float width = 2f;
                    float gap = 4f;
                    float fillH = h * percent;
                    queue.add(() -> draw2.rect(mat, x - width - gap , y + h - fillH, width, fillH, 0, healthColor));
                }
            }
        }
    }

    @EventSubscribe
    public void event(EventRender2DGui event) {
        while (!queue.isEmpty()) {
            queue.poll().run();
        }
    }

    private static void drawOutlineRect(Draw2D draw, Matrix4f mat, float x, float y, float w, float h, float thickness, int color) {
        draw.rect(mat, x, y, w, thickness, 0, color);
        draw.rect(mat, x, y + h - thickness, w, thickness, 0, color);
        draw.rect(mat, x, y, thickness, h, 0, color);
        draw.rect(mat, x + w - thickness, y, thickness, h, 0, color);
    }

    private static float healthPercent(Player player) {
        float heal = player.getHealth() + player.getAbsorptionAmount();
        return Math.min(Math.max(heal / player.getMaxHealth(), 0f), 1f);
    }

    private static int healthColor(float percent) {
        float hue = percent / 3f;
        return 0xFF000000 | (Color.HSBtoRGB(hue, 1f, 1f) & 0x00FFFFFF);
    }

    private static Rectangle projectAABB(EventRender3D event, AABB bb) {
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 camPos = minecraft.gameRenderer.getMainCamera().position();
        Matrix4f mv = event.getModelViewMatrix();
        Matrix4f proj = event.getProjectionMatrix();
        int sw = RenderUtil.getScaledWidth();
        int sh = RenderUtil.getScaledHeight();

        Vec3[] corners = {
                new Vec3(bb.minX, bb.minY, bb.minZ),
                new Vec3(bb.maxX, bb.minY, bb.minZ),
                new Vec3(bb.maxX, bb.minY, bb.maxZ),
                new Vec3(bb.minX, bb.minY, bb.maxZ),
                new Vec3(bb.minX, bb.maxY, bb.minZ),
                new Vec3(bb.maxX, bb.maxY, bb.minZ),
                new Vec3(bb.maxX, bb.maxY, bb.maxZ),
                new Vec3(bb.minX, bb.maxY, bb.maxZ)
        };

        Vector4f[] clip = new Vector4f[8];
        for (int i = 0; i < 8; i++) {
            Vec3 c = corners[i];
            Vector4f v = new Vector4f(
                    (float) (c.x - camPos.x),
                    (float) (c.y - camPos.y),
                    (float) (c.z - camPos.z),
                    1f);
            v.mul(mv);
            v.mul(proj);
            clip[i] = v;
        }

        int[][] edges = {
                {0,1},{1,2},{2,3},{3,0},
                {4,5},{5,6},{6,7},{7,4},
                {0,4},{1,5},{2,6},{3,7}
        };

        float epsilon = 0.05f;
        Rectangle rect = null;
        boolean visible = false;
        for (int[] e : edges) {
            Vector4f a = clip[e[0]];
            Vector4f b = clip[e[1]];
            boolean aIn = a.w() > epsilon;
            boolean bIn = b.w() > epsilon;
            if (!aIn && !bIn) continue;
            Vector4f ca = a;
            Vector4f cb = b;
            if (!aIn) {
                float t = (epsilon - a.w()) / (b.w() - a.w());
                ca = lerp4(a, b, t);
            } else if (!bIn) {
                float t = (epsilon - a.w()) / (b.w() - a.w());
                cb = lerp4(a, b, t);
            }
            visible = true;
            double ax = (ca.x() / ca.w() * 0.5 + 0.5) * sw;
            double ay = (0.5 - ca.y() / ca.w() * 0.5) * sh;
            double bx = (cb.x() / cb.w() * 0.5 + 0.5) * sw;
            double by = (0.5 - cb.y() / cb.w() * 0.5) * sh;
            if (rect == null) {
                rect = new Rectangle(ax, ay, ax, ay);
            }
            if (ax < rect.x) rect.x = ax;
            if (ay < rect.y) rect.y = ay;
            if (ax > rect.z) rect.z = ax;
            if (ay > rect.w) rect.w = ay;
            if (bx < rect.x) rect.x = bx;
            if (by < rect.y) rect.y = by;
            if (bx > rect.z) rect.z = bx;
            if (by > rect.w) rect.w = by;
        }
        if (rect == null) rect = new Rectangle(0, 0, 0, 0);
        rect.visible = visible;
        return rect;
    }

    private static Vector4f lerp4(Vector4f a, Vector4f b, float t) {
        return new Vector4f(
                a.x() + (b.x() - a.x()) * t,
                a.y() + (b.y() - a.y()) * t,
                a.z() + (b.z() - a.z()) * t,
                a.w() + (b.w() - a.w()) * t
        );
    }

    public static class Rectangle {
        public double x, y, z, w;
        public boolean visible;

        public Rectangle(double x, double y, double z, double w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }
    }
}
