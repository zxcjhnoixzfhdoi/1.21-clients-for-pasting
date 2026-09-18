package hack.echo.client.features.impl.render;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventRender3D;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.ColorSetting;
import hack.echo.client.features.settings.impl.FloatSetting;
import hack.echo.client.handlers.RotationHandler;
import hack.echo.client.utils.rotation.RotationUtils;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;


// Originally I thought of using Gizmo package
// However I wanted to do a more "low-level" implementation
public class AimVisualizerModule extends Feature {

    private final FloatSetting cubeSize = new FloatSetting(Concat.of("Size"), 0.15f, 0.05f, 0.5f, 0.01f);
    private final ColorSetting serverColor = new ColorSetting(Concat.of("Server Color"), 255, 0, 0, 200);
    private final ColorSetting clientColor = new ColorSetting(Concat.of("Client Color"), 0, 0, 255, 200);

    public AimVisualizerModule() {
        super(new FeatureInfo(
                Concat.of("Aim Visualizer"),
                Concat.of("Shows where server-side rotations are aiming"),
                Category.RENDER,
                false));
    }

    private Vec3 calculateTarget(Vec3 eyePos, float pitch, float yaw, double range, double hitboxMargin) {
        Vec3 lookDir = RotationHandler.getRotationVector(pitch, yaw);
        Vec3 endPos = eyePos.add(lookDir.scale(range));

        // Raycast for block
        BlockHitResult blockHit = mc.level.clip(new ClipContext(
                eyePos, endPos,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                mc.player));

        // Raycast for entity
        AABB entitySearchBox = mc.player.getBoundingBox().expandTowards(lookDir.scale(range)).inflate(hitboxMargin);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                mc.player, eyePos, endPos, entitySearchBox,
                entity -> !entity.isSpectator() && entity.isPickable(),
                (range * range));

        return determineTargetPosition(eyePos, blockHit, entityHit, endPos);
    }

    @EventSubscribe
    private void onRender3d(EventRender3D event) {
        if (isNull()) return;
        if (mc.player == null || mc.level == null) return;

        float tickDelta = event.getTickDelta();
        Vec3 eyePos = mc.player.getEyePosition(tickDelta);

        ItemStack mainHandItem = mc.player.getMainHandItem();
        AttackRange attackRangeComponent = mainHandItem.get(DataComponents.ATTACK_RANGE);
        double attackRange = getAttackRange(attackRangeComponent);
        double hitboxMargin = getHitboxMargin(attackRangeComponent);

        boolean showSilentTarget = RotationUtils.hasSilentRotation();
        Vec3 serverTargetPos = null;
        if (showSilentTarget) {
            float[] serverRotations = getServerRotations();
            float serverYaw = serverRotations[0];
            float serverPitch = serverRotations[1];
            serverTargetPos = calculateTarget(eyePos, serverPitch, serverYaw, attackRange, hitboxMargin);
        }

        float clientYaw = mc.player.getYRot(tickDelta);
        float clientPitch = mc.player.getXRot(tickDelta);
        Vec3 clientTargetPos = calculateTarget(eyePos, clientPitch, clientYaw, attackRange, hitboxMargin);

        // Render serverside cube (red)
        int serverColorInt = ARGB.colorFromFloat(
                serverColor.getAlphaNormalized(),
                serverColor.getRedNormalized(),
                serverColor.getGreenNormalized(),
                serverColor.getBlueNormalized());

        // Render client-side cube (blue)
        int clientColorInt = ARGB.colorFromFloat(
                clientColor.getAlphaNormalized(),
                clientColor.getRedNormalized(),
                clientColor.getGreenNormalized(),
                clientColor.getBlueNormalized());

        float size = cubeSize.getValue();

        // z fighting shenanigans
        double clientSize = size - 0.001;
        var draw = event.getDraw3D().getMinecraftTarget();
        if (showSilentTarget) {
            draw.box(
                    serverTargetPos.x - size * 0.5,
                    serverTargetPos.y - size * 0.5,
                    serverTargetPos.z - size * 0.5,
                    size, size, size,
                    serverColorInt
            );
        }
        draw.box(
                clientTargetPos.x - clientSize * 0.5,
                clientTargetPos.y - clientSize * 0.5,
                clientTargetPos.z - clientSize * 0.5,
                clientSize,
                clientSize,
                clientSize,
                clientColorInt
        );
    }

    private double getAttackRange(AttackRange attackRangeComponent) {
        if (attackRangeComponent == null) {
            return 3.0;
        }

        return attackRangeComponent.effectiveMaxRange(mc.player);
    }

    private double getHitboxMargin(AttackRange attackRangeComponent) {
        if (attackRangeComponent == null) {
            return 0.0;
        }

        return attackRangeComponent.hitboxMargin();
    }

    private float[] getServerRotations() {
        if (RotationUtils.hasSilentRotation()) {
            return RotationUtils.getLastRotations();
        }

        return new float[]{RotationHandler.getServerYaw(), RotationHandler.getServerPitch()};
    }

    private Vec3 determineTargetPosition(Vec3 eyePos, BlockHitResult blockHit, EntityHitResult entityHit, Vec3 fallbackPos) {
        double blockDist = Double.MAX_VALUE;
        double entityDist = Double.MAX_VALUE;

        if (blockHit != null && blockHit.getType() == HitResult.Type.BLOCK) {
            blockDist = eyePos.distanceToSqr(blockHit.getLocation());
        }

        if (entityHit != null) {
            entityDist = eyePos.distanceToSqr(entityHit.getLocation());
        }

        // Return closest hit, or fallback if no hits
        if (entityHit != null && entityDist < blockDist) {
            // Entity hit, use the actual hit location on the entity surface
            return entityHit.getLocation();
        } else if (blockHit != null && blockHit.getType() == HitResult.Type.BLOCK) {
            return blockHit.getLocation();
        }

        return fallbackPos;
    }
}
