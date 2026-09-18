package hack.echo.client.features.impl.render;

import hack.echo.client.Echo;
import hack.echo.client.config.ColorConfig;
import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventRender2DGui;
import hack.echo.client.event.impl.EventRender3D;
import hack.echo.client.api.TupleCompat;
import hack.echo.client.features.Feature;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.IntSetting;
import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.render2.impl.opengl.font.Fonts;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.render2.impl.opengl.utils.RenderUtil;
import hack.echo.client.utils.combat.TargetUtils;
import hack.echo.client.utils.strings.Concat;
import hack.echo.client.features.Category;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class Nametags extends Feature {

    public Nametags() {
        super(new FeatureInfo(
                Concat.of("Nametags"),
                Concat.of("Enhanced nametag rendering"),
                Category.RENDER));
    }

    private final BoolSetting healthBar = new BoolSetting(Concat.of("Health Bar"),
            true);
    private final IntSetting range = new IntSetting(Concat.of("Range"), 64, 0, 256);
    private final IntSetting scale = new IntSetting(Concat.of("Scale"), 100, 50, 200);
    private final BoolSetting dynamicScaling = new BoolSetting(
            Concat.of("Dynamic Scaling"), true);
    private final BoolSetting antiBot = new BoolSetting(Concat.of("AntiBot"), true);

    private final Map<Entity, TupleCompat<Rectangle, Boolean>> entityPositions = new HashMap<>();
    private final Color backgroundColor = ColorConfig.BACKGROUND_DARK;
    private final Color healthBarColor = ColorConfig.PRIMARY_ACCENT;

    @EventSubscribe
    public void onRender3D(EventRender3D event) {
        entityPositions.clear();

        if (isNull())
            return;

        DeltaTracker renderTickCounter = mc.getDeltaTracker();
        for (Player player : mc.level.players()) {
            if (player == null)
                continue;

            Vec3 prevPos = new Vec3(player.xOld, player.yOld, player.zOld);
            Vec3 interpolated = prevPos.add(
                    player.position().subtract(prevPos).scale(renderTickCounter.getGameTimeDeltaPartialTick(false)))
                    .add(0, 0.05f, 0);

            AABB boundingBox = new AABB(
                    interpolated.x,
                    interpolated.y,
                    interpolated.z,
                    interpolated.x,
                    interpolated.y + player.getBbHeight() + (player.isShiftKeyDown() ? -0.2 : 0),
                    interpolated.z);

            Vec3[] corners = {
                    new Vec3(boundingBox.minX, boundingBox.minY, boundingBox.minZ),
                    new Vec3(boundingBox.maxX, boundingBox.minY, boundingBox.minZ),
                    new Vec3(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ),
                    new Vec3(boundingBox.minX, boundingBox.minY, boundingBox.maxZ),
                    new Vec3(boundingBox.minX, boundingBox.maxY + 0.1, boundingBox.minZ),
                    new Vec3(boundingBox.maxX, boundingBox.maxY + 0.1, boundingBox.minZ),
                    new Vec3(boundingBox.maxX, boundingBox.maxY + 0.1, boundingBox.maxZ),
                    new Vec3(boundingBox.minX, boundingBox.maxY + 0.1, boundingBox.maxZ)
            };

            Rectangle rectangle = null;
            boolean visible = false;

            for (Vec3 corner : corners) {
                TupleCompat<Vec3, Boolean> projection = RenderUtil.project(
                        event.getModelViewMatrix(),
                        event.getProjectionMatrix(),
                        corner);

                if (projection.getB()) {
                    visible = true;
                }

                Vec3 projected = projection.getA();

                if (rectangle == null) {
                    rectangle = new Rectangle(
                            projected.x(),
                            projected.y(),
                            projected.x(),
                            projected.y());
                } else {
                    if (rectangle.x > projected.x()) {
                        rectangle.x = projected.x();
                    }
                    if (rectangle.y > projected.y()) {
                        rectangle.y = projected.y();
                    }
                    if (rectangle.z < projected.x()) {
                        rectangle.z = projected.x();
                    }
                    if (rectangle.w < projected.y()) {
                        rectangle.w = projected.y();
                    }
                }
            }

            entityPositions.put(player, new TupleCompat<>(rectangle, visible));
        }
    }

    @EventSubscribe
    public void onRender2D(EventRender2DGui event) {
        if (isNull())
            return;

        boolean hasVisibleEntities = entityPositions.entrySet().stream()
                .anyMatch(entry -> entry.getValue().getB());

        if (entityPositions.isEmpty() || !hasVisibleEntities)
            return;
        var draw = event.getDraw2D();
        for (Map.Entry<Entity, TupleCompat<Rectangle, Boolean>> entry : entityPositions.entrySet()) {
            TupleCompat<Rectangle, Boolean> pair = entry.getValue();
            if (!pair.getB())
                continue;

            Player player = (Player) entry.getKey();
            Freecam freecam = Echo.featureManager.getFeatureByClass(Freecam.class);
            if (player == mc.player && mc.options.getCameraType() == CameraType.FIRST_PERSON && !freecam.isEnabled())
                continue;
            if (!player.canBeSeenByAnyone() || player.isRemoved() || player.isSpectator())
                continue;
            if (antiBot.getValue() && TargetUtils.isBot(player))
                continue;

            float distance = mc.player.distanceTo(player);
            if (distance > range.getValue())
                continue;

            renderNameTag(draw, event, player, pair.getA(), distance);
        }
    }

    private void renderNameTag(Draw2D draw, EventRender2DGui event, Player player, Rectangle rect, float distance) {
        if (Fonts.inter == null)
            return;

        float currentScale = calculateScale(distance);
        float centerX = (float) ((rect.x + rect.z) / 2f);
        float y = (float) (rect.y - player.getBbHeight() - 10f * currentScale);

        float fontSize = 9 * currentScale;
        CharSequence playerName = Concat.of(
                player.getName().getString().chars().mapToObj(c -> String.valueOf((char) c)).toArray(String[]::new));
        float width = Fonts.inter.getWidth(playerName, fontSize);
        float height = 10 * currentScale;
        float padding = 2 * currentScale;

        event.getMatrixStack().pushPose();

        Matrix4f mat = event.getMatrixStack().last().pose();

        draw.rect(
                mat,
                centerX - width / 2 - padding,
                y - padding,
                width + 2 * padding,
                height + padding,
                0,
                backgroundColor.getRGB());

        if (healthBar.getValue()) {
            renderHealthBar(draw, mat, player, centerX, y, width, height, padding, currentScale);
        }

        draw.textWithShadow(
                Fonts.inter,
                event.getMatrixStack().last().pose(),
                playerName,
                centerX - Fonts.inter.getWidth(playerName, fontSize) / 2,
                y - (healthBar.getValue() ? padding : currentScale),
                fontSize,
                ColorConfig.TEXT_WHITE.getRGB());

        event.getMatrixStack().popPose();
    }

    private void renderHealthBar(Draw2D draw, Matrix4f mat, Player player, float centerX, float y,
            float width, float height, float padding, float currentScale) {
        float healthPercent = player.getHealth() / player.getMaxHealth();
        float healthWidth = width * healthPercent;
        float healthBarHeight = 2 * currentScale;
        draw.rect(
                mat,
                centerX - width / 2 - padding,
                y + height - healthBarHeight,
                width + 2 * padding,
                healthBarHeight,
                0,
                backgroundColor.getRGB());

        draw.rect(
                mat,
                centerX - width / 2 - padding,
                y + height - healthBarHeight,
                healthWidth + 2 * padding,
                healthBarHeight,
                0,
                healthBarColor.getRGB());

    }

    private float calculateScale(float distance) {
        if (dynamicScaling.getValue()) {
            float baseScale = scale.getValue() / 100f;
            float distanceFactor = 1.0f - (distance / range.getValue());
            return Math.max(baseScale * distanceFactor, 0.5f * baseScale);
        } else {
            return scale.getValue() / 100f;
        }
    }

    public static class Rectangle {
        public double x;
        public double y;
        public double z;
        public double w;

        public Rectangle(double x, double y, double z, double w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }
    }
}
