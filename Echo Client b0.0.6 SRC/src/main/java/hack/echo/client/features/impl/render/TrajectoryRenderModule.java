package hack.echo.client.features.impl.render;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventRender3D;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.ColorSetting;
import hack.echo.client.features.settings.impl.MultiModeSetting;
import hack.echo.client.mixin.accessors.AbstractArrowAccessor;
import hack.echo.client.render3.api.FramebufferTarget;
import hack.echo.client.utils.DrawUtils;
import hack.echo.client.utils.inventory.InventoryUtils;
import hack.echo.client.utils.strings.Concat;
import hack.echo.client.utils.trajectory.ArrowTrajectoryUtils;
import hack.echo.client.utils.trajectory.BowTrajectoryUtils;
import hack.echo.client.utils.trajectory.CrossbowTrajectoryUtils;
import hack.echo.client.utils.trajectory.PearlTrajectoryUtils;
import hack.echo.client.utils.trajectory.PotionTrajectoryUtils;
import hack.echo.client.utils.trajectory.WindChargeTrajectoryUtils;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class TrajectoryRenderModule extends Feature {

    private static final int LANDING_CIRCLE_SEGMENTS = 24;
    private static final double LANDING_CIRCLE_RADIUS = 0.4;
    private static final double LANDING_CIRCLE_SURFACE_OFFSET = 0.02;

    private final MultiModeSetting trajectories = new MultiModeSetting(
        Concat.of("Trajectories"),
        Concat.of("Pearl"),
        Concat.of("Potion"),
        Concat.of("Wind Charge"),
        Concat.of("Bow"),
        Concat.of("Crossbow"),
        Concat.of("Firework"),
        Concat.of("Arrow")
    );
    private final ColorSetting pearlColor = new ColorSetting(Concat.of("Pearl Color"), 102, 179, 255, 200, s -> allowsPearl(), true);
    private final ColorSetting potionColor = new ColorSetting(Concat.of("Potion Color"), 124, 192, 255, 200, s -> allowsPotion(), true);
    private final ColorSetting windChargeColor = new ColorSetting(Concat.of("Wind Charge Color"), 255, 209, 102, 200, s -> allowsWindCharge(), true);
    private final ColorSetting bowColor = new ColorSetting(Concat.of("Bow Color"), 148, 114, 255, 200, s -> allowsBow(), true);
    private final ColorSetting crossbowColor = new ColorSetting(Concat.of("Crossbow Color"), 114, 216, 180, 200, s -> allowsCrossbow(), true);
    private final ColorSetting fireworkColor = new ColorSetting(Concat.of("Firework Color"), 255, 196, 130, 200, s -> allowsFirework(), true);
    private final ColorSetting arrowColor = new ColorSetting(Concat.of("Arrow Color"), 255, 128, 128, 200, s -> allowsArrow(), true);

    public TrajectoryRenderModule() {
        super(new FeatureInfo(
            Concat.of("Trajectories"),
            Concat.of("Renders projectile trajectories."),
            Category.RENDER
        ));

        trajectories.setOption(Concat.of("Pearl"), true);
        trajectories.setOption(Concat.of("Potion"), true);
        trajectories.setOption(Concat.of("Wind Charge"), true);
        trajectories.setOption(Concat.of("Bow"), true);
        trajectories.setOption(Concat.of("Crossbow"), true);
        trajectories.setOption(Concat.of("Firework"), true);
        trajectories.setOption(Concat.of("Arrow"), true);
    }

    @SuppressWarnings("unused")
    @EventSubscribe
    private void onRender3D(EventRender3D event) {
        if (isNull()) return;

        var draw = event.getDraw3D().getMinecraftTarget();
        float pt = event.getTickDelta();

        if (allowsPearl() && InventoryUtils.isHoldingAnyHand(Items.ENDER_PEARL)) {
            drawTrajectory(draw, PearlTrajectoryUtils.sample(mc.player, pt), toColor(pearlColor));
        }

        if (allowsPearl()) {
            int color = toColor(pearlColor);
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity.getType() != EntityType.ENDER_PEARL) continue;
                drawTrajectory(draw, PearlTrajectoryUtils.sample(entity), color);
            }
        }

        if (allowsPotion() && isHoldingThrowablePotion()) {
            drawTrajectory(draw, PotionTrajectoryUtils.sample(mc.player, pt), toColor(potionColor));
        }

        if (allowsPotion()) {
            int color = toColor(potionColor);
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (!(entity instanceof AbstractThrownPotion)) continue;
                drawTrajectory(draw, PotionTrajectoryUtils.sample(entity), color);
            }
        }

        if (allowsWindCharge() && InventoryUtils.isHoldingAnyHand(Items.WIND_CHARGE)) {
            drawTrajectory(draw, WindChargeTrajectoryUtils.sample(mc.player, pt), toColor(windChargeColor));
        }

        if (allowsWindCharge()) {
            int color = toColor(windChargeColor);
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity.getType() != EntityType.WIND_CHARGE) continue;
                drawTrajectory(draw, WindChargeTrajectoryUtils.sample(entity), color);
            }
        }

        if (allowsBow() && InventoryUtils.isHoldingAnyHand(Items.BOW)) {
            drawTrajectory(draw, BowTrajectoryUtils.sample(mc.player, getBowChargeTicks(), pt), toColor(bowColor));
        }

        if (allowsCrossbow() && InventoryUtils.isHoldingAnyHand(Items.CROSSBOW)) {
            ItemStack crossbow = getHeldCrossbow();
            if (!crossbow.isEmpty()) {
                int color = toColor(crossbowColor);
                for (List<Vec3> points : CrossbowTrajectoryUtils.sample(mc.player, crossbow, pt)) {
                    drawTrajectory(draw, points, color);
                }
            }
        }

        if (allowsFirework()) {
            int color = toColor(fireworkColor);
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (!(entity instanceof FireworkRocketEntity firework)) continue;
                drawTrajectory(draw, CrossbowTrajectoryUtils.sampleFirework(firework), color);
            }
        }

        if (allowsArrow()) {
            int color = toColor(arrowColor);
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (!(entity instanceof AbstractArrow arrow)) continue;
                if (((AbstractArrowAccessor) arrow).echo$isInGround()) continue;
                drawTrajectory(draw, ArrowTrajectoryUtils.sample(arrow), color);
            }
        }
    }

    private int getBowChargeTicks() {
        if (!mc.player.isUsingItem()) return 20;

        ItemStack using = mc.player.getUseItem();
        if (!(using.getItem() instanceof BowItem)) return 20;

        int charge = using.getUseDuration(mc.player) - mc.player.getUseItemRemainingTicks();
        return Mth.clamp(charge, 0, 20);
    }

    private boolean allowsPearl() {
        return trajectories.isEnabled(Concat.of("Pearl"));
    }

    private boolean allowsWindCharge() {
        return trajectories.isEnabled(Concat.of("Wind Charge"));
    }

    private boolean allowsPotion() {
        return trajectories.isEnabled(Concat.of("Potion"));
    }

    private boolean allowsBow() {
        return trajectories.isEnabled(Concat.of("Bow"));
    }

    private boolean allowsCrossbow() {
        return trajectories.isEnabled(Concat.of("Crossbow"));
    }

    private boolean allowsFirework() {
        return trajectories.isEnabled(Concat.of("Firework"));
    }

    private boolean allowsArrow() {
        return trajectories.isEnabled(Concat.of("Arrow"));
    }

    private ItemStack getHeldCrossbow() {
        ItemStack main = mc.player.getMainHandItem();
        if (main.getItem() instanceof CrossbowItem) return main;

        ItemStack offhand = mc.player.getOffhandItem();
        return offhand.getItem() instanceof CrossbowItem ? offhand : ItemStack.EMPTY;
    }

    private boolean isHoldingThrowablePotion() {
        return InventoryUtils.isHoldingAnyHand(Items.SPLASH_POTION) || InventoryUtils.isHoldingAnyHand(Items.LINGERING_POTION);
    }

    private void drawTrajectory(FramebufferTarget draw, List<Vec3> points, int color) {
        if (points.size() < 2) return;

        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 from = points.get(i);
            Vec3 to = points.get(i + 1);
            draw.line(from.x, from.y, from.z, to.x, to.y, to.z, color);
        }

        Vec3 previous = points.get(points.size() - 2);
        Vec3 landing = points.get(points.size() - 1);
        Vec3 normal = new Vec3(0.0, 1.0, 0.0);

        if (mc.level != null) {
            BlockHitResult hit = mc.level.clip(new ClipContext(
                previous,
                landing,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mc.player
            ));

            if (hit.getType() != HitResult.Type.MISS) {
                landing = hit.getLocation();
                Direction direction = hit.getDirection();
                normal = new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ());

                Vec3 approach = previous.subtract(landing);
                if (approach.lengthSqr() > 1.0E-6 && normal.dot(approach) < 0.0) {
                    normal = normal.scale(-1.0);
                }
            }
        }

        Vec3 center = landing.add(normal.normalize().scale(LANDING_CIRCLE_SURFACE_OFFSET));
        DrawUtils.drawOutlinedCircle(
            draw,
            center,
            normal,
            LANDING_CIRCLE_RADIUS,
            LANDING_CIRCLE_SEGMENTS,
            color
        );
    }

    private int toColor(ColorSetting color) {
        return ARGB.colorFromFloat(
            color.getAlphaNormalized(),
            color.getRedNormalized(),
            color.getGreenNormalized(),
            color.getBlueNormalized()
        );
    }
}
